import { AppEnvironment } from "./admin";

// GH#422: categoriza a origem por camada, independente do `source` livre —
// 'app' (erro real do app, ex.: feature_crash), 'backend' (worker),
// 'ia' (pipeline de IA/custo), 'integration' (Firebase/BigQuery/GA4).
export type SystemErrorCategory = "app" | "backend" | "ia" | "integration";

export interface SystemError {
  id: string;
  timestamp: string;
  // Fase A: worker envia strings livres ('ingest', 'ai-usage', etc.).
  // Union mantida para compatibilidade com mocks; string captura o runtime real.
  source: "ai_gateway" | "android_app" | "worker" | "analytics_db" | (string & {});
  category?: SystemErrorCategory;
  message: string;
  stackTrace: string;
  count: number;
  environment: AppEnvironment;
  resolved: boolean;
  affectedUserCount: number;
  // GH#422: preenchidos quando o erro é tratado — responsável, data e observação.
  resolvedBy?: string;
  resolvedAt?: string | null;
  resolutionNote?: string;
}
