import {
  FirebaseIntegrationStatus,
  FirebaseAnalyticsSummary,
  FirebaseCrashlyticsSummary,
  FirebaseAppVersionCrashStats,
  FirebaseCrashIssue,
  FirebaseManagementStatus,
  FirebaseRemoteConfigStatus,
  FirebaseAppCheckStatus,
  FirebaseAppDistributionStatus,
  FirebaseFcmDeliveryStatus,
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

// GH#1343/#1344 — inventário técnico. App Check e FCM nascem vazios de propósito (nenhum
// provedor configurado no Console / SignallQ não usa push ainda) — os outros três mocks
// carregam dado real de exemplo.

export const mockFirebaseManagementStatus: FirebaseManagementStatus = {
  hasCredentials: true,
  status: "connected",
  lastSyncTimestamp: "2026-07-24T12:00:00.000Z",
  project: {
    projectId: "signallq-app",
    projectNumber: "741421457740",
    displayName: "SignallQ",
    state: "ACTIVE",
  },
  androidApps: [
    {
      appId: "1:741421457740:android:a8658a91308fba058fefe9",
      displayName: "SignallQ",
      packageName: "io.signallq.app",
      state: "ACTIVE",
    },
  ],
};

export const mockFirebaseRemoteConfigStatus: FirebaseRemoteConfigStatus = {
  hasCredentials: true,
  status: "connected",
  lastSyncTimestamp: "2026-07-24T12:00:00.000Z",
  parameterCount: 3,
  parameterKeys: ["diagnostico_ai_provider", "speedtest_timeout_ms", "onboarding_flow_v2"],
};

export const mockFirebaseAppCheckStatus: FirebaseAppCheckStatus = {
  hasCredentials: true,
  status: "connected",
  lastSyncTimestamp: null,
  services: null,
};

export const mockFirebaseAppDistributionStatus: FirebaseAppDistributionStatus = {
  hasCredentials: true,
  status: "connected",
  lastSyncTimestamp: "2026-07-24T12:00:00.000Z",
  releases: [
    {
      name: "projects/741421457740/apps/1:741421457740:android:a8658a91308fba058fefe9/releases/1",
      displayVersion: "0.25.0",
      buildVersion: "60",
      createTime: "2026-07-22T18:30:00.000Z",
      releaseNotesText: "Correções de estabilidade no diagnóstico Wi-Fi.",
    },
    {
      name: "projects/741421457740/apps/1:741421457740:android:a8658a91308fba058fefe9/releases/2",
      displayVersion: "0.24.2",
      buildVersion: "58",
      createTime: "2026-07-15T14:10:00.000Z",
      releaseNotesText: null,
    },
  ],
};

export const mockFirebaseFcmDeliveryStatus: FirebaseFcmDeliveryStatus = {
  hasCredentials: true,
  status: "connected",
  lastSyncTimestamp: "2026-07-24T12:00:00.000Z",
  androidDeliveryData: [],
};
