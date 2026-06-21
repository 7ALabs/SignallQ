import { apiClient } from "./apiClient";
import {
  OverviewMetricsResponse,
  mockOverviewProdToday,
  mockOverviewProd7d,
  mockOverviewProd30d,
  mockNetworkDistributionList,
  mockTopIssuesList,
  mockRecentAlertsList,
  mockAiProviderUsageList,
  mockTimelineToday,
  mockTimeline7d,
  mockTimeline30d,
  TopIssueItem,
  RecentAlertItem,
  ProviderUsageItem,
  NetworkDistItem,
  mockNetworkDistribution,
  mockOverviewMetrics,
} from "../mocks/overview.mock";
import { mockOperatorsList } from "../mocks/errors.mock";
import { OperatorRecord } from "../types/admin";

export interface DashboardFilters {
  environment?: "production" | "staging";
  period?: "today" | "7d" | "30d" | "custom";
}

export const adminMetricsService = {
  /**
   * Fetches overall aggregated dashboard metrics (Active users, diagnostic totals, latencies, cost)
   */
  async getOverviewMetrics(filters: DashboardFilters = {}): Promise<OverviewMetricsResponse> {
    const period = filters.period || "7d";
    const environment = filters.environment || "production";

    let baseMetrics: OverviewMetricsResponse;
    if (period === "today") {
      baseMetrics = mockOverviewProdToday;
    } else if (period === "30d") {
      baseMetrics = mockOverviewProd30d;
    } else {
      baseMetrics = mockOverviewProd7d;
    }

    if (!apiClient.isMockEnabled()) {
      try {
        const apiPeriod = period === "today" ? "1d" : period;
        const raw = await apiClient.request<{
          totalDiagnostics: number;
          activeSessions: number;
          avgNetworkScore: number;
          aiCallsToday: number;
          aiCostToday: number;
          aiTokensToday: number;
        }>("GET", `/admin/metrics/overview?period=${apiPeriod}`);

        const score = raw.avgNetworkScore ?? 0;
        const verdict = score >= 80 ? "Excelente" : score >= 60 ? "Bom" : score >= 40 ? "Regular" : "Fraco";

        return {
          ...baseMetrics,
          diagnosticsCount: {
            ...baseMetrics.diagnosticsCount,
            value: raw.totalDiagnostics,
            trend: { value: raw.activeSessions, changePercentage: 0, type: "neutral" as const, intervalLabel: `${raw.activeSessions} sessões ativas` },
          },
          activeUsers: {
            ...baseMetrics.activeUsers,
            value: `${score} · ${verdict}`,
            trend: { value: score, changePercentage: 0, type: score >= 60 ? "up" as const : "down" as const, intervalLabel: "score de rede" },
          },
          aiCost: {
            ...baseMetrics.aiCost,
            value: `$${(raw.aiCostToday ?? 0).toFixed(6)}`,
            trend: { value: raw.aiCallsToday, changePercentage: 0, type: "neutral" as const, intervalLabel: `${raw.aiCallsToday} chamadas hoje · ${raw.aiTokensToday} tokens` },
          },
        };
      } catch (e) {
        console.warn("[adminMetricsService] real API falhou, usando mock", e);
      }
    }

    // Simulate API fetch delay
    const response = await apiClient.simulateFetch(baseMetrics, filters);

    // Dynamic filtering for environment
    if (environment === "staging") {
      // Scale down staging numbers
      return {
        diagnosticsCount: {
          label: response.diagnosticsCount.label,
          value: Math.round(Number(response.diagnosticsCount.value) * 0.12),
          trend: response.diagnosticsCount.trend ? { ...response.diagnosticsCount.trend, value: 5.1 } : undefined,
        },
        activeUsers: {
          label: response.activeUsers.label,
          value: Math.round(Number(response.activeUsers.value) * 0.15),
          trend: response.activeUsers.trend ? { ...response.activeUsers.trend, value: 4.2 } : undefined,
        },
        successRate: {
          label: response.successRate.label,
          value: "95,1%",
          trend: { value: 0.1, changePercentage: 0.1, type: "down", intervalLabel: "instabilidade em staging" },
        },
        aiCost: {
          label: response.aiCost.label,
          value: typeof response.aiCost.value === "string" 
            ? `R$ ${(parseFloat(response.aiCost.value.replace("R$ ", "").replace(",", ".")) * 0.15).toFixed(2).replace(".", ",")}`
            : Number(response.aiCost.value) * 0.15,
          trend: response.aiCost.trend ? { ...response.aiCost.trend, value: 1.2 } : undefined,
        },
        topProblem: {
          label: response.topProblem.label,
          value: "DNS lento",
          trend: { value: 34, changePercentage: 34, type: "neutral", intervalLabel: "34% dos relatos staging" },
        },
        mostTestType: {
          label: response.mostTestType.label,
          value: "Rede móvel · 51%",
          trend: { value: 51, changePercentage: 51, type: "neutral", intervalLabel: "rede predominante" },
        },
        downloadsToday: {
          label: response.downloadsToday.label,
          value: Math.round(Number(response.downloadsToday.value) * 0.1),
          trend: response.downloadsToday.trend ? { ...response.downloadsToday.trend, value: 2.1 } : undefined,
        },
        activeInstalls: {
          label: response.activeInstalls.label,
          value: Math.round(Number(response.activeInstalls.value) * 0.1),
          trend: response.activeInstalls.trend ? { ...response.activeInstalls.trend, value: 0.5 } : undefined,
        },
        crashFreeUsers: {
          label: response.crashFreeUsers.label,
          value: "98,9%",
          trend: { value: 0.1, changePercentage: 0.1, type: "down", intervalLabel: "Crashlytics" },
        },
        prodVersion: {
          label: response.prodVersion.label,
          value: "0.18.1-stg",
          trend: response.prodVersion.trend ? { ...response.prodVersion.trend, value: 100 } : undefined,
        },
      };
    }

    return response;
  },

  /**
   * Fetches network connectivity shares (WIFI, Cellular, Fiber, etc.)
   */
  async getNetworkInsights(filters: DashboardFilters = {}): Promise<NetworkDistItem[]> {
    const environment = filters.environment || "production";
    const response = await apiClient.simulateFetch(mockNetworkDistributionList, filters);

    if (environment === "staging") {
      // Provide slightly different proportions for testing staging
      return [
        { name: "Wi-Fi", value: 35, color: "#6C2BFF" },
        { name: "Rede móvel", value: 51, color: "#22C55E" },
        { name: "Fibra", value: 11, color: "#38BDF8" },
        { name: "Ethernet", value: 3, color: "#F5A623" },
      ];
    }
    return response;
  },

  /**
   * Fetches the diagnostics historical timeline points for chart rendering
   */
  async getDiagnosticsTimeline(filters: DashboardFilters = {}): Promise<any[]> {
    const period = filters.period || "7d";
    let baseTimeline: any[] = mockTimeline7d;

    if (period === "today") {
      baseTimeline = mockTimelineToday;
    } else if (period === "30d") {
      baseTimeline = mockTimeline30d;
    }

    const response = await apiClient.simulateFetch(baseTimeline, filters);

    if (filters.environment === "staging") {
      // Scale down numerical counts for staging timeline
      return response.map((item) => ({
        ...item,
        completedDiagnostics: Math.round(item.completedDiagnostics * 0.15),
        activeUsers: Math.round(item.activeUsers * 0.15),
        criticalAlerts: Math.max(0, item.criticalAlerts - 1),
      }));
    }

    return response;
  },

  /**
   * Fetches the list of most common network problems
   */
  async getTopIssues(filters: DashboardFilters = {}): Promise<TopIssueItem[]> {
    const response = await apiClient.simulateFetch(mockTopIssuesList, filters);

    if (filters.environment === "staging") {
      // Sort or scale differently in staging
      return [
        { id: "issue_3", problem: "DNS lento", count: 48, percentage: 34 },
        { id: "issue_1", problem: "Wi-Fi fraco", count: 42, percentage: 30 },
        { id: "issue_2", problem: "Bufferbloat upload", count: 21, percentage: 15 },
        { id: "issue_4", problem: "Rede móvel congestionada", count: 18, percentage: 13 },
        { id: "issue_5", problem: "Gateway lento", count: 11, percentage: 8 },
      ];
    }

    return response;
  },

  /**
   * Fetches recent active critical/warn log alerts
   */
  async getRecentAlerts(filters: DashboardFilters = {}): Promise<RecentAlertItem[]> {
    const response = await apiClient.simulateFetch(mockRecentAlertsList, filters);

    if (filters.environment === "staging") {
      // In staging we might have fewer or different alert counts
      return response.map((alert) => ({
        ...alert,
        count: Math.max(2, Math.round(alert.count * 0.3)),
      }));
    }

    return response;
  },

  /**
   * Fetches AI provider allocation shares and context totals
   */
  async getAiProviderUsage(filters: DashboardFilters = {}): Promise<ProviderUsageItem[]> {
    const response = await apiClient.simulateFetch(mockAiProviderUsageList, filters);

    if (filters.environment === "staging") {
      // Different distribution of LLM calls in staging testing
      return [
        { name: "Gemini Flash", percentage: 70, tokensProcessed: 245000, color: "#6C2BFF" },
        { name: "Cloudflare Qwen", percentage: 25, tokensProcessed: 87500, color: "#38BDF8" },
        { name: "Fallback local", percentage: 5, tokensProcessed: 17500, color: "#6B7280" },
      ];
    }

    return response;
  },

  /**
   * Fetches raw physical performance metrics comparing network interfaces (latencies, channel interference, physical attenuation)
   */
  async getNetworkSpecs(filters: DashboardFilters = {}): Promise<{
    summaryStats: {
      wifiCount: number;
      cellCount: number;
      attenuationRate: number;
    };
    physicalAverages: Array<{
      medium: string;
      averageLatency: number;
      packetLoss: number;
      interference: number;
    }>;
  }> {
    const isStg = filters.environment === "staging";
    
    const summaryStats = {
      wifiCount: isStg ? 650 : 8120,
      cellCount: isStg ? 45 : 384,
      attenuationRate: isStg ? 16.8 : 14.1,
    };

    const physicalAverages = [
      { medium: "Wi-Fi 2.4G", averageLatency: isStg ? 49 : 45, packetLoss: isStg ? 2.1 : 1.8, interference: isStg ? 82 : 78 },
      { medium: "Wi-Fi 5G", averageLatency: isStg ? 17 : 14, packetLoss: isStg ? 0.6 : 0.5, interference: isStg ? 25 : 22 },
      { medium: "Rede Móvel 4G", averageLatency: isStg ? 58 : 52, packetLoss: isStg ? 2.5 : 2.1, interference: isStg ? 59 : 54 },
      { medium: "Rede Móvel 5G", averageLatency: isStg ? 32 : 28, packetLoss: isStg ? 1.0 : 0.8, interference: isStg ? 12 : 10 },
      { medium: "Fibra Optica", averageLatency: isStg ? 5 : 4, packetLoss: 0.05, interference: 1 },
      { medium: "Ethernet Cabo", averageLatency: isStg ? 3 : 2, packetLoss: 0.01, interference: 0 },
    ];

    return apiClient.simulateFetch({ summaryStats, physicalAverages }, filters);
  },

  /**
   * Fetches latency and throughput averages from telecom carrier groups
   */
  async getOperatorMetrics(filters: DashboardFilters = {}): Promise<OperatorRecord[]> {
    const list = await apiClient.simulateFetch(mockOperatorsList, filters);

    if (filters.environment) {
      console.log(`Filtering operator stats for environment: ${filters.environment}`);
    }
    return list;
  },
};
