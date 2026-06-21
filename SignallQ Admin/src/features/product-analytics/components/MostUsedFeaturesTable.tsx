import React from "react";
import { FeatureUsageMetric } from "../../../types/productAnalytics";
import { ArrowUpRight, ArrowDownRight, Activity } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";

interface MostUsedFeaturesTableProps {
  metrics: FeatureUsageMetric[];
}

export const MostUsedFeaturesTable: React.FC<MostUsedFeaturesTableProps> = ({ metrics }) => {
  return (
    <SectionCard
      title="Engajamento por Funcionalidade"
      description="Taxas de ativação, uso único e eficiência de execução por ponto de menu operacional."
    >
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-zinc-900 text-[10px] font-mono text-zinc-550 uppercase tracking-wider">
              <th className="py-3 px-4 font-normal">Função</th>
              <th className="py-3 px-4 text-right font-normal">Sessões / Uso</th>
              <th className="py-3 px-4 text-right font-normal">Usuários Únicos</th>
              <th className="py-3 px-4 text-right font-normal">Taxa Conclusão</th>
              <th className="py-3 px-4 text-right font-normal">Taxa Falha</th>
              <th className="py-3 px-4 text-right font-normal">Tempo Médio</th>
              <th className="py-3 px-4 text-right font-normal">Tendência</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-900/40 text-[11px] font-sans">
            {metrics.map((item) => (
              <tr key={item.feature} className="hover:bg-zinc-950/20 transition-colors">
                <td className="py-3.5 px-4">
                  <div className="font-bold text-white flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-purple-500" />
                    {item.label}
                  </div>
                  <span className="text-[10px] font-mono text-zinc-500 block uppercase mt-0.5">{item.feature}</span>
                </td>
                <td className="py-3.5 px-4 text-right font-mono font-medium text-zinc-200">
                  {item.usageCount.toLocaleString("pt-BR")}
                </td>
                <td className="py-3.5 px-4 text-right font-mono text-zinc-300">
                  {item.uniqueUsers.toLocaleString("pt-BR")}
                </td>
                <td className="py-3.5 px-3 text-right">
                  <div className="flex items-center justify-end gap-1.5">
                    <span className="text-zinc-200 font-mono">{(item.completionRate * 100).toFixed(0)}%</span>
                    <div className="w-12 bg-zinc-900 h-1.5 rounded-full overflow-hidden">
                      <div 
                        className="bg-emerald-500 h-full rounded-full" 
                        style={{ width: `${item.completionRate * 100}%` }}
                      />
                    </div>
                  </div>
                </td>
                <td className="py-3.5 px-4 text-right font-mono text-red-400">
                  {(item.failureRate * 100).toFixed(1)}%
                </td>
                <td className="py-3.5 px-4 text-right font-mono text-zinc-400">
                  {(item.avgDurationMs / 1000).toFixed(1)}s
                </td>
                <td className="py-3.5 px-4 text-right text-[10px]">
                  <div className={`inline-flex items-center gap-0.5 font-mono px-2 py-0.5 rounded ${
                    item.trendPercent >= 0 
                      ? "text-emerald-400 bg-emerald-950/20 border border-emerald-500/10"
                      : "text-red-400 bg-red-950/20 border border-red-500/10"
                  }`}>
                    {item.trendPercent >= 0 ? (
                      <ArrowUpRight className="w-3 h-3" />
                    ) : (
                      <ArrowDownRight className="w-3 h-3" />
                    )}
                    <span>{Math.abs(item.trendPercent)}%</span>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </SectionCard>
  );
};
