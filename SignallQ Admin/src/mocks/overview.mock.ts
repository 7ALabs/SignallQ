import { TimeSeriesData } from "../types/metrics";

export interface DashboardMetricValue {
  label: string;
  value: string | number;
  trend?: {
    value: number;
    changePercentage: number;
    type: "up" | "down" | "neutral";
    intervalLabel: string;
  };
}

export interface NetworkDistItem {
  name: string;
  value: number;
  color: string;
}

export interface NetworkDistResult {
  data: NetworkDistItem[];
  total: number;
}

export interface TopIssueItem {
  id: string;
  problem: string;
  count: number;
  percentage: number;
}

export interface RecentAlertItem {
  id: string;
  source: string;
  message: string;
  severity: "critical" | "warning";
  timestamp: string;
  count: number;
}

export interface ProviderUsageItem {
  name: string;
  percentage: number;
  tokensProcessed: number;
  color: string;
}

export interface OverviewMetricsResponse {
  diagnosticsCount: DashboardMetricValue;
  activeUsers: DashboardMetricValue;
  successRate: DashboardMetricValue;
  aiCost: DashboardMetricValue;
  topProblem: DashboardMetricValue;
  mostTestType: DashboardMetricValue;
  downloadsToday: DashboardMetricValue;
  activeInstalls: DashboardMetricValue;
  crashFreeUsers: DashboardMetricValue;
  prodVersion: DashboardMetricValue;
}

// 1. Core metric values for PROD - Daily baseline
export const mockOverviewProdToday: OverviewMetricsResponse = {
  diagnosticsCount: {
    label: "Diagnósticos Hoje",
    value: 1248,
    trend: { value: 12.4, changePercentage: 12.4, type: "up" as const, intervalLabel: "vs prévios" },
  },
  activeUsers: {
    label: "Usuários Ativos",
    value: 386,
    trend: { value: 8.2, changePercentage: 8.2, type: "up" as const, intervalLabel: "últimas 24h" },
  },
  successRate: {
    label: "Taxa de Sucesso",
    value: "96,4%",
    trend: { value: 0.15, changePercentage: 0.15, type: "up" as const, intervalLabel: "estabilidade" },
  },
  aiCost: {
    label: "Custo IA Hoje",
    value: "R$ 18,72",
    trend: { value: 3.42, changePercentage: 15.4, type: "up" as const, intervalLabel: "orçamento" },
  },
  topProblem: {
    label: "Problema mais comum",
    value: "Wi-Fi fraco",
    trend: { value: 31, changePercentage: 31, type: "neutral" as const, intervalLabel: "31% dos relatos" },
  },
  mostTestType: {
    label: "Tipo mais testado",
    value: "Wi-Fi · 72%",
    trend: { value: 72, changePercentage: 72, type: "up" as const, intervalLabel: "predominante" },
  },
  downloadsToday: {
    label: "Downloads Hoje",
    value: 214,
    trend: { value: 8.4, changePercentage: 8.4, type: "up" as const, intervalLabel: "Google Play" },
  },
  activeInstalls: {
    label: "Instalações Ativas",
    value: 4820,
    trend: { value: 1.2, changePercentage: 1.2, type: "up" as const, intervalLabel: "parque ativo" },
  },
  crashFreeUsers: {
    label: "Crash-Free Users",
    value: "99,2%",
    trend: { value: 0.05, changePercentage: 0.05, type: "up" as const, intervalLabel: "Crashlytics" },
  },
  prodVersion: {
    label: "Versão em Produção",
    value: "0.18.1",
    trend: { value: 100, changePercentage: 100, type: "neutral" as const, intervalLabel: "rollout 100%" },
  },
};

