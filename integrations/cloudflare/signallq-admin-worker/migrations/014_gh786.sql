-- GH#786: coleta regiao/UF agregada nas sessoes de diagnostico, para o mapa
-- "Onde o app e mais usado" (tela Redes & Provedores).
--
-- Fonte do dado: geolocalizacao de borda da propria Cloudflare
-- (request.cf.regionCode, calculada a partir do IP da requisicao no momento
-- do ingest) -- NUNCA o IP em si, que nem chega a ser persistido. Sem
-- nenhuma mudanca no app Android: o worker deriva a UF sozinho no POST
-- /ingest/diagnostic. Respeita a regra de privacidade do painel
-- (README_ADMIN_ARCHITECTURE.md: sem localizacao exata, so cidade/estado
-- agregado) -- regionCode e' aproximado (nivel de estado), nao endereco/geo
-- exata, e so aceito quando bate com uma das 27 UFs brasileiras validas
-- (ver UF_WHITELIST em index.ts); qualquer outro valor vira '' (nao
-- fabrica UF de requisicao fora do Brasil ou sem geo confiavel).
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/014_gh786.sql --remote
ALTER TABLE diagnostic_sessions ADD COLUMN uf TEXT DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_sessions_uf ON diagnostic_sessions(uf);
