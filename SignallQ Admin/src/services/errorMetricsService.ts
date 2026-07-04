import { apiClient } from "./apiClient";
import { SystemError } from "../types/errors";
import { DashboardFilters } from "./adminMetricsService";
import { InfraAlert, AiAlert, ErrorMetricSummary, ErrorByEndpointEntry } from "../mocks/errors.mock";

export const errorMetricsService = {
  async getSystemErrors(_filters: DashboardFilters & { search?: string } = {}): Promise<SystemError[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];

      const period = _filters.period === "today" ? "1d" : (_filters.period ?? "30d");
      const env = _filters.environment ?? "production";
      try {
        const raw = await apiClient.request<{ errors: Array<{
          id: string;
          source: string;
          category?: string;
          message: string;
          stackTrace: string;
          count: number;
          timestamp: string;
          affectedUserCount: number;
          resolved?: boolean;
          resolvedBy?: string;
          resolvedAt?: string | null;
          resolutionNote?: string;
        }> }>("GET", `/admin/metrics/errors?environment=${env}&period=${period}`);

        let results = (raw.errors ?? []).map((r): SystemError => ({
          id:               r.id,
          timestamp:        r.timestamp,
          source:           r.source,
          category:         (r.category as SystemError["category"]) ?? "backend",
          message:          r.message,
          stackTrace:       r.stackTrace ?? '',
          count:            r.count      ?? 1,
          environment:      "production",
          resolved:         r.resolved ?? false,
          affectedUserCount: r.affectedUserCount ?? 0,
          resolvedBy:       r.resolvedBy ?? '',
          resolvedAt:       r.resolvedAt ?? null,
          resolutionNote:   r.resolutionNote ?? '',
        }));

        if (_filters.search) {
          const q = _filters.search.toLowerCase();
          results = results.filter(e =>
            e.id.toLowerCase().includes(q) ||
            e.message.toLowerCase().includes(q) ||
            e.source.toLowerCase().includes(q) ||
            e.stackTrace.toLowerCase().includes(q)
          );
        }

        return results;
      } catch {
        return [];
      }
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
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return null;
      try {
        const period = filters.period === "today" ? "1d" : (filters.period ?? "30d");
        const env = filters.environment ?? "production";
        const raw = await apiClient.request<{ errors: Array<{
          source: string;
          count: number;
          timestamp: string;
        }> }>("GET", `/admin/metrics/errors?environment=${env}&period=${period}`);

        const errors = raw.errors ?? [];
        const activeErrors = errors.length;
        const events24h = errors.reduce((sum, e) => sum + (e.count ?? 0), 0);
        const sources = [...new Set(errors.map(e => e.source))];

        return {
          activeErrors: String(activeErrors),
          events24h: String(events24h),
          // affectedUserCount não é rastreado no D1 (sem PII) — exibe 0
          impactedUsers: "0",
          mainSources: sources.slice(0, 3).join(", ") || "—",
        };
      } catch {
        return null;
      }
    }
    const { mockErrorMetricSummary } = await import("../mocks/errors.mock");
    const env = (filters.environment === "staging" ? "staging" : "production") as "production" | "staging";
    return apiClient.simulateFetch(mockErrorMetricSummary[env], filters);
  },

  async getErrorByEndpoint(filters: DashboardFilters = {}): Promise<ErrorByEndpointEntry[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      try {
        const period = filters.period === "today" ? "1d" : (filters.period ?? "30d");
        const env = filters.environment ?? "production";
        const raw = await apiClient.request<{ errors: Array<{
          source: string;
          count: number;
        }> }>("GET", `/admin/metrics/errors?environment=${env}&period=${period}`);

        // Agrupa contagens por source para gerar série de barras
        const bySource: Record<string, number> = {};
        for (const e of (raw.errors ?? [])) {
          bySource[e.source] = (bySource[e.source] ?? 0) + (e.count ?? 1);
        }

        return Object.entries(bySource)
          .sort(([, a], [, b]) => b - a)
          .map(([name, erros]) => ({ name, erros }));
      } catch {
        return [];
      }
    }
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
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return { alerts: [], aiCostCeiling: 200 };
      try {
        const raw = await apiClient.request<{
          items: Array<{
            id: string;
            type: string;
            severity: string;
            title: string;
            message: string;
            created_at: number;
            timestamp: string;
            resolved: boolean;
          }>;
        }>("GET", "/admin/alerts");

        const alerts: AiAlert[] = (raw.items ?? [])
          .filter((r) => !r.resolved)
          .map((r) => ({
            id:          r.id,
            type:        (r.severity === "critical" ? "critical" : r.severity === "warning" ? "warning" : "info") as AiAlert["type"],
            title:       r.title,
            description: r.message,
            timestamp:   r.timestamp,
          }));

        return { alerts, aiCostCeiling: 200 };
      } catch {
        return { alerts: [], aiCostCeiling: 200 };
      }
    }
    const { mockAiAlerts } = await import("../mocks/errors.mock");
    const alerts = await apiClient.simulateFetch(mockAiAlerts, _filters);
    return { alerts, aiCostCeiling: 200 };
  },

  /**
   * Resolve um erro do sistema (worker/backend/IA/integração ou erro real do
   * app) via POST /admin/errors/:id/resolve (GH#422). O responsável é derivado
   * da sessão autenticada no worker; aqui só enviamos a observação opcional.
   */
  async resolveError(errorId: string, note?: string): Promise<{ success: boolean; message: string; resolvedBy?: string; resolvedAt?: string }> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return { success: false, message: "API não configurada" };
      try {
        const res = await apiClient.request<{ ok: boolean; id: string; resolvedBy: string; resolvedAt: string }>(
          "POST",
          `/admin/errors/${encodeURIComponent(errorId)}/resolve`,
          { note: note ?? '' }
        );
        return {
          success: true,
          message: `Erro ${errorId} marcado como resolvido por ${res.resolvedBy}.`,
          resolvedBy: res.resolvedBy,
          resolvedAt: res.resolvedAt,
        };
      } catch {
        return { success: false, message: "Falha ao comunicar resolução com o worker." };
      }
    }
    console.log(`[ApiClient Dispatch] Triggering remote error resolution for id: ${errorId}`);
    return {
      success: true,
      message: `Erro de ID ${errorId} marcado como resolvido com sucesso no banco principal.`,
      resolvedBy: "voce@signallq.io",
      resolvedAt: new Date().toISOString(),
    };
  },

  /**
   * Resolve um alerta ativo via POST /admin/alerts/:id/resolve.
   * Disponível em produção (SIG-133).
   */
  async resolveAlert(alertId: string): Promise<{ success: boolean; message: string }> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return { success: false, message: "API não configurada" };
      try {
        await apiClient.request<{ ok: boolean }>("POST", `/admin/alerts/${alertId}/resolve`);
        return { success: true, message: "Alerta resolvido." };
      } catch {
        return { success: false, message: "Erro ao resolver alerta." };
      }
    }
    return { success: true, message: `Alerta ${alertId} resolvido (mock).` };
  }
};
