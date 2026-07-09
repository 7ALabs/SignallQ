import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { DonutChart } from "../../../components/charts/DonutChart";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { ScreenNavigationMetric } from "../../../types/productAnalytics";
import { SQ_TOKENS } from "../../../config/designTokens";

interface ScreenSessionsDonutProps {
  screens: ScreenNavigationMetric[];
}

const SLICE_COLORS = [
  SQ_TOKENS.aiGemini,
  "var(--accent-blue)",
  "var(--success)",
  "var(--warning)",
  "var(--text-tertiary)",
];

// GH#781 (paridade mockup) — "Sessões por tela" substitui o antigo donut de
// "Distribuição por Tipo de Rede" nesta posição: screen_navigation
// (Firebase Analytics via /admin/metrics/analytics/product) já traz `views`
// por tela, que é o dado real mais próximo do pedido pelo mockup ("sessões
// por tela"). Distribuição por tipo de rede continua real e visível em
// Redes & Provedores — não foi descartada, só realocada pro lugar certo.
export const ScreenSessionsDonut: React.FC<ScreenSessionsDonutProps> = ({ screens }) => {
  const top = [...screens].sort((a, b) => b.views - a.views).slice(0, 5);

  return (
    <ChartCard
      title="Sessões por tela"
      description="Views por tela no período, via Firebase Analytics (screen_navigation)."
      id="screen-sessions-donut-card"
    >
      {top.length === 0 ? (
        <FeatureComingSoon
          feature="Sessões por tela"
          reason="Métrica ainda não disponível — aguardando exposição no worker"
        />
      ) : (
        <DonutChart
          data={top.map((s, idx) => ({
            name: s.label,
            value: s.views,
            color: SLICE_COLORS[idx % SLICE_COLORS.length],
          }))}
          height={180}
        />
      )}
    </ChartCard>
  );
};
