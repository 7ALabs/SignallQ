-- GH#1155: captura de e-mail dos modais "avisar quando lançar" do site
-- institucional (EmailCaptureDialog, usado por PlayStoreBadge no SignallQ e
-- por ProPage no SignallQ PRO). Até aqui só disparava telemetria — sem
-- destino real pro e-mail em si. Decisão da Claudete: tabela nova no MESMO
-- D1 do signallq-admin-worker, não um serviço terceiro de e-mail marketing
-- (custo/conta nova não se justifica pro volume de uma lista de espera).
--
-- platform segue a mesma convenção de coluna já usada desde GH#442 (origem
-- do dado) mesmo que hoje só 'web' escreva aqui — o site é o único emissor
-- deste ingest.
--
-- UNIQUE(email, product): clique duplicado da mesma pessoa no mesmo produto
-- não empilha linha nem gera erro visível pro usuário (idempotente do ponto
-- de vista de quem preenche o formulário — ver INSERT OR IGNORE no handler).
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/015_waitlist_signups.sql --remote
CREATE TABLE IF NOT EXISTS waitlist_signups (
  id          TEXT    PRIMARY KEY,
  email       TEXT    NOT NULL,
  product     TEXT    NOT NULL CHECK (product IN ('signallq', 'pro')),
  platform    TEXT    NOT NULL DEFAULT 'web',
  source_page TEXT    DEFAULT '',
  created_at  INTEGER NOT NULL,
  UNIQUE(email, product)
);

CREATE INDEX IF NOT EXISTS idx_waitlist_product ON waitlist_signups(product);
