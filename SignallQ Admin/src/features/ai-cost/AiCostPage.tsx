import React from "react";
import { aiUsageService } from "../../services/aiUsageService";
import { AiCostMetricGrid } from "./components/AiCostMetricGrid";
import { ProviderCostTable } from "./components/ProviderCostTable";
import { AiCostTimeline } from "./components/AiCostTimeline";
import { ProviderUsageChart } from "./components/ProviderUsageChart";
import { AiAlertsPanel } from "./components/AiAlertsPanel";
import { DataTable } from "../../components/ui/DataTable";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { AppEnvironment } from "../../types/admin";
import { AiModelInsights, AiUsageRecord, AiDailyUsage } from "../../types/ai";
import { Bot, RefreshCw } from "lucide-react";

interface AiCostPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const AiCostPage: React.FC<AiCostPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = React.useState(false);
  const [modelInsights, setModelInsights] = React.useState<AiModelInsights[]>([]);
  const [timelineData, setTimelineData] = React.useState<AiDailyUsage[]>([]);
  const [records, setRecords] = React.useState<AiUsageRecord[]>([]);

  const loadAiStats = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters = { environment, period };
      const [insights, dailyCosts, logs] = await Promise.all([
        aiUsageService.getAiUsageMetrics(filters),
        aiUsageService.getAiUsageTimeSeries(filters),
        aiUsageService.getAiUsageRecords(filters),
      ]);

      setModelInsights(insights ?? []);
      setTimelineData(dailyCosts);
      setRecords(logs);
    } catch (e: any) {
      console.error("Failed to sync AI usage insights", e);
      const code = e?.code;
      setError(code > 0 ? `Erro: ${code}` : "Sem conexão com o servidor");
    } finally {
      setLoading(false);
    }
  }, [environment, period, triggerRefreshCounter]);

  React.useEffect(() => {
    loadAiStats();
  }, [loadAiStats]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await new Promise((resolve) => setTimeout(resolve, 300));
    await loadAiStats();
    setIsRefreshing(false);
  };

  const tableColumns = [
    {
      header: "ID da execução",
      accessor: (row: AiUsageRecord) => (
        <span className="font-mono text-zinc-400 font-semibold">{row.id.slice(0, 8)}</span>
      ),
    },
    {
      header: "Modelo",
      accessor: (row: AiUsageRecord) => (
        <span className="font-sans font-medium text-white text-xs" title={row.model}>{row.provider}</span>
      ),
    },
    {
      header: "Diagnóstico associado",
      accessor: (row: AiUsageRecord) => row.diagnosisId ? (
        <span className="font-mono text-zinc-400">{row.diagnosisId.slice(0, 8)}</span>
      ) : (
        <span className="font-mono text-zinc-550">—</span>
      ),
    },
    {
      header: "Tokens (Prompt / Comp.)",
      accessor: (row: AiUsageRecord) => {
        if (row.promptTokens === 0 && row.completionTokens === 0) {
          return <span className="font-mono text-zinc-550">-</span>;
        }
        return (
          <span className="font-mono text-zinc-400">
            {row.promptTokens} <span className="text-zinc-650 font-normal">/</span> {row.completionTokens}
          </span>
        );
      },
    },
    {
      header: "Custo (USD)",
      accessor: (row: AiUsageRecord) => {
        if (row.costUsd === 0) return <span className="text-[10px] font-mono leading-none" style={{ color: "var(--sq-success)" }}>FREE</span>;
        return (
          <span className="font-mono font-semibold text-xs" style={{ color: "var(--sq-accent)" }}>
            ${row.costUsd.toFixed(2)}
          </span>
        );
      },
    },
    {
      header: "Status",
      accessor: (row: AiUsageRecord) => (
        <StatusBadge
          status={row.status === "success" ? "ok" : "critical"}
          customLabel={row.status === "success" ? "SUCESSO" : "ERRO"}
        />
      ),
    },
    {
      header: "Erro",
      accessor: (row: AiUsageRecord) => row.errorMessage ? (
        <span className="text-xs" style={{ color: "var(--sq-error)" }} title={row.errorMessage}>
          {row.errorMessage.length > 40 ? `${row.errorMessage.slice(0, 40)}…` : row.errorMessage}
        </span>
      ) : (
        <span className="font-mono text-zinc-550">—</span>
      ),
    },
  ];

  if (loading) {
    return <LoadingState message="Recuperando telemetria de tokens, faturas e laudos..." />;
  }

  if (error) {
    return (
      <div
        className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 rounded-[8px]"
        style={{
          border: "1px solid color-mix(in srgb, var(--sq-error) 20%, transparent)",
          backgroundColor: "color-mix(in srgb, var(--sq-error) 5%, transparent)",
        }}
      >
        <h4 className="text-sm font-semibold uppercase tracking-wider font-mono" style={{ color: "var(--sq-error)" }}>
          Erro de Telemetria
        </h4>
        <p className="text-xs mt-2" style={{ color: "var(--sq-text-secondary)" }}>{error}</p>
        <button
          onClick={() => { setError(null); loadAiStats(); }}
          className="mt-4 px-4 py-2 text-xs transition-all rounded-xl font-mono"
          style={{
            backgroundColor: "color-mix(in srgb, var(--sq-error) 10%, transparent)",
            border: "1px solid color-mix(in srgb, var(--sq-error) 20%, transparent)",
            color: "var(--sq-error)",
          }}
        >
          TENTAR NOVAMENTE
        </button>
      </div>
    );
  }

  if (modelInsights.length === 0) {
    return (
      <div
        className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 rounded-[8px]"
        style={{
          border: "1px solid var(--sq-border)",
          backgroundColor: "var(--sq-bg-card)",
        }}
      >
        <h4 className="text-xs font-semibold uppercase tracking-widest font-mono" style={{ color: "var(--sq-text-secondary)" }}>
          Sem dados
        </h4>
        <p className="text-xs mt-2" style={{ color: "var(--sq-text-secondary)" }}>
          Nenhuma inferência de IA registrada neste período.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 1. Metric card triggers row */}
      <div
        className="flex justify-between items-center rounded-[8px] p-4 select-none"
        style={{
          backgroundColor: "var(--sq-bg-card)",
          border: "1px solid var(--sq-border)",
        }}
      >
        <div className="flex items-center gap-3">
          <Bot className="w-5 h-5" style={{ color: "var(--sq-accent)" }} />
          <div>
            <h4 className="text-xs font-semibold font-mono uppercase" style={{ color: "var(--sq-text-primary)" }}>
              Monitoramento síncrono de IA
            </h4>
            <p className="text-[10px] mt-0.5" style={{ color: "var(--sq-text-tertiary)" }}>
              Visibilidade completa sobre o processamento e faturamento de pareceres técnicos.
            </p>
          </div>
        </div>
        <button
          onClick={handleRefresh}
          disabled={isRefreshing}
          className="flex items-center gap-1.5 px-3.5 py-2 font-mono text-xs rounded-xl transition-all cursor-pointer select-none"
          style={{
            backgroundColor: "var(--sq-bg-overlay)",
            border: "1px solid var(--sq-border)",
            color: "var(--sq-text-primary)",
          }}
        >
          <RefreshCw
            className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin" : ""}`}
            style={isRefreshing ? { color: "var(--sq-accent)" } : undefined}
          />
          <span>Sincronizar</span>
        </button>
      </div>

      <AiCostMetricGrid environment={environment} period={period} />

      {/* 2. Visual graphs section area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <AiCostTimeline timelineData={timelineData} />
        </div>
        <div>
          <ProviderUsageChart insights={modelInsights} />
        </div>
      </div>

      {/* 3. Splitted operational details - Costs and alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8">
          <ProviderCostTable insights={modelInsights} />
        </div>
        <div className="lg:col-span-4">
          <AiAlertsPanel />
        </div>
      </div>

      {/* 4. Demonstrativo por Função */}
      <SectionCard
        title="Faturamento e Consumo IA por Função"
        description="Mapeamento econômico de processamento de tokens por finalidade de IA e as devidas contingências."
      >
        <FeatureComingSoon
          feature="Faturamento e Consumo IA por Função"
          reason="Requer rota de breakdown por feature no worker"
        />
      </SectionCard>

      {/* 5. Fine-grained execution logging table */}
      <SectionCard
        title="Histórico de Varreduras e Execuções Síncronas"
        description="Faturamento granular e latências apuradas a cada laudo gerado nas interfaces móveis."
      >
        <DataTable
          data={records}
          columns={tableColumns}
          keyExtractor={(row) => row.id}
          emptyMessage="Nenhuma transação efetuada neste período."
          id="ai-fine-grained-table"
        />
      </SectionCard>
    </div>
  );
};
