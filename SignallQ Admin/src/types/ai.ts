export type AiProvider = "gemini_flash" | "cloudflare_qwen" | "openai" | "local_fallback";

export interface AiUsageRecord {
  id: string;
  timestamp: string; // ISO String
  modelSelected: AiProvider;
  promptTokens: number;
  completionTokens: number;
  costUsd: number;
  latencySec: number;
  status: "success" | "cached" | "failed";
  diagnosisId: string;
}

export interface AiModelInsights {
  provider: AiProvider;
  displayName: string;
  totalCalls: number;
  totalTokens: number;
  averageLatencyMs: number;
  estimatedCostUsd: number;
  reliabilityPercentage: number;
}