// 7 Days baseline (scaled for 7 days span)
export const mockOverviewProd7d: OverviewMetricsResponse = {
  diagnosticsCount: {
    label: "Diagnósticos (7d)",
    value: 8652,
    trend: { value: 14.2, changePercentage: 14.2, type: "up" as const, intervalLabel: "vs período anterior" },
  },
  activeUsers: {
    label: "Usuários Ativos (7d)",
    value: 2480,
    trend: { value: 10.5, changePercentage: 10.5, type: "up" as const, intervalLabel: "vs período anterior" },
  },
  successRate: {
    label: "Taxa de Sucesso (7d)",
    value: "96,8%",
    trend: { value: 0.2, changePercentage: 0.2, type: "up" as const, intervalLabel: "estabilidade linear" },
  },
  aiCost: {
    label: "Custo IA (7d)",
    value: "R$ 126,45",
    trend: { value: 18.25, changePercentage: 12.6, type: "up" as const, intervalLabel: "vs período anterior" },
  },
  topProblem: {
    label: "Problema mais comum",
    value: "Wi-Fi fraco",
    trend: { value: 29, changePercentage: 29, type: "neutral" as const, intervalLabel: "29% dos relatos" },
  },
  mostTestType: {
    label: "Tipo mais testado",
    value: "Wi-Fi · 70%",
    trend: { value: 70, changePercentage: 70, type: "up" as const, intervalLabel: "predominante" },
  },
  downloadsToday: {
    label: "Downloads (7d)",
    value: 1490,
    trend: { value: 6.2, changePercentage: 6.2, type: "up" as const, intervalLabel: "Google Play" },
  },
  activeInstalls: {
    label: "Instalações Ativas",
    value: 4820,
    trend: { value: 1.2, changePercentage: 1.2, type: "up" as const, intervalLabel: "parque ativo" },
  },
  crashFreeUsers: {
    label: "Crash-Free Users (7d)",
    value: "99,4%",
    trend: { value: 0.08, changePercentage: 0.08, type: "up" as const, intervalLabel: "Crashlytics" },
  },
  prodVersion: {
    label: "Versão em Produção",
    value: "0.18.1",
    trend: { value: 100, changePercentage: 100, type: "neutral" as const, intervalLabel: "rollout 100%" },
  },
};

// 30 Days baseline (scaled for 30 days span)
export const mockOverviewProd30d: OverviewMetricsResponse = {
  diagnosticsCount: {
    label: "Diagnósticos (30d)",
    value: 36840,
    trend: { value: 18.5, changePercentage: 18.5, type: "up" as const, intervalLabel: "vs período anterior" },
  },
  activeUsers: {
    label: "Usuários Ativos (30d)",
    value: 9410,
    trend: { value: 12.8, changePercentage: 12.8, type: "up" as const, intervalLabel: "vs período anterior" },
  },
  successRate: {
    label: "Taxa de Sucesso (30d)",
    value: "97,1%",
    trend: { value: 0.5, changePercentage: 0.5, type: "up" as const, intervalLabel: "estabilidade acumulada" },
  },
  aiCost: {
    label: "Custo IA (30d)",
    value: "R$ 518,90",
    trend: { value: 42.10, changePercentage: 8.8, type: "up" as const, intervalLabel: "vs período anterior" },
  },
  topProblem: {
    label: "Problema mais comum",
    value: "Wi-Fi fraco",
    trend: { value: 28, changePercentage: 28, type: "neutral" as const, intervalLabel: "28% dos relatos" },
  },
  mostTestType: {
    label: "Tipo mais testado",
    value: "Wi-Fi · 68%",
    trend: { value: 68, changePercentage: 68, type: "up" as const, intervalLabel: "predominante" },
  },
  downloadsToday: {
    label: "Downloads (30d)",
    value: 6240,
    trend: { value: 11.4, changePercentage: 11.4, type: "up" as const, intervalLabel: "Google Play" },
  },
  activeInstalls: {
    label: "Instalações Ativas",
    value: 4820,
    trend: { value: 1.2, changePercentage: 1.2, type: "up" as const, intervalLabel: "parque ativo" },
  },
  crashFreeUsers: {
    label: "Crash-Free Users (30d)",
    value: "99,1%",
    trend: { value: 0.01, changePercentage: 0.01, type: "up" as const, intervalLabel: "Crashlytics" },
  },
  prodVersion: {
    label: "Versão em Produção",
    value: "0.18.1",
    trend: { value: 100, changePercentage: 100, type: "neutral" as const, intervalLabel: "rollout 100%" },
  },
};

