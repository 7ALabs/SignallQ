import * as firebaseAdapter from "./firebase/firebaseAdapter";
import * as googlePlayAdapter from "./google-play/googlePlayAdapter";
import * as appStoreAdapter from "./app-store/appStoreAdapter";
import { DashboardFilters } from "../services/adminMetricsService";

/**
 * Unified integration service consolidating Firebase, Google Play Console and App Store Connect gateways.
 */
export const integrationsService = {
  // --- FIREBASE SUITE ---
  getFirebaseStatus: firebaseAdapter.getFirebaseIntegrationStatus,
  getFirebaseAnalytics: firebaseAdapter.getFirebaseAnalyticsSummary,
  getFirebaseCrashlytics: firebaseAdapter.getFirebaseCrashlyticsSummary,
  getFirebaseVersions: firebaseAdapter.getFirebaseAppVersions,
  getFirebaseIssues: firebaseAdapter.getFirebaseCrashIssues,
  triggerFirebaseSync: firebaseAdapter.syncFirebaseMetrics,

  // --- GOOGLE PLAY CONSOLE ---
  getGooglePlayStatus: googlePlayAdapter.getGooglePlayIntegrationStatus,
  getGooglePlayInstalls: googlePlayAdapter.getGooglePlayInstallMetrics,
  getGooglePlayTracks: googlePlayAdapter.getGooglePlayReleaseTracks,
  getGooglePlayVersions: googlePlayAdapter.getGooglePlayAppVersions,
  getGooglePlayRatings: googlePlayAdapter.getGooglePlayRatings,
  getGooglePlayReviews: googlePlayAdapter.getGooglePlayReviews,
  getGooglePlayCrashAnr: googlePlayAdapter.getGooglePlayCrashAnrSummary,
  triggerGooglePlaySync: googlePlayAdapter.syncGooglePlayMetrics,
  getGooglePlayTracksStatus: googlePlayAdapter.getGooglePlayTracksStatus,
  triggerGooglePlayTracksSync: googlePlayAdapter.syncGooglePlayTracks,
  triggerGooglePlayTracksBackfill: googlePlayAdapter.backfillGooglePlayTracks,
  getGooglePlayVitalsStatus: googlePlayAdapter.getGooglePlayVitalsStatus,
  triggerGooglePlayVitalsSync: googlePlayAdapter.syncGooglePlayVitals,
  getGooglePlayReviewsList: googlePlayAdapter.getGooglePlayReviewsList,

  // --- APP STORE FUTURE-READY ---
  getAppStoreStatus: appStoreAdapter.getAppStoreIntegrationStatus,
  getAppStoreDownloads: appStoreAdapter.getAppStoreDownloadMetrics,
  getAppStoreVersions: appStoreAdapter.getAppStoreVersions,
  getAppStoreRatings: appStoreAdapter.getAppStoreRatings,
  triggerAppStoreSync: appStoreAdapter.syncAppStoreMetrics,

  /**
   * Retrieves high level summary of all active integration packages.
   */
  async getAllStatus(filters: DashboardFilters = {}) {
    const [fb, gp, as, gpTracks] = await Promise.all([
      firebaseAdapter.getFirebaseIntegrationStatus(),
      googlePlayAdapter.getGooglePlayIntegrationStatus(),
      appStoreAdapter.getAppStoreIntegrationStatus(),
      googlePlayAdapter.getGooglePlayTracksStatus()
    ]);
    return {
      firebase: fb,
      googlePlay: gp,
      appStore: as,
      googlePlayTracks: gpTracks
    };
  }
};
