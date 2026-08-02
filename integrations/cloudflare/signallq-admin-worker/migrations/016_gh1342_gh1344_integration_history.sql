-- GH#1342/#1344: histórico de séries temporais para as integrações de Google Play e Firebase.
--
-- admin_settings (chave/valor) só guarda o último snapshot por integração -- suficiente pros
-- endpoints de "status", mas os épicos #1341/#1343 exigem série temporal/tendência (Android
-- Vitals, correlação por período) e a UI (ChartCard, plano da Lia em
-- SignallQ Admin/docs/product/plano-ux-google-play-firebase.md) não tem o que plotar sem
-- histórico -- cada sync sobrescrevia o anterior. Tabela append-only: cada sync grava uma linha
-- nova, nunca sobrescreve. admin_settings continua existindo, como cache de "última sincronização"
-- pros endpoints /status (evita escanear histórico só pra responder "quando foi o último sync").
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/016_gh1342_gh1344_integration_history.sql --remote
CREATE TABLE IF NOT EXISTS integration_metric_snapshots (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  provider      TEXT    NOT NULL,   -- google_play | firebase
  service       TEXT    NOT NULL,   -- play_developer_reporting | firebase_management | remote_config | app_check | app_distribution | fcm_data
  resource      TEXT    NOT NULL,   -- ex.: apps/io.signallq.app/anrRateMetricSet
  metric        TEXT,               -- ex.: anrRate -- nulo quando o snapshot é o payload inteiro, não uma métrica única
  period_start  TEXT,               -- data ISO (YYYY-MM-DD) -- nulo quando não aplicável (ex.: inventário de apps)
  period_end    TEXT,
  value_numeric REAL,               -- valor numérico quando existir (ex.: anrRatePercent) -- nulo caso contrário
  payload       TEXT    NOT NULL,   -- JSON bruto completo do registro (payload preservado, conforme exigido pelos épicos)
  synced_at     INTEGER NOT NULL    -- Unix timestamp (segundos)
);

CREATE INDEX IF NOT EXISTS idx_integration_snapshots_lookup
  ON integration_metric_snapshots(provider, service, resource, period_end);
