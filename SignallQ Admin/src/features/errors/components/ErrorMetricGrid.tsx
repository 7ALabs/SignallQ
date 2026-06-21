import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";

interface ErrorMetricGridProps {
  environment: "production" | "staging";
}

export const ErrorMetricGrid: React.FC<ErrorMetricGridProps> = ({ environment }) => {
  const isStg = environment === "staging";

  const metrics = [
    {
      label: "Gargalos / Erros Ativos",
      value: isStg ? "1 ativo" : "3 ativos",
      trend: { value: 25.0, changePercentage: 25.0, type: "down" as "up" | "down", intervalLabel: "estabilidade progressiva" }
    },
    {
      label: "Eventos nas últimas 24h",
      value: isStg ? "34 eventos" : "551 eventos",
      trend: { value: 14.8, changePercentage: 14.8, type: "down" as "up" | "down", intervalLabel: "vs período anterior" }
    },
    {
      label: "Usuários Impactados",
      value: isStg ? "15" : "1.480",
      trend: { value: 8.2, changePercentage: 8.2, type: "down" as "up" | "down", intervalLabel: "grupo de teste" }
    },
    {
      label: "Principais Fontes",
      value: "Android / AI Gateway",
      trend: { value: 0, changePercentage: 0, type: "neutral" as "neutral", intervalLabel: "interfaces móveis" }
    }
  ];

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
