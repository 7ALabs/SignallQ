import { apiClient } from "../../services/apiClient";
import { 
  mockAppStoreStatus, 
  mockAppStoreDownloads, 
  mockAppStoreVersions, 
  mockAppStoreRatings 
} from "./appStore.mock";
import { 
  AppStoreIntegrationStatus, 
  AppStoreDownloadMetrics, 
  AppStoreVersionStats, 
  AppStoreRatingSummary 
} from "./appStore.types";
import { DashboardFilters } from "../../services/adminMetricsService";

/**
 * Adapter for Apple App Store Connect API.
 * Marked as inactive (Planned) inside local build configurations.
 */
export async function getAppStoreIntegrationStatus(): Promise<AppStoreIntegrationStatus> {
  return apiClient.simulateFetch(mockAppStoreStatus, {});
}

export async function getAppStoreDownloadMetrics(filters: DashboardFilters = {}): Promise<AppStoreDownloadMetrics> {
  return apiClient.simulateFetch(mockAppStoreDownloads, filters);
}

export async function getAppStoreVersions(filters: DashboardFilters = {}): Promise<AppStoreVersionStats[]> {
  return apiClient.simulateFetch(mockAppStoreVersions, filters);
}

export async function getAppStoreRatings(filters: DashboardFilters = {}): Promise<AppStoreRatingSummary> {
  return apiClient.simulateFetch(mockAppStoreRatings, filters);
}

export async function syncAppStoreMetrics(): Promise<{ success: boolean; message: string }> {
  // Gracefully fails or informs that App Store sync isn't enabled yet
  await apiClient.simulateFetch(null, {});
  return {
    success: false,
    message: "A integração com a App Store Connect não está ativa nesta versão."
  };
}
