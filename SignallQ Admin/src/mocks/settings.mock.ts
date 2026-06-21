import { AdminSettingsPayload } from "../types/admin";

export const initialMockSettings: AdminSettingsPayload & {
  monthlyBudgetUsd: number;
  budgetAction: "block" | "alert" | "throttle";
  anonymizeIp: boolean;
  retentionDays: number;
  firebaseAnalyticsEnabled: boolean;
  maxAiTokensUserDaily: number;
  maxSpeedTestDataDailyMb: number;
  contextualAdsEnabled: boolean;
  contextualAdsCategories: string[];
} = {
  selectedDefaultAiModel: "gemini_flash",
  aiFallbackEnabled: true,
  maxTokensPerDiagnostic: 4096,
  speedtestIntervalSeconds: 300,
  androidLogsCollectionEnabled: true,
  stagingAlertWebhookUrl: "https://hooks.example.invalid/signallq/staging",
  productionAlertWebhookUrl: "https://hooks.example.invalid/signallq/production",
  cloudflareWorkerEndpoint: "https://telemetry-gateway.signallq.workers.dev",
  // additional requested cost/privacy config keys
  monthlyBudgetUsd: 200,
  budgetAction: "alert",
  anonymizeIp: true,
  retentionDays: 30,
  firebaseAnalyticsEnabled: true,
  maxAiTokensUserDaily: 150000,
  maxSpeedTestDataDailyMb: 250,
  contextualAdsEnabled: false,
  contextualAdsCategories: ["telefonia", "hardware", "streaming"]
};
