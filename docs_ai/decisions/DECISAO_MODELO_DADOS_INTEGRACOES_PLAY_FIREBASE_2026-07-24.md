# Decisão — Modelo de dados das integrações Google Play / Firebase (épicos #1341/#1343)

- **Status:** ativo
- **Última validação:** 2026-07-24
- **Fonte de verdade:** este documento
- **Escopo:** `integrations/cloudflare/signallq-admin-worker` — migration 016 (`integration_metric_snapshots`), `admin_settings`, PR #1346
- **Responsável:** Claudete (decisão), Camilo (execução na PR #1346)

## Contexto

PR #1346 (branch `feature/1342-1344-play-vitals-firebase-management`, ainda aberta) implementou 6
integrações novas (Play Developer Reporting/ANR, Firebase Management, Remote Config, App Check,
App Distribution, FCM Data API) sob pressão de tempo, sem revisão de arquitetura formal. Migration
016 criou `integration_metric_snapshots` (genérica, `provider/service/resource/metric/period_*/
value_numeric/payload JSON`) retroativamente, porque não havia histórico até o Luiz perguntar. O
Luiz pediu revisão real do desenho — tabela genérica vs. tabelas por provider — não validação do
que já existe.

## Levantamento (código real, não hipótese)

- `recordIntegrationSnapshot()` (`src/index.ts:246`) é chamado em **6 pontos** — um por
  integração — e hoje é **write-only**: não existe nenhum `SELECT ... FROM
  integration_metric_snapshots` no worker. Nada lê essa tabela ainda; a "necessidade de série
  temporal" citada no comentário da migration 016 não está, de fato, servindo nenhum endpoint.
- `admin_settings` (chave/valor) já cobria o cache de "último sync" antes da migration 016 e
  continua sendo a fonte lida pelos endpoints `/status` — padrão genérico pré-existente, não
  inventado pela PR #1346.
- Precedente direto no mesmo repo: `system_health_snapshots` (migration 013, GH#788) já é uma
  tabela genérica por `service` (`d1 | firebase | bigquery`) com séries temporais reais — mas com
  colunas tipadas (`status`, `latency_ms`), porque o schema é homogêneo entre os 3 serviços. Não é
  o mesmo caso de `integration_metric_snapshots`, onde o payload de cada `service` é heterogêneo
  (inventário de apps, lista de parâmetros, métrica numérica, etc.) e por isso caiu tudo em JSON.
- Precedente de join real: `play_console_tracks` (migration 012, GH#761) não é uma tabela genérica
  — é uma tabela dedicada com uma coluna explícita (`play_track`) adicionada nas 3 tabelas de
  telemetria que realmente precisavam do join. É o padrão já validado no repo para quando um
  cruzamento é real, não hipotético.
- Plano de UX da Lia (`SignallQ Admin/docs/product/plano-ux-google-play-firebase.md`) já responde
  objetivamente quais das 6 integrações precisam de série temporal/gráfico e quais não precisam:
  - **Precisa** (`LineChart`, série temporal real): Qualidade/Android Vitals (ANR, Play Developer
    Reporting API) — linha 116.
  - **Não precisa**, por decisão de produto já registrada na própria doc de UX: Remote Config e
    A/B Testing — linha 124, citação direta: *"Config Remota e A/B Testing são estado de
    configuração, não série temporal — tabela é suficiente, não forçar gráfico"*.
  - Firebase Management (inventário de apps) e App Check/App Distribution são estado/config, mesma
    categoria — nenhuma linha do plano de UX pede gráfico pra elas.
  - Estabilidade (Crashlytics) já é servida por `/errors` (`ErrorMetricGrid`/`TopCrashesCard`),
    fora do escopo de `integration_metric_snapshots` — linha 122.

## Resposta às 4 perguntas

**1) Tabela genérica ou separar por provider?** Nenhuma das duas como está. O erro não é "genérica
vs. separada por provider" — é ter tratado **6 integrações heterogêneas como se todas fossem série
temporal**, quando só 1 das 6 (ANR/Android Vitals) genuinamente é. Separar por provider (Google
Play / Firebase) não resolveria nada, porque dentro de "Firebase" já há 4 shapes diferentes
(inventário, config, lista de parâmetros, status). O critério certo não é a origem do dado
(provider), é a **natureza** do dado: métrica numérica com tendência vs. snapshot de estado/config.

**2) Critério objetivo:**
- Existe necessidade real (não hipotética) de plotar tendência/série temporal? → Se sim, cabe em
  `integration_metric_snapshots` com `value_numeric` preenchido de verdade. Se não, não precisa de
  tabela de histórico nenhuma — `admin_settings` já é a fonte correta e suficiente.
- Existe join real e implementado com outra fonte (não "poderia cruzar no futuro")? → Se sim,
  replica o padrão de `play_console_tracks`: coluna/tabela dedicada e explícita para aquele join
  específico, não uma tabela genérica torta pra acomodar todo cruzamento hipotético.
- Volume e frequência de escrita: sync é por cron/manual, não por evento de usuário — volume é
  baixo em qualquer desenho, não é o fator decisivo aqui.
- Homogeneidade de schema dentro do grupo: métricas numéricas com período (`metric/period_start/
  period_end/value_numeric`) são homogêneas o bastante pra uma tabela só, sem forçar JSON como
  única forma de consulta.

**3) Recomendação — refatorar agora, migration 016 é recente e sem dado real relevante ainda:**
Não criar tabelas por provider. Restringir `integration_metric_snapshots` ao que ela deveria ter
sido desde o início: histórico de **métricas numéricas de série temporal**. Hoje isso é só Play
Developer Reporting (ANR rate); qualquer métrica numérica real futura (ex.: Crashlytics
crash-free % se um dia sair de `/errors`) entra no mesmo modelo. Para as outras 5 integrações
(Firebase Management, Remote Config, App Check, App Distribution, FCM Data API), remover a
chamada de `recordIntegrationSnapshot()` — elas continuam servidas por `admin_settings` (cache do
último sync), que é o dado que a própria UI pede hoje. Não há necessidade de histórico pra config/
inventário até que apareça um requisito real de produto pedindo tendência desses dados — quando
isso acontecer, decide-se a modelagem daquele caso específico, não se generaliza preventivamente.

