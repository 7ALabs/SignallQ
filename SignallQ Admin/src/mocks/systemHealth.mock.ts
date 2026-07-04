import { SystemHealthResponse } from "../services/systemHealthService";

// GH#425 — mock reflete um estado plausível de painel em produção real, não perfeito:
// BigQuery ainda não configurado (Crashlytics export pendente) é o estado comum hoje,
// não um "tudo verde" artificial. Ingest com atividade recente (~40min atrás).
const lastIngestAgoMs = 41 * 60 * 1000;
const lastErrorAgoMs = 6 * 60 * 60 * 1000;

export const mockSystemHealth: SystemHealthResponse = {
  source: "worker",
  timestamp: new Date().toISOString(),
  checks: {
    worker: { status: "ok" },
    d1: { status: "ok", latencyMs: 38 },
    firebaseCredentials: { status: "ok", latencyMs: 210 },
    bigQuery: {
      status: "not_configured",
      message: "Requer credenciais Firebase válidas para autenticar no BigQuery.",
    },
    ingest: {
      status: "ok",
      keyConfigured: true,
      lastSuccessAt: new Date(Date.now() - lastIngestAgoMs).toISOString(),
    },
  },
  lastFailure: {
    source: "bigquery-crashlytics",
    message: "table_not_found",
    timestamp: new Date(Date.now() - lastErrorAgoMs).toISOString(),
  },
  lastSuccess: {
    source: "ingest",
    timestamp: new Date(Date.now() - lastIngestAgoMs).toISOString(),
  },
};
