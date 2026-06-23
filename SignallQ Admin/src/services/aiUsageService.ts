import { apiClient } from "./apiClient";
import { mockAiUsageRecords, mockAiModelInsights, mockAiDailyCostsTimeSeries } from "../mocks/aiUsage.mock";
import { AiUsageRecord, AiModelInsights } from "../types/ai";
import { DashboardFilters } from "./adminMetricsService";

export const aiUsageService = {
  async getAiUsageMetrics(filters: DashboardFilters = {}): Promise<AiModelInsights[]> {
    if (!apiClient.isMockEnabled()) {
      const period = filters.period === "today" ? "1d" : (filters.period ?? "7d");
      const raw = await apiClient.request<{ byModel: any[]; totals: any }>(
        "GET",
        `/admin/metrics/ai-usage?period=${period}`
      );
      return (raw.byModel ?? []).map((r: any) => ({
        provider: r.model as import("../types/ai").AiProvider,
        displayName: r.model,
        totalCalls: r.calls ?? 0,
        totalTokens: r.tokens ?? 0,
        averageLatencyMs: 0,
        estimatedCostUsd: r.cost_usd ?? 0,
        // O worker /admin/metrics/ai-usage não serve taxa de confiabilidade.
        // Sem fonte real — retornar null em vez de valor hardcoded.
        reliabilityPercentage: null,
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

  async getAiUsageRecords(filters: DashboardFilters = {}): Promise<AiUsageRecord[]> {
    if (!apiClient.isMockEnabled()) return [];
    return apiClient.simulateFetch(mockAiUsageRecords, filters);
  },

  async getAiCostSummary(filters: DashboardFilters = {}): Promise<{
    totalCostUsd: string;
    totalRequests: string;
    avgCostPerRequest: string;
    tokensSentM: string;
    tokensReceivedM: string;
    successRate: string;
  } | null> {
    if (!apiClient.isMockEnabled()) return null;

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
      totalCostUsd: `$${totalCost.toFixed(2)}`,
      totalRequests: totalCalls.toLocaleString("pt-BR"),
      avgCostPerRequest: `$${totalCalls > 0 ? (totalCost / totalCalls).toFixed(4) : "0.0000"}`,
      tokensSentM: `${sentM.toFixed(1)}M`,
      tokensReceivedM: `${receivedM.toFixed(1)}M`,
      successRate: `${avgReliability.toFixed(1)}%`,
    };
  },

  async getAiDailyCostsTimeSeries(filters: DashboardFilters = {}) {
    if (!apiClient.isMockEnabled()) return [];

    const timeline = await apiClient.simulateFetch(mockAiDailyCostsTimeSeries, filters);

    if (filters.environment === "staging") {
      return timeline.map(day => ({
        ...day,
        geminiCost: Number((day.geminiCost * 0.08).toFixed(3)),
        cloudflareCost: Number((day.cloudflareCost * 0.08).toFixed(3)),
        openaiCost: Number((day.openaiCost * 0.08).toFixed(3)),
      }));
    }
    return timeline;
  }
};
