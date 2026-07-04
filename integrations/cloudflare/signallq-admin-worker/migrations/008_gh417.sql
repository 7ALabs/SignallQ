-- GH#417: completa o contrato de /ingest/analytics com o mesmo contexto de
-- ambiente/dispositivo já presente em diagnostic_sessions e ai_usage.
-- Sem device_id, analytics_events não permite calcular retenção (D1/D7/D30) nem
-- segmentar por versão/canal/build — bloqueava o trabalho de #418 (Produto & Uso).
-- Aplicar: npx wrangler d1 execute signallq-admin-db --remote --file=migrations/008_gh417.sql
--
-- NOTA: D1 (SQLite) não suporta "IF NOT EXISTS" em ALTER TABLE.
-- Execute cada comando separadamente para tolerar falhas em colunas já existentes.
-- Ignorar erros "table already has column" — são esperados em D1 existente.

ALTER TABLE analytics_events ADD COLUMN device_id    TEXT    DEFAULT '';
ALTER TABLE analytics_events ADD COLUMN version_code INTEGER DEFAULT 0;
ALTER TABLE analytics_events ADD COLUMN dist_channel TEXT    DEFAULT '';
ALTER TABLE analytics_events ADD COLUMN build_type   TEXT    DEFAULT 'release';
-- duration_ms: preenchido apenas no evento session_end (SIG-295) — habilita
-- cálculo de tempo médio de sessão sem heurística.
ALTER TABLE analytics_events ADD COLUMN duration_ms  INTEGER DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_analytics_device_id ON analytics_events(device_id);
