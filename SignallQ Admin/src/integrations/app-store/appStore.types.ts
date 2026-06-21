export interface AppStoreIntegrationStatus {
  enabled: boolean;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  message: string;
  platform: string;
  lastSyncTimestamp: string;
}

export interface AppStoreDownloadMetrics {
  totalDownloads: number;
  activeInstalls: number;
  uninstallsThisWeek: number;
}

export interface AppStoreVersionStats {
  versionCode: string;
  activeUsersPercent: number;
}

export interface AppStoreCrashSummary {
  crashCountWeekly: number;
  crashFreeSessionRate: number;
}

export interface AppStoreRatingSummary {
  averageRating: number;
}
