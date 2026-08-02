import {
  GooglePlayIntegrationStatus,
  GooglePlayInstallMetrics,
  GooglePlayReleaseTrack,
  GooglePlayAppVersionStats,
  GooglePlayRatingSummary,
  GooglePlayReviewSummary,
  GooglePlayCrashAnrSummary,
  GooglePlayTracksStatus,
  GooglePlayTracksSyncResult,
  GooglePlayTracksBackfillResult,
  GooglePlayVitalsStatus,
  GooglePlayCrashRateStatus,
  GooglePlayStoreListingStatus
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

// GH#1341 — amostra com idioma/dispositivo/handlingStatus, pra exercitar Avaliações (item 2.3
// do plano de UX) com mock realista. Mocks separados de mockGooglePlayReviews (legado, usado
// pelo endpoint antigo /reviews sem esses campos) pra não quebrar quem já consumia o formato antigo.
export const mockGooglePlayReviewsList: GooglePlayReviewSummary[] = [
  {
    reviewId: "gp_rev_9041",
    rating: 5,
    comment: "Melhor app de speedtest e diagnóstico! O laudo da inteligência me ajudou a fixar o meu Wi-Fi de 5Ghz.",
    appVersion: "0.18.1",
    commentTime: "2026-07-20T14:00:00.000Z",
    replyText: "Ficamos extremamente felizes em ajudar! Conte sempre conosco.",
    replyTime: "2026-07-20T18:00:00.000Z",
    language: "pt-BR",
    device: "Samsung Galaxy S23",
    handlingStatus: "replied",
  },
  {
    reviewId: "gp_rev_2011",
    rating: 4,
    comment: "Interface impecável, muito bonita e as métricas de bufferbloat estão perfeitas. Falta apenas suporte a iOS.",
    appVersion: "0.18.1",
    commentTime: "2026-07-21T09:30:00.000Z",
    language: "pt-BR",
    device: "Motorola Edge 40",
    handlingStatus: "pending",
  },
  {
    reviewId: "gp_rev_5522",
    rating: 1,
    comment: "Trava toda vez que tento medir a velocidade com dados móveis. Já reinstalei duas vezes e continua o mesmo problema.",
    appVersion: "0.18.0",
    commentTime: "2026-07-10T11:15:00.000Z",
    language: "pt-BR",
    device: "Xiaomi Redmi Note 12",
    handlingStatus: "pending",
  },
  {
    reviewId: "gp_rev_5980",
    rating: 3,
    comment: "App bom, mas o diagnóstico de rede móvel demora demais pra concluir.",
    appVersion: "0.17.0",
    commentTime: "2026-07-08T08:00:00.000Z",
    language: "en-US",
    device: "Google Pixel 8",
    handlingStatus: "pending",
  },
  {
    reviewId: "gp_rev_6104",
    rating: 2,
    comment: "Notificação de monitoramento em segundo plano consome muita bateria.",
    appVersion: "0.18.1",
    commentTime: "2026-05-02T08:00:00.000Z",
    language: "pt-BR",
    device: "Samsung Galaxy A54",
    replyText: "Obrigado pelo relato! Já ajustamos o intervalo do monitoramento na versão 0.18.1 — atualize e nos conte se melhorou.",
    replyTime: "2026-05-03T10:00:00.000Z",
    handlingStatus: "replied",
  },
];

export const mockGooglePlayVitalsStatus: GooglePlayVitalsStatus = {
  status: "connected",
  hasCredentials: true,
  lastSyncTimestamp: "2026-07-24T10:00:00.000Z",
  anrRatePercent: 0.32,
  rangeStart: "2026-07-17",
  rangeEnd: "2026-07-23",
};

export const mockGooglePlayCrashAnr: GooglePlayCrashAnrSummary = {
  anrCountWeekly: 3,
  crashCountWeekly: 11,
  crashFreeSessionRate: 99.45
};

export const mockGooglePlayCrashRateStatus: GooglePlayCrashRateStatus = {
  status: "connected",
  hasCredentials: true,
  lastSyncTimestamp: "2026-07-24T10:00:00.000Z",
  crashRatePercent: 0.61,
  rangeStart: "2026-07-17",
  rangeEnd: "2026-07-23",
};

export const mockGooglePlayStoreListingStatus: GooglePlayStoreListingStatus = {
  status: "connected",
  hasCredentials: true,
  lastSyncTimestamp: "2026-07-24T10:00:00.000Z",
  listings: [
    {
      language: "pt-BR",
      title: "SignallQ — Diagnóstico de Wi-Fi e Internet",
      shortDescription: "Teste de velocidade, diagnóstico de rede e recomendações práticas.",
      fullDescription:
        "SignallQ mede sua velocidade de internet, analisa a qualidade do seu Wi-Fi e do sinal móvel, identifica problemas na sua rede doméstica e recomenda ações práticas para melhorar sua conexão.",
    },
  ],
};

export const mockGooglePlayTracksStatus: GooglePlayTracksStatus = {
  status: "connected",
  hasCredentials: true,
  lastSyncTimestamp: "21/06/2026 14:00",
  tracksCount: 4
};

export const mockGooglePlayTracksSyncResult: GooglePlayTracksSyncResult = {
  status: "ok",
  syncedAt: new Date().toISOString(),
  tracksCount: 4
};

export const mockGooglePlayTracksBackfillResult: GooglePlayTracksBackfillResult = {
  status: "ok",
  updated: {
    diagnostic_sessions: 128,
    ai_usage: 42,
    analytics_events: 310
  }
};
