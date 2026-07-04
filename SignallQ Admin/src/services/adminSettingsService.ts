import { AdminSettingsPayload } from "../types/admin";
import { initialMockSettings } from "../mocks/settings.mock";
import { apiClient } from "./apiClient";

const STORAGE_KEY = "@signallq/admin_settings_v1";

const REQUIRED_KEYS: (keyof ExtendedSettingsPayload)[] = [
  "aiDailyBudgetUsd",
  "errorSpikeThreshold",
  "criticalScoreThreshold",
];

function isValidSettings(obj: unknown): obj is ExtendedSettingsPayload {
  if (!obj || typeof obj !== "object") return false;
  return REQUIRED_KEYS.every((key) => key in (obj as object));
}

// Mantido como alias por compatibilidade de import — o contrato de settings
// hoje é só o AdminSettingsPayload (GH#426 removeu os campos decorativos).
export type ExtendedSettingsPayload = AdminSettingsPayload;

// GH#416: defaults operacionais reais do worker (mesmos fallbacks usados em
// GET /admin/metrics/alerts quando a tabela `admin_settings` ainda está vazia —
// ver AI_DAILY_BUDGET/ERROR_THRESHOLD/MIN_SCORE em index.ts).
// Não é dado simulado — é o que o worker efetivamente aplica na primeira execução.
// GH#426: contrato reduzido aos 3 campos com consumidor real (ver types/admin.ts).
const defaultAdminSettings: ExtendedSettingsPayload = {
  aiDailyBudgetUsd: 1.0,
  errorSpikeThreshold: 10,
  criticalScoreThreshold: 50,
};

export const adminSettingsService = {
  /**
   * Carrega configurações. Em produção, o worker D1 é a fonte da verdade.
   * localStorage funciona como cache de sessão para evitar re-fetch em cada render,
   * usado apenas quando o worker é inalcançável (dado real previamente obtido).
   * Mock: retorna dados do initialMockSettings.
   *
   * GH#416: nunca retorna dado mockado/fictício em produção. Se o worker falhar e
   * não houver cache real, propaga o erro — a UI deve exibir estado de "sem dados".
   */
  async getSettings(): Promise<ExtendedSettingsPayload> {
    if (apiClient.isMockEnabled()) {
      return { ...initialMockSettings };
    }

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
      // Worker respondeu, mas a tabela ainda está vazia (primeira execução real).
      return { ...defaultAdminSettings };
    } catch (e) {
      console.warn("Falha ao buscar settings do worker — tentando cache local.", e);

      // Fallback: cache local (dado real já obtido antes) se o worker estiver inacessível.
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          const parsed: unknown = JSON.parse(stored);
          if (isValidSettings(parsed)) return parsed;
        }
      } catch {
        // cache corrompido — ignora.
      }

      // Sem worker e sem cache real: não inventa configuração. A UI decide o estado.
      throw e;
    }
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
