import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { DataTable } from "../../../components/ui/DataTable";
import { TrendingUp, TrendingDown, RefreshCw, Layers } from "lucide-react";
import { diagnosticsService } from "../../../services/diagnosticsService";

export interface AggregateRow {
  networkType: string;
  diagnosticsCount: number;
  avgScore: number;
  avgDownload: string;
  avgUpload: string;
  avgPing: string;
  avgJitter: string;
  avgLoss: string;
  topIssue: string;
  trend: "up" | "down" | "stable";
  trendLabel: string;
}

export const mockAggregateData: AggregateRow[] = [
  {
    networkType: "Wi-Fi (Rede Local)",
    diagnosticsCount: 132840,
    avgScore: 74,
    avgDownload: "142 Mbps",
    avgUpload: "48 Mbps",
    avgPing: "28 ms",
    avgJitter: "9 ms",
    avgLoss: "0.8%",
    topIssue: "Wi-Fi fraco (31%)",
    trend: "up",
    trendLabel: "Aumento de ruído local"
  },
  {
    networkType: "Rede Móvel (Cellular)",
    diagnosticsCount: 38740,
    avgScore: 68,
    avgDownload: "45 Mbps",
    avgUpload: "12 Mbps",
    avgPing: "48 ms",
    avgJitter: "14 ms",
    avgLoss: "1.5%",
    topIssue: "Rede móvel congestionada (11%)",
    trend: "up",
    trendLabel: "Interferência em horários de pico"
  },
  {
    networkType: "Fibra (Banda Larga)",
    diagnosticsCount: 11070,
    avgScore: 92,
    avgDownload: "480 Mbps",
    avgUpload: "245 Mbps",
    avgPing: "8 ms",
    avgJitter: "1.2 ms",
    avgLoss: "0.05%",
    topIssue: "Nenhum detectado (92%)",
    trend: "stable",
    trendLabel: "Estabilidade mecânica impecável"
  },
  {
    networkType: "Ethernet Cabeada",
    diagnosticsCount: 1850,
    avgScore: 96,
    avgDownload: "910 Mbps",
    avgUpload: "880 Mbps",
    avgPing: "3 ms",
    avgJitter: "0.4 ms",
    avgLoss: "0.01%",
    topIssue: "Gateway lento (7%)",
    trend: "down",
    trendLabel: "Melhoria linear"
  }
];

interface DiagnosticsAggregateTableProps {
  environment: "production" | "staging";
}

export const DiagnosticsAggregateTable: React.FC<DiagnosticsAggregateTableProps> = ({ environment }) => {
  const [data, setData] = React.useState<AggregateRow[]>([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    let active = true;
    const fetchAggregate = async () => {
      setLoading(true);
      try {
        const result = await diagnosticsService.getAggregateDiagnostics({ environment });
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
  }, [environment]);

  const columns = [
    {
      header: "Tipo de Rede",
      accessor: (row: AggregateRow) => (
        <span className="font-sans font-semibold text-white uppercase text-xs">
          {row.networkType}
        </span>
      )
    },
    {
      header: "Diagnósticos (Total)",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-zinc-400 font-medium">
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
      header: "Down / Up Médio",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-[#38BDF8] font-semibold text-[11px]">
          {row.avgDownload} <span className="text-zinc-650 font-normal">/</span> {row.avgUpload}
        </span>
      )
    },
    {
      header: "Ping",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-zinc-300">
          {row.avgPing}
        </span>
      )
    },
    {
      header: "Jitter",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-zinc-400">
          {row.avgJitter}
        </span>
      )
    },
    {
      header: "Perda",
      accessor: (row: AggregateRow) => (
        <span className="font-mono text-[#FF4D4F] font-semibold">
          {row.avgLoss}
        </span>
      )
    },
    {
      header: "Principal Problema Mapeado",
      accessor: (row: AggregateRow) => (
        <span className="text-zinc-300 font-sans text-[11px] bg-zinc-900 border border-zinc-800 px-2 py-0.5 rounded">
          {row.topIssue}
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
              <span className="text-zinc-500">─ Estável</span>
            ) : isUp ? (
              <span className="text-[#FF4D4F] flex items-center gap-0.5">
                <TrendingUp className="w-3.5 h-3.5" /> Alerta
              </span>
            ) : (
              <span className="text-[#22C55E] flex items-center gap-0.5">
                <TrendingDown className="w-3.5 h-3.5" /> Tratado
              </span>
            )}
            <span className="text-[9px] text-zinc-550 max-w-[100px] truncate" title={row.trendLabel}>
              ({row.trendLabel})
            </span>
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
        emptyMessage="Nenhuma agregação disponível para estes parâmetros"
        id="aggregate-table"
      />
    </SectionCard>
  );
};
