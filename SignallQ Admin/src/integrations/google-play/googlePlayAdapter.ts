import { apiClient } from "../../services/apiClient";
import {
  mockGooglePlayStatus,
  mockGooglePlayInstallMetrics,
  mockGooglePlayTracks,
  mockGooglePlayAppVersions,
  mockGooglePlayRatings,
  mockGooglePlayReviews,
  mockGooglePlayCrashAnr
} from "./googlePlay.mock";
import {
  GooglePlayIntegrationStatus,
  GooglePlayInstallMetrics,
  GooglePlayReleaseTrack,
  GooglePlayAppVersionStats,
  GooglePlayRatingSummary,
  GooglePlayReviewSummary,
  GooglePlayCrashAnrSummary
} from "./googlePlay.types";
import { DashboardFilters } from "../../services/adminMetricsService";

// O painel NÃO acessa o Google Play Developer API diretamente.
// Não há rota equivalente no Admin Worker — todos os dados do Google Play são
// exclusivamente de modo mock. Em produção, retornar sentinelas vazias.

/**
 * Adapter for Google Play Developer API interface.
 * Retrieves live rollout percentage statistics and uninstallation velocities.
 */
export async function getGooglePlayIntegrationStatus(): Promise<GooglePlayIntegrationStatus> {
  if (!apiClient.isMockEnabled()) {
    return {
      enabled: false,
      status: "disabled",
      message: "Integração com o Google Play Developer API ainda não está disponível no Admin Worker.",
      platform: "Android (Google Play Console)",
      lastSyncTimestamp: "Nunca sincronizado",
      downloadsImported: 0
    };
  }
  return apiClient.simulateFetch(mockGooglePlayStatus, {});
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
  if (!apiClient.isMockEnabled()) return null;
  return apiClient.simulateFetch(mockGooglePlayRatings, filters);
}

export async function getGooglePlayReviews(filters: DashboardFilters = {}): Promise<GooglePlayReviewSummary[]> {
  if (!apiClient.isMockEnabled()) return [];
  return apiClient.simulateFetch(mockGooglePlayReviews, filters);
}

export async function getGooglePlayCrashAnrSummary(filters: DashboardFilters = {}): Promise<GooglePlayCrashAnrSummary | null> {
  if (!apiClient.isMockEnabled()) return null;
  return apiClient.simulateFetch(mockGooglePlayCrashAnr, filters);
}

export async function syncGooglePlayMetrics(): Promise<{ jobId: string; status: string; startedAt: string }> {
  if (!apiClient.isMockEnabled()) {
    return {
      jobId: "",
      status: "not_implemented",
      startedAt: new Date().toISOString()
    };
  }
  return {
    jobId: "job_gp_" + Math.random().toString(36).substring(7),
    status: "started",
    startedAt: new Date().toISOString()
  };
}
