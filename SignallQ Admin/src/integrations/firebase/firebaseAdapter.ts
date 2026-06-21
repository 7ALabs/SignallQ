import { apiClient } from "../../services/apiClient";
import {
  mockFirebaseStatus,
  mockFirebaseAnalytics,
  mockFirebaseCrashlytics,
  mockFirebaseAppVersions,
  mockFirebaseCrashIssues,
} from "./firebase.mock";
import {
  FirebaseIntegrationStatus,
  FirebaseAnalyticsSummary,
  FirebaseCrashlyticsSummary,
  FirebaseAppVersionCrashStats,
  FirebaseCrashIssue,
} from "./firebase.types";
import { DashboardFilters } from "../../services/adminMetricsService";

// O painel NÃO acessa o Firebase diretamente.
// Em modo mock: usa dados locais para desenvolvimento.
// Em modo real: todas as chamadas vão ao Cloudflare Admin Worker, que detém
// as credenciais do Firebase service account e serve dados já normalizados.

function buildQuery(filters: DashboardFilters): string {
  const params = new URLSearchParams();
  if (filters.environment) params.set("environment", filters.environment);
  if (filters.period) params.set("period", filters.period);
  return params.toString() ? `?${params.toString()}` : "";
}

export async function getFirebaseIntegrationStatus(): Promise<FirebaseIntegrationStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseStatus, {});
  }
  return apiClient.request<FirebaseIntegrationStatus>(
    "GET",
    "/admin/integrations/firebase/status"
  );
}

export async function getFirebaseAnalyticsSummary(
  filters: DashboardFilters = {}
): Promise<FirebaseAnalyticsSummary> {
  if (apiClient.isMockEnabled()) {
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
  return apiClient.request<FirebaseAnalyticsSummary>(
    "GET",
    `/admin/integrations/firebase/analytics${buildQuery(filters)}`
  );
}

export async function getFirebaseCrashlyticsSummary(
  filters: DashboardFilters = {}
): Promise<FirebaseCrashlyticsSummary> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseCrashlytics, filters);
    if (filters.environment === "staging") {
      return {
        unresolvedCrashesCount: 1,
        unresolvedNonFatalsCount: 3,
        affectedUsersCount: 4,
        totalCrashesTrend: "down",
      };
    }
    return result;
  }
  return apiClient.request<FirebaseCrashlyticsSummary>(
    "GET",
    `/admin/integrations/firebase/crashlytics${buildQuery(filters)}`
  );
}

export async function getFirebaseAppVersions(
  filters: DashboardFilters = {}
): Promise<FirebaseAppVersionCrashStats[]> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseAppVersions, filters);
    if (filters.environment === "staging") {
      return result.map((v) => ({
        ...v,
        crashCount: Math.round(v.crashCount * 0.1),
        nonFatalCount: Math.round(v.nonFatalCount * 0.1),
        status: "stable",
      }));
    }
    return result;
  }
  return apiClient.request<FirebaseAppVersionCrashStats[]>(
    "GET",
    `/admin/integrations/firebase/versions${buildQuery(filters)}`
  );
}

export async function getFirebaseCrashIssues(
  filters: DashboardFilters = {}
): Promise<FirebaseCrashIssue[]> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseCrashIssues, filters);
  }
  return apiClient.request<FirebaseCrashIssue[]>(
    "GET",
    `/admin/integrations/firebase/crash-issues${buildQuery(filters)}`
  );
}

export async function syncFirebaseMetrics(): Promise<{ jobId: string; status: string; startedAt: string }> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch({
      jobId: "job_fb_mock_" + Date.now().toString(36),
      status: "started",
      startedAt: new Date().toISOString(),
    });
  }
  return apiClient.request<{ jobId: string; status: string; startedAt: string }>(
    "POST",
    "/admin/integrations/firebase/sync",
    {}
  );
}
