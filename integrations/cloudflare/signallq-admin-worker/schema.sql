-- SignallQ Admin — D1 Schema
-- Aplicar: npx wrangler d1 execute signallq-admin-db --file=schema.sql --remote

-- Sessões de diagnóstico geradas pelo app
CREATE TABLE IF NOT EXISTS diagnostic_sessions (
  id           TEXT    PRIMARY KEY,
  created_at   INTEGER NOT NULL,          -- Unix timestamp (segundos)
  network_type TEXT    NOT NULL DEFAULT 'unknown', -- wifi | fibra | celular | ethernet | unknown
  status       TEXT    NOT NULL DEFAULT 'unknown', -- excelente | bom | regular | critico | inconclusivo | failed
  score        INTEGER,                   -- 0-100, calculado pelo motor local Android (ScoreEngine); o worker so armazena o valor recebido, nao recalcula
  download_mbps REAL,
  upload_mbps   REAL,
  latency_ms    INTEGER,
  jitter_ms     REAL,
  packet_loss   REAL,
  issues        TEXT    DEFAULT '[]',     -- JSON array de strings
  resolved      INTEGER NOT NULL DEFAULT 0, -- 0=aberto, 1=resolvido
  -- SIG-139: operadora do usuário
  operator      TEXT    DEFAULT '',
  -- SIG-143: contexto de ambiente e dispositivo
  environment   TEXT    DEFAULT 'production', -- production | staging | development
  dist_channel  TEXT    DEFAULT '',           -- play_store | firebase_app_distribution | sideload
  build_type    TEXT    DEFAULT 'release',    -- release | debug
  version_code  INTEGER DEFAULT 0,
  device_id     TEXT    DEFAULT ''            -- hash anônimo do dispositivo
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
  -- SIG-143: contexto de ambiente
  environment      TEXT    DEFAULT 'production',
  version_code     INTEGER DEFAULT 0,
  FOREIGN KEY (session_id) REFERENCES diagnostic_sessions(id)
);

-- Configurações do painel admin (chave única 'admin')
CREATE TABLE IF NOT EXISTS admin_settings (
  key        TEXT    PRIMARY KEY,
  value      TEXT    NOT NULL,        -- JSON serializado do payload completo
  updated_at INTEGER NOT NULL         -- Unix timestamp (segundos)
);

