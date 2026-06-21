import { AiProvider } from "./ai";

export type AppEnvironment = "production" | "staging";

export interface SystemErrorLog {
  id: string;
  timestamp: string;
  source: "worker" | "android_app" | "analytics_db" | "ai_gateway";
  message: string;
  stackTrace?: string;
  count: number;
  environment: AppEnvironment;
  resolved: boolean;
  affectedUserCount: number;
}

export interface AppVersionDetail {
  id: string;
  versionCode: string; // e.g. "v1.2.4"
  buildNumber: number; // e.g. 1024
  releaseDate: string;
  activeInstallsCount: number;
  rolloutPercentage: number; // 0 to 100
  crashFreeRatePercentage: number; // e.g. 99.8
  status: "stable" | "beta" | "deprecated" | "halted" | "planned";
  notes: string;
  platform?: string;
  source?: string;
  activeUsersPercentage?: number;
  diagnosticsCount?: number;
  successRatePercentage?: number;
  crashesCount?: number;
  anrsCount?: number;
  diagnosticErrorsCount?: number;
  aiErrorsCount?: number;
}

export interface OperatorRecord {
  id: string;
  name: string; // e.g. "Claro", "Vivo", "TIM", "Desktop Internet"
  country: string; // e.g. "Brasil"
  type: "mobile" | "fiber" | "cable";
  testCount: number;
  averageDownloadMbps: number;
  averageUploadMbps: number;
  averageLatencyMs: number;
  packetLossAverage: number;
  customerSatisfactionPercentage: number; // 0 to 100 based on diagnostics feedback
}

export interface AdminSettingsPayload {
  selectedDefaultAiModel: AiProvider;
  aiFallbackEnabled: boolean;
  maxTokensPerDiagnostic: number;
  speedtestIntervalSeconds: number;
  androidLogsCollectionEnabled: boolean;
  stagingAlertWebhookUrl: string;
  productionAlertWebhookUrl: string;
  cloudflareWorkerEndpoint: string;
}
