-- GH#951
-- Provider directory, support channels, assets and aggregated detections.
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/002_gh951_provider_directory.sql --remote

CREATE TABLE IF NOT EXISTS providers (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  legal_name TEXT,
  cnpj TEXT,
  provider_type TEXT NOT NULL DEFAULT 'UNKNOWN',
  status TEXT NOT NULL DEFAULT 'DRAFT',
  official_domain TEXT,
  logo_version INTEGER NOT NULL DEFAULT 0,
  last_verified_at TEXT,
  next_review_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_providers_status
  ON providers(status);

CREATE TABLE IF NOT EXISTS provider_identifiers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  provider_id TEXT NOT NULL,
  identifier_type TEXT NOT NULL,
  original_value TEXT NOT NULL,
  normalized_value TEXT NOT NULL,
  confidence TEXT NOT NULL,
  source_url TEXT,
  is_active INTEGER NOT NULL DEFAULT 1,
  verified_at TEXT,
  FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX IF NOT EXISTS idx_provider_identifiers_normalized
  ON provider_identifiers(normalized_value);

CREATE TABLE IF NOT EXISTS provider_channels (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  provider_id TEXT NOT NULL,
  channel_type TEXT NOT NULL,
  label TEXT,
  value TEXT NOT NULL,
  source_url TEXT,
  verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED',
  verified_at TEXT,
  next_review_at TEXT,
  priority INTEGER NOT NULL DEFAULT 0,
  is_active INTEGER NOT NULL DEFAULT 1,
  FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX IF NOT EXISTS idx_provider_channels_provider_id
  ON provider_channels(provider_id);

CREATE TABLE IF NOT EXISTS provider_assets (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  provider_id TEXT NOT NULL,
  asset_type TEXT NOT NULL,
  r2_key TEXT NOT NULL,
  source_url TEXT,
  file_hash TEXT,
  version INTEGER NOT NULL,
  verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED',
  created_at TEXT NOT NULL,
  FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE TABLE IF NOT EXISTS provider_detection_stats (
  detection_key TEXT PRIMARY KEY,
  provider_id TEXT,
  asn INTEGER,
  raw_name_sample TEXT,
  normalized_name TEXT,
  test_count_total INTEGER NOT NULL DEFAULT 0,
  test_count_since_review INTEGER NOT NULL DEFAULT 0,
  distinct_installations_approx INTEGER NOT NULL DEFAULT 0,
  distinct_days INTEGER NOT NULL DEFAULT 0,
  first_seen_at TEXT NOT NULL,
  last_seen_at TEXT NOT NULL,
  last_enrichment_queued_at TEXT,
  last_review_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_provider_detection_stats_provider_id
  ON provider_detection_stats(provider_id);

CREATE TABLE IF NOT EXISTS provider_enrichment_jobs (
  id TEXT PRIMARY KEY,
  provider_id TEXT,
  detection_key TEXT,
  reason TEXT NOT NULL,
  status TEXT NOT NULL,
  idempotency_key TEXT NOT NULL UNIQUE,
  attempt_count INTEGER NOT NULL DEFAULT 0,
  started_at TEXT,
  completed_at TEXT,
  error_message TEXT,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_provider_enrichment_jobs_status
  ON provider_enrichment_jobs(status);
