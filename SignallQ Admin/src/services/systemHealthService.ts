import { apiClient } from "./apiClient";
import { mockSystemHealth } from "../mocks/systemHealth.mock";

export type HealthStatus = "ok" | "error" | "not_configured" | "idle";

export interface HealthCheckResult {
  status: HealthStatus;
  latencyMs?: number;
  message?: string;
}

export interface IngestHealthResult extends HealthCheckResult {
  keyConfigured: boolean;
  lastSuccessAt: string | null;
}

export interface SystemHealthEvent {
  source: string;
  message?: string;
  timestamp: string;
}

export interface SystemHealthResponse {
  source: string;
  timestamp: string;
  checks: {
    worker: HealthCheckResult;
    d1: HealthCheckResult;
    firebaseCredentials: HealthCheckResult;
    bigQuery: HealthCheckResult;
    ingest: IngestHealthResult;
  };
  lastFailure: SystemHealthEvent | null;
  lastSuccess: SystemHealthEvent | null;
}

// GH#425 — cada status vem de uma verificação real do worker (/admin/system-health):
// D1 roda "SELECT 1", Firebase autentica um JWT real, BigQuery faz uma query de teste,
// ingest confere o último diagnóstico persistido. Sem mock silencioso em produção.
export const systemHealthService = {
  async getSystemHealth(): Promise<{ data: SystemHealthResponse; clientLatencyMs: number | null }> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      const data = await apiClient.simulateFetch(mockSystemHealth, { endpoint: "system-health" });
      return { data, clientLatencyMs: null };
    }

    const start = Date.now();
    const data = await apiClient.request<SystemHealthResponse>("GET", "/admin/system-health");
    return { data, clientLatencyMs: Date.now() - start };
  },
};
