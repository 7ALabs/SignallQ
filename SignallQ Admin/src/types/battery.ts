import { FeatureKey } from "./productAnalytics";

export interface BatteryImpactMetric {
  feature: FeatureKey;
  label: string;
  estimatedImpact: "low" | "medium" | "high";
  avgDurationMs: number;
  backgroundExecutionPercent: number;
  networkCallsAvg: number;
  retryRate: number;
  wakeSensitive: boolean;
  notes: string;
}
