import { NetworkTypeStat } from "../../services/adminMetricsService";
import { OperatorRecord } from "../../types/admin";
import { MetricVerdict } from "../../types/metrics";

export interface NetworkOverviewMetrics {
  avgNetworkScore: number | null;
  wifiSessionsPct: number | null;
  operatorsMonitored: number;
}

// GH#746 — mesma escala usada em DiagnosticsMetricGrid (score calculado pelo
// engine local do app, 0-100). Repetida aqui (não importada) porque o helper
// original não é exportado do módulo de diagnostics.
export function scoreVerdict(score: number): MetricVerdict {
  if (score >= 85) return "excelente";
  if (score >= 70) return "bom";
  if (score >= 55) return "regular";
  return "fraco";
}

// Cálculo compartilhado entre NetworksOperatorsPage ("Redes & Provedores") e a
// seção "Rede & Operadora" do Centro de Controle (Overview) — mesma fonte
// (getNetworkTypeStats/getOperatorMetrics), mesmo resultado, sem duplicar
// lógica (spec Lia, Md3DashboardContent.dc.html:68-71: "reaproveite o cálculo
// já existente em NetworksOperatorsPage.tsx").
export function computeNetworkOverviewMetrics(
  networkStats: NetworkTypeStat[],
  operators: OperatorRecord[]
): NetworkOverviewMetrics {
  const wifiStat = networkStats.find((s) => s.name.toLowerCase().includes("wi"));
  const mobileStat = networkStats.find((s) => {
    const n = s.name.toLowerCase();
    return n.includes("móvel") || n.includes("movel") || n.includes("mobile") || n.includes("4g") || n.includes("5g") || n.includes("cellular");
  });

  const scoredStats = networkStats.filter((s) => s.avgScore != null && s.count > 0);
  const scoredSessionCount = scoredStats.reduce((sum, s) => sum + s.count, 0);
  const avgNetworkScore = scoredSessionCount > 0
    ? Math.round(scoredStats.reduce((sum, s) => sum + (s.avgScore as number) * s.count, 0) / scoredSessionCount)
    : null;

  const wifiCount = wifiStat?.count ?? 0;
  const mobileCount = mobileStat?.count ?? 0;
  const wifiMobileTotal = wifiCount + mobileCount;
  const wifiSessionsPct = wifiMobileTotal > 0 ? Math.round((wifiCount / wifiMobileTotal) * 100) : null;

  return {
    avgNetworkScore,
    wifiSessionsPct,
    operatorsMonitored: operators.length,
  };
}
