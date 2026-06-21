import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { DataTable } from "../../../components/ui/DataTable";
import { Cpu, Server, Layers, HelpCircle, HardDrive } from "lucide-react";
import { AiModelInsights } from "../../../types/ai";

interface ProviderCostTableProps {
  insights: AiModelInsights[];
}

export const ProviderCostTable: React.FC<ProviderCostTableProps> = ({ insights }) => {
  const columns = [
    {
      header: "Modelo de Borda (LLM)",
      accessor: (row: AiModelInsights) => {
        let modelIcon = <Cpu className="w-4 h-4 text-purple-400" />;
        if (row.provider === "cloudflare_qwen") modelIcon = <Server className="w-4 h-4 text-amber-500 animate-pulse" />;
        if (row.provider === "openai") modelIcon = <Layers className="w-4 h-4 text-blue-400" />;
        if (row.provider === "local_fallback") modelIcon = <HardDrive className="w-4 h-4 text-zinc-550" />;
        
        return (
          <div className="flex items-center gap-2.5 font-sans">
            <span className="p-1.5 bg-zinc-900 border border-zinc-800 rounded-lg">{modelIcon}</span>
            <div>
              <span className="font-semibold text-white block text-xs">{row.displayName}</span>
              <span className="font-mono text-[9px] text-zinc-500 uppercase">{row.provider}</span>
            </div>
          </div>
        );
      }
    },
    {
      header: "Total de Chamadas",
      accessor: (row: AiModelInsights) => (
        <span className="font-mono text-zinc-400 text-xs">
          {row.totalCalls.toLocaleString("pt-BR")}
        </span>
      )
    },
    {
      header: "Tokens Consumidos",
      accessor: (row: AiModelInsights) => {
        if (row.totalTokens === 0) return <span className="font-mono text-zinc-600">-</span>;
        const millionTokens = (row.totalTokens / 1000000).toFixed(1);
        return (
          <span className="font-mono text-zinc-300 text-xs">
            {row.totalTokens.toLocaleString("pt-BR")}{" "}
            <span className="text-[10px] text-zinc-500 font-sans">({millionTokens}M)</span>
          </span>
        );
      }
    },
    {
      header: "Custo Estimado (USD)",
      accessor: (row: AiModelInsights) => {
        const costVal = row.estimatedCostUsd;
        if (costVal === 0) return <span className="text-[#22C55E] uppercase font-mono text-[10px] bg-emerald-950/20 border border-emerald-500/15 px-2 py-0.5 rounded font-bold">Grátis</span>;
        return (
          <span className="font-mono text-indigo-400 font-bold text-xs">
            {new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2 }).format(costVal)}
          </span>
        );
      }
    },
    {
      header: "Taxa de Sucesso",
      accessor: (row: AiModelInsights) => {
        const val = row.reliabilityPercentage;
        const color = val > 99 ? "text-emerald-400" : val > 97 ? "text-amber-500" : "text-red-400";
        return (
          <span className={`font-mono font-bold text-xs ${color}`}>
            {val.toFixed(2)}%
          </span>
        );
      }
    }
  ];

  return (
    <SectionCard
      title="Custo e Métricas Reais por Provedor"
      description="Taxação síncrona, volume de faturamento e tokens calculados para os Model Gateways."
      id="provider-cost-card"
    >
      <DataTable
        data={insights}
        columns={columns}
        keyExtractor={(row) => row.provider}
        emptyMessage="Nenhum dado consolidado no momento."
        id="provider-cost-table"
      />
    </SectionCard>
  );
};