// 2. Timeline data - diagnostics vs hour/day (supports line charts)
// Hourly timeline data for "Hoje" (today) screen
export const mockTimelineToday: any[] = [
  { timestamp: "08:00", completedDiagnostics: 45, activeUsers: 22, criticalAlerts: 1 },
  { timestamp: "10:00", completedDiagnostics: 120, activeUsers: 65, criticalAlerts: 2 },
  { timestamp: "12:00", completedDiagnostics: 210, activeUsers: 110, criticalAlerts: 3 },
  { timestamp: "14:00", completedDiagnostics: 185, activeUsers: 95, criticalAlerts: 1 },
  { timestamp: "16:00", completedDiagnostics: 260, activeUsers: 140, criticalAlerts: 4 },
  { timestamp: "18:00", completedDiagnostics: 280, activeUsers: 160, criticalAlerts: 2 },
  { timestamp: "20:00", completedDiagnostics: 148, activeUsers: 84, criticalAlerts: 1 },
];

export const mockTimeline7d: TimeSeriesData[] = [
  { timestamp: "15 Jun", activeUsers: 1200, completedDiagnostics: 4520, criticalAlerts: 14, averageLatency: 28 },
  { timestamp: "16 Jun", activeUsers: 1350, completedDiagnostics: 5120, criticalAlerts: 12, averageLatency: 29 },
  { timestamp: "17 Jun", activeUsers: 1480, completedDiagnostics: 5890, criticalAlerts: 16, averageLatency: 30 },
  { timestamp: "18 Jun", activeUsers: 1720, completedDiagnostics: 6420, criticalAlerts: 15, averageLatency: 28 },
  { timestamp: "19 Jun", activeUsers: 1950, completedDiagnostics: 7100, criticalAlerts: 19, averageLatency: 27 },
  { timestamp: "20 Jun", activeUsers: 2240, completedDiagnostics: 7920, criticalAlerts: 11, averageLatency: 26 },
  { timestamp: "21 Jun", activeUsers: 2480, completedDiagnostics: 8652, criticalAlerts: 8, averageLatency: 28 },
];

export const mockTimeline30d: TimeSeriesData[] = [
  { timestamp: "W1", activeUsers: 5400, completedDiagnostics: 21000, criticalAlerts: 48, averageLatency: 29 },
  { timestamp: "W2", activeUsers: 6800, completedDiagnostics: 24500, criticalAlerts: 52, averageLatency: 28 },
  { timestamp: "W3", activeUsers: 8100, completedDiagnostics: 29800, criticalAlerts: 41, averageLatency: 27 },
  { timestamp: "W4", activeUsers: 9410, completedDiagnostics: 36840, criticalAlerts: 35, averageLatency: 28 },
];

// 3. Network type distribution
export const mockNetworkDistributionList = [
  { name: "Wi-Fi", value: 72, color: "#6C2BFF" },
  { name: "Rede móvel", value: 21, color: "#22C55E" },
  { name: "Fibra", value: 6, color: "#38BDF8" },
  { name: "Ethernet", value: 1, color: "#F5A623" },
];

// 4. Top Issues list
export const mockTopIssuesList: TopIssueItem[] = [
  { id: "issue_1", problem: "Wi-Fi fraco", count: 387, percentage: 31 },
  { id: "issue_2", problem: "Bufferbloat upload", count: 225, percentage: 18 },
  { id: "issue_3", problem: "DNS lento", count: 175, percentage: 14 },
  { id: "issue_4", problem: "Rede móvel congestionada", count: 137, percentage: 11 },
  { id: "issue_5", problem: "Gateway lento", count: 87, percentage: 7 },
];

// 5. Recent Alerts
export const mockRecentAlertsList: RecentAlertItem[] = [
  {
    id: "alert_1",
    source: "Gemini Pro",
    message: "Aumento de timeout no Gemini Flash nas últimas 2 horas",
    severity: "critical",
    timestamp: "2026-06-21T10:15:00-07:00",
    count: 24,
  },
  {
    id: "alert_2",
    source: "App Core v0.18.1",
    message: "Versão 0.18.1 com falha elevada no diagnóstico móvel",
    severity: "critical",
    timestamp: "2026-06-21T09:42:00-07:00",
    count: 14,
  },
  {
    id: "alert_3",
    source: "Cloudflare Worker",
    message: "Worker /diagnosis/explain com latência acima de 3s",
    severity: "warning",
    timestamp: "2026-06-21T10:30:00-07:00",
    count: 8,
  },
  {
    id: "alert_4",
    source: "AI Gateway Routing",
    message: "Fallback Qwen acionado acima do normal",
    severity: "warning",
    timestamp: "2026-06-21T10:05:00-07:00",
    count: 19,
  },
];

