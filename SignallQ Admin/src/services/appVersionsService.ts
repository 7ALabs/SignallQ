import { apiClient } from "./apiClient";
import { DashboardFilters } from "./adminMetricsService";
import { mockAppVersions } from "../mocks/appVersions.mock";

export interface AppVersionUsage {
  appVersion: string;
  versionCode: number | null;
  distChannel: string;
  buildType: string;
  sessions: number;
  avgScore: number | null;
  firstSeen: number | null;
  lastSeen: number | null;
}

export interface AppVersionsResponse {
  versions: AppVersionUsage[];
  productionVersion: AppVersionUsage | null;
}

const EMPTY_RESPONSE: AppVersionsResponse = { versions: [], productionVersion: null };

export const appVersionsService = {
  /**
   * Fonte real: D1 `diagnostic_sessions` (via GET /admin/metrics/app-versions).
   * Não depende de Firebase/BigQuery — versão, canal de distribuição e volume de
   * sessões por release já são reportados pelo app no ingest (GH#423).
   */
  async getAppVersions(filters: DashboardFilters = {}): Promise<AppVersionsResponse> {
    if (apiClient.isMockEnabled()) {
      return apiClient.simulateFetch(mockAppVersionsFor(filters), filters);
    }

    if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return EMPTY_RESPONSE;

    try {
      const period = filters.period === "today" ? "1d" : (filters.period ?? "30d");
      const env = filters.environment ?? "production";
      const raw = await apiClient.request<{
        versions: AppVersionUsage[];
        productionVersion: AppVersionUsage | null;
      }>("GET", `/admin/metrics/app-versions?environment=${env}&period=${period}`);

      return {
        versions: raw.versions ?? [],
        productionVersion: raw.productionVersion ?? null,
      };
    } catch {
      return EMPTY_RESPONSE;
    }
  },
};

function mockAppVersionsFor(filters: DashboardFilters): AppVersionsResponse {
  if (filters.environment === "staging") {
    return {
      versions: mockAppVersions.versions.map((v) => ({
        ...v,
        sessions: Math.round(v.sessions * 0.08),
        distChannel: "firebase_app_distribution",
      })),
      productionVersion: null,
    };
  }
  return mockAppVersions;
}
