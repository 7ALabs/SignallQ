import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { DataTable } from "../../../components/ui/DataTable";
import { Cpu, Server, Layers, HardDrive } from "lucide-react";
import { AiModelInsights } from "../../../types/ai";
import { formatCurrency } from "../../../utils/format";
import { alpha } from "../../../utils/color";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { SQ_TOKENS } from "../../../config/designTokens";

interface ProviderCostTableProps {
  insights: AiModelInsights[];
}

// Resolve cor e ícone do provider a partir dos tokens do DS.
function providerMeta(provider: string): { color: string; Icon: React.ElementType } {
  switch (provider) {
    case "cloudflare_qwen": return { color: SQ_TOKENS.providerCloudflare, Icon: Server };
    case "openai":          return { color: SQ_TOKENS.providerOpenAI,      Icon: Layers };
    case "local_fallback":  return { color: SQ_TOKENS.providerLocal,        Icon: HardDrive };
    default:                return { color: SQ_TOKENS.accent,               Icon: Cpu };
  }
}

export const ProviderCostTable: React.FC<ProviderCostTableProps> = ({ insights }) => {
  const columns = [
    {
      header: "Modelo de Borda (LLM)",
      accessor: (row: AiModelInsights) => {
        const { color, Icon } = providerMeta(row.provider);
        return (
          <div className="flex items-center gap-2.5">
            <span
              className="p-1.5 rounded-lg"
              style={{
                backgroundColor: "var(--sq-bg-overlay)",
                border: "1px solid var(--sq-border)",
              }}
            >
              <Icon className="w-4 h-4" style={{ color }} />
            </span>
            <div>
              <span className="font-semibold block text-xs" style={{ color: "var(--sq-text-primary)" }}>
                {row.displayName}
              </span>
              <span className="font-mono text-[9px] uppercase" style={{ color: "var(--sq-text-tertiary)" }}>
                {row.provider}
              </span>
            </div>
          </div>
        );
      },
    },
    {
      header: "Total de Chamadas",
      accessor: (row: AiModelInsights) => (
        <span className="font-mono text-xs" style={{ color: "var(--sq-text-secondary)" }}>
          {row.totalCalls.toLocaleString("pt-BR")}
        </span>
      ),
    },
    {
      header: "Tokens Consumidos",
      accessor: (row: AiModelInsights) => {
        if (row.totalTokens === 0) return <span className="font-mono" style={{ color: "var(--sq-text-tertiary)" }}>-</span>;
        const millionTokens = (row.totalTokens / 1000000).toFixed(1);
        return (
          <span className="font-mono text-xs" style={{ color: "var(--sq-text-primary)" }}>
            {row.totalTokens.toLocaleString("pt-BR")}{" "}
            <span className="text-[10px]" style={{ color: "var(--sq-text-tertiary)" }}>({millionTokens}M)</span>
          </span>
        );
      },
    },
    {
      header: "Custo Estimado (R$)",
      accessor: (row: AiModelInsights) => {
        const costVal = row.estimatedCostUsd;
        if (costVal === 0) {
          return (
            <span
              className="uppercase font-mono text-[10px] px-2 py-0.5 rounded font-bold"
              style={{
                color: "var(--sq-success)",
                backgroundColor: alpha("var(--sq-success)", 10),
                border: `1px solid ${alpha("var(--sq-success)", 15)}`,
              }}
            >
              Grátis
            </span>
          );
        }
        return (
          <span className="font-mono font-bold text-xs" style={{ color: "var(--sq-accent)" }}>
            {formatCurrency(costVal)}
          </span>
        );
      },
    },
    {
      header: "Taxa de Sucesso",
      accessor: (row: AiModelInsights) => {
        if (row.reliabilityPercentage === null) {
          return <FeatureComingSoon feature="Taxa de Sucesso" compact />;
        }
        const val = row.reliabilityPercentage;
        const color = val > 99
          ? SQ_TOKENS.success
          : val > 97
          ? SQ_TOKENS.warning
          : SQ_TOKENS.error;
        return (
          <span className="font-mono font-bold text-xs" style={{ color }}>
            {val.toFixed(2)}%
          </span>
        );
      },
    },
  ];

  return (
    <SectionCard
      title="Custo e métricas por provedor"
      description="Custo, volume e tokens consumidos por provedor de IA no período."
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
