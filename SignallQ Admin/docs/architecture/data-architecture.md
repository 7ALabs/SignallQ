# Arquitetura de Dados do Painel Admin (SIG-295)

Data: 2026-07-04
Status: decidido e implementado (regra de arquitetura já aprovada pelo dono do produto)
Relacionado: SIG-295, SIG-294, GH#417, GH#415-427

## Decisão de arquitetura (não-negociável)

> **O Painel Admin (`SignallQ Admin/`) NUNCA acessa D1, Firebase, BigQuery ou Play
> Console diretamente. Toda leitura e escrita passa pela Admin API exposta pelo
> Worker `signallq-admin-worker` (`integrations/cloudflare/signallq-admin-worker/`).**

Motivo: o Worker é o único componente com credenciais (D1 binding, Firebase
service account, futura chave de Play Console). Expor qualquer uma dessas
credenciais no bundle do frontend (React/Vite, roda no navegador) seria vazamento
de segredo. O Worker também é o único lugar onde faz sentido normalizar dados de
fontes heterogêneas (D1 relacional, BigQuery colunar, GA4 REST) num contrato JSON
único e estável para o frontend consumir.

Isso não é uma mudança — é a arquitetura que já existe no código (`src/services/*`
do painel chamam só `apiClient` → Worker). Este documento formaliza a decisão e
documenta o estado real, porque nenhuma doc consolidava isso antes.

## Visão geral do fluxo de dados

```
                         ┌─────────────────────────┐
                         │   Android app (Kotlin)  │
                         │  io.signallq.app        │
                         └───────────┬─────────────┘
                                     │ POST /ingest/*  (Bearer INGEST_KEY)
                                     │ fire-and-forget + WorkManager retry (parcial — ver Gaps)
                                     ▼
┌───────────────────────────────────────────────────────────────────────┐
│                     signallq-admin-worker (Cloudflare)                │
│  - /ingest/*   escreve em D1 (autenticado por INGEST_KEY)              │
│  - /admin/*    lê/escreve em D1 (autenticado por sessão httpOnly)      │
│  - /admin/integrations/firebase/*  consulta BigQuery + GA4 Data API    │
│  - /flags, /feature-flags  público, sem auth (consumido pelo Android)  │
└───────┬───────────────────────────┬───────────────────┬───────────────┘
        │                           │                   │
        ▼                           ▼                   ▼
┌───────────────┐       ┌─────────────────────┐  ┌────────────────────┐
│  D1            │       │ BigQuery            │  │ GA4 Data API        │
│  signallq-     │       │ (export Firebase:    │  │ (Analytics Data API) │
│  admin-db      │       │  Crashlytics +       │  │  runReport           │
│                │       │  Analytics events_*) │  │                      │
└───────────────┘       └─────────────────────┘  └────────────────────┘
        ▲
        │ GET /admin/*  (cookie de sessão, credentials: include)
        │
┌───────────────────────┐
│  SignallQ Admin (SPA)  │
│  React + Vite + TS     │
└───────────────────────┘

Play Console: SEM integração hoje. Fase M3 (pré-lançamento). Quando existir,
entra como mais uma fonte por trás do Worker (nunca direto no frontend) — ver
seção "Play Console" abaixo.
```

## Fontes de dados — o que alimenta cada uma

| Fonte | O que fornece | Como o Worker acessa | Status |
|---|---|---|---|
| **D1** (`signallq-admin-db`) | Sessões de diagnóstico, uso de IA, eventos de produto, erros do worker, alertas, feature flags, configurações, auth | Binding nativo `env.DB` | Ativo, fonte primária |
| **Android app** | Origem de quase todo o dado real em D1 — via `/ingest/*` | HTTP POST autenticado por `INGEST_KEY` | Parcialmente ativo — ver Gaps |
| **Firebase Analytics (GA4)** | Métricas agregadas de sessão/usuário via API REST | `Analytics Data API` (`runReport`), OAuth via service account | Ativo (`/admin/integrations/firebase/analytics`) |
| **Firebase Crashlytics** | Crashes por versão, issues de crash | BigQuery export (`firebase_crashlytics.android_crashes_*`) | Ativo, mas retorna `no_data_yet` até o export ter volume |
| **BigQuery** | Camada de consulta para Crashlytics + GA4 export bruto | REST API `bigquery.googleapis.com`, mesma service account | Ativo |
| **Play Console** | Instalações, avaliações, ANR/crash rate nativos da loja | — | **Não integrado.** Bloqueado até M3 (pré-lançamento, ver `CLAUDE.md` — milestones). Quando entrar, é mais uma chamada server-side do Worker, nunca do frontend |

