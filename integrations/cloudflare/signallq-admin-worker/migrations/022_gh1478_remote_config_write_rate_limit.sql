-- GH#1478 (Épico #1347, Feature F2): rate limit dedicado das rotas de escrita do backend admin de
-- Feature Flags / Firebase Remote Config (publish, rollback, feature-flags/sync). Distinta de
-- `auth_rate_limit` (migration 005_sig13.sql) — aquela é por IP e protege só brute-force de login
-- (janela de 15min, teto 5 tentativas); esta é por usuário autenticado (já passou pela sessão) e
-- cobre qualquer sequência anormal de escrita real no template do Remote Config (publish/rollback/
-- sync), janela mais generosa (10min, teto 20) — pensada pra não travar um operador legítimo
-- fazendo várias publicações em sequência, só conter automação/erro em loop.
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/022_gh1478_remote_config_write_rate_limit.sql --remote

CREATE TABLE IF NOT EXISTS remote_config_write_rate_limit (
  actor_user_id TEXT    NOT NULL,
  route         TEXT    NOT NULL, -- 'publish' | 'rollback' | 'sync'
  count         INTEGER NOT NULL DEFAULT 1,
  window_start  INTEGER NOT NULL,
  PRIMARY KEY (actor_user_id, route)
);
