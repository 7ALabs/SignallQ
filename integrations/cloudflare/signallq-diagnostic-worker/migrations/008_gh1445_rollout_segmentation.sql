-- GH#1445
-- Rollout gradual + segmentacao do shadow mode (#952): rollout_percent ja
-- existia (migration 001) mas nunca era consumido de verdade -- publishRuleset
-- forcava sempre 100 (achado da auditoria #1441). Estas colunas adicionam
-- segmentacao minima (versao/canal do app) para a mesma decisao de rollout.
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/008_gh1445_rollout_segmentation.sql --remote

ALTER TABLE diagnostic_rulesets ADD COLUMN rollout_min_version_code INTEGER;
ALTER TABLE diagnostic_rulesets ADD COLUMN rollout_channels TEXT;

-- rollout_min_version_code NULL = sem restricao de versao minima.
-- rollout_channels NULL = sem restricao de canal; quando preenchido, lista
-- separada por virgula (ex.: "play_store,sideload") -- mesmo vocabulario de
-- distributionChannel() (Android, io.signallq.app.analytics).
