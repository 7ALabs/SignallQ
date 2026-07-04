export type AiProvider = "gemini_flash" | "cloudflare_qwen" | "openai" | "local_fallback";

/**
 * Execução real de IA persistida em `ai_usage` (D1), lida via
 * GET /admin/metrics/ai-usage/records (GH#421). Sem campo de latência: o
 * schema atual não registra tempo de resposta — não inventar o dado.
 */
export interface AiUsageRecord {
  id: string;
  timestamp: string; // ISO String, de created_at (unix seg)
  /** Nome técnico do modelo, ex.: "@cf/qwen/qwen3-30b-a3b-fp8", "gemini-2.5-flash". */
  model: string;
  /** Nome legível do provedor (mapeado no worker a partir de `model`). */
  provider: string;
  promptTokens: number;
  completionTokens: number;
  costUsd: number;
  status: "success" | "error";
  errorMessage: string | null;
  /** session_id correlacionado em diagnostic_sessions, ou null quando não há associação. */
  diagnosisId: string | null;
  environment: string;
}

/** Série temporal de uso de tokens por provedor por dia. */
export interface AiDailyUsage {
  /** Data no formato ISO YYYY-MM-DD. */
  date: string;
  /** Tokens por provedor: chave = nome do provedor (ex.: "Gemini", "Qwen / Workers AI"). */
  byProvider: Record<string, number>;
}

export interface AiModelInsights {
  provider: AiProvider;
  displayName: string;
  totalCalls: number;
  totalTokens: number;
  averageLatencyMs: number;
  estimatedCostUsd: number;
  reliabilityPercentage: number | null;
}
