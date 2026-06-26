import React from "react";
import { AppEnvironment } from "../../../types/admin";
import { MetricCard } from "../../../components/ui/MetricCard";
import { errorMetricsService } from "../../../services/errorMetricsService";

interface ErrorMetricGridProps {
  environment: AppEnvironment;
}

export const ErrorMetricGrid: React.FC<ErrorMetricGridProps> = ({ environment }) => {
  const [summary, setSummary] = React.useState<{
    activeErrors: string;
    events24h: string;
    impactedUsers: string;
    mainSources: string;
  } | null>(null);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    let active = true;
    setLoading(true);
    errorMetricsService.getErrorMetricSummary({ environment }).then((data) => {
      if (active) {
        setSummary(data);
        setLoading(false);
      }
    }).catch(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [environment]);

  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-24 bg-zinc-900/40 border border-[var(--border)] rounded-2xl animate-pulse" />
        ))}
      </div>
    );
  }

  const metrics = summary
    ? [
        {
          label: "Gargalos / Erros Ativos",
          value: summary.activeErrors,
          trend: { value: 25.0, changePercentage: 25.0, type: "down" as const, intervalLabel: "estabilidade progressiva" }
        },
        {
          label: "Eventos nas últimas 24h",
          value: summary.events24h,
          trend: { value: 14.8, changePercentage: 14.8, type: "down" as const, intervalLabel: "vs período anterior" }
        },
        {
          label: "Usuários Impactados",
          value: summary.impactedUsers,
          trend: { value: 8.2, changePercentage: 8.2, type: "down" as const, intervalLabel: "grupo de teste" }
        },
        {
          label: "Principais Fontes",
          value: summary.mainSources,
          trend: { value: 0, changePercentage: 0, type: "neutral" as const, intervalLabel: "interfaces móveis" }
        }
      ]
    : [];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {metrics.map((m, idx) => (
        <MetricCard
          key={idx}
          label={m.label}
          value={m.value}
          trend={m.trend}
          id={`error-metric-card-${idx}`}
        />
      ))}
    </div>
  );
};
