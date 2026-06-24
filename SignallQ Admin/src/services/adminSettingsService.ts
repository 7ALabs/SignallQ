import { AdminSettingsPayload } from "../types/admin";
import { initialMockSettings } from "../mocks/settings.mock";
import { apiClient } from "./apiClient";

export interface FeatureFlag {
  key: string;
  description: string;
  enabled: boolean;
}

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
   * Carrega configurações. Em produção, o worker D1 é a fonte da verdade.
   * localStorage funciona como cache de sessão para evitar re-fetch em cada render.
   * Mock: retorna dados do initialMockSettings.
   */
  async getSettings(): Promise<ExtendedSettingsPayload> {
    if (apiClient.isMockEnabled()) {
      return { ...initialMockSettings };
    }

    // Produção: consulta o worker primeiro.
    try {
      const remote = await apiClient.request<{ settings: unknown }>("GET", "/admin/settings");
      if (isValidSettings(remote.settings)) {
        // Atualiza cache local com o dado remoto autoritativo.
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(remote.settings));
        } catch {
          // localStorage indisponível (iframe, privado) — não é bloqueante.
        }
        return remote.settings;
      }
      // Worker retornou {} (primeira execução) — usa defaults e não grava cache.
    } catch (e) {
      console.warn("Falha ao buscar settings do worker — usando cache local ou padrões.", e);

      // Fallback: cache local se o worker estiver inacessível.
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          const parsed: unknown = JSON.parse(stored);
          if (isValidSettings(parsed)) return parsed;
        }
      } catch {
        // cache corrompido — ignora.
      }
    }

    return { ...initialMockSettings };
  },

  async getFeatureFlags(): Promise<FeatureFlag[]> {
    if (apiClient.isMockEnabled()) {
      return [
        { key: "ai_diagnosis_enabled", description: "Ativa o pipeline de diagnóstico por IA", enabled: true },
        { key: "speedtest_enabled", description: "Habilita o módulo de speed test no app", enabled: true },
        { key: "fibra_detection", description: "Detecção automática de conexão de fibra óptica", enabled: false },
        { key: "beta_ui_features", description: "Funcionalidades de UI em fase beta", enabled: false },
      ];
    }
    try {
      const raw = await apiClient.request<{ flags: FeatureFlag[] }>("GET", "/admin/feature-flags");
      return raw.flags ?? [];
    } catch {
      return [];
    }
  },

  async setFeatureFlags(flags: FeatureFlag[]): Promise<{ success: boolean }> {
    if (apiClient.isMockEnabled()) {
      return { success: true };
    }
    await apiClient.request<{ ok: boolean }>("POST", "/admin/feature-flags", { flags } as unknown as Record<string, unknown>);
    return { success: true };
  },

  /**
   * Persiste configurações no worker D1 (produção) ou apenas na memória (mock).
   */
  async saveSettings(settings: ExtendedSettingsPayload): Promise<{ success: boolean; message: string }> {
    if (apiClient.isMockEnabled()) {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
      } catch {
        // sem-op
      }
      return { success: true, message: "Configurações salvas (modo mock)." };
    }

    // Produção: persiste no D1 via worker.
    const remote = await apiClient.request<{ ok: boolean; settings: unknown }>(
      "POST",
      "/admin/settings",
      settings as unknown as Record<string, unknown>
    );

    if (!remote.ok) {
      throw new Error("Worker retornou ok=false ao salvar configurações.");
    }

    // Atualiza cache local após confirmação remota.
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch {
      // sem-op
    }

    return { success: true, message: "Configurações salvas com sucesso." };
  },
};
