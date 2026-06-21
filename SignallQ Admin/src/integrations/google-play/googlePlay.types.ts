export interface GooglePlayIntegrationStatus {
  enabled: boolean;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  message: string;
  platform: string;
  lastSyncTimestamp: string;
  downloadsImported: number;
}

export interface GooglePlayInstallMetrics {
  totalDownloads: number;
  activeInstalls: number;
  dailyDownloads: number;
  uninstallsThisWeek: number;
}

export interface GooglePlayReleaseTrack {
  trackName: string; // "production", "openTesting", "internal"
  versionCode: string;
  buildCode: number;
  rolloutPercentage: number;
  lastUpdated: string;
}

export interface GooglePlayAppVersionStats {
  versionCode: string;
  activeUsersPercent: number;
  activeDownloads: number;
  rolloutPercentage: number;
  status: "active" | "halted" | "completed";
}

export interface GooglePlayRatingSummary {
  averageRating: number;
  totalRatings: number;
  starDistribution: {
    five: number;
    four: number;
    three: number;
    two: number;
    one: number;
  };
}

export interface GooglePlayReviewSummary {
  reviewId: string;
  userName: string;
  rating: number;
  comment: string;
  appVersion: string;
  replyText?: string;
  commentTime: string;
}

export interface GooglePlayCrashAnrSummary {
  anrCountWeekly: number;
  crashCountWeekly: number;
  crashFreeSessionRate: number;
}
