import { apiClient } from "../../services/apiClient";
import {
  mockGooglePlayStatus,
  mockGooglePlayInstallMetrics,
  mockGooglePlayTracks,
  mockGooglePlayAppVersions,
  mockGooglePlayRatings,
  mockGooglePlayReviews,
  mockGooglePlayReviewsList,
  mockGooglePlayCrashAnr,
  mockGooglePlayTracksStatus,
  mockGooglePlayTracksSyncResult,
  mockGooglePlayTracksBackfillResult,
  mockGooglePlayVitalsStatus
} from "./googlePlay.mock";
import {
  GooglePlayIntegrationStatus,
  GooglePlayInstallMetrics,
  GooglePlayReleaseTrack,
  GooglePlayAppVersionStats,
  GooglePlayRatingSummary,
  GooglePlayReviewSummary,
  GooglePlayReviewHandlingStatus,
  GooglePlayCrashAnrSummary,
  GooglePlayTracksStatus,
  GooglePlayTracksSyncResult,
  GooglePlayTracksBackfillResult,
  GooglePlayVitalsStatus,
  GooglePlayVitalsSyncResult
} from "./googlePlay.types";
import { DashboardFilters } from "../../services/adminMetricsService";

// GH#761 — o painel acessa a Android Publisher API via Admin Worker (rota
// /admin/integrations/google-play/*), que detém a service account. A API não
// expõe contagem de downloads/instalações (isso só existe via export CSV pro
// Cloud Storage, configurado à parte no Play Console — não implementado
// aqui); o dado real disponível é a média de rating de uma amostra de reviews.

function formatSyncTimestamp(isoTimestamp: string | null | undefined): string {
  if (!isoTimestamp) return "Nunca sincronizado";
  const date = new Date(isoTimestamp);
  if (Number.isNaN(date.getTime())) return "Nunca sincronizado";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface GooglePlayStatusWorkerResponse {
  source: string;
  packageName?: string;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  hasCredentials: boolean;
  lastSyncTimestamp?: string | null;
  ratingAverage?: number | null;
  reviewsSampled?: number;
}

/**
 * Adapter for Google Play Developer API interface.
 * Retrieves live rollout percentage statistics and uninstallation velocities.
 */
export async function getGooglePlayIntegrationStatus(): Promise<GooglePlayIntegrationStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockGooglePlayStatus, {});
  }

  const raw = await apiClient.request<GooglePlayStatusWorkerResponse>(
    "GET",
    "/admin/integrations/google-play/status"
  );

  return {
    enabled: raw.hasCredentials,
    status: raw.status,
    message: raw.hasCredentials
      ? "Sincronizado via Android Publisher API (service account do Google Cloud)"
      : "Credenciais do Google Play ainda não configuradas no Admin Worker",
    platform: "Android (Google Play Console)",
    lastSyncTimestamp: formatSyncTimestamp(raw.lastSyncTimestamp),
    downloadsImported: 0,
    ratingAverage: raw.ratingAverage ?? null,
    reviewsSampled: raw.reviewsSampled ?? 0,
  };
}

export async function getGooglePlayInstallMetrics(filters: DashboardFilters = {}): Promise<GooglePlayInstallMetrics | null> {
  if (!apiClient.isMockEnabled()) return null;

  const result = await apiClient.simulateFetch(mockGooglePlayInstallMetrics, filters);
  if (filters.environment === "staging") {
    return {
      totalDownloads: Math.round(result.totalDownloads * 0.1),
      activeInstalls: Math.round(result.activeInstalls * 0.1),
      dailyDownloads: Math.round(result.dailyDownloads * 0.1),
      uninstallsThisWeek: Math.round(result.uninstallsThisWeek * 0.1)
    };
  }
  return result;
}

export async function getGooglePlayReleaseTracks(): Promise<GooglePlayReleaseTrack[]> {
  if (!apiClient.isMockEnabled()) return [];
  return apiClient.simulateFetch(mockGooglePlayTracks, {});
}

export async function getGooglePlayAppVersions(filters: DashboardFilters = {}): Promise<GooglePlayAppVersionStats[]> {
  if (!apiClient.isMockEnabled()) return [];

  const result = await apiClient.simulateFetch(mockGooglePlayAppVersions, filters);
  if (filters.environment === "staging") {
    return result.map(v => ({
      ...v,
      activeDownloads: Math.round(v.activeDownloads * 0.1)
    }));
  }
  return result;
}

export async function getGooglePlayRatings(filters: DashboardFilters = {}): Promise<GooglePlayRatingSummary | null> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockGooglePlayRatings, filters);
  }

  // GH#761 (follow-up) — o worker já sincroniza nota média real via
  // /admin/integrations/google-play/status (Android Publisher API,
  // reviews.list), mas esse adapter nunca tinha sido ligado a esse dado.
  // A API não expõe distribuição por estrela (1-5), só a média de uma
  // amostra de reviews — starDistribution fica zerado por falta de fonte,
  // não é dado fabricado.
  const raw = await apiClient.request<GooglePlayStatusWorkerResponse>(
    "GET",
    "/admin/integrations/google-play/status"
  );

  if (raw.ratingAverage == null) return null;

  return {
    averageRating: raw.ratingAverage,
    totalRatings: raw.reviewsSampled ?? 0,
    starDistribution: { five: 0, four: 0, three: 0, two: 0, one: 0 },
  };
}

export async function getGooglePlayReviews(filters: DashboardFilters = {}): Promise<GooglePlayReviewSummary[]> {
  if (!apiClient.isMockEnabled()) return [];
  return apiClient.simulateFetch(mockGooglePlayReviews, filters);
}

