import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { LineChart } from "../../../components/charts/LineChart";
import { AiDailyUsage } from "../../../types/ai";
import { SQ_TOKENS } from "../../../config/designTokens";

// Cores por provedor — tokens do design system SignallQ.
// Mantido em sincronia com --sq-provider-* em src/index.css via SQ_TOKENS.
const PROVIDER_COLORS: Record<string, string> = {
  "Gemini":             SQ_TOKENS.accent,
  "Qwen / Workers AI":  SQ_TOKENS.providerCloudflare,
  "local_fallback":     SQ_TOKENS.providerLocal,
  "OpenAI GPT":         SQ_TOKENS.providerOpenAI,
  "Anthropic Claude":   SQ_TOKENS.providerAnthropic,
};

const DEFAULT_COLOR = SQ_TOKENS.textSecondary;

interface AiCostTimelineProps {
  timelineData: AiDailyUsage[];
}

export const AiCostTimeline: React.FC<AiCostTimelineProps> = ({ timelineData }) => {
  if (timelineData.length === 0) {
    return (
      <ChartCard
        title="Uso de IA — Tokens por Provedor por Dia"
        description="Volume diário de tokens processados, segmentado por provedor de IA."
        id="ai-usage-timeline"
      >
        <div className="flex items-center justify-center h-[260px] text-center px-4">
          <div>
            <p
              className="text-xs font-mono uppercase tracking-widest"
              style={{ color: "var(--sq-text-tertiary)" }}
            >
              Sem dados no período
            </p>
            <p
              className="text-[11px] mt-1"
              style={{ color: "var(--sq-text-tertiary)" }}
            >
              Nenhuma inferência registrada no intervalo selecionado.
            </p>
          </div>
        </div>
      </ChartCard>
    );
  }

  // Coleta todos os provedores presentes na série para construir as linhas dinamicamente.
  const providerSet = new Set<string>();
  for (const day of timelineData) {
    for (const key of Object.keys(day.byProvider)) {
      providerSet.add(key);
    }
  }
  const providers = Array.from(providerSet);

  // Transforma AiDailyUsage[] → objeto achatado por provedor (shape esperado pelo LineChart).
  const chartData = timelineData.map(day => {
    const flat: Record<string, string | number> = { date: day.date };
    for (const p of providers) {
      flat[p] = day.byProvider[p] ?? 0;
    }
    return flat;
  });

  const series = providers.map(p => ({
    key:   p,
    name:  p,
    color: PROVIDER_COLORS[p] ?? DEFAULT_COLOR,
  }));

  return (
    <ChartCard
      title="Uso de IA — Tokens por Provedor por Dia"
      description="Volume diário de tokens processados, segmentado por provedor de IA."
      id="ai-usage-timeline"
    >
      <LineChart
        data={chartData}
        xAxisKey="date"
        series={series}
        height={260}
      />
    </ChartCard>
  );
};
