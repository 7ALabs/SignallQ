-- GH#422: fluxo operacional completo para a aba Erros.
-- system_errors não tinha resolução real (o botão "resolver" no painel só
-- funcionava em modo mock) nem diferenciava origem do erro (app/backend/IA/
-- integração) além do campo `source` livre — bloqueava o critério de aceite
-- "um erro exibido pode ser tratado e deixar de aparecer como ativo".
-- Aplicar: npx wrangler d1 execute signallq-admin-db --remote --file=migrations/009_gh422.sql
--
-- NOTA: D1 (SQLite) não suporta "IF NOT EXISTS" em ALTER TABLE.
-- Execute cada comando separadamente para tolerar falhas em colunas já existentes.
-- Ignorar erros "table already has column" — são esperados em D1 existente.

ALTER TABLE system_errors ADD COLUMN category        TEXT    NOT NULL DEFAULT 'backend';
ALTER TABLE system_errors ADD COLUMN resolved         INTEGER NOT NULL DEFAULT 0;
ALTER TABLE system_errors ADD COLUMN resolved_by      TEXT    DEFAULT '';
ALTER TABLE system_errors ADD COLUMN resolved_at      INTEGER DEFAULT 0;
ALTER TABLE system_errors ADD COLUMN resolution_note  TEXT    DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_system_errors_resolved ON system_errors(resolved);
CREATE INDEX IF NOT EXISTS idx_system_errors_category ON system_errors(category);
