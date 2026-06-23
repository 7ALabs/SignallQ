-- SignallQ Admin — D1 Schema
-- Aplicar: npx wrangler d1 execute signallq-admin-db --file=schema.sql --remote

-- Sessões de diagnóstico geradas pelo app
CREATE TABLE IF NOT EXISTS diagnostic_sessions (
  id           TEXT    PRIMARY KEY,
  created_at   INTEGER NOT NULL,          -- Unix timestamp (segundos)
  network_type TEXT    NOT NULL DEFAULT 'unknown', -- wifi | fibra | celular | ethernet | unknown
  status       TEXT    NOT NULL DEFAULT 'unknown', -- bom | regular | ruim | critico | inconclusivo
  score        INTEGER,                   -- 0-100, calculado pelo worker
  download_mbps REAL,
  upload_mbps   REAL,
  latency_ms    INTEGER,
  jitter_ms     REAL,
  packet_loss   REAL,
  issues        TEXT    DEFAULT '[]',     -- JSON array de strings
  resolved      INTEGER NOT NULL DEFAULT 0 -- 0=aberto, 1=resolvido
);

-- Uso de IA por sessão de diagnóstico
CREATE TABLE IF NOT EXISTS ai_usage (
  id               TEXT    PRIMARY KEY,
  session_id       TEXT,
  created_at       INTEGER NOT NULL,      -- Unix timestamp (segundos)
  model            TEXT    NOT NULL,
  prompt_tokens    INTEGER NOT NULL DEFAULT 0,
  completion_tokens INTEGER NOT NULL DEFAULT 0,
  total_tokens     INTEGER NOT NULL DEFAULT 0,
  cost_usd         REAL    NOT NULL DEFAULT 0,
  FOREIGN KEY (session_id) REFERENCES diagnostic_sessions(id)
);

-- Configurações do painel admin (chave única 'admin')
CREATE TABLE IF NOT EXISTS admin_settings (
  key        TEXT    PRIMARY KEY,
  value      TEXT    NOT NULL,        -- JSON serializado do payload completo
  updated_at INTEGER NOT NULL         -- Unix timestamp (segundos)
);

-- Índices para queries frequentes do painel
CREATE INDEX IF NOT EXISTS idx_sessions_created_at  ON diagnostic_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_sessions_network_type ON diagnostic_sessions(network_type);
CREATE INDEX IF NOT EXISTS idx_sessions_status       ON diagnostic_sessions(status);
CREATE INDEX IF NOT EXISTS idx_ai_usage_created_at  ON ai_usage(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_session_id  ON ai_usage(session_id);
