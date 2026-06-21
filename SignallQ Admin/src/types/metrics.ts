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
