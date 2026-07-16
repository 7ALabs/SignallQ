import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { BarChart } from "../../../components/charts/BarChart";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";

interface SessionsBarChartProps {
  timelineData: any[];
}

// Substitui o antigo DiagnosticsTimeline (LineChart de 2 séries: Sessões +
// Diagnósticos) — spec Lia (Md3DashboardContent.dc.html:33) pede BarChart de
// 1 série só, título "Sessões · 14 dias". Mesma origem de dado que já existia
// (activeUsers do worker é o proxy de sessões — "Sessões" ainda não tem
// contagem própria por dia no timeline do worker, ver GH#781); só muda o tipo
// de gráfico e o recorte de série, não a origem do dado. Título/legenda são
// texto literal fixo, não variam por período (mesmo padrão do componente
// anterior).
export const SessionsBarChart: React.FC<SessionsBarChartProps> = ({ timelineData }) => {
  return (
    <ChartCard
      title="Sessões · 14 dias"
      description="Volume de sessões no período selecionado."
      id="sessions-bar-chart-card"
    >
      {timelineData.length === 0 ? (
        <FeatureComingSoon
          feature="Gráfico de Sessões"
          reason="Requer série temporal no worker"
        />
      ) : (
        <BarChart
          data={timelineData}
          xAxisKey="timestamp"
          series={[{ key: "activeUsers", name: "Sessões", color: "var(--sq-accent)" }]}
          height={220}
        />
      )}
    </ChartCard>
  );
};
