import { apiClient } from "./apiClient";
import { SystemError } from "../types/errors";
import { DashboardFilters } from "./adminMetricsService";
import { InfraAlert, AiAlert, ErrorMetricSummary, ErrorByEndpointEntry } from "../mocks/errors.mock";

export const errorMetricsService = {
  /**
   * Retrieves fine-grained telemetry logging for errors occurring in the gateways, databases, and apps.
   * Sem rota real no worker — retorna [] em produção.
   */
  async getSystemErrors(_filters: DashboardFilters & { search?: string } = {}): Promise<SystemError[]> {
    if (!apiClient.isMockEnabled()) {
      return [];
    }

    // Modo mock: importação dinâmica para não incluir dados mock no bundle de produção
    const { mockSystemErrors } = await import("../mocks/errors.mock");
    const filters = _filters;
    let filtered = JSON.parse(JSON.stringify(mockSystemErrors)) as SystemError[];

    if (filters.environment) {
      filtered = filtered.filter(e => e.environment === filters.environment);
    }

    if (filters.search) {
      const q = filters.search.toLowerCase();
      filtered = filtered.filter(e =>
        e.id.toLowerCase().includes(q) ||
        e.message.toLowerCase().includes(q) ||
        e.source.toLowerCase().includes(q) ||
        e.stackTrace.toLowerCase().includes(q)
      );
    }

    return filtered;
  },

  async getErrorMetricSummary(filters: DashboardFilters = {}): Promise<ErrorMetricSummary | null> {
    if (!apiClient.isMockEnabled()) return null;
    const { mockErrorMetricSummary } = await import("../mocks/errors.mock");
    const env = (filters.environment === "staging" ? "staging" : "production") as "production" | "staging";
    return apiClient.simulateFetch(mockErrorMetricSummary[env], filters);
  },

  async getErrorByEndpoint(filters: DashboardFilters = {}): Promise<ErrorByEndpointEntry[]> {
    if (!apiClient.isMockEnabled()) return [];
    const { mockErrorByEndpoint } = await import("../mocks/errors.mock");
    const env = (filters.environment === "staging" ? "staging" : "production") as "production" | "staging";
    return apiClient.simulateFetch(mockErrorByEndpoint[env], filters);
  },

  async getInfraAlerts(_filters: DashboardFilters = {}): Promise<InfraAlert[]> {
    if (!apiClient.isMockEnabled()) return [];
    const { mockInfraAlerts } = await import("../mocks/errors.mock");
    return apiClient.simulateFetch(mockInfraAlerts, _filters);
  },

  async getAiAlerts(_filters: DashboardFilters = {}): Promise<{ alerts: AiAlert[]; aiCostCeiling: number }> {
    if (!apiClient.isMockEnabled()) return { alerts: [], aiCostCeiling: 200 };
    const { mockAiAlerts } = await import("../mocks/errors.mock");
    const alerts = await apiClient.simulateFetch(mockAiAlerts, _filters);
    return { alerts, aiCostCeiling: 200 };
  },

  /**
   * Simulates resolving an active system outage or error code in the control panel.
   * A rota /errors/resolve não existe no worker — retorna resposta estática em produção.
   */
  async resolveError(errorId: string): Promise<{ success: boolean; message: string }> {
    if (!apiClient.isMockEnabled()) {
      return { success: false, message: "Em implementação" };
    }
    console.log(`[ApiClient Dispatch] Triggering remote error resolution for id: ${errorId}`);
    return {
      success: true,
      message: `Erro de ID ${errorId} marcado como resolvido com sucesso no banco principal.`
    };
  }
};
