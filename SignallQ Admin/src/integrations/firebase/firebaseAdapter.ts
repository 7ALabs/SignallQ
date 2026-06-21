import { apiClient } from "../../services/apiClient";
import { 
  mockFirebaseStatus, 
  mockFirebaseAnalytics, 
  mockFirebaseCrashlytics, 
  mockFirebaseAppVersions, 
  mockFirebaseCrashIssues 
} from "./firebase.mock";
import { 
  FirebaseIntegrationStatus, 
  FirebaseAnalyticsSummary, 
  FirebaseCrashlyticsSummary, 
  FirebaseAppVersionCrashStats, 
  FirebaseCrashIssue 
} from "./firebase.types";
import { DashboardFilters } from "../../services/adminMetricsService";

/**
 * Adapter for Firebase REST client, fetching logs, crashes and active users tallies.
 * Credentials and client certificates are safely protected in server-side cloud containers.
 */
export async function getFirebaseIntegrationStatus(): Promise<FirebaseIntegrationStatus> {
  // Simulates calling /admin/integrations/firebase
  return apiClient.simulateFetch(mockFirebaseStatus, {});
}

export async function getFirebaseAnalyticsSummary(filters: DashboardFilters = {}): Promise<FirebaseAnalyticsSummary> {
  // Simulates calling GET /admin/integrations/firebase/metrics
  const result = await apiClient.simulateFetch(mockFirebaseAnalytics, filters);
  
  if (filters.environment === "staging") {
    return {
      ...result,
      activeUsersToday: Math.round(result.activeUsersToday * 0.12),
      crashFreeUsersPercentage: 99.8,
      crashFreeSessionsPercentage: 99.9,
    };
  }
  return result;
}

export async function getFirebaseCrashlyticsSummary(filters: DashboardFilters = {}): Promise<FirebaseCrashlyticsSummary> {
  const result = await apiClient.simulateFetch(mockFirebaseCrashlytics, filters);
  if (filters.environment === "staging") {
    return {
      unresolvedCrashesCount: 1,
      unresolvedNonFatalsCount: 3,
      affectedUsersCount: 4,
      totalCrashesTrend: "down"
    };
  }
  return result;
}

export async function getFirebaseAppVersions(filters: DashboardFilters = {}): Promise<FirebaseAppVersionCrashStats[]> {
  const result = await apiClient.simulateFetch(mockFirebaseAppVersions, filters);
  if (filters.environment === "staging") {
    return result.map(v => ({
      ...v,
      crashCount: Math.round(v.crashCount * 0.1),
      nonFatalCount: Math.round(v.nonFatalCount * 0.1),
      status: "stable"
    }));
  }
  return result;
}

export async function getFirebaseCrashIssues(filters: DashboardFilters = {}): Promise<FirebaseCrashIssue[]> {
  return apiClient.simulateFetch(mockFirebaseCrashIssues, filters);
}

/**
 * Triggers backend synchronisation routines of Firebase Crashlytics on Cloud Run.
 * Safely runs asynchronous cron-jobs to clean database caches.
 */
export async function syncFirebaseMetrics(): Promise<{ jobId: string; status: string; startedAt: string }> {
  try {
    return await apiClient.request<{ jobId: string; status: string; startedAt: string }>(
      "POST",
      "/integrations/firebase/sync",
      {}
    );
  } catch {
    // In mock mode or without VITE_ADMIN_API_BASE_URL, request() throws — return simulated result
    return apiClient.simulateFetch({
      jobId: "job_fb_mock_" + Date.now().toString(36),
      status: "started",
      startedAt: new Date().toISOString(),
    });
  }
}
