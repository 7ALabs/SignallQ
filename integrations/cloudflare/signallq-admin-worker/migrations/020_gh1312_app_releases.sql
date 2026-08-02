-- GH#1312: catálogo remoto de releases do app (produto+canal), consumido em leitura pelo
-- Android (comparação por versionCode) e administrado pelo Console (publicar/histórico/
-- desativar comunicação sem apagar). Consolida #1313/#1314/#1315 -- este é só o recorte de
-- backend/Worker; recepção/dedup/UI de Ajustes no Android ficam para o Camilo depois.
--
-- Decisão (sem ADR dedicado a #1312, aplicando o princípio institucional do ADR-008): sem
-- infra nova (R2/KV/Cron/Queue) -- reaproveita o D1 já usado por este worker.
--
-- Modelo: uma linha por release publicada (histórico append-only, nunca sobrescrita por
-- publicação nova). `status` controla qual linha é "a release ativa" de um product+channel:
--   active      -- a release comunicada no momento; é a que GET /app-updates retorna.
--   superseded  -- foi ativa, substituída por uma publicação de versionCode maior.
--   deactivated -- comunicação desligada manualmente pelo admin, sem apagar a linha (permite
--                  reverter uma decisão errada de push sem perder o registro/histórico).
-- Publicar novamente o MESMO versionCode (correção de nota/URL) faz UPDATE in-place na
-- mesma linha (release_id é determinístico: product-channel-versionCode) em vez de duplicar.
--
-- `channel` usa o mesmo enum já estabelecido em play_console_tracks/migration 012 (play_track):
-- internal | alpha | beta | production -- não inventa nomenclatura nova pro mesmo conceito.
--
-- Índice único parcial garante no nível de banco que nunca existam duas linhas 'active' para o
-- mesmo product+channel (reforça a regra de "impedir regressão acidental" da issue -- a troca de
-- ativa é sempre uma transação explícita no handler, nunca duas ativas coexistindo por acidente).
--
-- push_status/push_sent_at/push_error registram o ÚLTIMO disparo (não log completo -- a issue
-- pede "resultado e falhas registrados", não histórico de tentativas; histórico de tentativas
-- fica para quando/se reminderCampaignId precisar de mais de um disparo por release, tratado à
-- parte se/quando isso virar requisito real).
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/020_gh1312_app_releases.sql --remote

CREATE TABLE IF NOT EXISTS app_releases (
  release_id TEXT PRIMARY KEY, -- determinístico: `${product}-${channel}-${versionCode}`
  product TEXT NOT NULL, -- ex.: signallq | signallq_pro
  channel TEXT NOT NULL, -- internal | alpha | beta | production
  version_name TEXT NOT NULL,
  version_code INTEGER NOT NULL,
  release_notes TEXT NOT NULL DEFAULT '',
  store_url TEXT NOT NULL,
  notification_enabled INTEGER NOT NULL DEFAULT 1, -- 0/1 -- se essa release dispara comunicação
  reminder_campaign_id TEXT DEFAULT NULL, -- distingue um novo lembrete de duplicata (regra de dedup do Android)
  status TEXT NOT NULL DEFAULT 'active', -- active | superseded | deactivated
  published_at INTEGER NOT NULL, -- Unix timestamp (segundos)
  published_by TEXT NOT NULL DEFAULT '', -- email do admin (audit -- mesmo padrão de feature_flag_audit)
  push_status TEXT DEFAULT NULL, -- NULL (nunca disparado) | sent | error | not_configured
  push_sent_at INTEGER DEFAULT NULL,
  push_error TEXT DEFAULT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_app_releases_lookup
  ON app_releases(product, channel, status);

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_releases_active_unique
  ON app_releases(product, channel) WHERE status = 'active';
