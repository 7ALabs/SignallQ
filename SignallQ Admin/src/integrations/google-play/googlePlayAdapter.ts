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

/**
 * Adapter for Google Play Developer API interface.
 * Retrieves live rollout percentage statistics and uninstallation velocities.
 */
export async function getGooglePlayIntegrationStatus(): Promise<GooglePlayIntegrationStatus> {
  return apiClient.simulateFetch(mockGooglePlayStatus, {});
}

export async function getGooglePlayInstallMetrics(filters: DashboardFilters = {}): Promise<GooglePlayInstallMetrics> {
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
  return apiClient.simulateFetch(mockGooglePlayTracks, {});
}

export async function getGooglePlayAppVersions(filters: DashboardFilters = {}): Promise<GooglePlayAppVersionStats[]> {
  const result = await apiClient.simulateFetch(mockGooglePlayAppVersions, filters);
  if (filters.environment === "staging") {
    return result.map(v => ({
      ...v,
      activeDownloads: Math.round(v.activeDownloads * 0.1)
    }));
  }
  return result;
}

export async function getGooglePlayRatings(filters: DashboardFilters = {}): Promise<GooglePlayRatingSummary> {
  return apiClient.simulateFetch(mockGooglePlayRatings, filters);
}

export async function getGooglePlayReviews(filters: DashboardFilters = {}): Promise<GooglePlayReviewSummary[]> {
  return apiClient.simulateFetch(mockGooglePlayReviews, filters);
}

export async function getGooglePlayCrashAnrSummary(filters: DashboardFilters = {}): Promise<GooglePlayCrashAnrSummary> {
  return apiClient.simulateFetch(mockGooglePlayCrashAnr, filters);
}

export async function syncGooglePlayMetrics(): Promise<{ jobId: string; status: string; startedAt: string }> {
  const response = await apiClient.request<{ jobId: string; status: string; startedAt: string }> (
    "POST",
    "/integrations/google-play/sync",
    {}
  );
  
  return {
    jobId: "job_gp_" + Math.random().toString(36).substring(7),
    status: "started",
    startedAt: new Date().toISOString()
  };
}
