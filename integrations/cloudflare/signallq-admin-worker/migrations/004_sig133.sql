-- SIG-133: tabela de alertas persistidos — orçamento de IA e taxa de erros
-- Aplicar: npx wrangler d1 execute signallq-admin-db --remote --file=migrations/004_sig133.sql

CREATE TABLE IF NOT EXISTS alerts (
  id         TEXT    PRIMARY KEY,
  type       TEXT    NOT NULL,
  severity   TEXT    NOT NULL,
  title      TEXT    NOT NULL DEFAULT '',
  message    TEXT    NOT NULL,
  created_at INTEGER NOT NULL,
  resolved   INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON alerts(created_at);
CREATE INDEX IF NOT EXISTS idx_alerts_resolved   ON alerts(resolved);
