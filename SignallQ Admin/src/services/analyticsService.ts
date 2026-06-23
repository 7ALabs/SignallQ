import { apiClient } from "./apiClient";
import { mockSystemErrors, mockAppVersions } from "../mocks/errors.mock";
import { SystemErrorLog, AppVersionDetail, AppEnvironment } from "../types/admin";
import { DashboardFilters } from "./adminMetricsService";

export const analyticsService = {
  /**
   * Returns active system error metrics from Android SDK, DB pools, or Edge Cloudflare Worker API
   */
  async getErrorMetrics(filters: DashboardFilters = {}): Promise<SystemErrorLog[]> {
    // Sem endpoint no worker — retorna vazio em produção.
    if (!apiClient.isMockEnabled()) return [];

    const list = await apiClient.simulateFetch(mockSystemErrors, filters);

    let filtered = list;
    if (filters.environment) {
      filtered = filtered.filter(e => e.environment === filters.environment);
    }
    return filtered;
  },

  /**
   * Returns live app deployment rollouts, codes, release note guidelines, and crash ratios
   */
  async getAppVersionMetrics(filters: { search?: string; environment?: AppEnvironment } = {}): Promise<AppVersionDetail[]> {
    // Sem endpoint no worker — retorna vazio em produção.
    if (!apiClient.isMockEnabled()) return [];

    const list = await apiClient.simulateFetch(mockAppVersions, filters);
    
    let processed = list;
    if (filters.environment === "staging") {
      processed = list.map(v => {
        if (v.status === "planned") return v;
        return {
          ...v,
          activeInstallsCount: Math.round(v.activeInstallsCount * 0.12),
          diagnosticsCount: v.diagnosticsCount ? Math.round(v.diagnosticsCount * 0.12) : 0,
          crashesCount: v.crashesCount ? Math.round(v.crashesCount * 0.10) : 0,
          anrsCount: v.anrsCount ? Math.round(v.anrsCount * 0.10) : 0,
        };
      });
    }

    if (filters.search) {
      const q = filters.search.toLowerCase();
      return processed.filter(v => 
        v.versionCode.toLowerCase().includes(q) || 
        v.notes.toLowerCase().includes(q) ||
        v.status.toLowerCase().includes(q)
      );
    }
    return processed;
  },

};
