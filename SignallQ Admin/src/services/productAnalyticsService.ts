import { apiClient } from "./apiClient";
import {
  mockFeatureUsage,
  mockScreenNavigation,
  mockFeatureCrashes,
  mockRetention,
  mockFeatureAiUsage
} from "../mocks/productAnalytics.mock";
import {
  FeatureUsageMetric,
  ScreenNavigationMetric,
  FeatureCrashMetric,
  RetentionMetric,
  FeatureAiUsageMetric
} from "../types/productAnalytics";

export interface DashboardFilters {
  period?: "1d" | "7d" | "30d";
  environment?: string;
  appVersion?: string;
  platform?: string;
}

export class ProductAnalyticsService {
  private delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  async getFeatureUsage(filters?: DashboardFilters): Promise<FeatureUsageMetric[]> {
    if (!apiClient.isMockEnabled()) return [];
    await this.delay(300);
    // Period scaling simulations:
    const multiplier = filters?.period === "1d" ? 0.1 : filters?.period === "30d" ? 4.5 : 1.0;
    return mockFeatureUsage.map(f => ({
      ...f,
      usageCount: Math.round(f.usageCount * multiplier),
      uniqueUsers: Math.round(f.uniqueUsers * Math.min(multiplier, 1.8))
    }));
  }

  async getScreenNavigation(filters?: DashboardFilters): Promise<ScreenNavigationMetric[]> {
    if (!apiClient.isMockEnabled()) return [];
    await this.delay(300);
    const multiplier = filters?.period === "1d" ? 0.1 : filters?.period === "30d" ? 4.5 : 1.0;
    return mockScreenNavigation.map(s => ({
      ...s,
      views: Math.round(s.views * multiplier),
      uniqueUsers: Math.round(s.uniqueUsers * Math.min(multiplier, 1.8))
    }));
  }

  async getFeatureCrashes(filters?: DashboardFilters): Promise<FeatureCrashMetric[]> {
    if (!apiClient.isMockEnabled()) return [];
    await this.delay(300);
    // Filter by platform or version simulation
    let result = [...mockFeatureCrashes];
    if (filters?.appVersion && filters.appVersion !== "all") {
      result = result.filter(r => r.affectedVersions.includes(filters.appVersion!));
    }
    return result;
  }

  async getRetention(filters?: DashboardFilters): Promise<RetentionMetric[]> {
    if (!apiClient.isMockEnabled()) return [];
    await this.delay(200);
    return mockRetention;
  }

  async getFeatureAiUsage(filters?: DashboardFilters): Promise<FeatureAiUsageMetric[]> {
    if (!apiClient.isMockEnabled()) return [];
    await this.delay(200);
    const multiplier = filters?.period === "1d" ? 0.15 : filters?.period === "30d" ? 4.2 : 1.0;
    return mockFeatureAiUsage.map(ai => ({
      ...ai,
      aiCalls: Math.round(ai.aiCalls * multiplier),
      tokensInput: Math.round(ai.tokensInput * multiplier),
      tokensOutput: Math.round(ai.tokensOutput * multiplier),
      totalTokens: Math.round(ai.totalTokens * multiplier),
      estimatedCost: Number((ai.estimatedCost * multiplier).toFixed(2))
    }));
  }

  async getTokenUsageSummary(filters?: DashboardFilters): Promise<{
    totalTokensLabel: string;
    avgCostPerDiagnosis: string;
    mainProvider: string;
    failureRate: string;
  } | null> {
    if (!apiClient.isMockEnabled()) return null;
    await this.delay(200);
    const multiplier = filters?.period === "1d" ? 0.15 : filters?.period === "30d" ? 4.2 : 1.0;
    const items = mockFeatureAiUsage.map(ai => ({
      totalTokens: Math.round(ai.totalTokens * multiplier),
      aiCalls: Math.round(ai.aiCalls * multiplier),
      estimatedCost: Number((ai.estimatedCost * multiplier).toFixed(2))
    }));
    const totalTokens = items.reduce((s, i) => s + i.totalTokens, 0);
    const totalCalls = items.reduce((s, i) => s + i.aiCalls, 0);
    const totalCost = items.reduce((s, i) => s + i.estimatedCost, 0);
    const costPerDiag = totalCalls > 0 ? (totalCost / totalCalls) * 0.19 : 0; // USD→BRL estimado
    const tokensM = (totalTokens / 1_000_000).toFixed(1);
    return {
      totalTokensLabel: `${tokensM}M tokens`,
      avgCostPerDiagnosis: `R$ ${costPerDiag.toFixed(3).replace(".", ",")}`,
      mainProvider: "Google Gemini (100%)",
      failureRate: "0% (Invalidações: 0)",
    };
  }

  async getOverviewCards(filters?: DashboardFilters) {
    if (!apiClient.isMockEnabled()) return null;
    await this.delay(200);
    return {
      mostUsedFeature: "SpeedTest",
      mostUsedFeatureCount: filters?.period === "1d" ? "1.2K" : filters?.period === "30d" ? "56K" : "12.4K",
      topViewedScreen: "Início",
      topViewedScreenCount: filters?.period === "1d" ? "4.5K" : filters?.period === "30d" ? "203K" : "45.2K",
      worstAbandonedFlow: "Diagnóstico guiado",
      abandonRate: "35%",
      mostCrashingFeature: "Scan de Dispositivos",
      crashCount: filters?.period === "1d" ? "3 crashes" : filters?.period === "30d" ? "140 crashes" : "31" + " crashes",
      avgInstalledTime: "18,4 dias",
      d7Retention: "32%",
      highestAiConsumer: "Diagnóstico",
      highestAiConsumerCalls: filters?.period === "1d" ? "1.1K calls" : "7.8K calls",
      batteryHighestFeature: "Scan de Dispositivos",
      batteryHighestImpact: "Alto"
    };
  }
}

export const productAnalyticsService = new ProductAnalyticsService();
