-- GH#788: persistir série histórica de latência/uptime dos serviços (Saúde do Sistema).
-- A tela precisa de um gráfico "Latência P95 da API · 14 dias", mas o worker só
-- respondia o health check no instante da chamada (sem histórico). Esta tabela
-- guarda um snapshot por serviço a cada execução do Cron Trigger (ver `scheduled`
-- em src/index.ts), reaproveitando os mesmos checks reais já usados por
-- /admin/system-health (checkD1Health, checkFirebaseCredentialsHealth,
-- checkBigQueryHealth) — nenhum dado novo é fabricado, só passa a ser gravado.
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/013_gh788.sql --remote
CREATE TABLE IF NOT EXISTS system_health_snapshots (
  id         TEXT    PRIMARY KEY,
  service    TEXT    NOT NULL,   -- d1 | firebase | bigquery
  status     TEXT    NOT NULL,   -- ok | error | not_configured
  latency_ms INTEGER,            -- NULL quando status = not_configured
  created_at INTEGER NOT NULL    -- Unix timestamp (segundos)
);

CREATE INDEX IF NOT EXISTS idx_health_snapshots_service_created
  ON system_health_snapshots(service, created_at);
