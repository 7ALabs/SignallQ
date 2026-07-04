import { apiClient } from "./apiClient";
import { mockDiagnosticSessions, mockDiagnosticsSummary, mockAggregateData, mockIssueDetailsMap, IssueDetail, mockDiagnosticIntelligence, IntelligenceItem } from "../mocks/diagnostics.mock";
import { DiagnosticSession, DiagnosticsSummary, AggregateRow, DataPlatform } from "../types/diagnostics";
import { DashboardFilters } from "./adminMetricsService";

export const diagnosticsService = {
  async getDiagnosticsSummary(filters: DashboardFilters & { platform?: DataPlatform } = {}): Promise<DiagnosticsSummary> {
    if (!apiClient.isMockEnabled()) {
      const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
      const env = filters.environment ?? "production";
      const platformQuery = filters.platform ? `&platform=${filters.platform}` : "";
      const raw = await apiClient.request<{
        totalDiagnostics: number;
        criticalCount: number;
        activeSessions: number;
        averageScore: number | null;
        averageLatencyMs: number | null;
        averageJitterMs: number | null;
        averagePacketLossPercentage: number | null;
        averageDownloadMbps: number | null;
        averageUploadMbps: number | null;
      }>("GET", `/admin/metrics/diagnostics/summary?environment=${env}&period=${period}${platformQuery}`);

      return {
        totalTests:                  raw.totalDiagnostics ?? 0,
        criticalIssuesCount:         raw.criticalCount ?? 0,
        attentionIssuesCount:        raw.activeSessions ?? 0,
        averageScore:                raw.averageScore ?? 0,
        averageDownloadMbps:         raw.averageDownloadMbps ?? null,
        averageUploadMbps:           raw.averageUploadMbps ?? null,
        averageLatencyMs:            raw.averageLatencyMs ?? null,
        averageJitterMs:             raw.averageJitterMs ?? null,
        averagePacketLossPercentage: raw.averagePacketLossPercentage ?? null,
        issueDistribution: {} as Record<import("../types/diagnostics").DiagnosisIssue, number>,
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

  async getDiagnosticSessions(filters: DashboardFilters & { search?: string; platform?: DataPlatform } = {}): Promise<DiagnosticSession[]> {
    if (!apiClient.isMockEnabled()) {
      const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
      const env = filters.environment ?? "production";
      const platformQuery = filters.platform ? `&platform=${filters.platform}` : "";
      const raw = await apiClient.request<{ sessions: any[] }>(
        "GET",
        `/admin/metrics/diagnostics?environment=${env}&period=${period}&limit=100${platformQuery}`
      );
      const mapped: DiagnosticSession[] = (raw.sessions ?? []).map((r: any) => {
        // GH#442: worker so preenche 'platform' a partir da migration 011_gh442.sql —
        // registros anteriores nao tem a coluna e o worker ja normaliza para 'android'.
        const platform: DataPlatform = r.platform === "web" ? "web" : "android";
        return {
        id: r.id,
        deviceId: r.device_id ?? "",
        deviceModel: r.device_model || (platform === "web" ? "Navegador" : "Android"),
        osVersion: r.os_version ?? "",
        appVersion: r.app_version ?? "",
        timestamp: new Date(r.created_at * 1000).toISOString(),
        networkType: r.network_type ?? "unknown",
        // ambiente vem do dispositivo (production = instalado via Play Store, ver
        // AdminSyncWorker.kt / data-architecture.md) — nunca hardcodear "production".
        environment: (r.environment as DiagnosticSession["environment"]) ?? "production",
        operator: r.operator || undefined,
        score: r.score ?? 0,
        speed: {
          downloadMbps: r.download_mbps ?? 0,
          uploadMbps: r.upload_mbps ?? 0,
          latencyMs: r.latency_ms ?? 0,
          jitterMs: r.jitter_ms ?? 0,
          packetLossPercentage: r.packet_loss ?? 0,
          bufferbloatGrade: r.score >= 80 ? "A" : r.score >= 60 ? "B" : r.score >= 40 ? "C" : "D",
        },
        // rssi/banda_wifi/padrao_wifi (SIG-164) só existem quando o app enviou dados
        // de rádio — nem toda sessão tem (ex.: fibra/ethernet). Sem valor inventado.
        networkStrength: r.rssi != null ? {
          type: r.network_type ?? "unknown",
          signalStrengthDbm: r.rssi,
          signalQualityPercentage: Math.max(0, Math.min(100, Math.round((r.rssi + 100) * 2))),
          carrierName: r.operator || undefined,
          frequencyBandGhz: r.banda_wifi ?? undefined,
          wifiStandard: r.padrao_wifi ?? undefined,
        } : undefined,
        issues: Array.isArray(r.issues) ? r.issues : [],
        // Não existe status intermediário real vindo do worker hoje (pending/failed) —
        // só sabemos se um laudo IA foi persistido ou não. "completed"/"none" é o único
        // par honesto até o worker expor o estado real do pipeline de IA.
        aiStatus: r.ai_summary_report ? "completed" : "none",
        aiSummaryReport: r.ai_summary_report || undefined,
        distChannel: r.dist_channel ?? undefined,
        buildType: r.build_type ?? undefined,
        platform,
        };
      });

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
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
      const env = filters.environment ?? "production";
      try {
        const raw = await apiClient.request<{
          items: Array<{
            name: string;
            count: number;
            avg_score: number;
            avg_download_mbps: number;
            avg_latency_ms: number;
            percentage: number;
          }>;
        }>("GET", `/admin/metrics/network?environment=${env}&period=${period}`);
        return (raw.items ?? []).map((r) => ({
          networkType:      r.name,
          diagnosticsCount: r.count,
          avgScore:         Math.round(r.avg_score ?? 0),
          avgDownload:      `${(r.avg_download_mbps ?? 0).toFixed(1)} Mbps`,
          avgUpload:        "—",
          avgPing:          `${Math.round(r.avg_latency_ms ?? 0)} ms`,
          avgJitter:        "—",
          avgLoss:          "—",
          topIssue:         "—",
          trend:            "stable" as const,
          trendLabel:       `${r.percentage.toFixed(1)}% do total`,
        }));
      } catch {
        return [];
      }
    }

    const data = await apiClient.simulateFetch(mockAggregateData, filters);
    if (filters.environment === "staging") {
      return data.map(row => ({
        ...row,
        diagnosticsCount: Math.round(row.diagnosticsCount * 0.12)
      }));
    }
    return data;
  },

  async getDiagnosticIntelligence(_filters: DashboardFilters = {}): Promise<IntelligenceItem[]> {
    if (!apiClient.isMockEnabled()) return [];
    return apiClient.simulateFetch(mockDiagnosticIntelligence, _filters);
  },

  async getIssueDetail(issueName: string): Promise<IssueDetail | null> {
    if (!apiClient.isMockEnabled()) return null;
    await new Promise(resolve => setTimeout(resolve, 60));
    return mockIssueDetailsMap[issueName] ?? null;
  },

  async triggerReDiagnosis(sessionId: string): Promise<{ success: boolean; message: string; data?: any }> {
    // A rota /diagnosis/explain não existe no worker atual — endpoint não implementado.
    // Retorna erro informativo sem fazer chamada HTTP para evitar 404 silencioso.
    if (!apiClient.isMockEnabled()) {
      return {
        success: false,
        message: `Rediagnóstico remoto não disponível: endpoint não implementado no worker. (sessão: ${sessionId})`
      };
    }
    console.log(`[ApiClient Dispatch] Triggering remote diagnosis verification for id: ${sessionId}`);
    return {
      success: true,
      message: `Diagnóstico recapturado remoto efetuado com sucesso para a sessão ${sessionId}.`
    };
  }
};