-- GH#1342/#1344: histórico append-only das integrações Google Play/Firebase (ver migration 016).
CREATE TABLE IF NOT EXISTS integration_metric_snapshots (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  provider      TEXT    NOT NULL,
  service       TEXT    NOT NULL,
  resource      TEXT    NOT NULL,
  metric        TEXT,
  period_start  TEXT,
  period_end    TEXT,
  value_numeric REAL,
  payload       TEXT    NOT NULL,
  synced_at     INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_integration_snapshots_lookup
  ON integration_metric_snapshots(provider, service, resource, period_end);

-- Índices para queries frequentes do painel
CREATE INDEX IF NOT EXISTS idx_sessions_created_at  ON diagnostic_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_sessions_network_type ON diagnostic_sessions(network_type);
CREATE INDEX IF NOT EXISTS idx_sessions_status       ON diagnostic_sessions(status);
CREATE INDEX IF NOT EXISTS idx_ai_usage_created_at  ON ai_usage(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_session_id  ON ai_usage(session_id);

-- SIG-138: campos de contexto de dispositivo e laudo IA (migration para D1 existente)
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --remote --command="ALTER TABLE ..."
-- (D1/SQLite não suporta IF NOT EXISTS em ALTER TABLE — ignorar erros "column already exists")
ALTER TABLE diagnostic_sessions ADD COLUMN device_model       TEXT DEFAULT '';
ALTER TABLE diagnostic_sessions ADD COLUMN os_version         TEXT DEFAULT '';
ALTER TABLE diagnostic_sessions ADD COLUMN app_version        TEXT DEFAULT '';
ALTER TABLE diagnostic_sessions ADD COLUMN ai_summary_report  TEXT DEFAULT '';

-- SIG-139: operadora (adicionada manualmente no D1 antes desta migration ser documentada)
ALTER TABLE diagnostic_sessions ADD COLUMN operator TEXT DEFAULT '';

-- SIG-143: campos de contexto de ambiente e dispositivo
-- Aplicar via: migrations/001_sig143.sql (npx wrangler d1 execute --file=... --remote)
ALTER TABLE diagnostic_sessions ADD COLUMN environment  TEXT    DEFAULT 'production';
ALTER TABLE diagnostic_sessions ADD COLUMN dist_channel TEXT    DEFAULT '';
ALTER TABLE diagnostic_sessions ADD COLUMN build_type   TEXT    DEFAULT 'release';
ALTER TABLE diagnostic_sessions ADD COLUMN version_code INTEGER DEFAULT 0;
ALTER TABLE diagnostic_sessions ADD COLUMN device_id    TEXT    DEFAULT '';
ALTER TABLE ai_usage            ADD COLUMN environment  TEXT    DEFAULT 'production';
ALTER TABLE ai_usage            ADD COLUMN version_code INTEGER DEFAULT 0;

-- Índices para filtro por environment (SIG-143)
CREATE INDEX IF NOT EXISTS idx_sessions_environment ON diagnostic_sessions(environment);
CREATE INDEX IF NOT EXISTS idx_ai_usage_environment ON ai_usage(environment);

-- SIG-135 Fase A: pipeline de erros do worker
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

-- GH#422: fluxo operacional de erros — resolução real (responsável, data,
-- observação) e diferenciação de origem (app | backend | ia | integration).
-- Aplicar via: migrations/010_gh422.sql (npx wrangler d1 execute --file=... --remote)
ALTER TABLE system_errors ADD COLUMN category       TEXT    NOT NULL DEFAULT 'backend';
ALTER TABLE system_errors ADD COLUMN resolved       INTEGER NOT NULL DEFAULT 0;
ALTER TABLE system_errors ADD COLUMN resolved_by    TEXT    DEFAULT '';
ALTER TABLE system_errors ADD COLUMN resolved_at    INTEGER DEFAULT 0;
ALTER TABLE system_errors ADD COLUMN resolution_note TEXT   DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_system_errors_resolved ON system_errors(resolved);
CREATE INDEX IF NOT EXISTS idx_system_errors_category ON system_errors(category);

-- SIG-136: Auth própria via D1 — usuários admin, sessões httpOnly, rate limiting
CREATE TABLE IF NOT EXISTS admin_users (
  id            TEXT    PRIMARY KEY,
  email         TEXT    NOT NULL UNIQUE,
  password_hash TEXT    NOT NULL,
  role          TEXT    NOT NULL DEFAULT 'admin',
  active        INTEGER NOT NULL DEFAULT 1,
  created_at    INTEGER NOT NULL,
  last_login    INTEGER
);

CREATE TABLE IF NOT EXISTS admin_sessions (
  token_hash  TEXT    PRIMARY KEY,
  user_id     TEXT    NOT NULL,
  created_at  INTEGER NOT NULL,
  expires_at  INTEGER NOT NULL,
  last_seen   INTEGER NOT NULL,
  FOREIGN KEY (user_id) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_admin_sessions_expires ON admin_sessions(expires_at);

CREATE TABLE IF NOT EXISTS auth_rate_limit (
  ip           TEXT    PRIMARY KEY,
  count        INTEGER NOT NULL DEFAULT 0,
  window_start INTEGER NOT NULL
);

-- GH#417: analytics_events precisa do mesmo contexto de ambiente/dispositivo
-- que diagnostic_sessions e ai_usage já têm — sem isso não dá pra calcular
-- retenção (D1/D7/D30) nem segmentar por versão/canal/build.
-- Aplicar via: migrations/008_gh417.sql (npx wrangler d1 execute --file=... --remote)
ALTER TABLE analytics_events ADD COLUMN device_id    TEXT    DEFAULT '';
ALTER TABLE analytics_events ADD COLUMN version_code INTEGER DEFAULT 0;
ALTER TABLE analytics_events ADD COLUMN dist_channel TEXT    DEFAULT '';
ALTER TABLE analytics_events ADD COLUMN build_type   TEXT    DEFAULT 'release';
ALTER TABLE analytics_events ADD COLUMN duration_ms  INTEGER DEFAULT NULL; -- só em session_end
CREATE INDEX IF NOT EXISTS idx_analytics_device_id ON analytics_events(device_id);

-- GH#421: histórico de IA precisa auditar status/erro de cada execução —
-- ai_usage não tinha essas colunas, só tokens/custo agregado.
-- Aplicar via: migrations/009_gh421.sql (npx wrangler d1 execute --file=... --remote)
ALTER TABLE ai_usage ADD COLUMN status        TEXT DEFAULT 'success';
ALTER TABLE ai_usage ADD COLUMN error_message TEXT DEFAULT '';

-- GH#442: diferenciar origem do dado (Android vs WebApp/PWA, este último descontinuado).
-- Default 'android' preserva a semântica de todo o dado histórico (pré-existente ao ingest do PWA).
-- Aplicar via: migrations/011_gh442.sql (npx wrangler d1 execute --file=... --remote)
ALTER TABLE diagnostic_sessions ADD COLUMN platform TEXT DEFAULT 'android';
ALTER TABLE ai_usage            ADD COLUMN platform TEXT DEFAULT 'android';
ALTER TABLE analytics_events    ADD COLUMN platform TEXT DEFAULT 'android';
CREATE INDEX IF NOT EXISTS idx_sessions_platform ON diagnostic_sessions(platform);

-- Trilha do Play Console (internal/alpha/beta/production) por sessao/uso/evento —
-- diferencia tester de trilha fechada (dist_channel=play_store, mas nao e producao
-- real) de usuario real de producao, sem sobrescrever `environment` (dado historico
-- gravado pelo app). play_console_tracks mapeia version_code -> track via Android
-- Publisher API; play_track e' preenchido por backfill explicito, nao automatico.
-- Aplicar via: migrations/012_play_track.sql (npx wrangler d1 execute --file=... --remote)
CREATE TABLE IF NOT EXISTS play_console_tracks (
  version_code INTEGER PRIMARY KEY,
  track        TEXT    NOT NULL,
  synced_at    INTEGER NOT NULL
);
ALTER TABLE diagnostic_sessions ADD COLUMN play_track TEXT DEFAULT NULL;
ALTER TABLE ai_usage            ADD COLUMN play_track TEXT DEFAULT NULL;
ALTER TABLE analytics_events    ADD COLUMN play_track TEXT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_sessions_play_track  ON diagnostic_sessions(play_track);
CREATE INDEX IF NOT EXISTS idx_ai_usage_play_track   ON ai_usage(play_track);
CREATE INDEX IF NOT EXISTS idx_analytics_play_track  ON analytics_events(play_track);

-- GH#788: série histórica de latência/uptime por serviço (Saúde do Sistema),
-- snapshot gravado a cada execução do Cron Trigger (ver `scheduled` em src/index.ts).
-- Aplicar via: migrations/013_gh788.sql (npx wrangler d1 execute --file=... --remote)
CREATE TABLE IF NOT EXISTS system_health_snapshots (
  id         TEXT    PRIMARY KEY,
  service    TEXT    NOT NULL,   -- d1 | firebase | bigquery
  status     TEXT    NOT NULL,   -- ok | error | not_configured
  latency_ms INTEGER,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_health_snapshots_service_created
  ON system_health_snapshots(service, created_at);

-- GH#786: regiao/UF aproximada por sessao (mapa "Onde o app e mais usado",
-- Redes & Provedores). Derivada no worker via request.cf.regionCode
-- (geolocalizacao de borda da Cloudflare) no momento do ingest -- o IP em si
-- nunca e' persistido. So aceita as 27 UFs brasileiras validas (ver
-- UF_WHITELIST em src/index.ts); qualquer outro valor grava ''.
-- Aplicar via: migrations/014_gh786.sql (npx wrangler d1 execute --file=... --remote)
ALTER TABLE diagnostic_sessions ADD COLUMN uf TEXT DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_sessions_uf ON diagnostic_sessions(uf);

-- GH#1341: avaliações completas do Google Play (Android Publisher API v3, reviews.list) --
-- lista de registros identificáveis por reviewId (nota, comentário, idioma, dispositivo,
-- versão, resposta do dev, status de tratamento), diferente de admin_settings (estado pontual)
-- e de integration_metric_snapshots (série temporal, migration 016). UPDATE-em-lugar por
-- review_id -- a API não expõe histórico de edição. handling_status é campo admin-side, sync
-- nunca sobrescreve (ver exemplo de upsert na migration).
-- Aplicar via: migrations/017_gh1341_google_play_reviews.sql (npx wrangler d1 execute --file=... --remote)
CREATE TABLE IF NOT EXISTS google_play_reviews (
  review_id             TEXT    PRIMARY KEY,
  rating                INTEGER NOT NULL,
  comment_text          TEXT    NOT NULL DEFAULT '',
  language              TEXT    NOT NULL DEFAULT '',
  device                TEXT    NOT NULL DEFAULT '',
  android_os_version    INTEGER,
  app_version_code      INTEGER,
  app_version_name      TEXT    NOT NULL DEFAULT '',
  review_last_modified  INTEGER,
  developer_reply_text  TEXT    DEFAULT NULL,
  developer_reply_at    INTEGER DEFAULT NULL,
  handling_status       TEXT    NOT NULL DEFAULT 'pending', -- pending | replied | dismissed
  first_synced_at       INTEGER NOT NULL,
  last_synced_at        INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_google_play_reviews_rating          ON google_play_reviews(rating);
CREATE INDEX IF NOT EXISTS idx_google_play_reviews_last_synced     ON google_play_reviews(last_synced_at);
CREATE INDEX IF NOT EXISTS idx_google_play_reviews_handling_status ON google_play_reviews(handling_status);
