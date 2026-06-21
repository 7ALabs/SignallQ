import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";

interface AiCostMetricGridProps {
  environment: "production" | "staging";
}

export const AiCostMetricGrid: React.FC<AiCostMetricGridProps> = ({ environment }) => {
  const isStg = environment === "staging";

  const metrics = [
    {
      label: "Custo Total Previsto (USD)",
      value: isStg ? "$14,80" : "$184,50",
      trend: { value: 4.5, changePercentage: 4.5, type: "up" as "up", intervalLabel: "vs semana anterior" }
    },
    {
      label: "Total de Requisições IA",
      value: isStg ? "920" : "12.450",
      trend: { value: 8.2, changePercentage: 8.2, type: "up" as "up", intervalLabel: "volume de laudos" }
    },
    {
      label: "Custo Médio / Laudo",
      value: "$0,0148",
      trend: { value: 0.8, changePercentage: 0.8, type: "down" as "up" | "down", intervalLabel: "otimização Gemini" }
    },
    {
      label: "Tokens Enviados (M)",
      value: isStg ? "3,8M" : "45,2M",
      trend: { value: 12.4, changePercentage: 12.4, type: "up" as "up", intervalLabel: "contexto de rádio" }
    },
    {
      label: "Tokens Recebidos (M)",
      value: isStg ? "0,9M" : "12,8M",
      trend: { value: 6.8, changePercentage: 6.8, type: "up" as "up", intervalLabel: "laudos de mitigação" }
    },
    {
      label: "Sucesso de Conexão API",
      value: isStg ? "99,2%" : "99,6%",
      trend: { value: 0.02, changePercentage: 0.02, type: "up" as "up", intervalLabel: "taxa de resiliência" }
    }
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
      {metrics.map((m, idx) => (
        <MetricCard
          key={idx}
          label={m.label}
          value={m.value}
          trend={m.trend}
          id={`ai-cost-metric-${idx}`}
        />
      ))}
    </div>
  );
};
