export interface FirebaseIntegrationStatus {
  enabled: boolean;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  message: string;
  platform: string;
  lastSyncTimestamp: string;
  eventsImported: number;
  crashesImported: number;
}

export interface FirebaseEventMetric {
  eventName: string;
  count: number;
  uniques: number;
  trend: "up" | "down" | "stable";
}

export interface FirebaseAnalyticsSummary {
  activeUsersToday: number;
  averageSessionsDurationMs: number;
  crashFreeUsersPercentage: number;
  crashFreeSessionsPercentage: number;
  topEvents: FirebaseEventMetric[];
}

export interface FirebaseCrashlyticsSummary {
  unresolvedCrashesCount: number;
  unresolvedNonFatalsCount: number;
  affectedUsersCount: number;
  totalCrashesTrend: "up" | "down" | "stable";
}

export interface FirebaseAppVersionCrashStats {
  appVersion: string;
  crashCount: number;
  nonFatalCount: number;
  crashFreeUsersPercentage: number;
  status: "stable" | "unstable" | "critical";
}

export interface FirebaseCrashIssue {
  issueId: string;
  title: string;
  subtitle: string;
  affectedUsers: number;
  occurrences: number;
  platform: string;
  appVersion: string;
  status: "new" | "open" | "resolved";
}
