// Refs #1446 — tipos espelham o contrato real do signallq-diagnostic-worker
// (integrations/cloudflare/signallq-diagnostic-worker/src/contracts.ts e
// ruleset-store.ts). Nenhum campo aqui é inventado: o que o worker não expõe
// (ex.: histórico completo de auditoria por versão) simplesmente não tem tipo.

export type RulesetStatus = "DRAFT" | "PUBLISHED" | "ROLLED_BACK" | "BUNDLED_LOCAL";

export type DiagnosticFieldOperator =
  | "EQ" | "NEQ" | "GT" | "GTE" | "LT" | "LTE" | "IN" | "NOT_IN" | "EXISTS" | "NOT_EXISTS" | "BETWEEN";

export type DiagnosticGroupOperator = "ALL" | "ANY" | "NONE";

export interface DiagnosticCondition {
  field: string;
  operator: DiagnosticFieldOperator;
  value?: unknown;
}

export interface DiagnosticConditionGroup {
  operator: DiagnosticGroupOperator;
  conditions: DiagnosticCondition[];
}

export interface DiagnosticRuleResult {
  findingCode: string;
  category: "internet" | "wifi" | "dns" | "fibra" | "mobile" | "historico" | "wifi-canal" | "decisao";
  severity: "INFO" | "WARNING" | "ERROR";
  confidence: "LOW" | "MEDIUM" | "HIGH";
  recommendationId: string;
}

export interface DiagnosticRule {
  ruleId: string;
  ruleVersion: number;
  enabled: boolean;
  priority: number;
  minimumSchemaVersion: number;
  conditionGroup?: DiagnosticConditionGroup;
  conditions?: DiagnosticCondition[];
  result: DiagnosticRuleResult;
}

/** Corpo completo de um ruleset — é isto que o editor JSON edita. */
export interface DiagnosticRuleset {
  version: number;
  schemaVersion: number;
  engineVersion: number;
  publishedAt: string;
  rules: DiagnosticRule[];
}

/** Linha de `GET /admin/diagnostic/rulesets` — sem o corpo de regras. */
export interface RulesetListItem {
  version: number;
  schemaVersion: number;
  engineVersion: number;
  status: RulesetStatus;
  rolloutPercent: number;
  publishedAt: string | null;
  updatedAt: string | null;
}

/** `GET /admin/diagnostic/rulesets/:version` — inclui autor/justificativa/corpo. */
export interface RulesetDetail extends RulesetListItem {
  author: string | null;
  justification: string | null;
  ruleset: DiagnosticRuleset;
}

export interface RulesetValidationResult {
  ok: boolean;
  version?: number;
  rules?: number;
  errors?: string[];
}

// Formato de DiagnosticSnapshot usado pelas fixtures de simulação — subconjunto
// prático do contrato completo (integrations/.../contracts.ts), o bastante para
// exercitar os campos que as regras de diagnóstico hoje consultam.
export interface DiagnosticSnapshotFixture {
  id: string;
  label: string;
  description: string;
  snapshot: Record<string, unknown>;
}

export interface SimulateResult {
  ok: boolean;
  result?: {
    overallStatus: string;
    score: number;
    confidence: string;
    matchedRules: string[];
    findings: Array<{ findingCode: string; category: string; severity: string; confidence: string }>;
    humanSummary: string;
    primaryFlow: string;
  };
  errors?: string[];
}
