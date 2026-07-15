-- GH#935
-- Remote game catalog, sensitivity profiles and admin audit for plug-and-play Cloudflare control.
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/004_gh935_game_catalog.sql --remote

CREATE TABLE IF NOT EXISTS game_profiles (
  profile_code TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  latency_good_max REAL,
  latency_attention_max REAL,
  jitter_good_max REAL,
  jitter_attention_max REAL,
  loss_good_max REAL,
  loss_attention_max REAL,
  download_good_min REAL,
  download_attention_min REAL,
  bufferbloat_good_max REAL,
  bufferbloat_attention_max REAL,
  wifi_policy TEXT,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS game_catalog (
  game_id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  slug TEXT NOT NULL UNIQUE,
  active INTEGER NOT NULL DEFAULT 1,
  profile_code TEXT NOT NULL,
  test_strategy TEXT NOT NULL,
  region_code TEXT NOT NULL,
  result_label TEXT NOT NULL,
  provider_network_mode TEXT NOT NULL DEFAULT 'fallback_regional',
  sort_order INTEGER NOT NULL DEFAULT 0,
  icon_key TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  FOREIGN KEY (profile_code) REFERENCES game_profiles(profile_code)
);

CREATE INDEX IF NOT EXISTS idx_game_catalog_active_sort
  ON game_catalog(active, sort_order);

CREATE TABLE IF NOT EXISTS game_platforms (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  game_id TEXT NOT NULL,
  platform_code TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(game_id, platform_code),
  FOREIGN KEY (game_id) REFERENCES game_catalog(game_id)
);

CREATE INDEX IF NOT EXISTS idx_game_platforms_platform_code
  ON game_platforms(platform_code);

CREATE TABLE IF NOT EXISTS game_catalog_audit (
  id TEXT PRIMARY KEY,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  action TEXT NOT NULL,
  actor TEXT NOT NULL,
  before_json TEXT,
  after_json TEXT,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_game_catalog_audit_entity
  ON game_catalog_audit(entity_type, entity_id, created_at);
