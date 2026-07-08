export interface TimeSeriesData {
  timestamp: string; // "YYYY-MM-DD" or similar
  activeUsers: number;
  completedDiagnostics: number;
  criticalAlerts: number;
  averageLatency: number;
}

export interface MetricTrend {
  value: number;
  changePercentage: number;
  type: "up" | "down" | "neutral";
  intervalLabel: string; // e.g. "vs últimos 7 dias"
}

export interface MetricWithTrend {
  label: string;
  value: string | number;
  trend?: MetricTrend;
  format?: "number" | "percentage" | "ms" | "mbps" | "usd";
}

/**
 * GH#552: escala de veredito humano obrigatória para KPI — nunca número cru.
 * Mesma regra do app mobile (ver `.claude/skills/linka-design/HANDOFF_README.md`),
 * adaptada ao contexto de dashboard. "forte" e "excelente" têm o mesmo peso
 * semântico (tom positivo); mantidos como rótulos separados porque o contexto
 * de mercado às vezes pede um ou outro (ex.: "sinal forte" vs "retenção excelente").
 */
export type MetricVerdict = "excelente" | "bom" | "regular" | "fraco" | "forte";

/** Tom semântico derivado do veredito — usado para cor do badge no KPI card. */
export type MetricVerdictTone = "positive" | "neutral" | "negative";

export const METRIC_VERDICT_TONE: Record<MetricVerdict, MetricVerdictTone> = {
  excelente: "positive",
  forte: "positive",
  bom: "positive",
  regular: "neutral",
  fraco: "negative",
};
