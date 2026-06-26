-- SIG-129: pipeline de erros do worker via D1 (Fase A)
-- Aplicar: npx wrangler d1 execute signallq-admin-db --remote --file=migrations/003_sig129.sql

CREATE TABLE IF NOT EXISTS system_errors (
  id          TEXT    PRIMARY KEY,
  source      TEXT    NOT NULL,
  message     TEXT    NOT NULL,
  stack_trace TEXT    DEFAULT '',
  count       INTEGER NOT NULL DEFAULT 1,
  first_seen  INTEGER NOT NULL,
  last_seen   INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_system_errors_last_seen ON system_errors(last_seen);