export async function getGooglePlayCrashAnrSummary(filters: DashboardFilters = {}): Promise<GooglePlayCrashAnrSummary | null> {
  if (!apiClient.isMockEnabled()) return null;
  return apiClient.simulateFetch(mockGooglePlayCrashAnr, filters);
}

export async function syncGooglePlayMetrics(): Promise<{ jobId: string; status: string; startedAt: string; message?: string }> {
  if (apiClient.isMockEnabled()) {
    return {
      jobId: "job_gp_" + Math.random().toString(36).substring(7),
      status: "started",
      startedAt: new Date().toISOString()
    };
  }

  const raw = await apiClient.request<{ status: string; syncedAt?: string; message?: string }>(
    "POST",
    "/admin/integrations/google-play/sync"
  );
  return {
    jobId: raw.syncedAt ?? "",
    status: raw.status,
    startedAt: raw.syncedAt ?? new Date().toISOString(),
    // GH#873-followup: sem isso a mensagem real do worker (ex: "Falha ao
    // consultar reviews HTTP 403 — app pode ainda não estar publicado") era
    // descartada e a UI so mostrava um "worker retornou erro" generico.
    message: raw.message,
  };
}

// migration 012_play_track.sql — mapeamento version_code -> trilha do Play Console.
export async function getGooglePlayTracksStatus(): Promise<GooglePlayTracksStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockGooglePlayTracksStatus, {});
  }
  return apiClient.request<GooglePlayTracksStatus>("GET", "/admin/integrations/google-play/tracks/status");
}

export async function syncGooglePlayTracks(): Promise<GooglePlayTracksSyncResult> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockGooglePlayTracksSyncResult, {});
  }
  return apiClient.request<GooglePlayTracksSyncResult>("POST", "/admin/integrations/google-play/tracks/sync");
}

export async function backfillGooglePlayTracks(): Promise<GooglePlayTracksBackfillResult> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockGooglePlayTracksBackfillResult, {});
  }
  return apiClient.request<GooglePlayTracksBackfillResult>("POST", "/admin/integrations/google-play/tracks/backfill");
}

// --- GH#1341/#1346 — Android Vitals (ANR rate) e Avaliações completas ---

interface GooglePlayVitalsStatusWorkerResponse {
  source: string;
  packageName?: string;
  status: "connected" | "disabled";
  hasCredentials: boolean;
  lastSyncTimestamp: string | null;
  anrRatePercent: number | null;
  rangeStart: string | null;
  rangeEnd: string | null;
}

export async function getGooglePlayVitalsStatus(): Promise<GooglePlayVitalsStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockGooglePlayVitalsStatus, {});
  }

  const raw = await apiClient.request<GooglePlayVitalsStatusWorkerResponse>(
    "GET",
    "/admin/integrations/google-play/vitals/status"
  );

  return {
    status: raw.status,
    hasCredentials: raw.hasCredentials,
    lastSyncTimestamp: raw.lastSyncTimestamp,
    anrRatePercent: raw.anrRatePercent,
    rangeStart: raw.rangeStart,
    rangeEnd: raw.rangeEnd,
  };
}

export async function syncGooglePlayVitals(): Promise<GooglePlayVitalsSyncResult> {
  if (apiClient.isMockEnabled()) {
    return {
      status: "ok",
      anrRatePercent: mockGooglePlayVitalsStatus.anrRatePercent,
      rangeStart: mockGooglePlayVitalsStatus.rangeStart ?? undefined,
      rangeEnd: mockGooglePlayVitalsStatus.rangeEnd ?? undefined,
      syncedAt: new Date().toISOString(),
    };
  }
  return apiClient.request<GooglePlayVitalsSyncResult>("POST", "/admin/integrations/google-play/vitals/sync");
}

// Linha do D1 (migration 017_gh1341_google_play_reviews.sql) — snake_case, cru, nunca exposta
// direto pra tela; mapeada para GooglePlayReviewSummary por mapGooglePlayReviewRow.
interface GooglePlayReviewRow {
  review_id: string;
  rating: number;
  comment_text: string;
  language: string;
  device: string;
  android_os_version: number | null;
  app_version_code: number | null;
  app_version_name: string;
  review_last_modified: number | null;
  developer_reply_text: string | null;
  developer_reply_at: number | null;
  handling_status: GooglePlayReviewHandlingStatus;
  first_synced_at: number;
  last_synced_at: number;
}

function mapGooglePlayReviewRow(row: GooglePlayReviewRow): GooglePlayReviewSummary {
  return {
    reviewId: row.review_id,
    rating: row.rating,
    comment: row.comment_text ?? "",
    appVersion: row.app_version_name || (row.app_version_code ? String(row.app_version_code) : ""),
    replyText: row.developer_reply_text ?? undefined,
    replyTime: row.developer_reply_at ? new Date(row.developer_reply_at * 1000).toISOString() : undefined,
    commentTime: row.review_last_modified
      ? new Date(row.review_last_modified * 1000).toISOString()
      : new Date(row.last_synced_at * 1000).toISOString(),
    language: row.language || undefined,
    device: row.device || undefined,
    handlingStatus: row.handling_status,
  };
}

export async function getGooglePlayReviewsList(params: {
  handlingStatus?: GooglePlayReviewHandlingStatus;
  limit?: number;
} = {}): Promise<GooglePlayReviewSummary[]> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockGooglePlayReviewsList, params);
  }

  const query = new URLSearchParams();
  if (params.handlingStatus) query.set("handlingStatus", params.handlingStatus);
  query.set("limit", String(params.limit ?? 200));

  const raw = await apiClient.request<{ source: string; reviews: GooglePlayReviewRow[] }>(
    "GET",
    `/admin/integrations/google-play/reviews?${query.toString()}`
  );
  return (raw.reviews ?? []).map(mapGooglePlayReviewRow);
}
