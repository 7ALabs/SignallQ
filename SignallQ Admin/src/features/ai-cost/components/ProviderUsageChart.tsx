import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { DonutChart } from "../../../components/charts/DonutChart";
import { AiModelInsights } from "../../../types/ai";
import { SQ_TOKENS } from "../../../config/designTokens";

interface ProviderUsageChartProps {
  insights: AiModelInsights[];
}

export const ProviderUsageChart: React.FC<ProviderUsageChartProps> = ({ insights }) => {
  const chartData = React.useMemo(() => {
    return insights.map((item) => {
      let color: string = SQ_TOKENS.accent;
      if (item.provider === "cloudflare_qwen") color = SQ_TOKENS.providerCloudflare;
      if (item.provider === "openai")           color = SQ_TOKENS.providerOpenAI;
      if (item.provider === "local_fallback")   color = SQ_TOKENS.providerLocal;

      return {
        name:  item.displayName || item.provider,
        value: item.totalCalls,
        color,
      };
    });
  }, [insights]);

  return (
    <ChartCard
      title="Distribuição Volumétrica de Modelos"
      description="Quota de requisições de laudos inteligentes processadas por cada inteligência atrópica de rádio."
      id="provider-usage-chart-card"
    >
      <div className="py-2.5">
        <DonutChart data={chartData} height={170} id="ai-provider-usage-donut" />
      </div>
    </ChartCard>
  );
};
