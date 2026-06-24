-- SIG-143: campos de contexto de ambiente e dispositivo
-- Aplicar: npx wrangler d1 execute signallq-admin-db --file=migrations/001_sig143.sql --remote
--
-- NOTA: D1 (SQLite) não suporta "IF NOT EXISTS" em ALTER TABLE.
-- Execute cada comando separadamente para tolerar falhas em colunas já existentes.
-- Ignorar erros "table already has column" — são esperados em D1 existente.
--
-- Alternativa linha a linha:
--   npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE diagnostic_sessions ADD COLUMN environment TEXT DEFAULT 'production'"
--   npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE diagnostic_sessions ADD COLUMN dist_channel TEXT DEFAULT ''"
--   npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE diagnostic_sessions ADD COLUMN build_type TEXT DEFAULT 'release'"
--   npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE diagnostic_sessions ADD COLUMN version_code INTEGER DEFAULT 0"
--   npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE diagnostic_sessions ADD COLUMN device_id TEXT DEFAULT ''"
--   npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE ai_usage ADD COLUMN environment TEXT DEFAULT 'production'"
--   npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE ai_usage ADD COLUMN version_code INTEGER DEFAULT 0"
--   npx wrangler d1 execute signallq-admin-db --remote --command="CREATE INDEX IF NOT EXISTS idx_sessions_environment ON diagnostic_sessions(environment)"
--   npx wrangler d1 execute signallq-admin-db --remote --command="CREATE INDEX IF NOT EXISTS idx_ai_usage_environment ON ai_usage(environment)"

ALTER TABLE diagnostic_sessions ADD COLUMN environment  TEXT    DEFAULT 'production';
ALTER TABLE diagnostic_sessions ADD COLUMN dist_channel TEXT    DEFAULT '';
ALTER TABLE diagnostic_sessions ADD COLUMN build_type   TEXT    DEFAULT 'release';
ALTER TABLE diagnostic_sessions ADD COLUMN version_code INTEGER DEFAULT 0;
ALTER TABLE diagnostic_sessions ADD COLUMN device_id    TEXT    DEFAULT '';
ALTER TABLE ai_usage            ADD COLUMN environment  TEXT    DEFAULT 'production';
ALTER TABLE ai_usage            ADD COLUMN version_code INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_sessions_environment ON diagnostic_sessions(environment);
CREATE INDEX IF NOT EXISTS idx_ai_usage_environment ON ai_usage(environment);
