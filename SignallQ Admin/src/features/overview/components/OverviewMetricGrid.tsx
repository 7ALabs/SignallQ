import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { OverviewMetricsResponse } from "../../../mocks/overview.mock";

interface OverviewMetricGridProps {
  metrics: OverviewMetricsResponse;
}

// GH#746 — Centro de Controle é a tela-bandeira: wireframe pede 3-5 KPIs
// respondendo só "o SignallQ está saudável agora?" (volume, estabilidade, IA,
// custo). O grid anterior tinha 10 cards, incluindo dado já duplicado na mesma
// tela (topProblem já aparece em TopIssuesPanel; mostTestType já aparece em
// NetworkTypeDistribution, ambos logo abaixo) e dado que já tem casa própria em
// telas específicas (prodVersion → Releases & Qualidade/VersionsTab,
// crashFreeUsers → Problemas & Incidentes/ErrorsPage, downloadsToday/
// activeInstalls → Play Store, sem tela própria ainda porque o app não está
// publicado). Os campos continuam no contrato de `OverviewMetricsResponse`
// (services/adminMetricsService) para quando essas telas precisarem deles —
// só pararam de ser renderizados aqui, para não competir com o gráfico
// principal (DiagnosticsTimeline) por atenção.
export const OverviewMetricGrid: React.FC<OverviewMetricGridProps> = ({ metrics }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* 1. Volume — quanto o SignallQ está sendo usado agora */}
      <MetricCard
        label={metrics.diagnosticsCount.label}
        value={metrics.diagnosticsCount.value}
        trend={metrics.diagnosticsCount.trend}
        source="SignallQ Analytics"
        id="metric-diagnostics"
      />

      {/* 2. Alcance — quantas pessoas estão usando */}
      <MetricCard
        label={metrics.activeUsers.label}
        value={metrics.activeUsers.value}
        trend={metrics.activeUsers.trend}
        source="Firebase"
        id="metric-users"
      />

      {/* 3. Estabilidade — o diagnóstico está completando com sucesso */}
      <MetricCard
        label={metrics.successRate?.label ?? "Taxa de Sucesso"}
        value={metrics.successRate?.value ?? "sem dados"}
        trend={metrics.successRate?.trend}
        source="SignallQ Analytics"
        id="metric-success-rate"
      />

      {/* 4. Custo — a IA está consumindo orçamento sob controle */}
      <MetricCard
        label={metrics.aiCost.label}
        value={metrics.aiCost.value}
        trend={metrics.aiCost.trend}
        source="SignallQ Worker"
        id="metric-cost"
      />
    </div>
  );
};
