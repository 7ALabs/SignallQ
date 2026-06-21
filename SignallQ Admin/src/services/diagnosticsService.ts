import { apiClient } from "./apiClient";
import { mockDiagnosticSessions, mockDiagnosticsSummary, mockAggregateData } from "../mocks/diagnostics.mock";
import { DiagnosticSession, DiagnosticsSummary, AggregateRow } from "../types/diagnostics";
import { DashboardFilters } from "./adminMetricsService";

export const diagnosticsService = {
  async getDiagnosticsSummary(filters: DashboardFilters = {}): Promise<DiagnosticsSummary> {
    if (!apiClient.isMockEnabled()) {
      const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
      const raw = await apiClient.request<{
        totalDiagnostics: number;
        activeSessions: number;
        avgNetworkScore: number;
        aiCallsToday: number;
        aiCostToday: number;
        aiTokensToday: number;
      }>("GET", `/admin/metrics/overview?period=${period}`);

      return {
        totalTests: raw.totalDiagnostics ?? 0,
        criticalIssuesCount: 0,
        attentionIssuesCount: raw.activeSessions ?? 0,
        averageScore: raw.avgNetworkScore ?? 0,
        averageDownloadMbps: 0,
        averageUploadMbps: 0,
        averageLatencyMs: 0,
        averageJitterMs: 0,
        averagePacketLossPercentage: 0,
        issueDistribution: {},
      };
    }

    const summary = await apiClient.simulateFetch(mockDiagnosticsSummary, filters);

    if (filters.environment === "staging") {
      return {
        totalTests: Math.round(summary.totalTests * 0.08),
        criticalIssuesCount: Math.round(summary.criticalIssuesCount * 0.05),
        attentionIssuesCount: Math.round(summary.attentionIssuesCount * 0.06),
        averageDownloadMbps: 198,
        averageUploadMbps: 64,
        averageLatencyMs: 31,
        averageScore: 72,
        averageJitterMs: 11,
        averagePacketLossPercentage: 1.2,
        issueDistribution: {
          wifi_signal_weak: 120,
          bufferbloat_upload: 98,
          dns_latency_high: 140,
          mobile_congestion_suspected: 15,
          gateway_slow: 45,
          packet_loss: 12,
          upload_bottleneck: 58,
          unknown: 8
        }
      };
    }
    return summary;
  },

  async getDiagnosticSessions(filters: DashboardFilters & { search?: string } = {}): Promise<DiagnosticSession[]> {
    if (!apiClient.isMockEnabled()) {
      const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
      const raw = await apiClient.request<{ sessions: any[] }>(
        "GET",
        `/admin/metrics/diagnostics?period=${period}&limit=100`
      );
      const mapped: DiagnosticSession[] = (raw.sessions ?? []).map((r: any) => ({
        id: r.id,
        timestamp: new Date(r.created_at * 1000).toISOString(),
        environment: "production" as const,
        networkType: r.network_type ?? "unknown",
        deviceModel: "Android",
        status: r.status ?? "unknown",
        score: r.score ?? 0,
        resolved: !!r.resolved,
        speed: {
          downloadMbps: r.download_mbps ?? 0,
          uploadMbps: r.upload_mbps ?? 0,
          latencyMs: r.latency_ms ?? 0,
          jitterMs: r.jitter_ms ?? 0,
          packetLossPercentage: r.packet_loss ?? 0,
          bufferbloatGrade: r.score >= 80 ? "A" : r.score >= 60 ? "B" : r.score >= 40 ? "C" : "D",
        },
        issues: Array.isArray(r.issues) ? r.issues : [],
        networkStrength: undefined,
      }));

      if (filters.search) {
        const q = filters.search.toLowerCase();
        return mapped.filter(s =>
          s.id.toLowerCase().includes(q) ||
          s.networkType.toLowerCase().includes(q)
        );
      }
      return mapped;
    }

    const sessions = await apiClient.simulateFetch(mockDiagnosticSessions, filters);
    let filtered = sessions;

    if (filters.environment) {
      filtered = filtered.filter(s => s.environment === filters.environment);
    }

    if (filters.search) {
      const q = filters.search.toLowerCase();
      filtered = filtered.filter(s =>
        s.id.toLowerCase().includes(q) ||
        s.deviceModel.toLowerCase().includes(q) ||
        s.networkType.toLowerCase().includes(q) ||
        s.speed.bufferbloatGrade.toLowerCase().includes(q) ||
        (s.networkStrength?.ssid && s.networkStrength.ssid.toLowerCase().includes(q)) ||
        (s.networkStrength?.carrierName && s.networkStrength.carrierName.toLowerCase().includes(q))
      );
    }

    return filtered;
  },

  async getAggregateDiagnostics(filters: DashboardFilters = {}): Promise<AggregateRow[]> {
    if (!apiClient.isMockEnabled()) return [];

    const data = await apiClient.simulateFetch(mockAggregateData, filters);
    if (filters.environment === "staging") {
      return data.map(row => ({
        ...row,
        diagnosticsCount: Math.round(row.diagnosticsCount * 0.12)
      }));
    }
    return data;
  },

  async triggerReDiagnosis(sessionId: string): Promise<{ success: boolean; message: string; data?: any }> {
    console.log(`[ApiClient Dispatch] Triggering remote diagnosis verification for id: ${sessionId}`);
    await apiClient.request("POST", `/diagnosis/explain`, { sessionId });
    return {
      success: true,
      message: `Diagnóstico recapturado remoto efetuado com sucesso para a sessão ${sessionId}.`
    };
  }
};
