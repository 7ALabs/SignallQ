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
  sessions7d: 12860, // mesmo valor do protótipo md3-tobe (Md3DashboardContent.dc.html:27)
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
  source: "bigquery",
  unresolvedCrashes: 4,
  affectedUsers: 142,
  crashFreeUsersPercentage: 98.6
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
    id: "issue_fb_4892",
    title: "java.net.SocketTimeoutException at okhttp3.internal.connection.RealConnection.connect",
    totalCrashes: 410,
    affectedUsers: 84,
    lastSeen: Date.now() - 2 * 60 * 60 * 1000,
    appVersion: "0.16.0"
  },
  {
    id: "issue_fb_1204",
    title: "NullPointerException - Gemini Output Mapping",
    totalCrashes: 120,
    affectedUsers: 48,
    lastSeen: Date.now() - 26 * 60 * 60 * 1000,
    appVersion: "0.17.0"
  },
  {
    id: "issue_fb_3129",
    title: "IllegalArgumentException - Unknown Carrier Name Code",
    totalCrashes: 14,
    affectedUsers: 10,
    lastSeen: Date.now() - 5 * 24 * 60 * 60 * 1000,
    appVersion: "0.18.1"
  }
];
