-- SIG-134: eventos de analytics de produto
-- Recebe eventos do Android via POST /ingest/analytics
-- Alimenta GET /admin/analytics/product e /admin/analytics/battery

CREATE TABLE IF NOT EXISTS analytics_events (
  id               TEXT    PRIMARY KEY,
  event_name       TEXT    NOT NULL,   -- feature_used | screen_view | session_start | feature_crash | battery_snapshot
  session_id       TEXT    NOT NULL,
  created_at       INTEGER NOT NULL,   -- Unix timestamp (segundos)
  app_version      TEXT    DEFAULT '',
  feature_id       TEXT    DEFAULT '', -- feature_used, feature_crash
  screen_name      TEXT    DEFAULT '', -- screen_view
  error_type       TEXT    DEFAULT '', -- feature_crash
  battery_level    INTEGER DEFAULT NULL, -- battery_snapshot (0-100)
  battery_charging INTEGER DEFAULT NULL, -- battery_snapshot (0|1)
  environment      TEXT    DEFAULT 'production'
);

CREATE INDEX IF NOT EXISTS idx_analytics_event_name  ON analytics_events(event_name);
CREATE INDEX IF NOT EXISTS idx_analytics_created_at  ON analytics_events(created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_feature_id  ON analytics_events(feature_id);
CREATE INDEX IF NOT EXISTS idx_analytics_session_id  ON analytics_events(session_id);
CREATE INDEX IF NOT EXISTS idx_analytics_environment ON analytics_events(environment);