// 6. AI Providers Usage
export const mockAiProviderUsageList: ProviderUsageItem[] = [
  { name: "Gemini Flash", percentage: 83, tokensProcessed: 1824500, color: "#6C2BFF" },
  { name: "Cloudflare Qwen", percentage: 15, tokensProcessed: 329000, color: "#38BDF8" },
  { name: "Fallback local", percentage: 2, tokensProcessed: 43900, color: "#6B7280" },
];

// Legacy/Compatibility placeholders to avoid any breaking changes
export const mockOverviewMetrics = {
  activeUsers: {
    label: "Usuários Ativos (24h)",
    value: 386,
    trend: { value: 8.2, changePercentage: 8.2, type: "up" as const, intervalLabel: "vs semana anterior" },
  },
  diagnosticsCount: {
    label: "Diagnósticos Executados (24h)",
    value: 1248,
    trend: { value: 12.4, changePercentage: 12.4, type: "up" as const, intervalLabel: "vs semana anterior" },
  },
  avgLatency: {
    label: "Latência Média Global",
    value: "28 ms",
    trend: { value: 3.1, changePercentage: 3.1, type: "down" as const, intervalLabel: "melhoria vs ontem" },
  },
  packetLossAvg: {
    label: "Perda Média de Pacotes",
    value: "0.24 %",
    trend: { value: 0.05, changePercentage: 17.2, type: "down" as const, intervalLabel: "melhoria vs ontem" },
  },
  speedAvg: {
    label: "Velocidade Média Down",
    value: "84.2 Mbps",
    trend: { value: 4.5, changePercentage: 5.6, type: "up" as const, intervalLabel: "vs semana anterior" },
  },
  speedUpAvg: {
    label: "Velocidade Média Up",
    value: "35.8 Mbps",
    trend: { value: 0.8, changePercentage: 2.2, type: "up" as const, intervalLabel: "vs semana anterior" },
  },
  aiSuccessRate: {
    label: "Sucesso de Diagnósticos de IA",
    value: "96,4%",
    trend: { value: 0.02, changePercentage: 0.02, type: "up" as const, intervalLabel: "estabilidade" },
  },
};

export const mockTimeSeriesData: TimeSeriesData[] = [
  { timestamp: "15 Jun", activeUsers: 210, completedDiagnostics: 820, criticalAlerts: 14, averageLatency: 34 },
  { timestamp: "16 Jun", activeUsers: 240, completedDiagnostics: 910, criticalAlerts: 12, averageLatency: 31 },
  { timestamp: "17 Jun", activeUsers: 280, completedDiagnostics: 990, criticalAlerts: 16, averageLatency: 35 },
  { timestamp: "18 Jun", activeUsers: 310, completedDiagnostics: 1040, criticalAlerts: 15, averageLatency: 29 },
  { timestamp: "19 Jun", activeUsers: 340, completedDiagnostics: 1110, criticalAlerts: 19, averageLatency: 28 },
  { timestamp: "20 Jun", activeUsers: 370, completedDiagnostics: 1190, criticalAlerts: 11, averageLatency: 27 },
  { timestamp: "21 Jun", activeUsers: 386, completedDiagnostics: 1248, criticalAlerts: 8, averageLatency: 28 },
];

export const mockNetworkDistribution = [
  { name: "Wi-Fi (Rede Local)", value: 72, color: "#6C2BFF" },
  { name: "Rede Móvel (4G/5G)", value: 21, color: "#22C55E" },
  { name: "Fibra / Banda Larga", value: 6, color: "#38BDF8" },
  { name: "Ethernet Cabeada", value: 1, color: "#F5A623" },
];
