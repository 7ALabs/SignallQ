-- GH#1405 (Feature #1402/#1409): catálogo inicial real do `local_ads`, ponto de partida
-- pra o Luiz curar/ampliar pelo Console daqui pra frente (`/admin/local-ads`, CRUD da
-- `LocalAdsSection.tsx`, PR #1409) — NÃO é conteúdo descartável de teste, é a primeira
-- leva de anúncios de produção. Extraído/adaptado 1:1 das headlines reais do protótipo
-- `.claude/design-specs/2026-07-25-site-webapp-v2/` (AdRail.dc.html, AdBannerWide.dc.html,
-- ScreenHome.dc.html), todos apontando pro grupo de testadores fechados
-- (SIGNALLQ_TEST_GROUP_URL) — mesmo destino que o site já usa noutros CTAs de "Teste
-- fechado". Decisão do Luiz (2026-07-25): seed via `wrangler d1 execute` direto (não via
-- `POST /admin/local-ads`) porque o endpoint admin exige sessão httpOnly (SIG-136, login
-- Firebase do Console) indisponível numa sessão não-interativa — canal alternativo
-- aprovado, mesmo resultado final na tabela.
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/019_gh1405_seed_local_ads.sql --remote

INSERT INTO local_ads (id, title, description, cta_label, target_url, active, created_at, updated_at) VALUES
  ('seed-internet-caiu',   'A internet caiu de novo?',            'O app SignallQ mede cômodo a cômodo e mostra onde o sinal morre.',            'Entrar na lista de teste',  'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch()),
  ('seed-culpado-plano',   'O culpado não é o plano.',            'Mede o Wi-Fi de cada cômodo e mostra onde o sinal morre. Sem jargão.',        'Entrar na lista de teste',  'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch()),
  ('seed-ponto-cego',      'Todo Wi-Fi tem um ponto cego.',       'O SignallQ acha o seu em minutos.',                                            'Quero testar',              'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch()),
  ('seed-quarto-sala',     'Quarto, sala, cozinha.',              'No sofá tudo funciona. No quarto, a chamada trava.',                           'Entrar na lista de teste',  'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch()),
  ('seed-mede-comodo',     'O SignallQ mede cômodo a cômodo.',    'E mostra o porquê o sinal some em certos lugares da casa.',                    'Entrar na lista de teste',  'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch()),
  ('seed-sofa-vai',        'No sofá vai. No quarto, trava.',      'Mapeie a casa inteira em 3 minutos.',                                           'Entrar na lista de teste',  'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch()),
  ('seed-3-minutos',       '3 minutos para mapear a casa inteira.', 'E você já sabe onde vale trocar o roteador de lugar.',                        'Entrar no teste fechado',   'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch()),
  ('seed-wifi-some-quarto','O Wi-Fi some no quarto?',             'O app SignallQ mede cômodo a cômodo e mostra onde o sinal morre.',            'Entrar na lista de teste',  'https://groups.google.com/g/testadores-signallq', 1, unixepoch(), unixepoch());
