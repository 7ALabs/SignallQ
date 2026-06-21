import { AiUsageRecord, AiModelInsights } from "../types/ai";

export const mockAiUsageRecords: AiUsageRecord[] = [
  {
    id: "ai_req_9a2f1c8d",
    timestamp: "2026-06-21T10:15:05-07:00",
    modelSelected: "gemini_flash",
    promptTokens: 820,
    completionTokens: 310,
    costUsd: 0.000192,
    latencySec: 0.94,
    status: "success",
    diagnosisId: "diag_8f3d1e90",
  },
  {
    id: "ai_req_4b1a3765",
    timestamp: "2026-06-21T10:04:19-07:00",
    modelSelected: "gemini_flash",
    promptTokens: 790,
    completionTokens: 250,
    costUsd: 0.000168,
    latencySec: 0.82,
    status: "success",
    diagnosisId: "diag_2a9c4e12",
  },
  {
    id: "ai_req_12f8e910",
    timestamp: "2026-06-21T09:12:12-07:00",
    modelSelected: "cloudflare_qwen",
    promptTokens: 840,
    completionTokens: 290,
    costUsd: 0.000085, // Cloudflare Workers AI custom rate
    latencySec: 1.45,
    status: "success",
    diagnosisId: "diag_56a67e10",
  },
  {
    id: "ai_req_9f31dd62",
    timestamp: "2026-06-21T08:15:22-07:00",
    modelSelected: "openai",
    promptTokens: 810,
    completionTokens: 0,
    costUsd: 0.000000,
    latencySec: 15.00,
    status: "failed",
    diagnosisId: "diag_901c27df",
  },
  {
    id: "ai_req_ff619283",
    timestamp: "2026-06-21T07:44:01-07:00",
    modelSelected: "local_fallback",
    promptTokens: 0,
    completionTokens: 0,
    costUsd: 0.0,
    latencySec: 0.05,
    status: "cached",
    diagnosisId: "diag_mock_cached",
  },
  {
    id: "ai_req_c10a4590",
    timestamp: "2026-06-21T07:30:15-07:00",
    modelSelected: "gemini_flash",
    promptTokens: 805,
    completionTokens: 280,
    costUsd: 0.000181,
    latencySec: 0.89,
    status: "success",
    diagnosisId: "diag_901231ff",
  }
];

export const mockAiModelInsights: AiModelInsights[] = [
  {
    provider: "gemini_flash",
    displayName: "Google Gemini 1.5 Flash",
    totalCalls: 84500,
    totalTokens: 92850000,
    averageLatencyMs: 880,
    estimatedCostUsd: 14.85, // Highly optimized, fraction of a cent per call
    reliabilityPercentage: 99.96,
  },
  {
    provider: "cloudflare_qwen",
    displayName: "Qwen 2.5 on Workers AI",
    totalCalls: 12100,
    totalTokens: 13310000,
    averageLatencyMs: 1420,
    estimatedCostUsd: 2.42,
    reliabilityPercentage: 99.45,
  },
  {
    provider: "openai",
    displayName: "OpenAI GPT-4o Mini",
    totalCalls: 3100,
    totalTokens: 3410000,
    averageLatencyMs: 1250,
    estimatedCostUsd: 1.54,
    reliabilityPercentage: 98.12,
  },
  {
    provider: "local_fallback",
    displayName: "Fallback Offline Local",
    totalCalls: 850,
    totalTokens: 0,
    averageLatencyMs: 38,
    estimatedCostUsd: 0.0,
    reliabilityPercentage: 100.0,
  }
];

export const mockAiDailyCostsTimeSeries = [
  { date: "15 Jun", geminiCost: 1.25, cloudflareCost: 0.12, openaiCost: 0.15 },
  { date: "16 Jun", geminiCost: 1.44, cloudflareCost: 0.18, openaiCost: 0.22 },
  { date: "17 Jun", geminiCost: 1.82, cloudflareCost: 0.25, openaiCost: 0.354 },
  { date: "18 Jun", geminiCost: 2.10, cloudflareCost: 0.28, openaiCost: 0.24 },
  { date: "19 Jun", geminiCost: 2.45, cloudflareCost: 0.34, openaiCost: 0.29 },
  { date: "20 Jun", geminiCost: 2.95, cloudflareCost: 0.38, openaiCost: 0.18 },
  { date: "21 Jun", geminiCost: 3.12, cloudflareCost: 0.41, openaiCost: 0.12 },
];
