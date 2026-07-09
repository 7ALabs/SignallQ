import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { DonutChart } from "../../../components/charts/DonutChart";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { AiModelInsights } from "../../../types/ai";
import { SQ_TOKENS } from "../../../config/designTokens";

interface ProviderCostDonutProps {
  insights: AiModelInsights[];
}

const PROVIDER_COLORS: Record<string, string> = {
  gemini_flash: SQ_TOKENS.aiGemini,
  cloudflare_qwen: SQ_TOKENS.aiQwen,
  openai: SQ_TOKENS.aiOpenAI,
  local_fallback: SQ_TOKENS.aiFallback,
};

// GH#781 (paridade mockup) — "Custo por provedor" em donut, a partir do mesmo
// AiModelInsights[] já usado pela ProviderCostTable (estimatedCostUsd real).
export const ProviderCostDonut: React.FC<ProviderCostDonutProps> = ({ insights }) => {
  const withCost = insights.filter((i) => i.estimatedCostUsd > 0);

  return (
    <ChartCard
      title="Custo por provedor"
      description="Participação de cada provedor de IA no custo total do período."
      id="provider-cost-donut-card"
    >
      {withCost.length === 0 ? (
        <FeatureComingSoon
          feature="Custo por provedor"
          reason="Métrica ainda não disponível — aguardando exposição no worker"
        />
      ) : (
        <DonutChart
          data={withCost.map((i) => ({
            name: i.displayName,
            value: Number(i.estimatedCostUsd.toFixed(2)),
            color: PROVIDER_COLORS[i.provider] ?? SQ_TOKENS.aiFallback,
          }))}
          height={180}
        />
      )}
    </ChartCard>
  );
};
