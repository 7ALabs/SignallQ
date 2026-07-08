import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { RetentionMetric } from "../../../types/productAnalytics";
import { MetricVerdict } from "../../../types/metrics";

interface FeatureUsageGridProps {
  retention: RetentionMetric[];
  sessionDuration: { avgDurationMs: number | null; sessionCount: number } | null;
}

// GH#552 (Fase 3) — benchmark de mercado para apps utilitários brasileiros
// (ver conhecimento de domínio do Felipe / persona): D1 saudável 25-40%,
// abaixo de 20% é alerta; D7 aceitável 10-20%.
function retentionVerdict(value: number | null, kind: "d1" | "d7"): MetricVerdict | undefined {
  if (value == null) return undefined;
  if (kind === "d1") {
    if (value >= 25 && value <= 40) return "bom";
    if (value > 40) return "excelente";
    if (value >= 20) return "regular";
    return "fraco";
  }
  if (value >= 10 && value <= 20) return "bom";
  if (value > 20) return "excelente";
  if (value >= 6) return "regular";
  return "fraco";
}

function formatDuration(ms: number | null | undefined): string {
  if (ms == null) return "—";
  const totalSec = Math.round(ms / 1000);
  const min = Math.floor(totalSec / 60);
  const sec = totalSec % 60;
  return min > 0 ? `${min}m ${sec}s` : `${sec}s`;
}

export const FeatureUsageGrid: React.FC<FeatureUsageGridProps> = ({ retention, sessionDuration }) => {
  const cohort = retention[0];
  const d1 = cohort?.day1 ?? null;
  const d7 = cohort?.day7 ?? null;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {d1 != null ? (
        <MetricCard
          label="Retenção D1"
          value={`${d1.toFixed(0)}%`}
          verdict={retentionVerdict(d1, "d1")}
          verdictNote="referência de mercado: 25-40%"
          id="product-grid-metric-d1"
        />
      ) : (
        <FeatureComingSoon feature="Retenção D1" reason="Cohort ainda sem dispositivos com 1 dia de histórico" compact />
      )}

      {d7 != null ? (
        <MetricCard
          label="Retenção D7"
          value={`${d7.toFixed(0)}%`}
          verdict={retentionVerdict(d7, "d7")}
          verdictNote="referência de mercado: 10-20%"
          id="product-grid-metric-d7"
        />
      ) : (
        <FeatureComingSoon feature="Retenção D7" reason="Cohort ainda sem dispositivos com 7 dias de histórico" compact />
      )}

      <MetricCard
        label="Duração média de sessão"
        value={formatDuration(sessionDuration?.avgDurationMs)}
        id="product-grid-metric-session-duration"
      />

      <MetricCard
        label="Sessões no período"
        value={sessionDuration?.sessionCount ?? 0}
        id="product-grid-metric-session-count"
      />
    </div>
  );
};
