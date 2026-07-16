import { apiClient } from "./apiClient";
import { formatCurrency } from "../utils/format";
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
} from "../mocks/overview.mock";
import { mockOperatorsList } from "../mocks/errors.mock";
import { AppEnvironment, OperatorRecord } from "../types/admin";
import { SQ_TOKENS } from "../config/designTokens";
import { categoryForAlertType } from "../utils/alerts";

// GH#427: métricas por network_type — todas derivadas de colunas reais de
// diagnostic_sessions (ver "SignallQ Admin/docs/architecture/data-architecture.md").
export interface NetworkTypeStat {
  name: string;
  count: number;
  avgScore: number | null;
  avgDownloadMbps: number | null;
  avgUploadMbps: number | null;
  avgLatencyMs: number | null;
  avgJitterMs: number | null;
  avgPacketLoss: number | null;
}

export interface DashboardFilters {
  environment?: AppEnvironment;
  // period é string: o seletor global do App usa string e os services tratam
  // os valores conhecidos ("today"/"7d"/"30d"/"1d") por comparação.
  period?: string;
}

export const adminMetricsService = {
  async getOverviewMetrics(filters: DashboardFilters = {}): Promise<OverviewMetricsResponse | null> {
    const period = filters.period || "7d";

    if (!apiClient.isMockEnabled()) {
      // Sem baseUrl configurada, retorna null em vez de lançar.
      if (!(import.meta.env.VITE_ADMIN_API_BASE_URL)) return null;

      try {
        const apiPeriod = period === "today" ? "1d" : period;
        const env = filters.environment ?? "production";
        const raw = await apiClient.request<{
          totalDiagnostics: number;
          activeSessions: number;
          avgNetworkScore: number;
          aiCallsToday: number;
          aiCostToday: number;
          aiTokensToday: number;
          successRate?: number | null;
          topProblem?: string | null;
          mostTestType?: string | null;
          mostTestTypePercentage?: number | null;
        }>("GET", `/admin/metrics/overview?environment=${env}&period=${apiPeriod}`);

        const score = raw.avgNetworkScore ?? 0;
        const verdict = score >= 80 ? "Excelente" : score >= 60 ? "Bom" : score >= 40 ? "Regular" : "Fraco";

        const successRateValue = raw.successRate != null
          ? { label: "Taxa de Sucesso", value: `${raw.successRate.toFixed(1)}%`, trend: { value: raw.successRate, changePercentage: 0, type: "neutral" as const, intervalLabel: "sessões com status bom/excelente/regular" } }
          : null;

        const topProblemValue = raw.topProblem != null
          ? { label: "Principal Problema", value: raw.topProblem, trend: { value: 0, changePercentage: 0, type: "neutral" as const, intervalLabel: "issue mais frequente no período" } }
          : null;

        const mostTestTypeValue = raw.mostTestType != null
          ? { label: "Tipo de Rede Predominante", value: raw.mostTestTypePercentage != null ? `${raw.mostTestType} · ${raw.mostTestTypePercentage.toFixed(0)}%` : raw.mostTestType, trend: { value: raw.mostTestTypePercentage ?? 0, changePercentage: 0, type: "neutral" as const, intervalLabel: "rede predominante" } }
          : null;

        return {
          diagnosticsCount: {
            label: "Diagnósticos",
            value: raw.totalDiagnostics,
            trend: { value: raw.activeSessions, changePercentage: 0, type: "neutral" as const, intervalLabel: `${raw.activeSessions} sessões ativas` },
          },
          activeUsers: {
            label: "Score de Rede",
            value: `${score} · ${verdict}`,
            trend: { value: score, changePercentage: 0, type: score >= 60 ? "up" as const : "down" as const, intervalLabel: "score de rede" },
          },
          aiCost: {
            label: "Custo IA",
            value: formatCurrency(raw.aiCostToday ?? 0),
            trend: { value: raw.aiCallsToday, changePercentage: 0, type: "neutral" as const, intervalLabel: `${raw.aiCallsToday} chamadas hoje · ${raw.aiTokensToday} tokens` },
          },
          successRate: successRateValue,
          topProblem: topProblemValue,
          mostTestType: mostTestTypeValue,
          downloadsToday: null,
          activeInstalls: null,
          crashFreeUsers: null,
          prodVersion: null,
        };
      } catch {
        // Endpoint indisponível no worker — retorna null para exibir estado "sem dados".
        return null;
      }
    }

    let baseMetrics: OverviewMetricsResponse;
    if (period === "today") {
      baseMetrics = mockOverviewProdToday;
    } else if (period === "30d") {
      baseMetrics = mockOverviewProd30d;
    } else {
      baseMetrics = mockOverviewProd7d;
    }

    const response = await apiClient.simulateFetch(baseMetrics, filters);
    const environment = filters.environment || "production";

    if (environment === "staging") {
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

  async getNetworkInsights(filters: DashboardFilters = {}): Promise<NetworkDistItem[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      const period = filters.period || "7d";
      const apiPeriod = period === "today" ? "1d" : period;
      const envNetwork = filters.environment ?? "production";
      // Paleta fixa por nome de tipo de rede — cor não vem do worker (SIG-110).
      const colorMap: Record<string, string> = {
        wifi:     SQ_TOKENS.networkWifi,
        mobile:   SQ_TOKENS.networkMobile,
        cellular: SQ_TOKENS.networkMobile,
        fiber:    SQ_TOKENS.networkFiber,
        ethernet: SQ_TOKENS.networkEthernet,
      };
      function colorFor(name: string): string {
        const key = (name ?? "").toLowerCase();
        for (const [k, c] of Object.entries(colorMap)) {
          if (key.includes(k)) return c;
        }
        return SQ_TOKENS.networkUnknown;
      }
      try {
        const raw = await apiClient.request<{ items: Array<{ name: string; count: number; percentage: number }> }>(
          "GET",
          `/admin/metrics/network?environment=${envNetwork}&period=${apiPeriod}`
        );
        // O worker retorna `count` (nº de sessões) por tipo de rede; o donut calcula
        // o percentual a partir do total. Antes mapeava `item.value` (inexistente no
        // payload), gerando value=undefined e quebrando o render do DonutChart.
        return (raw.items ?? []).map((item) => ({
          name:  item.name,
          value: item.count ?? 0,
          color: colorFor(item.name),
        }));
      } catch {
        return [];
      }
    }

    const environment = filters.environment || "production";
    const response = await apiClient.simulateFetch(mockNetworkDistributionList, filters);

    if (environment === "staging") {
      return [
        { name: "Wi-Fi", value: 35, color: SQ_TOKENS.networkWifi },
        { name: "Rede móvel", value: 51, color: SQ_TOKENS.networkMobile },
        { name: "Fibra", value: 11, color: SQ_TOKENS.networkFiber },
        { name: "Ethernet", value: 3, color: SQ_TOKENS.networkEthernet },
      ];
    }
    return response;
  },

  async getDiagnosticsTimeline(filters: DashboardFilters = {}): Promise<any[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      const period = filters.period || "7d";
      const apiPeriod = period === "today" ? "1d" : period;
      const envTimeline = filters.environment ?? "production";
      try {
        const raw = await apiClient.request<{
          timeline: Array<{ date: string; completedDiagnostics: number; activeUsers: number; criticalAlerts: number }>;
        }>("GET", `/admin/metrics/timeline?environment=${envTimeline}&period=${apiPeriod}`);
        return (raw.timeline ?? []).map((item) => ({
          // Worker retorna 'date' (YYYY-MM-DD); frontend usa 'timestamp' ou 'date' indistintamente
          // — mantém ambos para compatibilidade com os tipos TimeSeriesData existentes.
          timestamp:            item.date,
          date:                 item.date,
          completedDiagnostics: item.completedDiagnostics,
          activeUsers:          item.activeUsers,
          criticalAlerts:       item.criticalAlerts,
        }));
      } catch {
        return [];
      }
    }

    const period = filters.period || "7d";
    let baseTimeline: any[] = mockTimeline7d;
    if (period === "today") baseTimeline = mockTimelineToday;
    else if (period === "30d") baseTimeline = mockTimeline30d;

    const response = await apiClient.simulateFetch(baseTimeline, filters);

    if (filters.environment === "staging") {
      return response.map((item) => ({
        ...item,
        completedDiagnostics: Math.round(item.completedDiagnostics * 0.15),
        activeUsers: Math.round(item.activeUsers * 0.15),
        criticalAlerts: Math.max(0, item.criticalAlerts - 1),
      }));
    }

    return response;
  },

  async getTopIssues(filters: DashboardFilters = {}): Promise<TopIssueItem[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      const period = filters.period || "7d";
      const apiPeriod = period === "today" ? "1d" : period;
      const envTopIssues = filters.environment ?? "production";
      try {
        const raw = await apiClient.request<{ items: TopIssueItem[] }>(
          "GET",
          `/admin/metrics/top-issues?environment=${envTopIssues}&period=${apiPeriod}`
        );
        return raw.items ?? [];
      } catch {
        return [];
      }
    }

    const response = await apiClient.simulateFetch(mockTopIssuesList, filters);

    if (filters.environment === "staging") {
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

  async getRecentAlerts(filters: DashboardFilters = {}): Promise<RecentAlertItem[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      try {
        const raw = await apiClient.request<{
          items: Array<{
            id: string;
            type?: string;
            severity: "critical" | "warning" | "info";
            title?: string;
            message: string;
            created_at: string | number;
            resolved?: boolean;
            count?: number;
          }>;
        }>("GET", `/admin/alerts`);
        return (raw.items ?? []).map((r) => ({
          id:        r.id,
          source:    r.title ?? r.type ?? "Sistema",
          message:   r.message,
          severity:  (r.severity === "info" ? "warning" : r.severity) as "critical" | "warning",
          _severity: r.severity,
          timestamp: typeof r.created_at === "number"
            ? new Date(r.created_at * 1000).toISOString()
            : r.created_at,
          count: r.count ?? 1,
          category: categoryForAlertType(r.type),
        } as RecentAlertItem & { _severity: string }));
      } catch {
        return [];
      }
    }

    const response = await apiClient.simulateFetch(mockRecentAlertsList, filters);

    if (filters.environment === "staging") {
      return response.map((alert) => ({
        ...alert,
        count: Math.max(2, Math.round(alert.count * 0.3)),
      }));
    }

    return response;
  },

  async getAiProviderUsage(filters: DashboardFilters = {}): Promise<ProviderUsageItem[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      const period = filters.period || "7d";
      const apiPeriod = period === "today" ? "1d" : period;
      const envAiProviders = filters.environment ?? "production";
      // Paleta de cores por provedor — não vem do worker (SIG-110).
      const providerColors: Record<string, string> = {
        "Gemini":              SQ_TOKENS.aiGemini,
        "Qwen / Workers AI":   SQ_TOKENS.aiQwen,
        "OpenAI GPT":          SQ_TOKENS.aiOpenAI,
        "Anthropic Claude":    SQ_TOKENS.aiAnthropic,
      };
      function colorFor(name: string): string {
        return providerColors[name] ?? SQ_TOKENS.aiFallback;
      }
      try {
        const raw = await apiClient.request<{
          items: Array<{ name: string; percentage: number; tokensProcessed: number; reliabilityPercentage?: number | null }>;
        }>("GET", `/admin/metrics/ai-providers?environment=${envAiProviders}&period=${apiPeriod}`);
        return (raw.items ?? []).map((item) => ({
          name:                  item.name,
          percentage:            item.percentage,
          tokensProcessed:       item.tokensProcessed,
          color:                 colorFor(item.name),
          reliabilityPercentage: item.reliabilityPercentage ?? null,
        }));
      } catch {
        return [];
      }
    }

    const response = await apiClient.simulateFetch(mockAiProviderUsageList, filters);

    if (filters.environment === "staging") {
      return [
        { name: "Gemini Flash", percentage: 70, tokensProcessed: 245000, color: SQ_TOKENS.aiGemini },
        { name: "Cloudflare Qwen", percentage: 25, tokensProcessed: 87500, color: SQ_TOKENS.aiQwen },
        { name: "Fallback local", percentage: 5, tokensProcessed: 17500, color: SQ_TOKENS.aiFallback },
      ];
    }

    return response;
  },

  // GH#427: substitui getNetworkSpecs (dados 100% inventados — contagem de
  // "torres"/"SSIDs" e índice de "interferência" que o app nunca mede). Consome
  // o mesmo endpoint real do donut (/admin/metrics/network), mas expõe os campos
  // agregados por network_type que o Worker calcula a partir de colunas reais de
  // diagnostic_sessions (download/upload/latência/jitter/perda de pacote/score).
  async getNetworkTypeStats(filters: DashboardFilters = {}): Promise<NetworkTypeStat[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      const period = filters.period || "7d";
      const apiPeriod = period === "today" ? "1d" : period;
      const envNetwork = filters.environment ?? "production";
      try {
        const raw = await apiClient.request<{
          items: Array<{
            name: string;
            count: number;
            avg_score: number | null;
            avg_download_mbps: number | null;
            avg_upload_mbps: number | null;
            avg_latency_ms: number | null;
            avg_jitter_ms: number | null;
            avg_packet_loss: number | null;
          }>;
        }>("GET", `/admin/metrics/network?environment=${envNetwork}&period=${apiPeriod}`);
        return (raw.items ?? []).map((item) => ({
          name:            item.name,
          count:           item.count ?? 0,
          avgScore:        item.avg_score ?? null,
          avgDownloadMbps: item.avg_download_mbps ?? null,
          avgUploadMbps:   item.avg_upload_mbps ?? null,
          avgLatencyMs:    item.avg_latency_ms ?? null,
          avgJitterMs:     item.avg_jitter_ms ?? null,
          avgPacketLoss:   item.avg_packet_loss ?? null,
        }));
      } catch {
        return [];
      }
    }

    const isStg = filters.environment === "staging";
    const mock: NetworkTypeStat[] = [
      { name: "Wi-Fi",      count: isStg ? 210 : 1840, avgScore: isStg ? 78 : 81, avgDownloadMbps: isStg ? 62.4 : 74.8,  avgUploadMbps: isStg ? 18.1 : 21.6, avgLatencyMs: isStg ? 24 : 19, avgJitterMs: isStg ? 6.2 : 4.8,  avgPacketLoss: isStg ? 1.1 : 0.7 },
      { name: "Rede móvel", count: isStg ? 96  : 640,  avgScore: isStg ? 64 : 68, avgDownloadMbps: isStg ? 28.9 : 33.5,  avgUploadMbps: isStg ? 8.4  : 9.7,  avgLatencyMs: isStg ? 58 : 49, avgJitterMs: isStg ? 14.3 : 11.5, avgPacketLoss: isStg ? 2.8 : 2.1 },
      { name: "Fibra",      count: isStg ? 28  : 205,  avgScore: isStg ? 91 : 93, avgDownloadMbps: isStg ? 210.5 : 265.2, avgUploadMbps: isStg ? 98.3 : 112.6, avgLatencyMs: isStg ? 9 : 6,  avgJitterMs: isStg ? 1.4 : 0.9,  avgPacketLoss: isStg ? 0.1 : 0.05 },
      { name: "Ethernet",   count: isStg ? 5   : 34,   avgScore: isStg ? 95 : 96, avgDownloadMbps: isStg ? 320.1 : 401.7, avgUploadMbps: isStg ? 180.4 : 220.9, avgLatencyMs: isStg ? 4 : 3, avgJitterMs: isStg ? 0.6 : 0.4,  avgPacketLoss: isStg ? 0.03 : 0.02 },
    ];
    return apiClient.simulateFetch(mock, filters);
  },

  async getOperatorMetrics(filters: DashboardFilters = {}): Promise<OperatorRecord[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];

      const period = filters.period === "today" ? "1d" : (filters.period ?? "30d");
      const envOperators = filters.environment ?? "production";
      try {
        const raw = await apiClient.request<{ operators: Array<{
          operator: string;
          total_diagnostics: number;
          avg_score: number | null;
          avg_download: number | null;
          avg_upload: number | null;
          avg_latency: number | null;
          packetLossAverage: number | null;
          type: string | null;
          completed: number;
          resolved: number;
        }> }>("GET", `/admin/metrics/operators?environment=${envOperators}&period=${period}`);

        // GH#757 — avg_download/avg_upload vêm crus do D1 (ex.: 150.23826166666666).
        // Arredonda pra 1 casa decimal antes de exibir, mesmo espírito do
        // round1 do worker (signallq-admin-worker/src/index.ts).
        const round1 = (v: number | null) => (v == null ? null : Math.round(v * 10) / 10);

        return (raw.operators ?? []).map((r, idx) => ({
          id:                             `op_${idx}`,
          name:                           r.operator,
          country:                        "Brasil",
          type:                           r.type,
          testCount:                      r.total_diagnostics,
          averageDownloadMbps:            round1(r.avg_download),
          averageUploadMbps:              round1(r.avg_upload),
          averageLatencyMs:               r.avg_latency,
          packetLossAverage:              r.packetLossAverage,
          averageScorePercentage:         r.avg_score,
        }));
      } catch {
        return [];
      }
    }

    const list = await apiClient.simulateFetch(mockOperatorsList, filters);

    if (filters.environment) {
      console.log(`Filtering operator stats for environment: ${filters.environment}`);
    }
    return list;
  },
};
