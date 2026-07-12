import { GeminiQuotaResponse } from "../services/geminiQuotaService";

// #884 — mock reflete o estado honesto mais comum hoje: teto não configurado
// em admin_settings (ninguém checou o valor real no AI Studio ainda), não um
// cenário "tudo verde" fabricado.
export const mockGeminiQuota: GeminiQuotaResponse = {
  source: "not_configured",
  timestamp: new Date().toISOString(),
  quota: {
    requestsPerMinute: {
      available: false, used: null, limit: null, percentage: null,
      reason: "Teto do free tier não configurado — a API do Gemini não expõe consulta de quota em tempo real (só via login em Google AI Studio); preencher manualmente em admin_settings.geminiFreeTierLimits após checar o valor atual no AI Studio.",
    },
    tokensPerMinute: {
      available: false, used: null, limit: null, percentage: null,
      reason: "Teto do free tier não configurado — a API do Gemini não expõe consulta de quota em tempo real (só via login em Google AI Studio); preencher manualmente em admin_settings.geminiFreeTierLimits após checar o valor atual no AI Studio.",
    },
    requestsPerDay: {
      available: false, used: null, limit: null, percentage: null,
      reason: "Teto do free tier não configurado — a API do Gemini não expõe consulta de quota em tempo real (só via login em Google AI Studio); preencher manualmente em admin_settings.geminiFreeTierLimits após checar o valor atual no AI Studio.",
    },
  },
};
