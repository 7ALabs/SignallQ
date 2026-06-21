import { BatteryImpactMetric } from "../types/battery";
import { mockBatteryImpactData } from "../mocks/batteryInsights.mock";

export class BatteryInsightsService {
  private delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  async getBatteryImpactMetrics(): Promise<BatteryImpactMetric[]> {
    await this.delay(200);
    return mockBatteryImpactData;
  }
}

export const batteryInsightsService = new BatteryInsightsService();
