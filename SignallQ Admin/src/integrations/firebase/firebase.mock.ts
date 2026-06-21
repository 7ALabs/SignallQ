import { 
  FirebaseIntegrationStatus, 
  FirebaseAnalyticsSummary, 
  FirebaseCrashlyticsSummary, 
  FirebaseAppVersionCrashStats, 
  FirebaseCrashIssue 
} from "./firebase.types";

export const mockFirebaseStatus: FirebaseIntegrationStatus = {
  enabled: true,
  status: "connected",
  message: "Sincronizado via Conta de Serviço do Google Cloud Platform (GCP)",
  platform: "Android",
  lastSyncTimestamp: "21/06/2026 14:05",
  eventsImported: 1245800,
  crashesImported: 14201
};

export const mockFirebaseAnalytics: FirebaseAnalyticsSummary = {
  activeUsersToday: 4820,
  averageSessionsDurationMs: 412000, // ~6.87 minutes
  crashFreeUsersPercentage: 99.2,
  crashFreeSessionsPercentage: 99.6,
  topEvents: [
    { eventName: "diagnosis_started", count: 8640, uniques: 4120, trend: "up" },
    { eventName: "diagnosis_completed", count: 8210, uniques: 3950, trend: "up" },
    { eventName: "speedtest_completed", count: 12400, uniques: 4720, trend: "stable" },
    { eventName: "ai_generation_triggered", count: 4210, uniques: 2840, trend: "up" },
    { eventName: "network_exception_logged", count: 780, uniques: 410, trend: "down" }
  ]
};

export const mockFirebaseCrashlytics: FirebaseCrashlyticsSummary = {
  unresolvedCrashesCount: 4,
  unresolvedNonFatalsCount: 28,
  affectedUsersCount: 142,
  totalCrashesTrend: "down"
};

export const mockFirebaseAppVersions: FirebaseAppVersionCrashStats[] = [
  {
    appVersion: "0.18.1",
    crashCount: 120,
    nonFatalCount: 450,
    crashFreeUsersPercentage: 99.4,
    status: "stable"
  },
  {
    appVersion: "0.17.0",
    crashCount: 310,
    nonFatalCount: 1210,
    crashFreeUsersPercentage: 98.9,
    status: "stable"
  },
  {
    appVersion: "0.16.0",
    crashCount: 880,
    nonFatalCount: 4500,
    crashFreeUsersPercentage: 97.4,
    status: "unstable"
  }
];

export const mockFirebaseCrashIssues: FirebaseCrashIssue[] = [
  {
    issueId: "issue_fb_4892",
    title: "java.net.SocketTimeoutException",
    subtitle: "at okhttp3.internal.connection.RealConnection.connect(RealConnection.java)",
    affectedUsers: 84,
    occurrences: 410,
    platform: "Android",
    appVersion: "0.16.0",
    status: "open"
  },
  {
    issueId: "issue_fb_1204",
    title: "NullPointerException - Gemini Output Mapping",
    subtitle: "at com.signallq.diagnostic.ai.IntelligenceAdapter.parseFindings(IntelligenceAdapter.kt)",
    affectedUsers: 48,
    occurrences: 120,
    platform: "Android",
    appVersion: "0.17.0",
    status: "new"
  },
  {
    issueId: "issue_fb_3129",
    title: "IllegalArgumentException - Unknown Carrier Name Code",
    subtitle: "at com.signallq.diagnostic.io.CellularMonitor.resolveOperator(CellularMonitor.kt)",
    affectedUsers: 10,
    occurrences: 14,
    platform: "Android",
    appVersion: "0.18.1",
    status: "resolved"
  }
];
