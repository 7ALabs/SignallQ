import React from "react";
import { FeatureAiUsageMetric } from "../../../types/productAnalytics";
import { Cpu, Info } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { productAnalyticsService } from "../../../services/productAnalyticsService";

interface FeatureTokenUsagePanelProps {
  metrics: FeatureAiUsageMetric[];
  period?: string;
}

export const FeatureTokenUsagePanel: React.FC<FeatureTokenUsagePanelProps> = ({ metrics, period }) => {
  const [summary, setSummary] = React.useState<{
    totalTokensLabel: string;
    avgCostPerDiagnosis: string;
    mainProvider: string;
    failureRate: string;
  } | null>(null);

  React.useEffect(() => {
    let active = true;
    const p = (period === "today" ? "1d" : period) as "1d" | "7d" | "30d" | undefined;
    productAnalyticsService.getTokenUsageSummary({ period: p }).then((data) => {
      if (active) setSummary(data);
    });
    return () => { active = false; };
  }, [period]);

  return (
    <SectionCard
      title="Consumo de IA e Unidades de Computação por Função"
      description="Rastreamento operacional de tokens consumidos e custos de inferência da API, por feature."
    >
      <div className="space-y-6">

        {/* Top aggregate metric cards */}
        {summary ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 select-none">
            <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
              <span className="block text-[8px] font-mono text-zinc-500 uppercase">Total Tokens Hoje</span>
              <div className="text-sm font-bold text-white font-mono mt-1">{summary.totalTokensLabel}</div>
            </div>
            <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
              <span className="block text-[8px] font-mono text-zinc-500 uppercase">Custo Médio p/ Diagnóstico</span>
              <div className="text-sm font-bold text-amber-500 font-mono mt-1">{summary.avgCostPerDiagnosis}</div>
            </div>
            <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
              <span className="block text-[8px] font-mono text-zinc-500 uppercase">Provedor Principal</span>
              <div className="text-sm font-bold text-[var(--text-primary)] font-sans mt-1">{summary.mainProvider}</div>
            </div>
            <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
              <span className="block text-[8px] font-mono text-zinc-500 uppercase">Falhas / JSON Inválido</span>
              <div className="text-sm font-bold text-emerald-500 font-mono mt-1">{summary.failureRate}</div>
            </div>
          </div>
        ) : (
          <p className="text-xs text-zinc-500 font-mono py-2">Resumo de tokens indisponível neste ambiente.</p>
        )}

        {/* Dynamic bar breakdown list */}
        {metrics.length > 0 && (
          <div className="space-y-4">
            <span className="block text-[10px] font-mono text-zinc-400 uppercase tracking-wider font-semibold">Consumo por Segmento</span>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {metrics.map((item) => (
                <div
                  key={item.feature}
                  className="p-4 bg-zinc-950/60 border border-zinc-900 rounded-xl space-y-3 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-bold text-white font-sans">{item.label}</span>
                      <Cpu className="w-3.5 h-3.5 text-[var(--text-secondary)]" />
                    </div>

                    <div className="h-1 bg-zinc-900 w-full rounded overflow-hidden mb-3">
                      <div
                        className="bg-[var(--text-primary)] h-full rounded"
                        style={{ width: `${Math.min(100, (item.totalTokens / 15000000) * 100)}%` }}
                      />
                    </div>

                    <div className="space-y-1.5 text-[10px] font-mono text-zinc-450">
                      <div className="flex justify-between">
                        <span>Chamadas IA:</span>
                        <strong className="text-zinc-200">{item.aiCalls.toLocaleString("pt-BR")}</strong>
                      </div>
                      <div className="flex justify-between">
                        <span>Pre-fill (Input):</span>
                        <span className="text-zinc-300">{(item.tokensInput / 1000000).toFixed(1)}M tokens</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Criação (Output):</span>
                        <span className="text-zinc-300">{(item.tokensOutput / 1000000).toFixed(1)}M tokens</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Latência Média:</span>
                        <span className="text-zinc-300">{item.avgLatencyMs}ms</span>
                      </div>
                      {item.computeUnits !== undefined && (
                        <div className="flex justify-between text-indigo-400">
                          <span>Uni. Computação:</span>
                          <span>{item.computeUnits} CU</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="border-t border-zinc-900/60 pt-2.5 flex items-center justify-between text-[11px] font-bold text-white">
                    <span>Custo Estimado</span>
                    <span className="text-amber-500 font-mono">R$ {item.estimatedCost.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="text-[10px] bg-zinc-950 px-3 py-2 border border-zinc-900 rounded-lg flex items-center gap-2 text-zinc-450 italic font-mono select-none">
          <Info className="w-3.5 h-3.5 text-zinc-500 font-normal mt-0.5 shrink-0" />
          <span>Observação: Métricas de inferência são estimadas com base em contagem de tokens reais. A métrica de Compute Units (CU) é de preenchimento opcional dependendo do provedor.</span>
        </div>
      </div>
    </SectionCard>
  );
};
