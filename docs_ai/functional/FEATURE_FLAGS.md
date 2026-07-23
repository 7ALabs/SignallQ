# Feature Flags remotas — SignallQ Android + Admin Panel

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Fonte de verdade:** este arquivo, para o efeito de produto das flags remotas (rollout gradual,
  kill switch). Mecanismo técnico completo (endpoints, schema D1) referenciado em
  `docs_ai/TECNICO.md` seção 5.2 — não duplicado lá. **Não cobre** as feature flags de compile-time
  (`FeatureFlags.kt`, `BuildConfig.FEATURE_*`) — essas são inteiramente distintas e estão em
  `docs_ai/FUNCIONAL.md` seção 8.3 / `docs_ai/TECNICO.md` seção 5.2.
- **Escopo:** sistema de feature flags remotas — Admin Worker (`signallq-admin-worker`) + consumo
  Android (`FeatureFlagRepository`/`FeatureFlagManager`).
- **Responsável:** Camilo (implementação Android/Worker). Revisão anterior citava Felipe (painel) e
  Gema (review) — **ambos fora do squad desde 2026-07-09/07-10** (ver `.claude/CLAUDE.md`,
  decisões de demissão/substituição), corrigido nesta revisão.

> Segue o template de **Especificação Funcional**
> (`.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 10) — spec pontual, mais focada que
> `FUNCIONAL.md`.
>
> **Reescrita integral em 2026-07-23:** a versão anterior deste documento era majoritariamente
> especulativa — endpoints, nomes de tabela D1 e nomes de flag não batiam com o código real
> (`signallq-admin-worker/src/index.ts`, `signallq-admin-worker/migrations/005_sig13.sql`,
> `FeatureFlagRepository.kt`). Conteúdo abaixo vem de leitura direta desses três arquivos.

---

## 1. Objetivo

Permitir ativar/desativar funcionalidades do app Android remotamente, sem publicar nova versão na
Play Store/Firebase Distribution — para rollout gradual, kill switch em incidente, e (potencial)
A/B testing.

---

## 2. Contexto e problema

Nem toda mudança de comportamento do app pode esperar o ciclo de release (build + upload +
propagação). Quando um componente do backend cai (ex.: worker de IA) ou uma feature precisa ser
desligada rapidamente, o app precisa de um mecanismo de controle que não dependa de nova versão
instalada pelo usuário.

---

## 3. Personas e casos de uso

- **Camilo (engenharia)** — ativa/desativa flag via Admin Worker quando uma feature tem problema em
  produção, ou faz rollout gradual de uma feature nova.
- **App Android (usuário final, indireto)** — consome o estado das flags sem interação direta;
  efeito é a feature aparecer/desaparecer ou mudar de comportamento sem update.

Casos de uso: kill switch de emergência (worker de IA caiu → desabilitar diagnóstico IA); rollout
gradual de feature nova; toggle sem exigir novo build/upload.

---

## 4. Histórias de usuário

- Como responsável técnico, quero desativar uma feature com problema em produção sem esperar um
  novo build, para conter o impacto rapidamente.
- Como responsável técnico, quero ver o histórico de mudanças de uma flag (quem, quando, de/para),
  para auditar decisões de rollout.

---

## 5. Fluxo principal

1. Flag é lida/gravada em D1, tabela `feature_flags` (Admin Worker, `signallq-admin-worker`).
2. App Android faz fetch em dois endpoints públicos ao sincronizar (`FeatureFlagRepository.
   sincronizarFlags()`), mescla os dois resultados e persiste em DataStore
   (`FeatureFlagStore`, módulo `:coreDatastore`).
3. Leitura local (`lerFlags()`) sempre retorna um mapa completo — combina o cache salvo com um mapa
   de defaults (`true`), garantindo que uma flag nunca fique ausente no app mesmo se o fetch falhar.
4. Alteração de flag (ativar/desativar) acontece via Admin Worker (`PUT /admin/feature-flags/:key`,
   sessão autenticada) — não há UI documentada neste momento; qualquer painel visual de toggle
   ainda não foi confirmado no código.

---

## 6. Requisitos funcionais

### RF-01 — Dois sistemas remotos coexistem, sem sobreposição de storage

O código real confirma **dois mecanismos remotos independentes**, com armazenamento e schema
totalmente distintos — não é uma única tabela com dois formatos de resposta:

| | Sistema legado | Sistema SIG-13 (atual) |
|---|---|---|
| Storage | JSON blob em `admin_settings` (chave `feature_flags`) | Tabela dedicada `feature_flags` (D1) |
| Endpoint público (Android) | `GET /feature-flags` | `GET /flags` |
| Endpoint admin (listar) | — (não encontrado endpoint admin para o blob legado) | `GET /admin/feature-flags` |
| Endpoint admin (escrever) | — (não encontrado) | `PUT /admin/feature-flags/:key` (sessão autenticada, confirmado no código) |
| Filtro de retorno público | `scope === 'public'` | nenhum — todas retornadas |
| Flags seed reais | `ai_diagnosis_enabled`, `speedtest_enabled`, `fibra_module_enabled` (`scope: public`), `new_ui_diagnostics` (`scope: internal`, nunca aparece no endpoint público) | `feature_speedtest`, `feature_wifi`, `feature_fibra`, `feature_diagnostico_ia`, `feature_devices`, `feature_dns` |
| Audit log | não encontrado | tabela `feature_flag_audit` (`id`, `flag_key`, `old_enabled`, `new_enabled`, `changed_at`, `changed_by`) |

> Correção sobre a versão anterior deste documento: não existe tabela `d1_feature_flags` nem
> `d1_feature_flags_audit` — os nomes reais são `feature_flags` e `feature_flag_audit`. Não existe
> endpoint `GET /admin/feature-flags?device_id=X` nem `POST /admin/feature-flags/:name/toggle` — o
> endpoint real de escrita é `PUT /admin/feature-flags/:key` com corpo `{"enabled": boolean}`. Não
> há `device_id` em nenhum dos endpoints reais — a leitura não é segmentada por device.

### RF-02 — Consumo Android (`FeatureFlagRepository`)

Fonte: `android/app/src/main/kotlin/io/veloo/app/kotlin/featureflags/FeatureFlagRepository.kt`.

- `sincronizarFlags()`: busca **ambos** os endpoints (`/feature-flags` primeiro, depois `/flags`
  sobrescreve em caso de chave conflitante) e persiste o mapa mesclado via `FeatureFlagStore`.
- Timeout de conexão/leitura: **8000ms** por endpoint (`TIMEOUT_MS`).
- Falha em qualquer endpoint é silenciosa — retorna mapa vazio para aquele endpoint, sem lançar
  exceção; o outro endpoint (se bem-sucedido) ainda é aplicado.
- `lerFlags()`: combina defaults hardcoded no app (`DEFAULTS`, todas `true`) com o que estiver
  salvo — uma flag nova nunca fica ausente/`null` no app, mesmo sem fetch recente.
- Não há TTL/cache-expiry explícito no código lido — a sincronização acontece por chamada explícita
  de `sincronizarFlags()`, não por polling automático agendado; `[a confirmar]` se algum
  `WorkManager`/trigger periódico invoca esse método (não encontrado nesta revisão).

### RF-03 — Escrita e auditoria (sistema SIG-13)

`PUT /admin/feature-flags/:key`, corpo `{"enabled": boolean}`, requer sessão autenticada
(`authenticateSession`, confirmado no roteamento). Grava simultaneamente (D1 `batch`):
1. `UPDATE feature_flags SET enabled=?, updated_at=?, updated_by=? WHERE key=?`
2. `INSERT INTO feature_flag_audit (...)` com `old_enabled` capturado antes do update.

`updated_by`/`changed_by` vêm do e-mail do usuário da sessão (`admin_users`), com fallback
`'admin'` se não encontrado.

---

## 7. Requisitos não funcionais

- **Sem PII:** nenhuma das duas leituras públicas exige nem retorna identificador de device/usuário
  — corrige a versão anterior deste documento, que descrevia (sem confirmação no código) um
  parâmetro `device_id` e rate-limit por device.
- **Tolerância a falha:** app sempre funciona com os defaults locais quando o backend está
  indisponível — nunca bloqueia uma tela por falha de fetch de flag.
- **Auditoria obrigatória:** toda escrita no sistema SIG-13 grava audit log na mesma transação
  (`DB.batch`), nunca como etapa separada que poderia falhar silenciosamente.

---

## 8. Critérios de aceite

- Uma flag ausente no backend (fetch falho ou flag nova ainda não sincronizada) nunca causa erro
  visível ao usuário — cai no default `true` do app.
- Toda alteração de flag via `PUT /admin/feature-flags/:key` fica registrada em
  `feature_flag_audit` com valor antigo e novo.
- O endpoint público (`/flags` ou `/feature-flags`) nunca expõe uma flag `scope: internal` (sistema
  legado) fora do painel admin.

---

## 9. Fora de escopo

- **UI do painel admin para toggle de flag** — não confirmada nesta revisão; o endpoint de escrita
  existe (`PUT /admin/feature-flags/:key`), mas nenhuma tela React foi localizada/lida para
  confirmar a existência de uma interface visual real (a versão anterior deste documento descrevia
  um mockup ASCII especulativo, removido nesta reescrita).
- **A/B testing segmentado por device** — os endpoints reais não recebem `device_id`; qualquer
  segmentação por device não está implementada.
- **Rate limiting** — não encontrado no código dos endpoints públicos nesta revisão; a versão
  anterior descrevia um rate-limit por device que não foi confirmado.
- **Feature flags de compile-time** (`FeatureFlags.kt`) — sistema totalmente separado, coberto em
  `docs_ai/FUNCIONAL.md` seção 8.3 e `docs_ai/TECNICO.md` seção 5.2, não neste documento.

---

## 10. Métricas de sucesso

`[a confirmar]` — não encontrada meta formal (ex.: tempo médio de propagação de uma flag, taxa de
uso do kill switch) em código ou doc ativa.

---

## 11. Referências

- `docs_ai/TECNICO.md` seção 5.2 — mecanismo técnico completo das feature flags (compile-time e
  remotas).
- `android/app/src/main/kotlin/io/veloo/app/kotlin/featureflags/FeatureFlagRepository.kt`,
  `FeatureFlagManager.kt` — implementação Android.
- `integrations/cloudflare/signallq-admin-worker/src/index.ts` (linhas ~3490-3580) — implementação
  dos endpoints.
- `integrations/cloudflare/signallq-admin-worker/migrations/005_sig13.sql` — schema D1 do sistema
  SIG-13 (`feature_flags`, `feature_flag_audit`).
