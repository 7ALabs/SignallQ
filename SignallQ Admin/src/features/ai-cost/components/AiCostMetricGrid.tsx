import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { aiUsageService } from "../../../services/aiUsageService";

interface AiCostMetricGridProps {
  environment: "production" | "staging";
  period?: string;
}

export const AiCostMetricGrid: React.FC<AiCostMetricGridProps> = ({ environment, period }) => {
  const [summary, setSummary] = React.useState<{
    totalCostUsd: string;
    totalRequests: string;
    avgCostPerRequest: string;
    tokensSentM: string;
    tokensReceivedM: string;
    successRate: string;
  } | null>(null);

  React.useEffect(() => {
    let active = true;
    const p = (period === "today" ? "1d" : period) as "today" | "7d" | "30d" | undefined;
    aiUsageService.getAiCostSummary({ environment, period: p }).then((data) => {
      if (active) setSummary(data);
    });
    return () => { active = false; };
  }, [environment, period]);

  if (!summary) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {Array.from({ length: 6 }).map((_, idx) => (
          <div key={idx} className="h-24 bg-zinc-950/40 border border-zinc-900 rounded-xl animate-pulse" />
        ))}
      </div>
    );
  }

  const metrics = [
    {
      label: "Custo Total Previsto (USD)",
      value: summary.totalCostUsd,
      trend: { value: 4.5, changePercentage: 4.5, type: "up" as const, intervalLabel: "vs semana anterior" }
    },
    {
      label: "Total de Requisições IA",
      value: summary.totalRequests,
      trend: { value: 8.2, changePercentage: 8.2, type: "up" as const, intervalLabel: "volume de laudos" }
    },
    {
      label: "Custo Médio / Laudo",
      value: summary.avgCostPerRequest,
      trend: { value: 0.8, changePercentage: 0.8, type: "down" as const, intervalLabel: "otimização Gemini" }
    },
    {
      label: "Tokens Enviados (M)",
      value: summary.tokensSentM,
      trend: { value: 12.4, changePercentage: 12.4, type: "up" as const, intervalLabel: "contexto de rádio" }
    },
    {
      label: "Tokens Recebidos (M)",
      value: summary.tokensReceivedM,
      trend: { value: 6.8, changePercentage: 6.8, type: "up" as const, intervalLabel: "laudos de mitigação" }
    },
    {
      label: "Sucesso de Conexão API",
      value: summary.successRate,
      trend: { value: 0.02, changePercentage: 0.02, type: "up" as const, intervalLabel: "taxa de resiliência" }
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
