import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { FeatureUsageMetric } from "../../../types/productAnalytics";

interface FeatureRankingBarsProps {
  metrics: FeatureUsageMetric[];
}

// GH#781 (paridade mockup) — ranking de barras de progresso por funcionalidade,
// no lugar do BarChart genérico. Mesmo dado real de feature_usage, composição
// mais próxima da referência ("FUNCIONALIDADE MAIS USADA · SESSÕES 7 DIAS").
export const FeatureRankingBars: React.FC<FeatureRankingBarsProps> = ({ metrics }) => {
  const total = metrics.reduce((sum, f) => sum + f.usageCount, 0);
  const ranked = [...metrics].sort((a, b) => b.usageCount - a.usageCount);

  return (
    <ChartCard
      title="Funcionalidade mais usada"
      description="Participação de cada funcionalidade no volume total de uso, no período."
      id="feature-ranking-bars-card"
    >
      <div className="space-y-4 py-1">
        {ranked.map((f) => {
          const pct = total > 0 ? Math.round((f.usageCount / total) * 100) : 0;
          return (
            <div key={f.feature}>
              <div className="flex justify-between items-baseline mb-1.5">
                <span className="text-[13px] font-medium font-sans text-[var(--text-primary)]">{f.label}</span>
                <span className="text-[13px] font-semibold font-sans text-[var(--text-secondary)]">
                  {pct}% &middot; {f.usageCount.toLocaleString("pt-BR")}
                </span>
              </div>
              <div className="h-2 rounded-full overflow-hidden" style={{ backgroundColor: "var(--bg-base)" }}>
                <div
                  className="h-full rounded-full"
                  style={{ width: `${pct}%`, backgroundColor: "var(--primary)" }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </ChartCard>
  );
};
