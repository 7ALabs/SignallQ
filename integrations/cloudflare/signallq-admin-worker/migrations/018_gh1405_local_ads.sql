-- GH#1405 (Task da Feature #1402): catálogo de anúncios locais (house ads) exibido no Site/PWA
-- quando o AdSense não preenche o slot (no-fill) ou não está configurado. Decisão do Luiz
-- (2026-07-25): conteúdo administrável via Console, sorteado aleatoriamente a cada exibição —
-- não fixo em código. Escopo inicial cobre só o espaço de anúncio já existente (AdBanner da tela
-- de Resultado do Site), sem criar espaço novo.
--
-- `active` segue a mesma convenção booleana (0/1) de `feature_flags.enabled` — filtro do endpoint
-- público (GH#1406) e da UI de gerenciamento (GH#1407, Console/Lia).
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/018_gh1405_local_ads.sql --remote

CREATE TABLE IF NOT EXISTS local_ads (
  id          TEXT    PRIMARY KEY,
  title       TEXT    NOT NULL,
  description TEXT    NOT NULL DEFAULT '',
  cta_label   TEXT    NOT NULL,
  target_url  TEXT    NOT NULL,
  active      INTEGER NOT NULL DEFAULT 1, -- 0=inativo, 1=ativo — só ativos entram no sorteio público
  created_at  INTEGER NOT NULL,           -- Unix timestamp (segundos)
  updated_at  INTEGER NOT NULL            -- Unix timestamp (segundos)
);

CREATE INDEX IF NOT EXISTS idx_local_ads_active ON local_ads(active);
