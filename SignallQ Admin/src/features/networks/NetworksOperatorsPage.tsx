import React from "react";
import { adminMetricsService, NetworkTypeStat } from "../../services/adminMetricsService";
import { SectionCard } from "../../components/ui/SectionCard";
import { ChartCard } from "../../components/ui/ChartCard";
import { DataTable } from "../../components/ui/DataTable";
import { BarChart } from "../../components/charts/BarChart";
import { LoadingState } from "../../components/ui/LoadingState";
import { MetricCard } from "../../components/ui/MetricCard";
import { GlobalFilters } from "../../components/ui/GlobalFilters";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { OperatorSessionsList } from "./components/OperatorSessionsList";
import { OperatorRecord, AppEnvironment } from "../../types/admin";
import { MetricVerdict } from "../../types/metrics";
import { Award, Globe, ChevronDown } from "lucide-react";

// GH#746 — mesma escala usada em DiagnosticsMetricGrid (score calculado pelo
// engine local do app, 0-100). Repetida aqui em vez de importada porque o
// helper original não é exportado do módulo de diagnostics.
function scoreVerdict(score: number): MetricVerdict {
  if (score >= 85) return "excelente";
  if (score >= 70) return "bom";
  if (score >= 55) return "regular";
  return "fraco";
}

// Latência de rede — banda de referência de mercado para conexão residencial/móvel
// saudável no Brasil (<30ms excelente, <60ms bom, <100ms ainda aceitável).
function latencyVerdict(ms: number): MetricVerdict {
  if (ms < 30) return "excelente";
  if (ms < 60) return "bom";
  if (ms < 100) return "regular";
  return "fraco";
}

interface NetworksOperatorsPageProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

