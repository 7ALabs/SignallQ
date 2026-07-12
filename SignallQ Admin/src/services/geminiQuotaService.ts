import { apiClient } from "./apiClient";
import { mockGeminiQuota } from "../mocks/geminiQuota.mock";

export interface QuotaMetric {
  available: boolean;
  used: number | null;
  limit: number | null;
  percentage: number | null;
  reason?: string;
}

export interface GeminiQuotaResponse {
  source: "d1" | "not_configured";
  timestamp: string;
  quota: {
    requestsPerMinute: QuotaMetric;
    tokensPerMinute: QuotaMetric;
    requestsPerDay: QuotaMetric;
  };
}

// #884 — a API do Gemini não expõe consulta de quota em tempo real (só via
// login em Google AI Studio). O "usado" vem de contagem real no worker
// (ai_usage); o teto do free tier é configurável em admin_settings e, sem ele,
// a resposta já vem com available: false + motivo — sem número fabricado.
export const geminiQuotaService = {
  async getQuota(): Promise<GeminiQuotaResponse> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return apiClient.simulateFetch(mockGeminiQuota, { endpoint: "ai-quota" });
    }

    return apiClient.request<GeminiQuotaResponse>("GET", "/admin/metrics/ai-quota");
  },
};
