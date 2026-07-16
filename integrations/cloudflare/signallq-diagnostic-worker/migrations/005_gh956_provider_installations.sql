-- GH#956
-- Registro de installationHash real por deteccao de provedor, pra que
-- distinct_installations_approx pare de ser fabricado (MAX(x,3) hardcoded no
-- segundo hit de qualquer detection_key, ignorando 100% do installationHash
-- recebido no contrato ProviderDetectionInput). Um unico device nao pode mais
-- aprovar sozinho um provedor pro enrichment automatico.
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/005_gh956_provider_installations.sql --remote

CREATE TABLE IF NOT EXISTS provider_detection_installations (
  detection_key TEXT NOT NULL,
  installation_hash TEXT NOT NULL,
  first_seen_at TEXT NOT NULL,
  PRIMARY KEY (detection_key, installation_hash)
);

CREATE INDEX IF NOT EXISTS idx_provider_detection_installations_key
  ON provider_detection_installations(detection_key);
