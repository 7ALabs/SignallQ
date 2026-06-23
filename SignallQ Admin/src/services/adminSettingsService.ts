import { AdminSettingsPayload } from "../types/admin";
import { initialMockSettings } from "../mocks/settings.mock";
import { apiClient } from "./apiClient";

const STORAGE_KEY = "@signallq/admin_settings_v1";

const REQUIRED_KEYS: (keyof ExtendedSettingsPayload)[] = [
  "monthlyBudgetUsd",
  "budgetAction",
  "anonymizeIp",
  "retentionDays",
  "firebaseAnalyticsEnabled",
  "maxAiTokensUserDaily",
  "maxSpeedTestDataDailyMb",
  "contextualAdsEnabled",
  "contextualAdsCategories",
];

function isValidSettings(obj: unknown): obj is ExtendedSettingsPayload {
  if (!obj || typeof obj !== "object") return false;
  return REQUIRED_KEYS.every((key) => key in (obj as object));
}

export interface ExtendedSettingsPayload extends AdminSettingsPayload {
  monthlyBudgetUsd: number;
  budgetAction: "block" | "alert" | "throttle";
  anonymizeIp: boolean;
  retentionDays: number;
  // Advanced behavior & monetization properties
  firebaseAnalyticsEnabled: boolean;
  maxAiTokensUserDaily: number;
  maxSpeedTestDataDailyMb: number;
  contextualAdsEnabled: boolean;
  contextualAdsCategories: string[];
}

export const adminSettingsService = {
  /**
   * Retrieves current configuration payload from LocalStorage or mock fallback
   */
  async getSettings(): Promise<ExtendedSettingsPayload> {
    // Em produção: localStorage tem precedência (edições locais pendentes de sync).
    // Worker /admin/settings retorna {} por ora — usado apenas como fallback futuro.
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed: unknown = JSON.parse(stored);
        if (isValidSettings(parsed)) {
          return parsed;
        }
        console.warn("Settings schema inválido ou desatualizado — descartando e usando padrões.");
        localStorage.removeItem(STORAGE_KEY);
      }
    } catch (e) {
      console.warn("Could not load settings from storage, using initial mock data", e);
    }

    // Sem dados locais: tenta buscar do worker se mock estiver desabilitado.
    if (!apiClient.isMockEnabled()) {
      try {
        const remote = await apiClient.request<{ settings: unknown }>("GET", "/admin/settings");
        if (isValidSettings(remote.settings)) {
          return remote.settings;
        }
      } catch {
        // Worker retorna {} — sem settings remotas ainda. Cai no padrão local.
      }
    }

    return { ...initialMockSettings };
  },

  /**
   * Updates configuration payload in LocalStorage
   */
  async saveSettings(settings: ExtendedSettingsPayload): Promise<{ success: boolean; message: string }> {
    // Worker POST /admin/settings é stub — não persiste no D1.
    // Persiste localmente; sincronização remota será implementada quando o worker suportar.
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
      return {
        success: true,
        message: "Configurações salvas localmente. Sincronização remota pendente de implementação no worker."
      };
    } catch (e) {
      console.error("Could not write config payload", e);
      throw new Error("Falha ao persistir alterações na memória local.");
    }
  }
};
