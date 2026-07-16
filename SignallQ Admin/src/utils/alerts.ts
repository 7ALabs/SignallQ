export type AlertCategory = "app" | "ia" | "sistema";

export const ALERT_CATEGORY_LABEL: Record<AlertCategory, string> = {
  app: "App",
  ia: "IA & Custos",
  sistema: "Sistema",
};

// Mapeamento determinístico dos tipos reais gerados por generateAndPersistAlerts
// (signallq-admin-worker/src/index.ts) para as 3 categorias do design
// (md3-tobe, Md3DashboardContent.dc.html:94,102,110). Não há um campo
// "category" persistido em `alerts` hoje — o worker só grava `type`
// (AI_BUDGET/ERROR_SPIKE/LOW_SCORE/CLOUDFLARE_USAGE/GEMINI_QUOTA).
//
// AI_BUDGET e GEMINI_QUOTA são inequivocamente custo/IA. CLOUDFLARE_USAGE é
// quota de infra (Workers/D1), não é bug de app nem custo de IA. ERROR_SPIKE
// conta TODOS os system_errors (app/backend/ia/integration somados, sem
// filtro por categoria no worker) e LOW_SCORE é sinal de qualidade de rede —
// nenhum dos dois tem uma categoria "app" defensável na origem, então os dois
// caem em "sistema" (mesmo bucket do exemplo do protótipo, latência de
// Worker) em vez de inventar que são bug de versão do app. Hoje nenhum tipo
// real gerado mapeia para "app" — o bucket existe no componente para quando o
// worker tiver um alerta app-específico (ex.: pico de crash por versão).
const TYPE_TO_CATEGORY: Record<string, AlertCategory> = {
  AI_BUDGET: "ia",
  GEMINI_QUOTA: "ia",
  CLOUDFLARE_USAGE: "sistema",
  ERROR_SPIKE: "sistema",
  LOW_SCORE: "sistema",
};

export function categoryForAlertType(type: string | undefined): AlertCategory | undefined {
  if (!type) return undefined;
  return TYPE_TO_CATEGORY[type];
}

export function alertCategoryLabel(category: AlertCategory | undefined): string {
  return category ? ALERT_CATEGORY_LABEL[category] : "Não disponível";
}
