import { apiClient } from "./apiClient";
import { mockAiUsageRecords, mockAiModelInsights, mockAiDailyUsageTimeSeries } from "../mocks/aiUsage.mock";
import { AiUsageRecord, AiModelInsights, AiDailyUsage } from "../types/ai";
import { DashboardFilters } from "./adminMetricsService";

export const aiUsageService = {
  async getAiUsageMetrics(filters: DashboardFilters = {}): Promise<AiModelInsights[] | null> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return null;
      const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
      const env = filters.environment ?? "production";
      const raw = await apiClient.request<{ byModel: any[]; totals: any }>(
        "GET",
        `/admin/metrics/ai-usage?environment=${env}&period=${period}`
      );
      return (raw.byModel ?? []).map((r: any) => ({
        provider: r.model as import("../types/ai").AiProvider,
        displayName: r.model,
        totalCalls: r.calls ?? 0,
        totalTokens: r.tokens ?? 0,
        averageLatencyMs: 0,
        estimatedCostUsd: r.cost_usd ?? 0,
        // SIG-125: reliabilityPercentage agora vem do worker (completion_tokens > 0 como proxy de sucesso).
        // null quando o modelo não tem registros no período.
        reliabilityPercentage: r.reliabilityPercentage ?? null,
      }));
    }

    const insights = await apiClient.simulateFetch(mockAiModelInsights, filters);

    if (filters.environment === "staging") {
      return insights.map(m => ({
        ...m,
        totalCalls: Math.round(m.totalCalls * 0.05),
        totalTokens: Math.round(m.totalTokens * 0.05),
        estimatedCostUsd: Number((m.estimatedCostUsd * 0.05).toFixed(4)),
        reliabilityPercentage: Math.max(95, m.reliabilityPercentage - 0.5)
      }));
    }
    return insights;
  },

  /** Histórico de execuções reais de IA (GH#421) — cada item vem de uma linha de `ai_usage`. */
  async getAiUsageRecords(filters: DashboardFilters = {}): Promise<AiUsageRecord[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      try {
        const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
        const env = filters.environment ?? "production";
        const raw = await apiClient.request<{ records: any[] }>(
          "GET",
          `/admin/metrics/ai-usage/records?environment=${env}&period=${period}`
        );
        return (raw.records ?? []).map((r: any) => ({
          id: r.id,
          timestamp: r.timestamp,
          model: r.model ?? "",
          provider: r.provider ?? r.model ?? "",
          promptTokens: r.promptTokens ?? 0,
          completionTokens: r.completionTokens ?? 0,
          costUsd: r.costUsd ?? 0,
          status: r.status === "error" ? "error" : "success",
          errorMessage: r.errorMessage || null,
          diagnosisId: r.diagnosisId ?? null,
          environment: r.environment ?? env,
        }));
      } catch (e) {
        console.error("Failed to load AI usage records", e);
        return [];
      }
    }
    return apiClient.simulateFetch(mockAiUsageRecords, filters);
  },

  async getAiCostSummary(filters: DashboardFilters = {}): Promise<{
    totalCostUsd: string;
    totalRequests: string;
    avgCostPerRequest: string;
    tokensSentM: string;
    tokensReceivedM: string;
    successRate: string;
    reliabilityPercentage: number | null;
  } | null> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return null;
      try {
        const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
        const env = filters.environment ?? "production";
        const [costsRaw, usageRaw] = await Promise.all([
          apiClient.request<{
            totalCostUsd: number;
            totalRequests: number;
            avgCostPerRequest: number;
            promptTokens: number;
            completionTokens: number;
          }>("GET", `/admin/metrics/ai-costs?environment=${env}&period=${period}`),
          apiClient.request<{
            byModel: Array<{ model: string; calls: number; tokens: number; cost_usd: number; reliabilityPercentage?: number }>;
            totals: { reliabilityPercentage?: number };
          }>("GET", `/admin/metrics/ai-usage?environment=${env}&period=${period}`).catch(() => null),
        ]);
        const sentM     = (costsRaw.promptTokens     ?? 0) / 1_000_000;
        const receivedM = (costsRaw.completionTokens  ?? 0) / 1_000_000;
        const totalReq  = costsRaw.totalRequests ?? 0;
        const totalCost = costsRaw.totalCostUsd  ?? 0;
        const avgCost   = costsRaw.avgCostPerRequest ?? 0;
        // SIG-125: reliabilityPercentage geral — média ponderada por chamadas dos modelos.
        // Exclui modelos sem dados (reliabilityPercentage null) para não inflar a média.
        const byModelRaw = usageRaw?.byModel ?? [];
        const modelsWithRel = byModelRaw.filter(m => m.reliabilityPercentage != null);
        const totalCallsForRel = modelsWithRel.reduce((s, m) => s + (m.calls ?? 0), 0);
        const reliability: number | null = totalCallsForRel > 0
          ? Math.round(
              modelsWithRel.reduce((s, m) => s + (m.reliabilityPercentage! * (m.calls ?? 0)), 0)
              / totalCallsForRel * 100
            ) / 100
          : null;
        return {
          totalCostUsd:          `$${totalCost.toFixed(2)}`,
          totalRequests:         totalReq.toLocaleString("pt-BR"),
          avgCostPerRequest:     `$${avgCost.toFixed(2)}`,
          tokensSentM:           `${sentM.toFixed(1)}M`,
          tokensReceivedM:       `${receivedM.toFixed(1)}M`,
          successRate:           "—",
          reliabilityPercentage: reliability,
        };
      } catch {
        return null;
      }
    }

    const isStg = filters.environment === "staging";
    const scale = isStg ? 0.05 : 1.0;

    const insights = await apiClient.simulateFetch(mockAiModelInsights, filters);
    const realInsights = insights.filter(i => i.provider !== "local_fallback");

    const totalCost = realInsights.reduce((s, i) => s + i.estimatedCostUsd * scale, 0);
    const totalCalls = realInsights.reduce((s, i) => s + Math.round(i.totalCalls * scale), 0);
    const sentM = realInsights.reduce((s, i) => s + Math.round(i.totalTokens * scale * 0.72), 0) / 1_000_000;
    const receivedM = realInsights.reduce((s, i) => s + Math.round(i.totalTokens * scale * 0.28), 0) / 1_000_000;
    const avgReliability = realInsights.reduce((s, i) => s + (i.reliabilityPercentage ?? 100), 0) / realInsights.length;

    return {
      totalCostUsd:          `$${totalCost.toFixed(2)}`,
      totalRequests:         totalCalls.toLocaleString("pt-BR"),
      avgCostPerRequest:     `$${totalCalls > 0 ? (totalCost / totalCalls).toFixed(2) : "0.00"}`,
      tokensSentM:           `${sentM.toFixed(1)}M`,
      tokensReceivedM:       `${receivedM.toFixed(1)}M`,
      successRate:           `${avgReliability.toFixed(1)}%`,
      reliabilityPercentage: avgReliability,
    };
  },

  /** Série temporal de uso de tokens por provedor por dia. */
  async getAiUsageTimeSeries(filters: DashboardFilters = {}): Promise<AiDailyUsage[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      try {
        const days = filters.period === "today" ? 1 : filters.period === "7d" ? 7 : 30;
        const env = filters.environment ?? "production";
        const raw = await apiClient.request<{ source: string; days: number; series: any[] }>(
          "GET",
          `/admin/metrics/ai-usage/timeline?environment=${env}&days=${days}`
        );
        return (raw.series ?? []).map((entry: any) => ({
          date:       entry.date as string,
          byProvider: (entry.byProvider ?? {}) as Record<string, number>,
        }));
      } catch {
        return [];
      }
    }

    const timeline = await apiClient.simulateFetch(mockAiDailyUsageTimeSeries, filters);

    if (filters.environment === "staging") {
      // Staging tem volume ~5% do produção.
      return timeline.map(day => ({
        ...day,
        byProvider: Object.fromEntries(
          Object.entries(day.byProvider).map(([k, v]) => [k, Math.round((v as number) * 0.05)])
        ),
      }));
    }
    return timeline;
  },

  /** @deprecated Use getAiUsageTimeSeries — mantido para retrocompatibilidade. */
  async getAiDailyCostsTimeSeries(filters: DashboardFilters = {}): Promise<AiDailyUsage[]> {
    return this.getAiUsageTimeSeries(filters);
  }
};
