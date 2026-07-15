-- GH#952
-- Diagnostic rulesets, publish lifecycle and divergence tracking for the
-- remote deterministic engine.
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/001_gh952_diagnostic_rules.sql --remote

CREATE TABLE IF NOT EXISTS diagnostic_rulesets (
  version INTEGER PRIMARY KEY,
  schema_version INTEGER NOT NULL,
  engine_version INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'DRAFT',
  rollout_percent INTEGER NOT NULL DEFAULT 0,
  published_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  author TEXT,
  justification TEXT DEFAULT '',
  rules_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_diagnostic_rulesets_status
  ON diagnostic_rulesets(status);

CREATE TABLE IF NOT EXISTS diagnostic_rule_audit_log (
  id TEXT PRIMARY KEY,
  ruleset_version INTEGER NOT NULL,
  action TEXT NOT NULL,
  actor TEXT,
  created_at TEXT NOT NULL,
  details_json TEXT NOT NULL,
  FOREIGN KEY (ruleset_version) REFERENCES diagnostic_rulesets(version)
);

CREATE INDEX IF NOT EXISTS idx_diagnostic_rule_audit_log_ruleset
  ON diagnostic_rule_audit_log(ruleset_version);

-- GH#961 — tabela `diagnostic_divergences` removida: criada nesta migration
-- mas nunca usada em codigo nenhum (schema morto, sem wiring de nenhuma
-- feature). Worker nunca foi deployado com database_id real (wrangler.toml
-- usava placeholder), entao remover aqui e seguro — nao ha dado em producao
-- pra migrar. Se um dia existir necessidade real de registrar divergencia
-- local-vs-remoto, recriar via nova migration com o wiring completo (endpoint
-- + call site), nao so o schema solto.
