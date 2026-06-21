export type FeatureKey =
  | "speedtest"
  | "diagnosis"
  | "wifi_analysis"
  | "mobile_analysis"
  | "dns_test"
  | "devices_scan"
  | "fiber_modem"
  | "history"
  | "settings"
  | "laudo"
  | "guided_questions";

export type ScreenKey =
  | "home"
  | "speedtest"
  | "signal"
  | "history"
  | "settings"
  | "diagnosis"
  | "laudo"
  | "dns"
  | "devices"
  | "fiber_modem"
  | "privacy"
  | "novidades";

export type ProductEventName =
  | "screen_view"
  | "feature_started"
  | "feature_completed"
  | "feature_failed"
  | "diagnosis_started"
  | "diagnosis_completed"
  | "speedtest_started"
  | "speedtest_completed"
  | "ai_explanation_requested"
  | "guided_question_answered"
  | "app_opened"
  | "app_backgrounded"
  | "app_uninstalled_estimated";

export interface FeatureUsageMetric {
  feature: FeatureKey;
  label: string;
  usageCount: number;
  uniqueUsers: number;
  completionRate: number;
  failureRate: number;
  avgDurationMs: number;
  trendPercent: number;
}

export interface ScreenNavigationMetric {
  screen: ScreenKey;
  label: string;
  views: number;
  uniqueUsers: number;
  avgTimeOnScreenSec: number;
  exitRate: number;
  nextMostCommonScreen: ScreenKey | null;
}

export interface FeatureCrashMetric {
  feature: FeatureKey;
  label: string;
  crashes: number;
  nonFatalErrors: number;
  anrs: number;
  crashRate: number;
  affectedVersions: string[];
  severity: "ok" | "attention" | "critical";
}

export interface RetentionMetric {
  cohort: string;
  day1: number;
  day7: number;
  day30: number;
  avgInstalledDays: number;
  uninstallRate: number;
}

export interface FeatureAiUsageMetric {
  feature: FeatureKey;
  label: string;
  aiCalls: number;
  tokensInput: number;
  tokensOutput: number;
  totalTokens: number;
  estimatedCost: number;
  avgLatencyMs: number;
  providerBreakdown: {
    provider: string; // "gemini" | "openai" | "cohere" etc.
    calls: number;
    tokensInput: number;
    tokensOutput: number;
    estimatedCost: number;
    avgLatencyMs: number;
  }[];
  computeUnits?: number;
}
