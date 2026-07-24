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

/**
 * `handling_status` (migration 017, google_play_reviews) — campo admin-side, marcado
 * manualmente no Console, nunca sobrescrito pelo sync. Distinto do `DisplayReviewStatus`
 * calculado em `features/google-play/reviewStatus.ts` (nota + resposta), que é o que a UI
 * usa pra badge/ordenação — ver item 2.3.2 do plano de UX.
 */
export type GooglePlayReviewHandlingStatus = "pending" | "replied" | "dismissed";

export interface GooglePlayReviewSummary {
  reviewId: string;
  /** Android Publisher API (reviews.list) não retorna nome de autor — sempre ausente hoje. */
  userName?: string;
  rating: number;
  comment: string;
  appVersion: string;
  replyText?: string;
  replyTime?: string;
  commentTime: string;
  /** GH#1341 — idioma (BCP-47, ex. "pt-BR") e dispositivo, hoje expostos pelo endpoint real. */
  language?: string;
  device?: string;
  handlingStatus?: GooglePlayReviewHandlingStatus;
}

export interface GooglePlayCrashAnrSummary {
  anrCountWeekly: number;
  crashCountWeekly: number;
  crashFreeSessionRate: number;
}

/**
 * GH#1341/#1346 — Android Vitals ANR rate (Play Developer Reporting API v1beta1,
 * anrRateMetricSet), média DAILY numa janela de 7 dias terminando no último dia com dado
 * (freshnessInfo). Sem série temporal — o worker não guarda histórico dia-a-dia consumível
 * pelo frontend ainda (só o agregado da última sincronização em admin_settings).
 */
export interface GooglePlayVitalsStatus {
  status: "connected" | "disabled";
  hasCredentials: boolean;
  lastSyncTimestamp: string | null;
  anrRatePercent: number | null;
  rangeStart: string | null;
  rangeEnd: string | null;
}

export interface GooglePlayVitalsSyncResult {
  status: "ok" | "error" | "not_configured";
  message?: string;
  anrRatePercent?: number | null;
  rangeStart?: string;
  rangeEnd?: string;
  syncedAt?: string;
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
