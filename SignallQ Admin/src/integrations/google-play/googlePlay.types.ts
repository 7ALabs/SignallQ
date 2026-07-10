export interface GooglePlayIntegrationStatus {
  enabled: boolean;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  message: string;
  platform: string;
  lastSyncTimestamp: string;
  downloadsImported: number;
  /** GH#761 — nao vem da Android Publisher API (so via export CSV/GCS, nao implementado). */
  ratingAverage?: number | null;
  reviewsSampled?: number;
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

// migration 012_play_track.sql — mapeamento version_code -> trilha do Play Console
// (internal/alpha/beta/production), sincronizado via Android Publisher API e aplicado
// aos dados históricos por um backfill explícito e separado.
export interface GooglePlayTracksStatus {
  status: "connected" | "disabled";
  hasCredentials: boolean;
  lastSyncTimestamp: string | null;
  tracksCount: number;
}

export interface GooglePlayTracksSyncResult {
  status: "ok" | "error" | "not_configured";
  message?: string;
  syncedAt?: string;
  tracksCount?: number;
}

export interface GooglePlayTracksBackfillResult {
  status: "ok" | "error";
  message?: string;
  updated?: {
    diagnostic_sessions: number;
    ai_usage: number;
    analytics_events: number;
  };
}
