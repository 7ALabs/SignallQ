import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { OverviewMetricsResponse } from "../../../mocks/overview.mock";

interface OverviewMetricGridProps {
  metrics: OverviewMetricsResponse;
}

export const OverviewMetricGrid: React.FC<OverviewMetricGridProps> = ({ metrics }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
      {/* 1. Diagnósticos Hoje */}
      <MetricCard
        label={metrics.diagnosticsCount.label}
        value={metrics.diagnosticsCount.value}
        trend={metrics.diagnosticsCount.trend}
        source="SignallQ Analytics"
        id="metric-diagnostics"
      />

      {/* 2. Usuários Ativos */}
      <MetricCard
        label={metrics.activeUsers.label}
        value={metrics.activeUsers.value}
        trend={metrics.activeUsers.trend}
        source="Firebase"
        id="metric-users"
      />

      {/* 3. Taxa de Sucesso */}
      <MetricCard
        label={metrics.successRate.label}
        value={metrics.successRate.value}
        trend={metrics.successRate.trend}
        source="SignallQ Analytics"
        id="metric-success-rate"
      />

      {/* 4. Custo IA Hoje */}
      <MetricCard
        label={metrics.aiCost.label}
        value={metrics.aiCost.value}
        trend={metrics.aiCost.trend}
        source="SignallQ Worker"
        id="metric-cost"
      />

      {/* 5. Downloads Hoje */}
      <MetricCard
        label={metrics?.downloadsToday?.label || "Downloads Hoje"}
        value={metrics?.downloadsToday?.value !== undefined ? metrics.downloadsToday.value : 0}
        trend={metrics?.downloadsToday?.trend}
        source="Google Play"
        id="metric-downloads"
      />

      {/* 6. Instalações Ativas */}
      <MetricCard
        label={metrics?.activeInstalls?.label || "Instalações Ativas"}
        value={metrics?.activeInstalls?.value !== undefined ? metrics.activeInstalls.value : 0}
        trend={metrics?.activeInstalls?.trend}
        source="Google Play"
        id="metric-installs"
      />

      {/* 7. Crash-free users */}
      <MetricCard
        label={metrics?.crashFreeUsers?.label || "Crash-Free Users"}
        value={metrics?.crashFreeUsers?.value !== undefined ? metrics.crashFreeUsers.value : "100.0%"}
        trend={metrics?.crashFreeUsers?.trend}
        source="Firebase Crashlytics"
        id="metric-crash-free"
      />

      {/* 8. Versão em produção */}
      <MetricCard
        label={metrics?.prodVersion?.label || "Versão em Produção"}
        value={metrics?.prodVersion?.value !== undefined ? metrics.prodVersion.value : "0.0.0"}
        trend={metrics?.prodVersion?.trend}
        source="Google Play"
        id="metric-prod-version"
      />

      {/* 9. Problema mais comum */}
      <MetricCard
        label={metrics.topProblem.label}
        value={metrics.topProblem.value}
        trend={metrics.topProblem.trend}
        source="SignallQ Analytics"
        id="metric-common-issue"
      />

      {/* 10. Tipo mais testado */}
      <MetricCard
        label={metrics.mostTestType.label}
        value={metrics.mostTestType.value}
        trend={metrics.mostTestType.trend}
        source="SignallQ Analytics"
        id="metric-most-test-type"
      />
    </div>
  );
};
