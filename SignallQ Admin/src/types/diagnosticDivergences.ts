// Refs #1446 (shadow mode) — tipos espelham GET /admin/diagnostic/divergences do
// signallq-diagnostic-worker (proxied via signallq-admin-worker), schema
// DiagnosticDivergenceRecord/DiagnosticDivergenceInput em signallq-diagnostic-worker.yaml.
// Registro já classificado no cliente Android (DiagnosticDivergenceClassifier) — o
// worker nunca recebe o DiagnosticReportPayload completo, só este resumo sanitizado.

export type DivergenceClassification =
  | "EXACT_MATCH"
  | "EQUIVALENT_RESULT"
  | "MINOR_DIVERGENCE"
  | "MAJOR_DIVERGENCE"
  | "REMOTE_ERROR"
  | "LOCAL_ERROR";

export const DIVERGENCE_CLASSIFICATIONS: DivergenceClassification[] = [
  "EXACT_MATCH",
  "EQUIVALENT_RESULT",
  "MINOR_DIVERGENCE",
  "MAJOR_DIVERGENCE",
  "REMOTE_ERROR",
  "LOCAL_ERROR",
];

export interface DiagnosticDivergenceRecord {
  id: string;
  executionId: string;
  snapshotHash: string;
  classification: DivergenceClassification;
  localStatus: string | null;
  remoteStatus: string | null;
  localScore: number | null;
  remoteScore: number | null;
  localFlow: string | null;
  remoteFlow: string | null;
  localSource: string | null;
  remoteSource: string | null;
  localDurationMs: number | null;
  remoteDurationMs: number | null;
  appVersion: string | null;
  versionCode: number | null;
  createdAt: string;
}

/** Contagem por classification — ex.: { EXACT_MATCH: 12, MAJOR_DIVERGENCE: 1 }. */
export type DiagnosticDivergencesSummary = Partial<Record<DivergenceClassification, number>>;
