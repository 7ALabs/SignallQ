-- GH#1461
-- Trilha de auditoria (provider_audit_log) para as escritas administrativas
-- do diretorio de provedores (upsert, review, support update, logo upload,
-- sync-seed). Mesmo proposito/formato de game_catalog_audit (GH#935,
-- migration 004) e diagnostic_rule_audit_log (GH#952, migration 001) -- ja
-- descrita no modelo de dados do corpo de #951, nunca criada ate agora.
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/009_gh1461_provider_audit_log.sql --remote

CREATE TABLE IF NOT EXISTS provider_audit_log (
  id TEXT PRIMARY KEY,
  provider_id TEXT NOT NULL,
  operation TEXT NOT NULL,
  before_json TEXT,
  after_json TEXT,
  actor_user_id TEXT NOT NULL,
  actor_email TEXT,
  source TEXT NOT NULL,
  reason TEXT,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_provider_audit_log_provider
  ON provider_audit_log(provider_id, created_at);