// GH#552 (Fase 2) — fusão de `networks/` + `operators/` (wireframe "Redes & Provedores").
// Continua sem coluna de região/UF: diagnostic_sessions não coleta esse campo hoje —
// não inventar dado que o Android não envia (ver comentário original em NetworksTab).
export const NetworksOperatorsPage: React.FC<NetworksOperatorsPageProps> = ({
  environment,
  period,
  onNavigate,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [networkStats, setNetworkStats] = React.useState<NetworkTypeStat[]>([]);
  const [operators, setOperators] = React.useState<OperatorRecord[]>([]);
  const [operatorFilter, setOperatorFilter] = React.useState<string>("all");
  // GH#746 — detalhamento secundário (download por operadora, latência/perda de
  // pacote por tipo de rede) fica colapsado por padrão. O gráfico principal
  // (score por operadora) já responde à pergunta-guia sozinho.
  const [showDetails, setShowDetails] = React.useState(false);

  const loadData = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [netStats, ops] = await Promise.all([
        adminMetricsService.getNetworkTypeStats({ environment, period }),
        adminMetricsService.getOperatorMetrics({ environment, period }),
      ]);
      setNetworkStats(netStats);
      setOperators(ops);
    } catch (e) {
      console.error("Failed to load network/operator metrics", e);
      setError(e instanceof Error ? e.message : "Não foi possível carregar as métricas de rede.");
    } finally {
      setLoading(false);
    }
  }, [environment, period]);

  React.useEffect(() => {
    loadData();
  }, [loadData, triggerRefreshCounter]);

  if (loading) {
    return <LoadingState message="Agregando qualidade de rede por tipo e operadora..." />;
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[var(--error)]/20 bg-[var(--error)]/5 rounded-[var(--radius-card)]">
        <h4 className="text-sm font-semibold text-[var(--error)] uppercase tracking-wider font-sans">Erro ao carregar rede</h4>
        <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">{error}</p>
        <button
          onClick={loadData}
          className="mt-4 px-4 py-2 text-xs bg-[var(--error)]/10 border border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 transition-all rounded-xl font-sans"
        >
          Tentar novamente
        </button>
      </div>
    );
  }

  const wifiStat = networkStats.find((s) => s.name.toLowerCase().includes("wi"));
  const mobileStat = networkStats.find((s) => {
    const n = s.name.toLowerCase();
    return n.includes("móvel") || n.includes("movel") || n.includes("mobile") || n.includes("4g") || n.includes("5g") || n.includes("cellular");
  });
  const fmt = (v: number | null, suffix = "") => (v == null ? "sem dados" : `${v.toLocaleString("pt-BR")}${suffix}`);

  const filteredOperators = operatorFilter === "all"
    ? operators
    : operators.filter((op) => op.id === operatorFilter);

  // KPIs — média entre operadoras com dado, operadora com pior score, tipo de rede mais testado.
  const withDownload = operators.filter((op) => op.averageDownloadMbps != null);
  const avgDownload = withDownload.length
    ? Math.round((withDownload.reduce((sum, op) => sum + (op.averageDownloadMbps as number), 0) / withDownload.length) * 10) / 10
    : null;

  const withLatency = operators.filter((op) => op.averageLatencyMs != null);
  const avgLatency = withLatency.length
    ? Math.round(withLatency.reduce((sum, op) => sum + (op.averageLatencyMs as number), 0) / withLatency.length)
    : null;

  const worstScoreOperator = operators
    .filter((op) => op.averageScorePercentage != null)
    .sort((a, b) => (a.averageScorePercentage as number) - (b.averageScorePercentage as number))[0];

  const mostTestedType = [...networkStats].sort((a, b) => b.count - a.count)[0];

  const latencyLeader = operators
    .filter((op) => op.averageLatencyMs != null)
    .sort((a, b) => (a.averageLatencyMs as number) - (b.averageLatencyMs as number))[0];

  const worstPacketLossOperator = operators
    .filter((op) => op.packetLossAverage != null)
    .sort((a, b) => (b.packetLossAverage as number) - (a.packetLossAverage as number))[0];

  const insightText = latencyLeader && worstPacketLossOperator
    ? `${latencyLeader.name} lidera em latência (${latencyLeader.averageLatencyMs}ms). ${worstPacketLossOperator.name} tem a maior perda de pacote média (${worstPacketLossOperator.packetLossAverage}%) — vale investigar se é padrão consistente ou ruído do período.`
    : null;

  const noData = (
    <span
      className="font-mono text-[var(--text-tertiary)]"
      title="Sem medição suficiente no período"
      aria-label="Sem medição suficiente no período"
    >
      —
    </span>
  );

  const tableColumns = [
    {
      header: "Operadora",
      accessor: (row: OperatorRecord) => (
        <div className="flex items-center gap-2">
          <Globe className="w-3.5 h-3.5 text-[var(--text-tertiary)]" />
          <span className="font-semibold text-[var(--text-primary)]">{row.name}</span>
        </div>
      ),
    },
    {
      header: "Tipo de Rede Dominante",
      accessor: (row: OperatorRecord) =>
        row.type ? (
          <span className="uppercase text-[10px] font-sans bg-zinc-900 border border-zinc-800 text-[var(--text-secondary)] px-2 py-0.5 rounded">
            {row.type}
          </span>
        ) : (
          <span className="text-[10px] font-sans text-[var(--text-tertiary)] italic">Sem dado</span>
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
      accessor: (row: OperatorRecord) =>
        row.averageDownloadMbps != null ? (
          <span className="font-mono text-[var(--info)] font-bold">
            {row.averageDownloadMbps} <span className="text-[10px] text-[var(--text-tertiary)] font-normal">Mbps</span>
          </span>
        ) : noData,
    },
    {
      header: "Upload Médio",
      accessor: (row: OperatorRecord) =>
        row.averageUploadMbps != null ? (
          <span className="font-mono text-indigo-400">
            {row.averageUploadMbps} <span className="text-[10px] text-[var(--text-tertiary)] font-normal">Mbps</span>
          </span>
        ) : noData,
    },
    {
      header: "Latência Média",
      accessor: (row: OperatorRecord) =>
        row.averageLatencyMs != null ? (
          <span className="font-mono text-[var(--text-primary)]">{row.averageLatencyMs} ms</span>
        ) : noData,
    },
    {
      header: "Perda de Pacote",
      accessor: (row: OperatorRecord) =>
        row.packetLossAverage != null ? (
          <span className="font-mono text-[var(--text-secondary)]">{row.packetLossAverage}%</span>
        ) : noData,
    },
    {
      header: "Score Médio de Diagnóstico",
      accessor: (row: OperatorRecord) =>
        row.averageScorePercentage != null ? (
          <span className="font-mono text-[var(--success)] font-semibold">{row.averageScorePercentage}%</span>
        ) : noData,
    },
  ];

  const handleExportOperators = () => {
    const header = "Operadora,Tipo,Testes,DownloadMbps,UploadMbps,LatenciaMs,PerdaPacote,ScoreMedio\r\n";
    const rows = filteredOperators
      .map((op) => [
        `"${op.name.replace(/"/g, '""')}"`, op.type ?? "", op.testCount,
        op.averageDownloadMbps ?? "", op.averageUploadMbps ?? "",
        op.averageLatencyMs ?? "", op.packetLossAverage ?? "", op.averageScorePercentage ?? "",
      ].join(","))
      .join("\r\n");
    const csvContent = `data:text/csv;charset=utf-8,${header}${rows}`;
    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", `signallq_operadoras_${environment}_${period}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="networks-section-intro"
        overline="REDES & PROVEDORES"
        question="Onde e em que tipo de rede o app é mais usado?"
        description="Contexto de uso por tipo de conexão, operadora e região — para entender a base instalada, não para avaliar a qualidade da rede do usuário."
        source="FONTE · SIGNALLQ ANALYTICS (SESSÕES AGREGADAS)"
      />

      {/* 1. Filtros globais */}
      <GlobalFilters
        id="networks-global-filters"
        filters={[
          {
            key: "operator",
            label: "Operadora",
            value: operatorFilter,
            onChange: setOperatorFilter,
            options: [{ label: "Todas", value: "all" }, ...operators.map((op) => ({ label: op.name, value: op.id }))],
          },
        ]}
      />

      {/* 2. KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Velocidade média download"
          value={avgDownload != null ? `${avgDownload} Mbps` : "sem dados"}
          id="metric-avg-download"
        />
        <MetricCard
          label="Latência média"
          value={avgLatency != null ? `${avgLatency} ms` : "sem dados"}
          verdict={avgLatency != null ? latencyVerdict(avgLatency) : undefined}
          verdictNote="referência: <30ms excelente"
          id="metric-avg-latency"
        />
        <MetricCard
          label="Operadora com pior score"
          value={worstScoreOperator ? `${worstScoreOperator.name} (${worstScoreOperator.averageScorePercentage}%)` : "sem dados"}
          verdict={
            worstScoreOperator && worstScoreOperator.averageScorePercentage != null
              ? scoreVerdict(worstScoreOperator.averageScorePercentage)
              : undefined
          }
          verdictNote="score 0-100, calculado no dispositivo"
          id="metric-worst-operator"
        />
        <MetricCard
          label="Tipo de rede mais testado"
          value={mostTestedType ? mostTestedType.name : "sem dados"}
          id="metric-most-tested-type"
        />
      </div>

      {/* 3. Composição paridade mockup — onde o app é mais usado (mapa por UF,
          sem coluna de região no worker hoje) + sessões por operadora (real). */}
      <div className="grid grid-cols-1 lg:grid-cols-[1.2fr_1fr] gap-6">
        <ChartCard
          title="Onde o app é mais usado"
          description="Volume de sessões por estado — não é indicador de qualidade de rede."
          id="regions-map-card"
        >
          <FeatureComingSoon
            feature="Mapa de sessões por UF"
            reason="Métrica ainda não disponível — aguardando exposição no worker (diagnostic_sessions não coleta região/UF hoje)"
          />
        </ChartCard>
        <OperatorSessionsList operators={filteredOperators} wifiStat={wifiStat} mobileStat={mobileStat} />
      </div>

      {/* Gráfico complementar — responde "onde a qualidade varia": score médio
          (agrega latência, perda de pacote e velocidade) por operadora. Mantido
          como contexto adicional fora da composição fixa do mockup. */}
      <ChartCard
        title="Score Médio de Diagnóstico por Operadora"
        description="Score calculado pelo engine local do app (0 a 100), agregado por operadora. Não é pesquisa de satisfação — essa fonte de dado não existe hoje."
        id="networks-main-chart"
      >
        <BarChart
          data={filteredOperators}
          xAxisKey="name"
          series={[{ key: "averageScorePercentage", name: "Score médio", color: "var(--success)" }]}
        />
      </ChartCard>

      {/* 4. Bloco de explicação */}
      {insightText && <InsightBlock id="networks-insight-block">{insightText}</InsightBlock>}

      {/* Detalhe por tipo de rede — Wi-Fi vs móvel */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MetricCard
          label="Wi-Fi · diagnósticos no período"
          value={(wifiStat?.count ?? 0).toLocaleString("pt-BR")}
          verdict={wifiStat?.avgScore != null ? scoreVerdict(wifiStat.avgScore) : undefined}
          verdictNote={`score médio ${fmt(wifiStat?.avgScore ?? null)} · latência média ${fmt(wifiStat?.avgLatencyMs ?? null, " ms")}`}
          id="metric-wifi-summary"
        />
        <MetricCard
          label="Rede móvel · diagnósticos no período"
          value={(mobileStat?.count ?? 0).toLocaleString("pt-BR")}
          verdict={mobileStat?.avgScore != null ? scoreVerdict(mobileStat.avgScore) : undefined}
          verdictNote={`score médio ${fmt(mobileStat?.avgScore ?? null)} · latência média ${fmt(mobileStat?.avgLatencyMs ?? null, " ms")}`}
          id="metric-mobile-summary"
        />
      </div>

      {/* Visão secundária colapsável — GH#746: os outros 3 gráficos (download por
          operadora, latência e perda de pacote por tipo de rede) eram peso igual
          ao gráfico principal na versão anterior. Os mesmos números já aparecem
          na tabela de investigação por operadora; aqui ficam disponíveis para
          quem quiser o recorte por tipo de rede, sem competir com o gráfico
          principal. */}
      <button
        type="button"
        onClick={() => setShowDetails((v) => !v)}
        className="w-full flex items-center justify-between gap-3 px-5 py-3.5 rounded-xl transition-colors cursor-pointer select-none"
        style={{ background: "var(--bg-surface)", border: "1px solid var(--border)", color: "var(--text-secondary)" }}
        aria-expanded={showDetails}
        aria-controls="networks-secondary-detail"
        id="networks-secondary-detail-toggle"
      >
        <span className="text-xs font-sans font-semibold uppercase tracking-[0.06em]">
          Detalhamento por tipo de rede e operadora
        </span>
        <ChevronDown className={`w-4 h-4 shrink-0 transition-transform duration-200 ${showDetails ? "rotate-180" : ""}`} />
      </button>

      {showDetails && (
        <div id="networks-secondary-detail" className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <ChartCard
            title="Velocidade média de download por operadora"
            description="Amostragem em megabits por segundo, via speedtest local integrado no SDK."
          >
            <BarChart
              data={filteredOperators}
              xAxisKey="name"
              series={[{ key: "averageDownloadMbps", name: "Velocidade Down (Mbps)", color: "var(--info)" }]}
            />
          </ChartCard>

          <ChartCard
            title="Latência média por tipo de rede"
            description="Média de latency_ms reportada pelo app, agrupada por network_type, no período selecionado."
          >
            <BarChart
              data={networkStats}
              xAxisKey="name"
              series={[{ key: "avgLatencyMs", name: "Latência média (ms)", color: "var(--info)" }]}
            />
          </ChartCard>

          <ChartCard
            title="Perda de pacote média por tipo de rede"
            description="Média de packet_loss reportada pelo app, agrupada por network_type, no período selecionado."
          >
            <BarChart
              data={networkStats}
              xAxisKey="name"
              series={[{ key: "avgPacketLoss", name: "Perda de pacote (%)", color: "var(--attention)" }]}
            />
          </ChartCard>
        </div>
      )}

      {/* 5. Tabela de investigação */}
      <SectionCard
        title="Estudo Técnico Comparativo de Conectividade"
        description="Agregado de latências de rádio e perdas de pacotes das operadoras atuantes no território brasileiro."
        id="operators-main-card"
        actions={
          latencyLeader ? (
            <div className="flex items-center gap-1.5 text-xs text-[var(--text-secondary)] bg-[var(--bg-surface-muted)] border border-[var(--border)] px-3 py-1 rounded-xl">
              <Award className="w-3.5 h-3.5" />
              <span className="font-semibold">
                {latencyLeader.name} líder em latência ({latencyLeader.averageLatencyMs}ms)
              </span>
            </div>
          ) : undefined
        }
      >
        <DataTable
          data={filteredOperators}
          columns={tableColumns}
          keyExtractor={(row) => row.id}
          emptyMessage="Nenhuma medição consolidade de operadora encontrada."
        />
      </SectionCard>

      {/* 6. Ações */}
      <ActionsRow
        id="networks-actions-row"
        actions={[
          { label: "Exportar relatório por operadora", onClick: handleExportOperators },
          { label: "Ver diagnósticos desse tipo de rede", onClick: () => onNavigate("/diagnostics"), variant: "secondary" },
        ]}
      />
    </div>
  );
};
