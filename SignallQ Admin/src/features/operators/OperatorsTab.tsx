import React from "react";
import { adminMetricsService } from "../../services/adminMetricsService";
import { SectionCard } from "../../components/ui/SectionCard";
import { ChartCard } from "../../components/ui/ChartCard";
import { DataTable } from "../../components/ui/DataTable";
import { BarChart } from "../../components/charts/BarChart";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { OperatorRecord } from "../../types/admin";
import { AppEnvironment } from "../../types/admin";
import { Award, Globe } from "lucide-react";

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

  if (operators.length === 0) {
    return (
      <FeatureComingSoon
        feature="Benchmarks de Operadoras"
        reason="Requer rota de métricas de operadoras no worker"
      />
    );
  }

  const tableColumns = [
    {
      header: "Operadora CO",
      accessor: (row: OperatorRecord) => (
        <div className="flex items-center gap-2">
          <Globe className="w-3.5 h-3.5 text-[var(--text-tertiary)]" />
          <span className="font-semibold text-[var(--text-primary)]">{row.name}</span>
        </div>
      ),
    },
    {
      header: "Tipo Físico",
      accessor: (row: OperatorRecord) => (
        <span className="uppercase text-[10px] font-sans bg-zinc-900 border border-zinc-800 text-[var(--text-secondary)] px-2 py-0.5 rounded">
          {row.type}
        </span>
      ),
    },
    {
      header: "Testes Consolidados",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-[var(--text-secondary)]">{row.testCount.toLocaleString("pt-BR")}</span>
      ),
    },
    {
      header: "Download Médio",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-[var(--info)] font-bold">
          {row.averageDownloadMbps} <span className="text-[10px] text-[var(--text-tertiary)] font-normal">Mbps</span>
        </span>
      ),
    },
    {
      header: "Upload Médio",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-indigo-400">
          {row.averageUploadMbps} <span className="text-[10px] text-[var(--text-tertiary)] font-normal">Mbps</span>
        </span>
      ),
    },
    {
      header: "Latência Média",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-[var(--text-primary)]">{row.averageLatencyMs} ms</span>
      ),
    },
    {
      header: "Perda de Pacote",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-[var(--text-secondary)]">{row.packetLossAverage}%</span>
      ),
    },
    {
      header: "Aprovamento",
      accessor: (row: OperatorRecord) => (
        <span className="font-mono text-[var(--success)] font-semibold">{row.customerSatisfactionPercentage}%</span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Top benchmark cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <ChartCard
          title="Velocidade Média de Download por Operadora"
          description="Amostragem em megabits por segundo monitorados via speedtest local integrado no SDK."
        >
          <BarChart
            data={operators}
            xAxisKey="name"
            series={[{ key: "averageDownloadMbps", name: "Velocidade Down (Mbps)", color: "var(--info)" }]}
          />
        </ChartCard>

        <ChartCard
          title="Índice de Qualidade Percebido por Telecom"
          description="Estatística baseada no feedback estruturado do app Android e laudo de IA final (0 a 100%)."
        >
          <BarChart
            data={operators}
            xAxisKey="name"
            series={[{ key: "customerSatisfactionPercentage", name: "Satisfação (%)", color: "var(--success)" }]}
          />
        </ChartCard>
      </div>

      {/* Structured data table */}
      <SectionCard
        title="Estudo Técnico Comparativo de Conectividade"
        description="Agregado de latências de rádio e perdas de pacotes das operadoras atuantes no território brasileiro."
        id="operators-main-card"
        actions={
          <div className="flex items-center gap-1.5 text-xs text-[var(--success)] bg-emerald-950/20 border border-emerald-500/25 px-3 py-1 rounded-xl">
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
