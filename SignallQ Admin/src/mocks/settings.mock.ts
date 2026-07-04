import { AdminSettingsPayload } from "../types/admin";

/**
 * Defaults espelham os mesmos fallbacks usados pelo worker em
 * GET /admin/metrics/alerts (ver index.ts: AI_DAILY_BUDGET, ERROR_THRESHOLD, MIN_SCORE).
 */
export const initialMockSettings: AdminSettingsPayload = {
  aiDailyBudgetUsd: 1.0,
  errorSpikeThreshold: 10,
  criticalScoreThreshold: 50,
};