## Regra de ambiente (`environment`)

Todas as tabelas relevantes (`diagnostic_sessions`, `ai_usage`, `analytics_events`)
carregam a coluna `environment` (`production` | `staging`). O valor é decidido no
**Android**, não no Worker: `production` somente quando o app foi instalado via
Play Store; qualquer outro canal (Firebase App Distribution, sideload) manda
`staging`. O filtro `?environment=` nas rotas `/admin/metrics/*` e
`/admin/analytics/*` existe para permitir que o painel separe ruído de
homologação dos dados reais de usuário. Ver `AdminSyncWorker.kt` (Android) para a
lógica de `getDistributionChannel`.

## Catálogo de tabelas D1

| Tabela | Alimentada por | Consumida por |
|---|---|---|
| `diagnostic_sessions` | `POST /ingest/diagnostic` | Overview, Diagnósticos, Redes & RF, Operadoras |
| `ai_usage` | `POST /ingest/ai-usage` | Overview, IA & Custo |
| `analytics_events` | `POST /ingest/analytics` | Produto & Uso (parcial — ver Gaps) |
| `system_errors` | Erros internos do próprio Worker (`logError`, fire-and-forget) | Erros |
| `alerts` | Gerada pelo Worker (`generateAndPersistAlerts`, thresholds em `admin_settings`) | Overview, Saúde do Sistema |
| `admin_settings` | `POST /admin/settings` (painel) | Configurações, thresholds de alerta |
| `feature_flags` / `feature_flag_audit` | `PUT /admin/feature-flags/:key` (painel) | Feature Flags (painel) + `GET /flags` (Android) |
| `admin_users` / `admin_sessions` / `auth_rate_limit` | Auth do painel (SIG-136) | Login do painel |

## Contrato de ingest (GH#417) — schema dos eventos enviados pelo Android

> O PWA (`pwa/`) foi descontinuado em 2026-07-04. Os trechos abaixo sobre o PWA
> descrevem uma fonte de dados histórica (`platform: 'web'`) que já não envia
> nada — mantidos porque a coluna `platform` e os dados antigos continuam no D1.

Autenticação: `Authorization: Bearer <INGEST_KEY>` (chave separada do
`ADMIN_SECRET` — escopo limitado a `POST /ingest/*`, vai embarcada no APK via
`BuildConfig`/`local.properties`; o PWA injetava a sua só no lado do servidor via
`ADMIN_INGEST_KEY` da Cloudflare Pages Function `pwa/functions/api/admin/ingest.ts`,
que não existe mais).

**GH#442 — campo `platform` (origem do dado):** todas as três tabelas de ingest
(`diagnostic_sessions`, `ai_usage`, `analytics_events`) têm a coluna `platform`
(`android` | `web`, migration `011_gh442.sql`). O Android ainda não envia esse
campo — o Worker aplica default `'android'` para preservar a semântica de todo
o dado histórico. O extinto PWA enviava `platform: 'web'` explicitamente em todo
payload de `/ingest/diagnostic` (ver histórico em `pwa/src/features/diagnosis/adminIngestPayload.ts`,
arquivo removido junto com o produto). Os endpoints `/admin/metrics/overview`,
`/admin/metrics/diagnostics` e `/admin/metrics/diagnostics/summary` aceitam
`?platform=android|web` (mesmo padrão de `?environment=`) para segmentar por
origem no painel — `web` hoje só retorna dado histórico.

### `POST /ingest/diagnostic` → `diagnostic_sessions`

