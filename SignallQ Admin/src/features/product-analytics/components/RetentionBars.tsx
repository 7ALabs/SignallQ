import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { RetentionMetric } from "../../../types/productAnalytics";

interface RetentionBarsProps {
  metrics: RetentionMetric[];
}

// GH#781 (paridade mockup) — retenção D1/D7/D30 em barras verticais, mesmo
// dado real já usado no KPI grid e no RetentionPanel (productAnalyticsService.getRetention).
export const RetentionBars: React.FC<RetentionBarsProps> = ({ metrics }) => {
  const m = metrics[0];
  const points = [
    { label: "D1", value: m?.day1 ?? null },
    { label: "D7", value: m?.day7 ?? null },
    { label: "D30", value: m?.day30 ?? null },
  ];
  const hasAny = points.some((p) => p.value != null);

  return (
    <ChartCard
      title="Retenção"
      description="Percentual de dispositivos do cohort que retornaram em D1, D7 e D30."
      id="retention-bars-card"
    >
      {!hasAny ? (
        <FeatureComingSoon
          feature="Retenção D1/D7/D30"
          reason="Métrica ainda não disponível — aguardando exposição no worker"
        />
      ) : (
        <div className="flex items-end justify-around gap-6 h-32 pt-2">
          {points.map((p) => (
            <div key={p.label} className="flex-1 flex flex-col items-center justify-end gap-2 h-full">
              <span className="text-sm font-bold font-sans text-[var(--text-primary)]">
                {p.value != null ? `${p.value.toFixed(0)}%` : "—"}
              </span>
              <div
                className="w-9 rounded-t-md"
                style={{
                  height: p.value != null ? `${Math.max(6, p.value)}%` : "4px",
                  backgroundColor: p.value != null ? "var(--primary)" : "var(--border)",
                }}
              />
              <span className="text-[11px] font-sans text-[var(--text-tertiary)]">{p.label}</span>
            </div>
          ))}
        </div>
      )}
    </ChartCard>
  );
};
