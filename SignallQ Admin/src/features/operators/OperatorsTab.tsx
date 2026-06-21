import React from "react";
import { adminMetricsService } from "../../services/adminMetricsService";
import { SectionCard } from "../../components/ui/SectionCard";
import { ChartCard } from "../../components/ui/ChartCard";
import { DataTable } from "../../components/ui/DataTable";
import { BarChart } from "../../components/charts/BarChart";
import { LoadingState } from "../../components/ui/LoadingState";
import { OperatorRecord } from "../../types/admin";
import { AppEnvironment } from "../../types/admin";
import { Award, Globe, HelpCircle } from "lucide-react";

interface OperatorsTabProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const OperatorsTab: React.FC<OperatorsTabProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [operators, setOperators] = React.useState<OperatorRecord[]>([]);

  React.useEffect(() => {
    let active = true;
    async function loadData() {
      setLoading(true);
      try {
        const data = await adminMetricsService.getOperatorMetrics({ environment, period });
        if (active) {
          setOperators(data);
        }
      } catch (err) {
        console.error(err);
      } finally {
        if (active) setLoading(false);
      }
    }
    loadData();
    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  if (loading) {
    return <LoadingState message="Recuperando benchmarks de operadoras no repositório de analytics..." />;
  }

  // Format columns for carriers comparison
  const tableColumns = [
    {
      header: "Operadora CO",
      accessor: (row: OperatorRecord) => (
        <div className="flex items-center gap-2">
          <Globe className="w-3.5 h-3.5 text-zinc-500" />
          <span className="font-semibold text-white">{row.name}</span>
        </div>
      ),
    },
    {
      header: "Tipo Físico",
      accessor: (row: OperatorRecord) => (
        <span className="uppercase text-[10px] font-mono bg-zinc-900 border border-zinc-800 text-zinc-300 px-2 py-0.5 rounded">
          {row.type}
        </span>
      ),
    },
    {
      header: "Testes Consolidados",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-zinc-400">{row.testCount.toLocaleString("pt-BR")}</span>
      ),
    },
    {
      header: "Download Médio",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-[#38BDF8] font-bold">
          {row.averageDownloadMbps} <span className="text-[10px] text-zinc-650 font-normal">Mbps</span>
        </span>
      ),
    },
    {
      header: "Upload Médio",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-indigo-400">
          {row.averageUploadMbps} <span className="text-[10px] text-zinc-650 font-normal">Mbps</span>
        </span>
      ),
    },
    {
      header: "Latência Média",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-white">{row.averageLatencyMs} ms</span>
      ),
    },
    {
      header: "Perda de Pacote",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-neutral-400">{row.packetLossAverage}%</span>
      ),
    },
    {
      header: "Aprovamento",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-[#22C55E] font-semibold">{row.customerSatisfactionPercentage}%</span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Top benchmark cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Barchart comparison speed */}
        <ChartCard
          title="Velocidade Média de Download por Operadora"
          description="Amostragem em megabits por segundo monitorados via speedtest local integrado no SDK."
        >
          <BarChart
            data={operators}
            xAxisKey="name"
            series={[{ key: "averageDownloadMbps", name: "Velocidade Down (Mbps)", color: "#38BDF8" }]}
          />
        </ChartCard>

        {/* Barchart satisfaction */}
        <ChartCard
          title="Índice de Qualidade Percebido por Telecom"
          description="Estatística baseada no feedback estruturado do app Android e laudo de IA final (0 a 100%)."
        >
          <BarChart
            data={operators}
            xAxisKey="name"
            series={[{ key: "customerSatisfactionPercentage", name: "Satisfação (%)", color: "#22C55E" }]}
          />
        </ChartCard>
      </div>

      {/* Structured data table */}
      <SectionCard
        title="Estudo Técnico Comparativo de Conectividade"
        description="Agregado de latências de rádio e perdas de pacotes das operadoras atuantes no território brasileiro."
        id="operators-main-card"
        actions={
          <div className="flex items-center gap-1.5 text-xs text-[#22C55E] bg-emerald-950/20 border border-emerald-500/25 px-3 py-1 rounded-xl">
            <Award className="w-3.5 h-3.5" />
            <span className="font-semibold">Vivo Fibra Líder em Latência (8ms)</span>
          </div>
        }
      >
        <DataTable
          data={operators}
          columns={tableColumns}
          keyExtractor={(row) => row.id}
          emptyMessage="Nenhuma medição consolidade de operadora encontrada."
        />
      </SectionCard>
    </div>
  );
};
