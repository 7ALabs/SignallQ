-- GH#442: diferenciar origem do dado (Android vs WebApp/PWA) no SignallQ Console.
-- Sem essa coluna o painel não sabe se uma sessão de diagnóstico, uso de IA ou
-- evento de analytics veio do app Android ou do PWA — métricas ficam misturadas
-- e o filtro de plataforma no painel não tem o que ler.
-- Default 'android' preserva a semântica de todo o dado já existente em D1
-- (100% originado do Android até o PWA passar a enviar ingest real, GH#441).
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/011_gh442.sql --remote
ALTER TABLE diagnostic_sessions ADD COLUMN platform TEXT DEFAULT 'android';
ALTER TABLE ai_usage            ADD COLUMN platform TEXT DEFAULT 'android';
ALTER TABLE analytics_events    ADD COLUMN platform TEXT DEFAULT 'android';

CREATE INDEX IF NOT EXISTS idx_sessions_platform ON diagnostic_sessions(platform);
