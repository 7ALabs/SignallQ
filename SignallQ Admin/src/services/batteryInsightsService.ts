import { apiClient } from "./apiClient";
import { BatteryImpactMetric } from "../types/battery";
import { mockBatteryImpactData } from "../mocks/batteryInsights.mock";

interface BatteryAnalyticsResponse {
  source: string;
  period: string;
  no_data_yet: boolean;
  summary: {
    avg_battery_level: number | null;
    charging_sessions_pct: number;
    total_snapshots: number;
  } | null;
  items: BatteryImpactMetric[];
}

export class BatteryInsightsService {
  private delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  async getBatteryImpactMetrics(): Promise<BatteryImpactMetric[]> {
    if (apiClient.isMockEnabled()) {
      await this.delay(200);
      return mockBatteryImpactData;
    }

    try {
      const data = await apiClient.request<BatteryAnalyticsResponse>(
        "GET",
        "/admin/analytics/battery"
      );
      if (data.no_data_yet || !data.items?.length) return [];
      return data.items;
    } catch {
      return [];
    }
  }
}

export const batteryInsightsService = new BatteryInsightsService();
