import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { AiModelInsights } from "../../../types/ai";
import { MetricVerdict } from "../../../types/metrics";

interface AiCostMetricGridProps {
  costSummary: {
    totalCostUsd: string;
    reliabilityPercentage: number | null;
  } | null;
  modelInsights: AiModelInsights[];
}

// GH#552 (Fase 3) — taxa de fallback (chamadas fora do provider primário Gemini):
// abaixo de 10% é operação normal, acima disso já indica instabilidade do
// primário sendo compensada pelo fallback Qwen3 (ver docs_ai/technical/CLOUDFLARE.md).
function fallbackVerdict(rate: number): MetricVerdict {
  if (rate < 5) return "excelente";
  if (rate < 10) return "bom";
  if (rate < 25) return "regular";
  return "fraco";
}

// Taxa de falha: proxy de (100 - reliabilityPercentage), ver SIG-125.
function failureVerdict(rate: number): MetricVerdict {
  if (rate < 2) return "excelente";
  if (rate < 5) return "bom";
  if (rate < 10) return "regular";
  return "fraco";
}

export const AiCostMetricGrid: React.FC<AiCostMetricGridProps> = ({ costSummary, modelInsights }) => {
  const totalCalls = modelInsights.reduce((s, m) => s + m.totalCalls, 0);
  const fallbackCalls = modelInsights
    .filter((m) => !`${m.provider} ${m.displayName}`.toLowerCase().includes("gemini"))
    .reduce((s, m) => s + m.totalCalls, 0);
  const fallbackRate = totalCalls > 0 ? (fallbackCalls / totalCalls) * 100 : null;

  const reliability = costSummary?.reliabilityPercentage ?? null;
  const failureRate = reliability != null ? Math.max(0, 100 - reliability) : null;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <MetricCard
        label="Custo no período"
        value={costSummary?.totalCostUsd ?? "—"}
        id="ai-cost-metric-total"
      />

      {fallbackRate !== null ? (
        <MetricCard
          label="Taxa de fallback"
          value={`${fallbackRate.toFixed(1)}%`}
          verdict={fallbackVerdict(fallbackRate)}
          verdictNote="chamadas fora do provider primário (Gemini)"
          id="ai-cost-metric-fallback"
        />
      ) : (
        <FeatureComingSoon feature="Taxa de Fallback" reason="Sem chamadas de IA registradas no período" compact />
      )}

      {failureRate !== null ? (
        <MetricCard
          label="Taxa de falha"
          value={`${failureRate.toFixed(1)}%`}
          verdict={failureVerdict(failureRate)}
          verdictNote="proxy: completion_tokens = 0"
          id="ai-cost-metric-failure"
        />
      ) : (
        <FeatureComingSoon feature="Taxa de Falha" reason="Sem chamadas de IA registradas no período" compact />
      )}

      {/* Latência P95 pedida pelo wireframe não é rastreada no schema hoje
          (ai_usage não tem coluna de tempo de resposta) — sem número inventado. */}
      <FeatureComingSoon feature="Latência P95 da IA" reason="Schema ai_usage não registra tempo de resposta" compact />
    </div>
  );
};
