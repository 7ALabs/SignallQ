import React from "react";
import { aiUsageService } from "../../services/aiUsageService";
import { alpha } from "../../utils/color";
import { AiCostMetricGrid } from "./components/AiCostMetricGrid";
import { ProviderCostTable } from "./components/ProviderCostTable";
import { AiCostTimeline } from "./components/AiCostTimeline";
import { AiAlertsPanel } from "./components/AiAlertsPanel";
import { DataTable } from "../../components/ui/DataTable";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
import { GlobalFilters } from "../../components/ui/GlobalFilters";
import { AppEnvironment } from "../../types/admin";
import { AiModelInsights, AiUsageRecord, AiDailyUsage } from "../../types/ai";
import { Bot, RefreshCw } from "lucide-react";

interface AiCostPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
  onNavigate?: (path: string) => void;
}

export const AiCostPage: React.FC<AiCostPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
  onNavigate,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = React.useState(false);
  const [modelInsights, setModelInsights] = React.useState<AiModelInsights[]>([]);
  const [timelineData, setTimelineData] = React.useState<AiDailyUsage[]>([]);
  const [records, setRecords] = React.useState<AiUsageRecord[]>([]);
  const [costSummary, setCostSummary] = React.useState<{ totalCostUsd: string; reliabilityPercentage: number | null } | null>(null);
  const [providerFilter, setProviderFilter] = React.useState("all");

  const loadAiStats = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters = { environment, period };
      const [insights, dailyCosts, logs, summary] = await Promise.all([
        aiUsageService.getAiUsageMetrics(filters),
        aiUsageService.getAiUsageTimeSeries(filters),
        aiUsageService.getAiUsageRecords(filters),
        aiUsageService.getAiCostSummary(filters),
      ]);

      setModelInsights(insights ?? []);
      setTimelineData(dailyCosts);
      setRecords(logs);
      setCostSummary(summary);
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

  const providerOptions = React.useMemo(() => {
    const names = Array.from(new Set(modelInsights.map((m) => m.displayName || m.provider)));
    return [{ label: "Todos", value: "all" }, ...names.map((n) => ({ label: n, value: n }))];
  }, [modelInsights]);

  const filteredRecords = React.useMemo(() => {
    if (providerFilter === "all") return records;
    return records.filter((r) => r.provider === providerFilter);
  }, [records, providerFilter]);

  // GH#552 (Fase 3) — bloco de explicação: variação de fallback entre os dois
  // provedores presentes na timeline (últimos dois dias com dado), sem inventar
  // tendência além do que a série já carregada mostra.
  const insightText = React.useMemo(() => {
    if (timelineData.length < 2) return null;
    const last = timelineData[timelineData.length - 1];
    const prev = timelineData[timelineData.length - 2];
    const fallbackKey = Object.keys(last.byProvider).find((k) => !k.toLowerCase().includes("gemini"));
    if (!fallbackKey) return null;
    const lastVal = last.byProvider[fallbackKey] ?? 0;
    const prevVal = prev.byProvider[fallbackKey] ?? 0;
    const lastTotal = Object.values(last.byProvider).reduce((s, v) => s + v, 0);
    const prevTotal = Object.values(prev.byProvider).reduce((s, v) => s + v, 0);
    const lastShare = lastTotal > 0 ? (lastVal / lastTotal) * 100 : 0;
    const prevShare = prevTotal > 0 ? (prevVal / prevTotal) * 100 : 0;
    if (Math.abs(lastShare - prevShare) < 1) return null;
    const direction = lastShare > prevShare ? "subiu" : "caiu";
    return `Uso de "${fallbackKey}" ${direction} de ${prevShare.toFixed(0)}% para ${lastShare.toFixed(0)}% do volume diário — ${
      lastShare > prevShare
        ? "vale monitorar se é instabilidade momentânea do provider primário."
        : "provider primário voltou a concentrar a maior parte das chamadas."
    }`;
  }, [timelineData]);

  const handleExportCostReport = () => {
    const header = "Provider,Modelo,Chamadas,Tokens,Custo (USD),Confiabilidade\r\n";
    const rows = modelInsights
      .map((m) => [m.provider, m.displayName, m.totalCalls, m.totalTokens, m.estimatedCostUsd.toFixed(4), m.reliabilityPercentage ?? ""].join(","))
      .join("\r\n");
    const csvContent = `data:text/csv;charset=utf-8,${header}${rows}`;
    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", `signallq_custo_ia_${environment}_${period}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
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
        className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 rounded-[var(--radius-card)]"
        style={{
          border: `1px solid ${alpha("var(--sq-error)", 20)}`,
          backgroundColor: alpha("var(--sq-error)", 5),
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
            backgroundColor: alpha("var(--sq-error)", 10),
            border: `1px solid ${alpha("var(--sq-error)", 20)}`,
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
        className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 rounded-[var(--radius-card)]"
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
      {/* 1. Toolbar de sincronismo + filtro por provider */}
      <div
        className="flex flex-wrap justify-between items-center gap-3 rounded-[var(--radius-card)] p-4 select-none"
        style={{
          backgroundColor: "var(--sq-bg-card)",
          border: "1px solid var(--sq-border)",
        }}
      >
        <div className="flex items-center gap-3">
          <Bot className="w-5 h-5" style={{ color: "var(--sq-accent)" }} />
          <div>
            <h4 className="text-xs font-semibold font-mono uppercase" style={{ color: "var(--sq-text-primary)" }}>
              Uso e custo de IA
            </h4>
            <p className="text-[10px] mt-0.5" style={{ color: "var(--sq-text-tertiary)" }}>
              Custo e volume de diagnósticos gerados por IA no período.
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

      <GlobalFilters
        id="ai-cost-global-filters"
        filters={[
          { key: "provider", label: "Provider", value: providerFilter, onChange: setProviderFilter, options: providerOptions },
        ]}
      />

      {/* 2. KPIs — 4 cards, com veredito humano (GH#552 Fase 3, substitui grid de 7) */}
      <AiCostMetricGrid costSummary={costSummary} modelInsights={modelInsights} />

      {/* 3. Gráfico principal — custo/volume diário por provider */}
      <AiCostTimeline timelineData={timelineData} />

      {/* 4. Bloco de explicação */}
      {insightText && <InsightBlock id="ai-cost-insight-block">{insightText}</InsightBlock>}

      {/* 5. Tabela de investigação — custo por provider + alertas de orçamento */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8">
          <ProviderCostTable insights={modelInsights} />
        </div>
        <div className="lg:col-span-4">
          <AiAlertsPanel />
        </div>
      </div>

      {/* Faturamento por função pedido pelo wireframe não tem rota de breakdown
          por feature no worker (session_id de ai_usage e de analytics_events
          não são a mesma entidade hoje — gap documentado em data-architecture.md) */}
      <SectionCard
        title="Faturamento e Consumo IA por Função"
        description="Mapeamento econômico de processamento de tokens por finalidade de IA e as devidas contingências."
      >
        <FeatureComingSoon
          feature="Faturamento e Consumo IA por Função"
          reason="Requer rota de breakdown por feature no worker"
        />
      </SectionCard>

      {/* Auditoria de sessão — outlier de tokens/custo, filtrável por provider */}
      <SectionCard
        title="Histórico de Varreduras e Execuções Síncronas"
        description="Faturamento granular apurado a cada laudo gerado nas interfaces móveis. Latência por execução não disponível (schema ai_usage não registra tempo de resposta)."
      >
        <DataTable
          data={filteredRecords}
          columns={tableColumns}
          keyExtractor={(row) => row.id}
          emptyMessage="Nenhuma transação efetuada neste período."
          id="ai-fine-grained-table"
        />
      </SectionCard>

      {/* 6. Ações */}
      <ActionsRow
        id="ai-cost-actions-row"
        actions={[
          { label: "Exportar relatório de custo", onClick: handleExportCostReport, variant: "secondary" },
          ...(onNavigate ? [{ label: "Ver Diagnósticos", onClick: () => onNavigate("/diagnostics") }] : []),
        ]}
      />
    </div>
  );
};
