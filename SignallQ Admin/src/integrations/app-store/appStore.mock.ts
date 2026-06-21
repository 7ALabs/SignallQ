import { 
  AppStoreIntegrationStatus,
  AppStoreDownloadMetrics,
  AppStoreVersionStats,
  AppStoreCrashSummary,
  AppStoreRatingSummary
} from "./appStore.types";

export const mockAppStoreStatus: AppStoreIntegrationStatus = {
  enabled: false,
  status: "planned",
  message: "Integração planejada para futuro aplicativo iOS nativo. Não ativo no momento.",
  platform: "Apple iOS (App Store Connect)",
  lastSyncTimestamp: "Nunca"
};

export const mockAppStoreDownloads: AppStoreDownloadMetrics = {
  totalDownloads: 0,
  activeInstalls: 0,
  uninstallsThisWeek: 0
};

export const mockAppStoreVersions: AppStoreVersionStats[] = [];

export const mockAppStoreCrash: AppStoreCrashSummary = {
  crashCountWeekly: 0,
  crashFreeSessionRate: 100
};

export const mockAppStoreRatings: AppStoreRatingSummary = {
  averageRating: 0
};
