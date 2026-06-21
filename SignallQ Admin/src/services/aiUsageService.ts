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
        id: r.model,
        name: r.model,
        totalCalls: r.calls ?? 0,
        totalTokens: r.tokens ?? 0,
        estimatedCostUsd: r.cost_usd ?? 0,
        reliabilityPercentage: 99.5,
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
