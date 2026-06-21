import { apiClient } from "./apiClient";
import { mockAiUsageRecords, mockAiModelInsights, mockAiDailyCostsTimeSeries } from "../mocks/aiUsage.mock";
import { AiUsageRecord, AiModelInsights } from "../types/ai";
import { DashboardFilters } from "./adminMetricsService";

export const aiUsageService = {
  /**
   * Fetches insights about individual AI provider metrics (total call counters, reliability ratios, expenditures)
   */
  async getAiUsageMetrics(filters: DashboardFilters = {}): Promise<AiModelInsights[]> {
    const insights = await apiClient.simulateFetch(mockAiModelInsights, filters);
    
    // Scale values for staging
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

  /**
   * Retrieves fine-grained telemetry logging for AI report generation operations
   */
  async getAiUsageRecords(filters: DashboardFilters = {}): Promise<AiUsageRecord[]> {
    const records = await apiClient.simulateFetch(mockAiUsageRecords, filters);
    return records;
  },

  /**
   * Fetches daily token cost analytics for graph charting
   */
  async getAiDailyCostsTimeSeries(filters: DashboardFilters = {}) {
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
