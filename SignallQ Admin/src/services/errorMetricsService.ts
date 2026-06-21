import { apiClient } from "./apiClient";
import { mockSystemErrors } from "../mocks/errors.mock";
import { SystemError } from "../types/errors";
import { DashboardFilters } from "./adminMetricsService";

export const errorMetricsService = {
  /**
   * Retrieves fine-grained telemetry logging for errors occurring in the gateways, databases, and apps
   */
  async getSystemErrors(filters: DashboardFilters & { search?: string } = {}): Promise<SystemError[]> {
    const list = await apiClient.simulateFetch(mockSystemErrors, filters) as unknown as SystemError[];
    
    let filtered = list;

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

  /**
   * Simulates resolving an active system outage or error code in the control panel
   */
  async resolveError(errorId: string): Promise<{ success: boolean; message: string }> {
    console.log(`[ApiClient Dispatch] Triggering remote error resolution for id: ${errorId}`);
    await apiClient.request("POST", `/errors/resolve`, { errorId });
    return {
      success: true,
      message: `Erro de ID ${errorId} marcado como resolvido com sucesso no banco principal.`
    };
  }
};
