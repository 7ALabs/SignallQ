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
import { AiModelInsights, AiUsageRecord } from "../../types/ai";
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
  const [timelineData, setTimelineData] = React.useState<any[]>([]);
  const [records, setRecords] = React.useState<AiUsageRecord[]>([]);

  const loadAiStats = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters = { environment, period };
      const [insights, dailyCosts, logs] = await Promise.all([
        aiUsageService.getAiUsageMetrics(filters),
        aiUsageService.getAiDailyCostsTimeSeries(filters),
        aiUsageService.getAiUsageRecords(filters),
      ]);

      setModelInsights(insights);
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
      header: "Pedido ID",
      accessor: (row: AiUsageRecord) => (
        <span className="font-mono text-zinc-400 font-semibold">{row.id.replace("ai_req_", "")}</span>
      ),
    },
    {
      header: "Modelo Selecionado",
      accessor: (row: AiUsageRecord) => {
        let text = "Google Gemini";
        if (row.modelSelected === "cloudflare_qwen") text = "Workers AI Qwen";
        if (row.modelSelected === "openai") text = "OpenAI GPT-4o";
        if (row.modelSelected === "local_fallback") text = "Fallback Off";
        return (
          <span className="font-sans font-medium text-white text-xs">{text}</span>
        );
      },
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
        if (row.costUsd === 0) return <span className="text-emerald-400 text-[10px] font-mono leading-none">FREE</span>;
        return (
          <span className="font-mono text-indigo-400 font-semibold text-xs">
            ${row.costUsd.toFixed(6)}
          </span>
        );
      },
    },
    {
      header: "Latência",
      accessor: (row: AiUsageRecord) => (
        <span className="font-mono text-zinc-305">
          {row.latencySec.toFixed(2)}s
        </span>
      ),
    },
    {
      header: "Status",
      accessor: (row: AiUsageRecord) => {
        const severity = row.status === "success" 
          ? "ok" 
          : row.status === "cached" 
          ? "attention" 
          : "critical";
        return (
          <StatusBadge
            status={severity}
            customLabel={row.status.toUpperCase()}
          />
        );
      },
    },
  ];

  if (loading) {
    return <LoadingState message="Recuperando telemetria de tokens, faturas e laudos..." />;
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-red-500/20 bg-[#FF4D4F]/5 rounded-2xl">
        <h4 className="text-sm font-semibold text-[#FF4D4F] uppercase tracking-wider font-mono">Erro de Telemetria</h4>
        <p className="text-xs text-neutral-400 mt-2 font-sans">{error}</p>
        <button
          onClick={() => { setError(null); loadAiStats(); }}
          className="mt-4 px-4 py-2 text-xs bg-[#FF4D4F]/10 border border-[#FF4D4F]/20 text-[#FF4D4F] hover:bg-[#FF4D4F]/20 transition-all rounded-xl font-mono"
        >
          TENTAR NOVAMENTE
        </button>
      </div>
    );
  }

  if (modelInsights.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[#262626] bg-[#111111] rounded-2xl">
        <h4 className="text-xs font-semibold text-[#9CA3AF] uppercase tracking-widest font-mono">Sem dados</h4>
        <p className="text-xs text-[#9CA3AF] mt-2 font-sans">Nenhuma inferência de IA registrada neste período.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 1. Metric card triggers row */}
      <div className="flex justify-between items-center bg-[#111111] border border-[#262626] rounded-2xl p-4 select-none">
        <div className="flex items-center gap-3">
          <Bot className="w-5 h-5 text-purple-400" />
          <div>
            <h4 className="text-xs font-semibold font-mono text-zinc-300 uppercase">Monitoramento síncrono de IA</h4>
            <p className="text-[10px] text-zinc-500 font-sans mt-0.5">Visibilidade completa sobre o processamento e faturamento de pareceres técnicos.</p>
          </div>
        </div>
        <button
          onClick={handleRefresh}
          disabled={isRefreshing}
          className="flex items-center gap-1.5 px-3.5 py-2 bg-[#18181B] border border-[#262626] hover:border-zinc-700 font-mono text-xs text-white rounded-xl active:bg-zinc-900 transition-all cursor-pointer select-none"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin text-purple-400" : ""}`} />
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
