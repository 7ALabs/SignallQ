export type AppEnvironment = "production" | "staging" | "all";

export interface SystemErrorLog {
  id: string;
  timestamp: string;
  source: "worker" | "android_app" | "analytics_db" | "ai_gateway";
  // GH#422: categoria por camada — opcional para não quebrar mocks antigos.
  category?: "app" | "backend" | "ia" | "integration";
  message: string;
  stackTrace?: string;
  count: number;
  environment: AppEnvironment;
  resolved: boolean;
  affectedUserCount: number;
  resolvedBy?: string;
  resolvedAt?: string | null;
  resolutionNote?: string;
}

export interface AppVersionDetail {
  id: string;
  versionCode: string; // e.g. "v1.2.4"
  buildNumber: number; // e.g. 1024
  releaseDate: string;
  activeInstallsCount: number;
  rolloutPercentage: number; // 0 to 100
  crashFreeRatePercentage: number; // e.g. 99.8
  status: "stable" | "beta" | "deprecated" | "halted" | "planned";
  notes: string;
  platform?: string;
  source?: string;
  activeUsersPercentage?: number;
  diagnosticsCount?: number;
  successRatePercentage?: number;
  crashesCount?: number;
  anrsCount?: number;
  diagnosticErrorsCount?: number;
  aiErrorsCount?: number;
}

export interface OperatorRecord {
  id: string;
  name: string; // e.g. "Claro", "Vivo", "TIM", "Desktop Internet"
  country: string; // e.g. "Brasil"
  // Tipo de rede dominante nos diagnósticos da operadora (network_type real vindo
  // do Android: "wifi" | "4g" | "5g" | "ethernet" | "mobile" | "fiber" | "cable").
  // null quando o Worker não conseguiu determinar um tipo dominante — nunca inventar valor.
  type: string | null;
  testCount: number;
  averageDownloadMbps: number | null;
  averageUploadMbps: number | null;
  averageLatencyMs: number | null;
  packetLossAverage: number | null;
  // Score médio de diagnóstico (0 a 100), calculado pelo engine local no device.
  // NÃO é pesquisa de satisfação do cliente — não existe essa fonte de dado hoje.
  averageScorePercentage: number | null;
}

/**
 * Ajustes persistidos em `admin_settings` (chave 'admin') e efetivamente
 * consumidos pelo signallq-admin-worker em GET /admin/metrics/alerts
 * (ver GH#426 e docs_ai/technical/admin-api-schema.md).
 *
 * Todo campo deste contrato precisa ter consumidor real no worker ou no app.
 * Campos sem consumidor comprovado (roteamento de IA, quotas de speedtest,
 * webhooks de alerta, retenção, monetização) foram removidos daqui — ver
 * GH#426 para o levantamento completo do que era decorativo.
 */
export interface AdminSettingsPayload {
  /** Custo de IA (USD) acumulado nas últimas 24h acima do qual o alerta AI_BUDGET dispara. */
  aiDailyBudgetUsd: number;
  /** Erros na última hora acima do qual o alerta ERROR_SPIKE dispara. */
  errorSpikeThreshold: number;
  /** Score médio (0-100) nas últimas 24h abaixo do qual o alerta LOW_SCORE dispara. */
  criticalScoreThreshold: number;
}
