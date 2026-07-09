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
          <div key={i} className="h-24 bg-zinc-900/40 border border-[var(--border)] rounded-[var(--radius-card)] animate-pulse" />
        ))}
      </div>
    );
  }

  // GH#552: o worker expõe só o período selecionado, sem comparação com o
  // período anterior (ver errorMetricsService.getErrorMetricSummary) — não há
  // dado real de tendência hoje. Mostrar volume cru sem trend em vez de
  // inventar uma variação percentual, na mesma linha do resto do painel
  // (Overview/Product Analytics/Networks recusam correlação sem dado real).
  const metrics = summary
    ? [
        { label: "Gargalos / Erros Ativos", value: summary.activeErrors },
        { label: "Eventos nas últimas 24h", value: summary.events24h },
        { label: "Usuários Impactados", value: summary.impactedUsers },
        { label: "Principais Fontes", value: summary.mainSources },
      ]
    : [];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {metrics.map((m, idx) => (
        <MetricCard
          key={idx}
          label={m.label}
          value={m.value}
          id={`error-metric-card-${idx}`}
        />
      ))}
    </div>
  );
};
