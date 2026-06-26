import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { DataTable } from "../../../components/ui/DataTable";
import { TrendingUp, TrendingDown } from "lucide-react";
import { diagnosticsService } from "../../../services/diagnosticsService";
import { AggregateRow } from "../../../types/diagnostics";
import { AppEnvironment } from "../../../types/admin";

interface DiagnosticsAggregateTableProps {
  environment: AppEnvironment;
  period?: string;
}

export const DiagnosticsAggregateTable: React.FC<DiagnosticsAggregateTableProps> = ({ environment, period }) => {
  const [data, setData] = React.useState<AggregateRow[]>([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    let active = true;
    const fetchAggregate = async () => {
      setLoading(true);
      try {
        const result = await diagnosticsService.getAggregateDiagnostics({ environment, period: period as any });
        if (active) {
          setData(result);
        }
      } catch (err) {
        console.error("Failed to load aggregate table data:", err);
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    fetchAggregate();
    return () => {
      active = false;
    };
  }, [environment, period]);

  const columns = [
    {
      header: "Tipo de Rede",
      accessor: (row: AggregateRow) => (
        <span className="font-sans font-semibold text-[var(--text-primary)] uppercase text-xs">
          {row.networkType}
        </span>
      )
    },
    {
      header: "Sessões",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-[var(--text-secondary)] font-medium">
          {row.diagnosticsCount.toLocaleString("pt-BR")}
        </span>
      )
    },
    {
      header: "Score Médio",
      accessor: (row: AggregateRow) => {
        const value = row.avgScore;
        const color = value >= 85 ? "text-emerald-400" : value >= 70 ? "text-amber-500" : "text-red-400";
        return (
          <span className={`font-mono font-bold ${color}`}>
            {value}/100
          </span>
        );
      }
    },
    {
      header: "Download Médio (Mbps)",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-[var(--info)] font-semibold text-[11px]">
          {row.avgDownload}
        </span>
      )
    },
    {
      header: "Latência Média (ms)",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-[var(--text-secondary)]">
          {row.avgPing}
        </span>
      )
    },
    {
      header: "% do Total",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-[var(--text-secondary)] text-[11px]">
          {row.trendLabel}
        </span>
      )
    },
    {
      header: "Tendência",
      accessor: (row: AggregateRow) => {
        const isUp = row.trend === "up";
        const isStable = row.trend === "stable";
        return (
          <div className="flex items-center gap-1.5 font-mono text-[10px]">
            {isStable ? (
              <span className="text-[var(--text-tertiary)]">─ Estável</span>
            ) : isUp ? (
              <span className="text-[var(--error)] flex items-center gap-0.5">
                <TrendingUp className="w-3.5 h-3.5" /> Alerta
              </span>
            ) : (
              <span className="text-[var(--success)] flex items-center gap-0.5">
                <TrendingDown className="w-3.5 h-3.5" /> Tratado
              </span>
            )}
          </div>
        );
      }
    }
  ];

  return (
    <SectionCard
      title="Análise Agregada de Diagnósticos"
      description="Consolidado multivariado de telemetria segmentado pelas interfaces ativas."
      id="diagnostics-aggregate-card"
    >
      <DataTable
        data={data}
        columns={columns}
        keyExtractor={(row) => row.networkType}
        emptyMessage="Sem dados de rede para este período"
        id="aggregate-table"
      />
    </SectionCard>
  );
};