**4) DDL (ajuste, não tabela nova):**
```sql
-- integration_metric_snapshots fica como está estruturalmente (migration 016 não muda) —
-- só passa a ser escrita exclusivamente por métricas numéricas reais. Sem migration adicional.
```
Nenhuma migration nova é necessária. A correção é em `src/index.ts`: remover as 5 chamadas de
`recordIntegrationSnapshot()` que não são Play Developer Reporting/ANR (Firebase Management,
Remote Config, App Check, App Distribution, FCM Data API), mantendo intactos os `writeXxxSyncState()`
que já persistem em `admin_settings`. Quando/se `payload` deixar de ser necessário para a métrica
de ANR (hoje preservado "para não perder campo novo que a fonte adicionar"), reavaliar se vale
reduzir a coluna — não é bloqueio agora.

## Quando reabrir essa decisão

- Se um requisito real de produto pedir tendência histórica de Remote Config/App Check/App
  Distribution/FCM (não hipótese) — desenhar a tabela daquele caso específico então.
- Se `integration_metric_snapshots` crescer para múltiplas métricas numéricas com padrões de query
  divergentes o bastante para o `payload` JSON virar gargalo real de leitura — não antes disso.
- Se surgir um join real (não hipotético) entre métrica e outra fonte (ex.: ANR rate ×
  `version_code` de `play_console_tracks`) — replicar o padrão de coluna dedicada, não generalizar
  a tabela de métricas para acomodá-lo.

## Ação

Camilo aplica o trim em `src/index.ts` (remover as 5 chamadas de `recordIntegrationSnapshot` fora
do escopo de ANR) na própria PR #1346, antes do merge — sem migration nova, sem mudança de
contrato de API pública (nenhum endpoint lê `integration_metric_snapshots` hoje).
