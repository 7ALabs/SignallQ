import { apiClient } from "./apiClient";
import { mockDiagnosticSessions, mockDiagnosticsSummary, mockAggregateData } from "../mocks/diagnostics.mock";
import { DiagnosticSession, DiagnosticsSummary, AggregateRow } from "../types/diagnostics";
import { DashboardFilters } from "./adminMetricsService";

export const diagnosticsService = {
  /**
   * Evaluates aggregate diagnostics counters (Total, high severity issues, speed averages, issues map)
   */
  async getDiagnosticsSummary(filters: DashboardFilters = {}): Promise<DiagnosticsSummary> {
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

  /**
   * Retrieves paginated or filtered diagnostic histories
   */
  async getDiagnosticSessions(filters: DashboardFilters & { search?: string } = {}): Promise<DiagnosticSession[]> {
    const sessions = await apiClient.simulateFetch(mockDiagnosticSessions, filters);
    
    let filtered = sessions;

    // Filter by environment (which is a core property inside each diagnostic log session)
    if (filters.environment) {
      filtered = filtered.filter(s => s.environment === filters.environment);
    }

    // Filter by query search
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

  /**
   * Retrieves fine-grained telemetry summaries segmented by connection interfaces
   */
  async getAggregateDiagnostics(filters: DashboardFilters = {}): Promise<AggregateRow[]> {
    const data = await apiClient.simulateFetch(mockAggregateData, filters);
    if (filters.environment === "staging") {
      return data.map(row => ({
        ...row,
        diagnosticsCount: Math.round(row.diagnosticsCount * 0.12)
      }));
    }
    return data;
  },

  /**
   * Simulates calling live Cloudflare Worker or backend router to solve a technical issue
   */
  async triggerReDiagnosis(sessionId: string): Promise<{ success: boolean; message: string; data?: any }> {
    console.log(`[ApiClient Dispatch] Triggering remote diagnosis verification for id: ${sessionId}`);
    await apiClient.request("POST", `/diagnosis/explain`, { sessionId });
    return {
      success: true,
      message: `Diagnóstico recapturado remoto efetuado com sucesso para a sessão ${sessionId}.`
    };
  }
};