Idempotente: `INSERT OR REPLACE` por `id` (UUID gerado no app no início da
sessão). Reenviar o mesmo `id` sobrescreve, não duplica — seguro para retry.

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `id` | string (UUID) | sim | Chave de idempotência |
| `created_at` | int (unix seg) | não (default: `now()` do Worker) | Quando o diagnóstico ocorreu no device |
| `network_type` | string | não | `wifi` \| `4g` \| `5g` \| `ethernet` \| `unknown` |
| `status` | string | não | `completed` \| `failed` \| `partial` (mapeado para `bom`/`regular`/`ruim`/`critico`/`inconclusivo` no schema de leitura) |
| `score` | int 0-100 | não | Score calculado pelo engine local |
| `download_mbps`, `upload_mbps`, `latency_ms`, `jitter_ms`, `packet_loss` | number | não | Métricas de rede cruas |
| `issues` | string[] | não | Labels snake_case (`sinal_fraco`, `alta_latencia`, ...) |
| `operator` | string | não | Operadora móvel/ISP identificada |
| `device_model`, `os_version`, `app_version` | string | não | Contexto de dispositivo |
| `ai_summary_report` | string | não | Laudo IA gerado ao final do diagnóstico |
| `environment` | string | não (default `production`) | `production` \| `staging` — ver regra acima |
| `dist_channel` | string | não | `play_store` \| `sideload` \| `unknown` |
| `build_type` | string | não (default `release`) | `release` \| `debug` |
| `version_code` | int | não | versionCode do app |
| `device_id` | string | não | UUID anônimo persistente do dispositivo (sem PII) |
| `rssi`, `banda_wifi`/`bandaWifi`, `padrao_wifi`/`padraoWifi` | number/string | não | Sinal Wi-Fi (Gap 3 do SIG-164) |
| `platform` | string | não (default `android`) | `android` \| `web` — origem do dado (GH#442) |

**Campos que o extinto PWA nunca enviava (limitação real de navegador, histórico):**
`operator`, `device_model`, `os_version`, `rssi`/`banda_wifi`/`padrao_wifi` — o
navegador não expõe operadora móvel, modelo de hardware nem rádio Wi-Fi. O PWA
também sempre enviava `network_type: 'unknown'`: a Network Information API expõe
apenas uma estimativa de velocidade (`effectiveType`), não o meio físico
(wifi vs. celular vs. ethernet).

### `POST /ingest/ai-usage` → `ai_usage`

Idempotente: `INSERT OR REPLACE` por `id`.

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `id` | string (UUID) | sim | Chave de idempotência |
| `model` | string | sim | Ex.: `@cf/qwen/qwen3-30b-a3b-fp8`, `gemini-2.5-flash` |
| `session_id` | string | não | Correlaciona com `diagnostic_sessions.id` |
| `created_at` | int (unix seg) | não | — |
| `prompt_tokens`, `completion_tokens`, `total_tokens` | int | não | Default 0; `total_tokens` default `prompt + completion` |
| `cost_usd` | number \| null | não | Se omitido, Worker calcula fallback (`costForModel`, hoje 100% free-tier → 0, ou tarifa aproximada Qwen3 quando `total_tokens > 0` e custo ausente) |
| `environment`, `dist_channel`, `build_type`, `version_code`, `device_id` | — | não | Mesmo contrato de contexto do diagnóstico |
| `status` | string | não (GH#421) | `success` \| `error`, default `success`. Requer migration `009_gh421.sql` |
| `platform` | string | não (default `android`) | `android` \| `web` — mesmo critério do diagnóstico (GH#442). Hoje o PWA não envia `ai-usage` (o diagnóstico via IA do PWA chama o AI Worker diretamente, sem passar por este endpoint — ver Gaps) |
| `error_message` / `error` | string | não (GH#421) | Mensagem de erro quando `status = 'error'` |

### `POST /ingest/analytics` → `analytics_events` (batch, até 500 eventos)

Body: `{ "events": [ {...}, {...} ] }`. Eventos com `name` fora da whitelist são
descartados silenciosamente (sem erro — evita que um evento novo não reconhecido
derrube o batch inteiro).

Whitelist (`VALID_ANALYTICS_EVENTS`): `feature_used`, `screen_view`,
`session_start`, `session_end`, `feature_crash`, `battery_snapshot`.

**Alterado nesta correção (GH#417):** o worker aceita `id` opcional por evento
(idempotência de retry — ver Gaps) e persiste `device_id`, `version_code`,
`dist_channel`, `build_type` (mesmo contexto das outras duas tabelas) e
`duration_ms` (só relevante em `session_end`). Migration: `008_gh417.sql`.

| Campo (por evento) | Tipo | Obrigatório | Aplica-se a |
|---|---|---|---|
| `id` | string (UUID) | não* | todos — *ver nota de idempotência abaixo |
| `name` | string | sim | todos — deve estar na whitelist |
| `session_id` | string | não | todos |
| `timestamp` | int (unix seg) | não (default `now()`) | todos |
| `app_version` | string | não | todos |
| `feature_id` | string | não | `feature_used`, `feature_crash` |
| `screen_name` | string | não | `screen_view` |
| `error_type` | string | não | `feature_crash` |
| `battery_level` (0-100), `battery_charging` (bool) | number/bool | não | `battery_snapshot` |
| `duration_ms` | int | não | `session_end` — tempo de sessão em ms |
| `environment` | string | não (default `production`) | todos |
| `device_id`, `version_code`, `dist_channel`, `build_type` | — | não | todos (novo neste PR) |
| `platform` | string | não (default `android`) | todos — `android` \| `web` (GH#442). Nenhum cliente envia analytics ainda (ver Gaps) |

## Endpoints administrativos (leitura, `/admin/*`)

Autenticação: cookie de sessão httpOnly (`SIG-136`), exceto `/admin/auth/login`.
Todos aceitam `?period=1d|7d|30d|90d` e `?environment=production|staging|all`
quando aplicável.

| Rota | Página do painel | Fonte |
|---|---|---|
| `GET /admin/metrics/overview` | Visão Geral | D1 (`diagnostic_sessions`, `ai_usage`) |
| `GET /admin/metrics/diagnostics`, `/diagnostics/summary` | Diagnósticos | D1 |
| `GET /admin/metrics/ai-usage`, `/ai-costs`, `/ai-providers`, `/ai-usage/timeline`, `/ai-usage/records` | IA & Custo | D1 (`ai_usage`, `/records` também faz LEFT JOIN em `diagnostic_sessions`) |
| `GET /admin/metrics/network` | Redes & RF | D1 (agregado por `network_type`) |
| `GET /admin/metrics/operators` | Operadoras | D1 (agregado por `operator`) |
| `GET /admin/metrics/top-issues`, `/intelligence` | Overview / Diagnósticos | D1 (parse de `issues` JSON) |
| `GET /admin/metrics/errors` | Erros | D1 (`system_errors` — erros do Worker, não do app) |
| `GET/POST /admin/alerts` | Overview / Saúde do Sistema | D1 (`alerts`, gerado por thresholds) |
| `GET /admin/analytics/product`, `/analytics/battery` | Produto & Uso | D1 (`analytics_events`) |
| `GET /admin/integrations/firebase/*` | Versões Android, Saúde do Sistema | GA4 Data API + BigQuery |
| `GET/POST /admin/settings` | Configurações | D1 (`admin_settings`) |
| `GET/PUT /admin/feature-flags*` | Feature Flags | D1 (`feature_flags`) |

## Gaps conhecidos (para quem for trabalhar em #416, #418-427)

1. **Retry/backoff/fila local no Android — não implementado.** `AdminIngestRepository`
   (Android) é fire-and-forget: falha de rede é logada e descartada, nunca
   re-enfileirada. `AdminSyncWorker` (WorkManager) faz sync retroativo em batch
   com checkpoint, mas o checkpoint avança mesmo se `sendDiagnostic`/`sendAiUsage`
   falhar silenciosamente para um item individual do batch — ou seja, um item que
   falhou no envio é tratado como enviado. Isso é trabalho de **Camilo** (Android),
   não coberto por este PR (fora do escopo de `signallq-admin-worker`).
2. **Nenhum evento de `analytics_events` é enviado pelo Android hoje.** O endpoint
   `POST /ingest/analytics` existe e está completo no Worker, mas não há chamador
   no app — `feature_used`, `screen_view`, `session_start` etc. nunca são
   emitidos. A aba "Produto & Uso" (#418) não tem dado real até isso ser
   implementado no Android. Contrato já documentado acima para quem for
   implementar.
3. **Idempotência de `/ingest/analytics` depende do cliente enviar `id`.** Antes
   deste PR, o Worker gerava um UUID aleatório por evento — um retry de rede
   duplicava a linha. Agora aceita `id` do cliente, mas nenhum cliente existe
   ainda (ver gap 2) — quando o Android implementar o envio, deve gerar `id`
   determinístico por evento (não por retry).
4. **Play Console:** sem integração. Não é bloqueio para M0-M2. Endpoint
   futuro entraria como `/admin/integrations/play-console/*`, mesmo padrão dos
   demais (Worker busca com credencial própria, nunca o frontend).
5. **`system_errors` não diferencia origem** (app vs. Worker vs. IA vs.
   integração) além do campo `source` livre — relevante para #422.
6. **Android ainda não envia `status`/`error_message` em `POST /ingest/ai-usage`
   (GH#421).** O contrato e as colunas D1 já existem (migration `009_gh421.sql`),
   e o Worker aceita os campos como opcionais com default `success` — mas hoje o
   app só grava `ai_usage` ao final de uma chamada concluída, então toda execução
   aparece como `success` até o Android também reportar falhas de inferência
   (timeout, erro de rede, resposta vazia). Trabalho de **Camilo** (Android),
   fora do escopo deste PR.
7. **GH#418 — computado no Worker, mas ainda sem dado real (gap 2 acima):**
   `GET /admin/analytics/product` agora calcula retenção D1/D7/D30 por cohort
   de `device_id` e tempo médio de sessão real (`avg_session_duration_ms`, a
   partir de `session_end.duration_ms`). O frontend (`RetentionPanel`) parou de
   exibir números fabricados como fallback — mostra "—" quando não há cohort.
   Segue bloqueado por dado real (gap 2), mas a lógica agora é honesta: zero
   dado real → zero exibido, não estimativa inventada.
8. **GH#418 — associação de uso de IA por feature não implementada.**
   `ai_usage.session_id` referencia `diagnostic_sessions.id`;
   `analytics_events.session_id` referencia uma sessão de app/tela — são
   espaços de ID diferentes hoje. Cruzar os dois por `session_id` produziria
   uma associação estatisticamente incorreta. `getFeatureAiUsage()` no painel
   continua retornando vazio em produção (nunca mock/fabricado). Decisão
   necessária antes de implementar: Android precisa emitir um identificador de
   correlação comum (ex.: gravar o `diagnostic_sessions.id` também como
   `session_id` do `feature_used`/`session_start` quando a feature ativa for
   um diagnóstico), ou o Worker precisa de uma estratégia de correlação por
   janela de tempo (mais fraca, não recomendada sem aprovação).
9. **GH#441 resolvido nesta correção — PWA agora envia `POST /ingest/diagnostic`
   real** ao final de cada teste (`pwa/src/App.tsx` chama
   `sendAdminDiagnostic`/`buildAdminDiagnosticPayload`, fire-and-forget, mesma
   postura do Android — sem retry/fila local, ver gap 1). O PWA **não** envia
   `ai-usage` nem `analytics_events` — o diagnóstico por IA do PWA chama o AI
   Worker direto (`functions/api/ai/diagnostico-conexao.ts`), sem persistir em
   `ai_usage`, e não há emissão de eventos de produto no PWA. Ambos ficam como
   follow-up (issue própria) se o produto quiser custo de IA e retenção também
   para o WebApp.
10. **GH#442 resolvido parcialmente — filtro de `platform` no Console só está
    ligado na página Diagnósticos** (`DiagnosticsPage`/`DiagnosticsFilters`,
    endpoints `/admin/metrics/diagnostics` e `/diagnostics/summary`). O Worker
    já aceita `?platform=` em `/admin/metrics/overview` também, mas a página
    Visão Geral não tem controle de UI para esse filtro ainda (exigiria
    threading de estado global igual ao `environment` em `App.tsx`/`AppLayout`
    — escopo maior, deixado como follow-up). `/admin/analytics/product`
    (Produto & Uso) não recebeu filtro de `platform` nesta correção: a query
    de retenção usa CTEs encadeadas e não há dado real de `analytics_events`
    para nenhuma plataforma ainda (gap 2) — sem valor imediato em filtrar o
    que está vazio, e risco desnecessário de quebrar a query nesta correção.

## Navegação do painel (SIG-294)

A navegação lateral (`src/config/navigation.ts`, `Sidebar.tsx`) foi agrupada em
seções que espelham a proveniência dos dados (ver `NAVIGATION_SECTIONS`):

- **Visão Geral** — consolida D1 (`diagnostic_sessions` + `ai_usage`).
- **Produto & Diagnóstico** — D1, granularidade por sessão/rede/operadora.
- **IA & Confiabilidade** — custo de IA (D1) + crashes (BigQuery/Firebase).
- **Operação** — feature flags, saúde do sistema e configurações (D1 + checagem
  ativa de integrações).

Cada página do painel agora expõe, no cabeçalho (`PageHeader`), uma legenda de
"Fonte de dados" — o objetivo é que nenhuma tela deixe implícito de onde o
número vem (crítico para não repetir o problema descrito em #415/#416: dado
mockado indistinguível de dado real). Isso é incremental — não um redesenho
visual completo.
