import { apiClient } from "./apiClient";
import { mockSystemErrors, mockAppVersions } from "../mocks/errors.mock";
import { SystemErrorLog, AppVersionDetail, AdminSettingsPayload, AppEnvironment } from "../types/admin";
import { DashboardFilters } from "./adminMetricsService";

const SETTINGS_LOCAL_STORAGE_KEY = "signallq_admin_settings_v1";

const DEFAULT_SETTINGS: AdminSettingsPayload = {
  selectedDefaultAiModel: "gemini_flash",
  aiFallbackEnabled: true,
  maxTokensPerDiagnostic: 1024,
  speedtestIntervalSeconds: 30,
  androidLogsCollectionEnabled: true,
  stagingAlertWebhookUrl: "https://hooks.example.invalid/signallq/staging",
  productionAlertWebhookUrl: "https://hooks.example.invalid/signallq/production",
  cloudflareWorkerEndpoint: "https://worker.signallq.workers.dev"
};

export const analyticsService = {
  /**
   * Returns active system error metrics from Android SDK, DB pools, or Edge Cloudflare Worker API
   */
  async getErrorMetrics(filters: DashboardFilters = {}): Promise<SystemErrorLog[]> {
    const list = await apiClient.simulateFetch(mockSystemErrors, filters);
    
    let filtered = list;
    if (filters.environment) {
      filtered = filtered.filter(e => e.environment === filters.environment);
    }
    return filtered;
  },

  /**
   * Returns live app deployment rollouts, codes, release note guidelines, and crash ratios
   */
  async getAppVersionMetrics(filters: { search?: string; environment?: AppEnvironment } = {}): Promise<AppVersionDetail[]> {
    const list = await apiClient.simulateFetch(mockAppVersions, filters);
    
    let processed = list;
    if (filters.environment === "staging") {
      processed = list.map(v => {
        if (v.status === "planned") return v;
        return {
          ...v,
          activeInstallsCount: Math.round(v.activeInstallsCount * 0.12),
          diagnosticsCount: v.diagnosticsCount ? Math.round(v.diagnosticsCount * 0.12) : 0,
          crashesCount: v.crashesCount ? Math.round(v.crashesCount * 0.10) : 0,
          anrsCount: v.anrsCount ? Math.round(v.anrsCount * 0.10) : 0,
        };
      });
    }

    if (filters.search) {
      const q = filters.search.toLowerCase();
      return processed.filter(v => 
        v.versionCode.toLowerCase().includes(q) || 
        v.notes.toLowerCase().includes(q) ||
        v.status.toLowerCase().includes(q)
      );
    }
    return processed;
  },

  /**
   * Retrieves active operations variables, loaded from localStorage or default configuration state
   */
  async getAdminSettings(): Promise<AdminSettingsPayload> {
    // Artificial Api call
    await apiClient.simulateFetch(null, {});
    
    if (typeof window !== "undefined") {
      const data = localStorage.getItem(SETTINGS_LOCAL_STORAGE_KEY);
      if (data) {
        try {
          return { ...DEFAULT_SETTINGS, ...JSON.parse(data) };
        } catch (e) {
          console.error("Failed to parse local storage settings, reverting to default parameters", e);
        }
      }
    }
    return DEFAULT_SETTINGS;
  },

  /**
   * Commits modified options either to server PATCH endpoint or save them securely in localStorage state
   */
  async updateAdminSettings(payload: Partial<AdminSettingsPayload>): Promise<{ success: boolean; data: AdminSettingsPayload }> {
    // Real Axios/Fetch PATCH simulation
    await apiClient.request("PATCH", "/admin/settings", payload);
    
    const current = await this.getAdminSettings();
    const updated = { ...current, ...payload };
    
    if (typeof window !== "undefined") {
      localStorage.setItem(SETTINGS_LOCAL_STORAGE_KEY, JSON.stringify(updated));
      console.log("[ApiClient Saved] Successfully synchronized local browser cache with mock telemetry configs.", updated);
    }
    
    return {
      success: true,
      data: updated
    };
  }
};
