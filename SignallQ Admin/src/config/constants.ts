export const APP_NAME = "7Agents Admin Console";

export const DEV_ENVIRONMENT_LABEL = "Staging";
export const PROD_ENVIRONMENT_LABEL = "Produção";

// Tokens de cor via CSS custom properties em src/index.css (--sq-*).
// Não usar valores hardcoded — referenciar var(--sq-accent), var(--sq-error), etc.

export const REFRESH_INTERVALS = [
  { label: "Manual", value: 0 },
  { label: "A cada 30s", value: 30000 },
  { label: "A cada 1min", value: 60000 },
  { label: "A cada 5min", value: 300000 },
];

export const PERIOD_FILTERS = [
  { label: "Hoje", value: "today" },
  { label: "Últimos 7 dias", value: "7d" },
  { label: "Últimos 30 dias", value: "30d" },
  { label: "Personalizado", value: "custom" },
];

/**
 * GH#552 (Fase 1) — presets universais para `GlobalFilters` (período/OS aparecem
 * em quase toda tela do wireframe). Listas de versão, operadora e tipo de rede
 * são específicas de cada tela e vêm do contrato de dados dela (Fase 2/3), não
 * daqui — não inventar valores fixos onde o dado real varia por tela.
 */
export const GLOBAL_PERIOD_OPTIONS = [
  { label: "Últimas 24h", value: "24h" },
  { label: "Últimos 7 dias", value: "7d" },
  { label: "Últimos 30 dias", value: "30d" },
];

export const GLOBAL_OS_OPTIONS = [
  { label: "Todos", value: "all" },
  { label: "Android", value: "android" },
  { label: "iOS", value: "ios" },
];
