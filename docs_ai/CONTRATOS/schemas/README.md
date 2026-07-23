# Schemas do monorepo SignallQ — índice de contratos

- **Status:** ativo
- **Última validação:** 2026-07-16
- **Fonte de verdade:** este arquivo referencia os schemas reais nos caminhos de origem — não
  copia conteúdo
- **Escopo:** monorepo `7ALabs/linka-android` — Room (Android), D1 (Cloudflare), analytics
  (Firebase GA4)
- **Responsável:** Claudete, aplicado por Camilo (mudanças de schema) e Rhodolfo (documentação)

## 1. Decisão de referência vs. cópia

Este README **referencia os caminhos de origem como fonte canônica** dos schemas — não copia o
conteúdo integral de nenhum JSON do Room nem de nenhuma migration SQL. Motivo: qualquer cópia
divergiria do real assim que uma nova migration/versão fosse adicionada, e o repositório já tem
histórico de documentação desatualizada citada como dívida conhecida
(`.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 4.6). Cada entrada abaixo aponta
para o arquivo real; para ver o schema completo, abra o caminho indicado.

Exceção avaliada e descartada: cópia integral do schema Room v13 (só 4 tabelas, caberia aqui).
Descartada mesmo assim — o arquivo já é gerado automaticamente pelo Room a cada build
(`exportSchema`), então qualquer cópia manual ficaria obsoleta na primeira migration nova sem que
ninguém percebesse.

## 2. Inventário de schemas reais

| Schema | Versão atual confirmada | Caminho de origem | Consumidor(es) |
|---|---|---|---|
| Room — `SignallQDatabase` (atual) | **v14** | `android/core/database/schemas/io.signallq.app.core.database.SignallQDatabase/14.json` (definição em `SignallQDatabase.kt`; GH#1027 adiciona `bandaWifi`) | App Android (`:app`, `:coreDatabase`, DAOs consumidos por `:featureHistory`, `:featureDiagnostico`, `:featureDevices` etc.) |
| Room — `VelooDatabase` (residual) | v10 (única versão presente) | `android/core/database/schemas/io.signallq.app.core.database.VelooDatabase/10.json` | Nenhum — histórico de schema mantido pelo Room, não referenciado por código ativo |
| Room — `LinkaDatabase` (legado) | v10 (mais alta presente; 1–10) | `android/core/database/schemas/io.linka.app.kotlin.core.database.LinkaDatabase/1.json` … `/10.json` | Nenhum — histórico de schema mantido pelo Room, não referenciado por código ativo |
| D1 — `signallq-admin-db` | migration 014 (`014_gh786.sql`) | `integrations/cloudflare/signallq-admin-worker/migrations/001_sig143.sql` … `014_gh786.sql` | `signallq-admin-worker` + SignallQ Console (`SignallQ Admin/`, via API do worker) |
| D1 — `signallq-diagnostic-db` | migration 006 (`006_gh965_provider_logo_d1.sql`) | `integrations/cloudflare/signallq-diagnostic-worker/migrations/001_gh952_diagnostic_rules.sql` … `006_gh965_provider_logo_d1.sql` | `signallq-diagnostic-worker` + endpoints `/admin/*` do próprio worker |
| Analytics — eventos GA4 (Firebase Analytics) | documento vivo, sem número de versão formal | `docs_ai/technical/analytics-events-schema.md` | App Android (`FirebaseAnalyticsTracker`) → GA4 → `analytics_events` (D1 admin, migration 006) → `ProductAnalyticsPage` no SignallQ Console |

## 3. História dos 3 nomes de banco Room (Linka → Veloo → SignallQ)

Confirmado pela numeração de versão nos diretórios de schema, sem carimbo de data no JSON — não
foi possível confirmar datas exatas de cada transição, só a ordem relativa:

- **`LinkaDatabase`** (nome original, marca Linka) — evoluiu de v1 a v10. É a árvore de schema
  mais longa, coerente com ser a base histórica do produto antes do primeiro rebrand.
- **`VelooDatabase`** (marca intermediária Veloo) — só tem **v10** registrada. Não houve migration
  nova sob esse nome; o rebrand de classe aconteceu sem alterar o schema (mesma v10 herdada de
  `LinkaDatabase`), e o nome já mudou de novo antes de qualquer versão v11 nascer sob `Veloo`.
- **`SignallQDatabase`** (nome atual) — retomou a numeração a partir de **v10** e evoluiu até
  **v14** (confirmado em `SignallQDatabase.kt`, `version = 14`, idêntico ao `14.json` — GH#1027 adiciona `bandaWifi` para capturar banda Wi-Fi durante medição). É a única
  classe `RoomDatabase` presente no código Kotlin atual — `LinkaDatabase`/`VelooDatabase` não têm
  arquivo `.kt` correspondente, existem só como histórico de schema JSON gerado pelo Room em
  builds anteriores ao rebrand.

Consistente com `.claude/CLAUDE.md`: "Marca anterior: Linka -> Veloo -> SignallQ (rebrand em
0.16.0)".

## 4. D1 — `signallq-admin-worker` (14 migrations)

| Arquivo | O que faz |
|---|---|
| `001_sig143.sql` | Adiciona colunas de ambiente/canal/build/device em `diagnostic_sessions` e `ai_usage` + índices |
| `002_sig136.sql` | Cria `admin_users`, `admin_sessions`, `auth_rate_limit` |
| `003_sig129.sql` | Cria `system_errors` + índice por `last_seen` |
| `004_sig133.sql` | Cria `alerts` + índices por `created_at`/`resolved` |
| `005_sig13.sql` | Cria `feature_flags` e `feature_flag_audit` + índices |
| `006_sig134.sql` | Cria `analytics_events` (eventos GA4 espelhados) + índices |
| `007_sig164.sql` | Adiciona colunas de canal/build/device em `ai_usage`, RSSI/banda/padrão Wi-Fi em `diagnostic_sessions` |
| `008_gh417.sql` | Adiciona device/version_code/canal/build/duração em `analytics_events` + índice |
| `009_gh421.sql` | Adiciona `status`/`error_message` em `ai_usage` |
| `010_gh422.sql` | Adiciona campos de resolução em `system_errors` + índices |
| `011_gh442.sql` | Adiciona coluna `platform` em `diagnostic_sessions`, `ai_usage`, `analytics_events` + índice |
| `012_play_track.sql` | Cria `play_console_tracks` + coluna `play_track` nas três tabelas principais + índices |
| `013_gh788.sql` | Cria `system_health_snapshots` + índice composto |
| `014_gh786.sql` | Adiciona coluna `uf` em `diagnostic_sessions` + índice |

## 5. D1 — `signallq-diagnostic-worker` (6 migrations)

| Arquivo | O que faz |
|---|---|
| `001_gh952_diagnostic_rules.sql` | Cria `diagnostic_rulesets` e `diagnostic_rule_audit_log` |
| `002_gh951_provider_directory.sql` | Cria `providers`, `provider_identifiers`, `provider_channels`, `provider_assets`, `provider_detection_stats`, `provider_enrichment_jobs` |
| `003_admin_auth.sql` | Cria `admin_users`, `admin_sessions`, `auth_rate_limit` (auth do `/admin/*` deste worker) |
| `004_gh935_game_catalog.sql` | Cria `game_profiles`, `game_catalog`, `game_platforms`, `game_catalog_audit` |
| `005_gh956_provider_installations.sql` | Cria `provider_detection_installations` + índice |
| `006_gh965_provider_logo_d1.sql` | Adiciona `data_base64`/`content_type` em `provider_assets` — logo servida como BLOB do D1 (R2 descartado por exigir cartão de crédito mesmo no tier grátis) |

## 6. Eventos de analytics (GA4 / Firebase Analytics)

`docs_ai/technical/analytics-events-schema.md` tem um schema formal de evento — nome, parâmetros,
tipo, descrição, ponto de disparo — para 5 eventos (`feature_used`, `screen_view`,
`app_session_start`, `feature_crash`, `battery_snapshot`). Incluído neste índice porque é, na
prática, um contrato de dado real consumido por dois lados (SDK Firebase no app +
`analytics_events` no D1 admin, migration `006_sig134.sql`) — mesmo critério que qualifica os
schemas Room/D1 acima. Não copiado aqui pelo mesmo motivo dos demais: o documento já existe como
fonte única em `docs_ai/technical/`.

## 7. Pendência — link de volta nos schemas Room

Não foi possível adicionar um comentário/link de volta para este README dentro dos JSONs de schema
do Room porque são artefatos gerados automaticamente (`exportSchema`) — editá-los manualmente
seria alterar código de produção/build output, fora do escopo desta tarefa (só documentação).
**Registrar como pendência:** Camilo pode adicionar, na próxima alteração de schema Room, um
comentário no arquivo fonte `SignallQDatabase.kt` (acima da anotação `@Database`) apontando para
este README.
