import { 
  GooglePlayIntegrationStatus, 
  GooglePlayInstallMetrics, 
  GooglePlayReleaseTrack, 
  GooglePlayAppVersionStats, 
  GooglePlayRatingSummary, 
  GooglePlayReviewSummary, 
  GooglePlayCrashAnrSummary 
} from "./googlePlay.types";

export const mockGooglePlayStatus: GooglePlayIntegrationStatus = {
  enabled: true,
  status: "connected",
  message: "Autorizado via OAuth2 Google APIs Developer Credentials",
  platform: "Android (Google Play Services)",
  lastSyncTimestamp: "21/06/2026 14:00",
  downloadsImported: 18500
};

export const mockGooglePlayInstallMetrics: GooglePlayInstallMetrics = {
  totalDownloads: 18450,
  activeInstalls: 4820,
  dailyDownloads: 214,
  uninstallsThisWeek: 110
};

export const mockGooglePlayTracks: GooglePlayReleaseTrack[] = [
  {
    trackName: "Produção",
    versionCode: "v0.18.1",
    buildCode: 1045,
    rolloutPercentage: 100,
    lastUpdated: "19/06/2026"
  },
  {
    trackName: "Open Testing (Beta)",
    versionCode: "v0.19.0-rc1",
    buildCode: 1050,
    rolloutPercentage: 40,
    lastUpdated: "21/06/2026"
  },
  {
    trackName: "Canal Interno",
    versionCode: "v0.19.0-dev",
    buildCode: 1052,
    rolloutPercentage: 100,
    lastUpdated: "21/06/2026"
  }
];

export const mockGooglePlayAppVersions: GooglePlayAppVersionStats[] = [
  {
    versionCode: "0.18.1",
    activeUsersPercent: 61,
    activeDownloads: 11250,
    rolloutPercentage: 100,
    status: "completed"
  },
  {
    versionCode: "0.17.0",
    activeUsersPercent: 24,
    activeDownloads: 4420,
    rolloutPercentage: 100,
    status: "completed"
  },
  {
    versionCode: "0.16.0",
    activeUsersPercent: 9,
    activeDownloads: 1650,
    rolloutPercentage: 100,
    status: "completed"
  },
  {
    versionCode: "0.19.0-rc1",
    activeUsersPercent: 6,
    activeDownloads: 1130,
    rolloutPercentage: 40,
    status: "active"
  }
];

export const mockGooglePlayRatings: GooglePlayRatingSummary = {
  averageRating: 4.6,
  totalRatings: 840,
  starDistribution: {
    five: 610,
    four: 150,
    three: 50,
    two: 20,
    one: 10
  }
};

export const mockGooglePlayReviews: GooglePlayReviewSummary[] = [
  {
    reviewId: "gp_rev_9041",
    userName: "Lucas Silva",
    rating: 5,
    comment: "Melhor app de speedtest e diagnóstico! O laudo da inteligência me ajudou a fixar o meu Wi-Fi de 5Ghz.",
    appVersion: "0.18.1",
    commentTime: "20/06/2026",
    replyText: "Ficamos extremamente felizes em ajudar Lucas! Conte sempre conosco."
  },
  {
    reviewId: "gp_rev_2011",
    userName: "Mariana Costa",
    rating: 4,
    comment: "Interface impecável, muito bonita e as métricas de bufferbloat estão perfeitas. Falta apenas suporte a iOS.",
    appVersion: "0.18.1",
    commentTime: "21/06/2026",
    replyText: "Olá Mariana! O suporte a iOS já está em planejamento na nossa arquitetura central de telemetria."
  }
];

export const mockGooglePlayCrashAnr: GooglePlayCrashAnrSummary = {
  anrCountWeekly: 3,
  crashCountWeekly: 11,
  crashFreeSessionRate: 99.45
};
