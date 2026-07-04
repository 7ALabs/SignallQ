-- GH#421: histórico de IA precisa auditar status e erro de cada execução —
-- `ai_usage` não tinha essas colunas, então a aba "IA & Custo" nunca conseguia
-- listar execuções reais além de tokens/custo.
-- Aplicar: npx wrangler d1 execute signallq-admin-db --remote --file=migrations/009_gh421.sql
--
-- NOTA: D1 (SQLite) não suporta "IF NOT EXISTS" em ALTER TABLE.
-- Execute cada comando separadamente para tolerar falhas em colunas já existentes.
-- Ignorar erros "table already has column" — são esperados em D1 existente.

-- status default 'success': registros retroativos completaram sem erro reportado
-- (o app só envia ai_usage ao final de uma chamada bem-sucedida hoje).
ALTER TABLE ai_usage ADD COLUMN status        TEXT DEFAULT 'success';
ALTER TABLE ai_usage ADD COLUMN error_message TEXT DEFAULT '';
