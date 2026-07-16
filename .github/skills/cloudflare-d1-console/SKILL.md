---
description: Padrões de schema, migrations e queries D1 (Cloudflare) do SignallQ Console — baseado nas migrations reais em integrations/cloudflare/signallq-admin-worker/. Consultar antes de criar tabela, coluna, índice ou migration novos no Console. Camilo consulta antes de mudar schema; Lia não edita este código, só entende o dado real ao desenhar telas.
---

## Quando usar

Antes de qualquer mudança de schema no SignallQ Console (`signallq-admin-worker`): nova tabela, nova coluna, novo índice, ou query que impacte performance.

## Estrutura do banco (fonte real)

- Schema base: `integrations/cloudflare/signallq-admin-worker/schema.sql`
- Migrations incrementais: `integrations/cloudflare/signallq-admin-worker/migrations/NNN_<slug>.sql`
- Numeração sequencial de 3 dígitos + slug da issue de origem (ex.: `011_gh442.sql` = GitHub #442; migrations mais antigas usam `sigNNN` = referência histórica do Linear, migradas para GitHub em 2026-07-09 — ver convenção de issue no `CLAUDE.md`).
- `database_name = signallq-admin-db`, binding `DB` em `wrangler.toml`.

### Tabelas centrais

- **`diagnostic_sessions`** — sessão de diagnóstico gerada pelo app (score, métricas de rede, contexto de ambiente/dispositivo, `platform`, `play_track`).
- **`ai_usage`** — uso de IA por sessão (tokens, custo, status/erro, `FOREIGN KEY session_id → diagnostic_sessions`).
- **`analytics_events`** — eventos de analytics (device_id, version_code, dist_channel, duration_ms só em `session_end`).
- **`admin_settings`** — configuração do painel, chave única, `value` é JSON serializado.
- **`system_errors`** — pipeline de erros do worker (category, resolved, resolved_by).
- **`admin_users` / `admin_sessions` / `auth_rate_limit`** — auth própria do painel (não confundir com auth do app).
- **`play_console_tracks`** — mapeamento `version_code → track` (internal/alpha/beta/production) via Android Publisher API, preenchido por backfill explícito, não automático.

## Convenção de migration (extraída de `011_gh442.sql` e `012_play_track.sql`)

- Comentário de cabeçalho explicando **por que** a migration existe (motivo de negócio/issue), não só a sintaxe SQL.
- Comentário com o comando exato de aplicação, sempre:
  ```
  npx wrangler d1 execute signallq-admin-db --file=migrations/NNN_slug.sql --remote
  ```
- **D1/SQLite não suporta `IF NOT EXISTS` em `ALTER TABLE ADD COLUMN`** — rodar duas vezes dá erro "column already exists" e isso é esperado/ignorável (documentado explicitamente no `schema.sql`, linha 60).
- Sempre `DEFAULT` explícito em coluna nova, preservando a semântica do dado histórico (ex.: `platform TEXT DEFAULT 'android'` — todo dado pré-existente era Android antes do PWA existir).
- Criar índice (`CREATE INDEX IF NOT EXISTS`, esse sim idempotente) para toda coluna nova usada em filtro do painel — padrão repetido em toda migration (`idx_sessions_platform`, `idx_sessions_play_track`, etc.).
- Réplica de coluna entre as 3 tabelas principais quando o filtro cruza domínios (`platform` e `play_track` estão em `diagnostic_sessions`, `ai_usage` e `analytics_events` simultaneamente) — desnormalização deliberada, não descuido.

## Fluxo de aplicação de migration

1. Escrever o `.sql` em `migrations/` com número sequencial seguinte ao último existente.
2. Testar local: `npx wrangler d1 execute signallq-admin-db --file=migrations/NNN.sql --local`
3. Aplicar em produção: mesmo comando com `--remote`, só depois de validado local.
4. Nunca editar uma migration já aplicada em produção — criar uma nova.

## Queries e performance

- Toda coluna usada em `WHERE`/`ORDER BY` no painel precisa de índice.
- D1 é SQLite — evitar JOIN complexo; preferir desnormalização leve (como `platform`/`play_track` replicados nas 3 tabelas) quando o filtro cruza domínios diferentes em vez de fazer JOIN caro.

## Segredos e bindings

- Nunca commitar valor de secret; usar `npx wrangler secret put <NOME>` (ver comentários em `wrangler.toml`: `ADMIN_SECRET`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY`, `INGEST_KEY`).
- `INGEST_KEY` tem escopo limitado (só autentica o app Android em `POST /ingest/*`) — não confundir com `ADMIN_SECRET` (acesso de leitura do painel).

## Limites

- Esta skill cobre schema/migration/query D1 — não cobre UI/React (ver design da Lia) nem arquitetura geral do Worker (endpoints, auth flow) além do que toca o banco.
