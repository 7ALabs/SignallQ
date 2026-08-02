-- GH#1444 (parte de #952) — Shadow mode: comparacao local-vs-remoto.
-- Recria `diagnostic_divergences`, removida em GH#961 por ter sido criada na
-- migration 001 sem nenhum wiring (nenhum endpoint, nenhum call site). Desta
-- vez a tabela nasce junto com o wiring completo:
--   - POST /ingest/diagnostic-divergence (handleDiagnosticDivergenceIngest, index.ts)
--   - GET  /admin/diagnostic/divergences (handleDiagnosticDivergencesList, index.ts)
--   - Android: DiagnosticDivergenceReporter (feature/diagnostico/remote), disparado
--     em paralelo pelo RemoteDiagnosticRepository.evaluateShadow (GH#1444).
--
-- Classificacao e comparacao acontecem no cliente (Android) — esta tabela so
-- armazena o registro ja classificado e sanitizado (nunca o relatorio local
-- completo, nunca dado sensivel — ver DiagnosticSnapshotMapper.kt).
--
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/007_gh1444_diagnostic_divergences.sql --remote

CREATE TABLE IF NOT EXISTS diagnostic_divergences (
  id TEXT PRIMARY KEY,
  execution_id TEXT NOT NULL,
  snapshot_hash TEXT NOT NULL,
  classification TEXT NOT NULL,
  local_status TEXT,
  remote_status TEXT,
  local_score INTEGER,
  remote_score INTEGER,
  local_flow TEXT,
  remote_flow TEXT,
  local_source TEXT,
  remote_source TEXT,
  local_duration_ms INTEGER,
  remote_duration_ms INTEGER,
  app_version TEXT,
  version_code INTEGER,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_diagnostic_divergences_classification
  ON diagnostic_divergences(classification);

CREATE INDEX IF NOT EXISTS idx_diagnostic_divergences_created_at
  ON diagnostic_divergences(created_at);
