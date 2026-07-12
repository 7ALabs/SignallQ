import { apiClient } from "./apiClient";
import {
  mockFeatureUsage,
  mockScreenNavigation,
  mockFeatureCrashes,
  mockRetention,
  mockFeatureAiUsage,
  mockDeviceBreakdown
} from "../mocks/productAnalytics.mock";
import {
  FeatureUsageMetric,
  ScreenNavigationMetric,
  FeatureCrashMetric,
  RetentionMetric,
  FeatureAiUsageMetric,
  DeviceBreakdownMetric
} from "../types/productAnalytics";

export interface DashboardFilters {
  period?: "1d" | "7d" | "30d";
  environment?: string;
  appVersion?: string;
  platform?: string;
}

interface ProductAnalyticsResponse {
  source: string;
  period: string;
  environment: string;
  no_data_yet: boolean;
  feature_usage: Array<{
    feature: string;
    label: string;
    usageCount: number;
    uniqueUsers: number;
    completionRate: number;
    failureRate: number;
    avgDurationMs: number;
    trendPercent: number;
  }>;
  screen_navigation: Array<{
    screen: string;
    label: string;
    views: number;
    uniqueUsers: number;
    avgTimeOnScreenSec: number;
    exitRate: number;
    nextMostCommonScreen: string | null;
  }>;
  feature_crashes: Array<{
    feature: string;
    label: string;
    crashes: number;
    nonFatalErrors: number;
    anrs: number;
    crashRate: number;
    affectedVersions: string[];
    severity: "ok" | "attention" | "critical";
  }>;
  avg_session_duration_ms: number | null;
  session_count: number;
  retention: Array<{
    cohort: string;
    cohortSize: number;
    day1: number | null;
    day7: number | null;
    day30: number | null;
    avgInstalledDays: number | null;
    uninstallRate: number | null;
  }>;
}

export class ProductAnalyticsService {
  private delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private buildParams(filters?: DashboardFilters): string {
    const params = new URLSearchParams();
    if (filters?.period) params.set("period", filters.period);
    if (filters?.environment && filters.environment !== "all") params.set("environment", filters.environment);
    return params.toString() ? `?${params.toString()}` : "";
  }

  private async fetchProductAnalytics(filters?: DashboardFilters): Promise<ProductAnalyticsResponse | null> {
    try {
      return await apiClient.request<ProductAnalyticsResponse>(
        "GET",
        `/admin/metrics/analytics/product${this.buildParams(filters)}`
      );
    } catch {
      return null;
    }
  }

  async getFeatureUsage(filters?: DashboardFilters): Promise<FeatureUsageMetric[]> {
    if (apiClient.isMockEnabled()) {
      await this.delay(300);
      const multiplier = filters?.period === "1d" ? 0.1 : filters?.period === "30d" ? 4.5 : 1.0;
      return mockFeatureUsage.map(f => ({
        ...f,
        usageCount: Math.round(f.usageCount * multiplier),
        uniqueUsers: Math.round(f.uniqueUsers * Math.min(multiplier, 1.8))
      }));
    }

    const data = await this.fetchProductAnalytics(filters);
    if (!data || data.no_data_yet) return [];
    return data.feature_usage as FeatureUsageMetric[];
  }

  async getScreenNavigation(filters?: DashboardFilters): Promise<ScreenNavigationMetric[]> {
    if (apiClient.isMockEnabled()) {
      await this.delay(300);
      const multiplier = filters?.period === "1d" ? 0.1 : filters?.period === "30d" ? 4.5 : 1.0;
      return mockScreenNavigation.map(s => ({
        ...s,
        views: Math.round(s.views * multiplier),
        uniqueUsers: Math.round(s.uniqueUsers * Math.min(multiplier, 1.8))
      }));
    }

    const data = await this.fetchProductAnalytics(filters);
    if (!data || data.no_data_yet) return [];
    return data.screen_navigation as ScreenNavigationMetric[];
  }

  async getFeatureCrashes(filters?: DashboardFilters): Promise<FeatureCrashMetric[]> {
    if (apiClient.isMockEnabled()) {
      await this.delay(300);
      let result = [...mockFeatureCrashes];
      if (filters?.appVersion && filters.appVersion !== "all") {
        result = result.filter(r => r.affectedVersions.includes(filters.appVersion!));
      }
      return result;
    }

    const data = await this.fetchProductAnalytics(filters);
    if (!data || data.no_data_yet) return [];
    return data.feature_crashes as FeatureCrashMetric[];
  }

  async getRetention(filters?: DashboardFilters): Promise<RetentionMetric[]> {
    if (apiClient.isMockEnabled()) {
      await this.delay(200);
      return mockRetention;
    }

    const data = await this.fetchProductAnalytics(filters);
    if (!data || data.no_data_yet) return [];
    return data.retention as RetentionMetric[];
  }

