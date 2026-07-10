-- Trilha do Play Console (internal/alpha/beta/production) por sessao/uso/evento.
-- Sem isso, um tester de trilha fechada instalado via link do Play Console tem
-- initiatingPackageName == "com.android.vending" -> dist_channel="play_store" ->
-- environment="production" no dado enviado pelo app (ver DistributionChannel.kt),
-- indistinguivel hoje de um usuario real de producao.
--
-- play_console_tracks guarda o mapeamento version_code -> track sincronizado via
-- Android Publisher API (rota /admin/integrations/google-play/tracks/sync).
-- play_track (nullable) nas 3 tabelas de telemetria e' preenchido via backfill
-- explicito (/admin/integrations/google-play/tracks/backfill), que so aplica o
-- mapeamento ja sincronizado -- nao chama a API do Google.
--
-- Decisao: NAO sobrescrever nem redefinir a coluna `environment` existente.
-- `environment` e' gravado pelo app Android no momento do insert (dado historico
-- imutavel); play_track e' um dado complementar, calculado depois pelo worker,
-- que preserva a distincao entre "o que o app enviou" e "o que o admin descobriu
-- depois consultando o Play Console".
--
-- NOTA: D1 (SQLite) nao suporta "IF NOT EXISTS" em ALTER TABLE ADD COLUMN.
-- Execute cada comando separadamente e tolere erros "column already exists" ao reexecutar.
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/012_play_track.sql --remote

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
