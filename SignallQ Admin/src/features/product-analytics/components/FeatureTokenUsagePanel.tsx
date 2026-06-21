import React from "react";
import { FeatureAiUsageMetric } from "../../../types/productAnalytics";
import { Cpu, DollarSign, RefreshCw, BarChart2, Radio, Info } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";

interface FeatureTokenUsagePanelProps {
  metrics: FeatureAiUsageMetric[];
}

export const FeatureTokenUsagePanel: React.FC<FeatureTokenUsagePanelProps> = ({ metrics }) => {
  return (
    <SectionCard
      title="Consumo de IA e Unidades de Computação por Função"
      description="Rastreamento operacional de tokens consumidos, custos de inferência da API e eficácia do fallback local de modelos."
    >
      <div className="space-y-6">
        
        {/* Top aggregate metric cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 select-none">
          <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Total Tokens Hoje</span>
            <div className="text-sm font-bold text-white font-mono mt-1">26.1M tokens</div>
          </div>
          <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Custo Médio p/ Diagnóstico</span>
            <div className="text-sm font-bold text-amber-500 font-mono mt-1">R$ 0,014</div>
          </div>
          <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Provedor Principal</span>
            <div className="text-sm font-bold text-purple-400 font-sans mt-1">Google Gemini (100%)</div>
          </div>
          <div className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Falhas / JSON Inválido</span>
            <div className="text-sm font-bold text-emerald-500 font-mono mt-1">0% (Invalidações: 0)</div>
          </div>
        </div>

        {/* Dynamic bar breakdown list */}
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
                    <Cpu className="w-3.5 h-3.5 text-purple-400" />
                  </div>
                  
                  {/* Progress bar */}
                  <div className="h-1 bg-zinc-900 w-full rounded overflow-hidden mb-3">
                    <div 
                      className="bg-purple-500 h-full rounded"
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

            {/* Offline Fallback display card - Static or Mock represented */}
            <div className="p-4 bg-zinc-950/20 border border-dashed border-zinc-900 rounded-xl space-y-3 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-bold text-zinc-400 font-sans">Fallback Local (On-device)</span>
                  <Radio className="w-3.5 h-3.5 text-zinc-500" />
                </div>
                
                <div className="h-1 bg-zinc-900 w-full rounded overflow-hidden mb-3">
                  <div className="bg-zinc-700 h-full rounded w-1/12" />
                </div>

                <div className="space-y-1.5 text-[10px] font-mono text-zinc-500">
                  <div className="flex justify-between">
                    <span>Ajustes Efetuados:</span>
                    <strong>410 instâncias</strong>
                  </div>
                  <div className="flex justify-between">
                    <span>Tokens Remotos:</span>
                    <span>0 tokens (100% offline)</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Mecanismo:</span>
                    <span>Análise Rígida Regex</span>
                  </div>
                </div>
              </div>

              <div className="border-t border-dashed border-zinc-900 pt-2.5 flex items-center justify-between text-[11px] font-bold text-zinc-500">
                <span>Custo Adic. API</span>
                <span className="font-mono">R$ 0,00</span>
              </div>
            </div>
          </div>
          
        </div>

        {/* Compute guidance message */}
        <div className="text-[10px] bg-zinc-950 px-3 py-2 border border-zinc-900 rounded-lg flex items-center gap-2 text-zinc-450 italic font-mono select-none">
          <Info className="w-3.5 h-3.5 text-zinc-500 font-normal mt-0.5 shrink-0" />
          <span>Observação: Métricas de inferência são estimadas com base em contagem de tokens reais. A métrica de Compute Units (CU) é de preenchimento opcional dependendo do provedor.</span>
        </div>

      </div>
    </SectionCard>
  );
};