  async getSessionDuration(filters?: DashboardFilters): Promise<{ avgDurationMs: number | null; sessionCount: number } | null> {
    if (apiClient.isMockEnabled()) {
      await this.delay(150);
      const multiplier = filters?.period === "1d" ? 0.1 : filters?.period === "30d" ? 4.5 : 1.0;
      return { avgDurationMs: 187_000, sessionCount: Math.round(6200 * multiplier) };
    }

    const data = await this.fetchProductAnalytics(filters);
    if (!data || data.no_data_yet) return null;
    return { avgDurationMs: data.avg_session_duration_ms, sessionCount: data.session_count };
  }

  async getFeatureAiUsage(filters?: DashboardFilters): Promise<FeatureAiUsageMetric[]> {
    if (apiClient.isMockEnabled()) {
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
    // Associação IA <-> feature exigiria um identificador de sessão compartilhado entre
    // `ai_usage.session_id` (hoje referencia diagnostic_sessions.id) e `analytics_events.session_id`
    // (sessão de app/tela) — esses dois IDs não são a mesma entidade hoje. Cruzar por session_id
    // produziria associação incorreta. Gap documentado em
    // SignallQ Admin/docs/architecture/data-architecture.md — requer decisão de contrato (Android)
    // antes de implementar o endpoint. Ver GH#418 follow-up.
    return [];
  }

  async getTokenUsageSummary(filters?: DashboardFilters): Promise<{
    totalTokensLabel: string;
    avgCostPerDiagnosis: string;
    mainProvider: string;
    failureRate: string;
  } | null> {
    if (apiClient.isMockEnabled()) {
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
      const costPerDiag = totalCalls > 0 ? (totalCost / totalCalls) * 0.19 : 0;
      const tokensM = (totalTokens / 1_000_000).toFixed(1);
      return {
        totalTokensLabel: `${tokensM}M tokens`,
        avgCostPerDiagnosis: `R$ ${costPerDiag.toFixed(3).replace(".", ",")}`,
        mainProvider: "Google Gemini (100%)",
        failureRate: "0% (Invalidações: 0)",
      };
    }
    return null;
  }

  // #785 — fonte própria (diagnostic_sessions via D1), não fetchProductAnalytics
  // (que agrega analytics_events) — são tabelas/fontes diferentes.
  async getDeviceBreakdown(filters?: DashboardFilters): Promise<DeviceBreakdownMetric[]> {
    if (apiClient.isMockEnabled()) {
      await this.delay(200);
      return mockDeviceBreakdown;
    }

    try {
      const data = await apiClient.request<{ no_data_yet: boolean; items: DeviceBreakdownMetric[] }>(
        "GET",
        `/admin/analytics/devices${this.buildParams(filters)}`
      );
      if (!data || data.no_data_yet) return [];
      return data.items;
    } catch {
      return [];
    }
  }

  async getOverviewCards(filters?: DashboardFilters) {
    if (apiClient.isMockEnabled()) {
      await this.delay(200);
      return {
        mostUsedFeature: "SpeedTest",
        mostUsedFeatureCount: filters?.period === "1d" ? "1.2K" : filters?.period === "30d" ? "56K" : "12.4K",
        topViewedScreen: "Início",
        topViewedScreenCount: filters?.period === "1d" ? "4.5K" : filters?.period === "30d" ? "203K" : "45.2K",
        worstAbandonedFlow: "Diagnóstico guiado",
        abandonRate: "35%",
        mostCrashingFeature: "Scan de Dispositivos",
        crashCount: filters?.period === "1d" ? "3 crashes" : filters?.period === "30d" ? "140 crashes" : "31 crashes",
        avgInstalledTime: "18,4 dias",
        d7Retention: "32%",
        highestAiConsumer: "Diagnóstico",
        highestAiConsumerCalls: filters?.period === "1d" ? "1.1K calls" : "7.8K calls",
        batteryHighestFeature: "Scan de Dispositivos",
        batteryHighestImpact: "Alto"
      };
    }

    const data = await this.fetchProductAnalytics(filters);
    if (!data || data.no_data_yet) return null;

    const topFeature = data.feature_usage[0];
    const topScreen  = data.screen_navigation[0];
    const topCrash   = data.feature_crashes[0];

    return {
      mostUsedFeature:      topFeature?.label ?? "—",
      mostUsedFeatureCount: topFeature ? String(topFeature.usageCount) : "—",
      topViewedScreen:      topScreen?.label ?? "—",
      topViewedScreenCount: topScreen ? String(topScreen.views) : "—",
      worstAbandonedFlow:   "—",
      abandonRate:          "—",
      mostCrashingFeature:  topCrash?.label ?? "—",
      crashCount:           topCrash ? `${topCrash.crashes} crashes` : "0 crashes",
      avgInstalledTime:     "—",
      d7Retention:          "—",
      highestAiConsumer:    "—",
      highestAiConsumerCalls: "—",
      batteryHighestFeature: "—",
      batteryHighestImpact:  "—",
    };
  }
}

export const productAnalyticsService = new ProductAnalyticsService();
