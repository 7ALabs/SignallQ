import React from "react";
import { diagnosticsService } from "../../services/diagnosticsService";
import { DiagnosticsFilters } from "./components/DiagnosticsFilters";
import { DiagnosticsMetricGrid } from "./components/DiagnosticsMetricGrid";
import { DiagnosticsAggregateTable } from "./components/DiagnosticsAggregateTable";
import { DataTable } from "../../components/ui/DataTable";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { LoadingState } from "../../components/ui/LoadingState";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { BarChart } from "../../components/charts/BarChart";
import { ChartCard } from "../../components/ui/ChartCard";
import { DiagnosticSession, DiagnosticsSummary, DistChannel, BuildType, DataPlatform, AggregateRow } from "../../types/diagnostics";
import { AppEnvironment } from "../../types/admin";
import { Smartphone, Clock, Server, Sparkles, Zap, Info, ShieldCheck, AlertOctagon } from "lucide-react";

interface DiagnosticsPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
  onNavigate?: (path: string) => void;
}

export const DiagnosticsPage: React.FC<DiagnosticsPageProps> = ({
  environment: propEnv,
  period: propPeriod,
  triggerRefreshCounter,
  onNavigate,
}) => {
  // Advanced filter states
  const [localEnv, setLocalEnv] = React.useState<AppEnvironment>(propEnv);
  const [localPeriod, setLocalPeriod] = React.useState<string>(propPeriod);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [selectedNetwork, setSelectedNetwork] = React.useState("all");
  const [selectedOperator, setSelectedOperator] = React.useState("all");
  const [selectedScore, setSelectedScore] = React.useState("all");
  const [selectedIssue, setSelectedIssue] = React.useState("all");
  const [selectedVersion, setSelectedVersion] = React.useState("all");
  const [availableVersions, setAvailableVersions] = React.useState<string[]>([]);
  const [selectedDistChannel, setSelectedDistChannel] = React.useState<DistChannel | ("")>("");
  const [selectedBuildType, setSelectedBuildType] = React.useState<BuildType | ("")>("");
  const [selectedPlatform, setSelectedPlatform] = React.useState<DataPlatform | ("")>("");

  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = React.useState(false);
  const [sessions, setSessions] = React.useState<DiagnosticSession[]>([]);
  const [selectedSession, setSelectedSession] = React.useState<DiagnosticSession | null>(null);
  const [summary, setSummary] = React.useState<DiagnosticsSummary | null>(null);
  const [aggregate, setAggregate] = React.useState<AggregateRow[]>([]);

  // Re-diagnosing states
  const [diagnosingId, setDiagnosingId] = React.useState<string | null>(null);
  const [statusMessage, setStatusMessage] = React.useState<string | null>(null);

  React.useEffect(() => {
    setLocalEnv(propEnv);
    setLocalPeriod(propPeriod);
  }, [propEnv, propPeriod]);

  const loadSessionsData = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [sessionsData, summaryResponse] = await Promise.all([
        diagnosticsService.getDiagnosticSessions({
          environment: localEnv,
          period: localPeriod,
          search: searchQuery,
          platform: selectedPlatform || undefined,
        }),
        diagnosticsService.getDiagnosticsSummary({
          environment: localEnv,
          period: localPeriod,
          platform: selectedPlatform || undefined,
        })
      ]);

      setSummary(summaryResponse);

      // GH#552 (Fase 3) — mesma fonte usada pela tabela de investigação e pelo
      // gráfico principal (barras por tipo de rede), buscada uma única vez aqui.
      diagnosticsService
        .getAggregateDiagnostics({ environment: localEnv, period: localPeriod })
        .then(setAggregate)
        .catch((err) => console.error("Failed to load aggregate data:", err));

      // Opções de versão disponíveis vêm do conjunto completo retornado pela API,
      // antes de qualquer filtro local — senão o próprio filtro de versão se auto-restringe.
      const versions = Array.from(new Set(sessionsData.map(s => s.appVersion).filter(Boolean))).sort();
      setAvailableVersions(versions);

      // Filter with local parameters
      let filtered = sessionsData;

      if (selectedNetwork !== "all") {
        filtered = filtered.filter(s => s.networkType === selectedNetwork);
      }

      if (selectedOperator !== "all") {
        filtered = filtered.filter(s =>
          s.operator?.toLowerCase().includes(selectedOperator.toLowerCase())
        );
      }

      if (selectedScore !== "all") {
        filtered = filtered.filter(s => {
          if (selectedScore === "poor") return s.score < 60;
          if (selectedScore === "medium") return s.score >= 60 && s.score <= 80;
          return s.score > 80;
        });
      }

      if (selectedIssue !== "all") {
        filtered = filtered.filter(s =>
          s.issues.some(iss => iss.issue === selectedIssue)
        );
      }

      if (selectedVersion !== "all") {
        filtered = filtered.filter(s => s.appVersion === selectedVersion);
      }

      if (selectedDistChannel !== "") {
        filtered = filtered.filter(s => s.distChannel === selectedDistChannel);
      }

      if (selectedBuildType !== "") {
        filtered = filtered.filter(s => s.buildType === selectedBuildType);
      }

      setSessions(filtered);
      if (filtered.length > 0) {
        setSelectedSession(filtered[0]);
      } else {
        setSelectedSession(null);
      }
    } catch (err: any) {
      console.error("Failed to fetch diagnostics sessions:", err);
      const code = err?.code;
      setError(code > 0 ? `Erro: ${code}` : "Sem conexão com o servidor");
    } finally {
      setLoading(false);
    }
  }, [localEnv, localPeriod, searchQuery, selectedNetwork, selectedOperator, selectedScore, selectedIssue, selectedVersion, selectedDistChannel, selectedBuildType, selectedPlatform, triggerRefreshCounter]);

  React.useEffect(() => {
    loadSessionsData();
  }, [loadSessionsData]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await new Promise((resolve) => setTimeout(resolve, 300));
    await loadSessionsData();
    setIsRefreshing(false);
  };

  const handleTriggerReDiagnosis = async (id: string) => {
    setDiagnosingId(id);
    setStatusMessage(null);
    try {
      const res = await diagnosticsService.triggerReDiagnosis(id);
      // res.message já explica o motivo (implementado ou não) — mostrar sempre,
      // não só no caminho de sucesso, senão o clique falha silenciosamente em produção.
      setStatusMessage(res.message);

      if (res.success && selectedSession && selectedSession.id === id) {
        setSelectedSession({
          ...selectedSession,
          aiStatus: "completed",
          aiSummaryReport: "Ajuste e recalculado efetuado com sucesso via Gemini 1.5 Flash. O robô reavaliou que as flutuações eletromagnéticas de radiofrequência local foram dirimidas de forma parcial, mantendo apenas latência leve no DNS local.",
          issues: selectedSession.issues.map(i => ({ ...i, severity: "attention" })),
        });
      }
    } catch (e) {
      console.error("Critical connection failure with remote router", e);
      setStatusMessage("Erro crítico de conexão com o Cloudflare Workers Gateway.");
    } finally {
      setDiagnosingId(null);
    }
  };

  // GH#552 (Fase 3) — tipo de rede com mais sessões no período, derivado da
  // agregação real (D1, network_type) — alimenta o KPI "tipo mais comum".
  const topNetworkType = React.useMemo(() => {
    if (aggregate.length === 0) return null;
    const top = [...aggregate].sort((a, b) => b.diagnosticsCount - a.diagnosticsCount)[0];
    const totalCount = aggregate.reduce((acc, r) => acc + r.diagnosticsCount, 0);
    const percentage = totalCount > 0 ? (top.diagnosticsCount / totalCount) * 100 : 0;
    return { name: top.networkType, percentage };
  }, [aggregate]);

  // Bloco de explicação — compara o tipo de rede líder com o de pior score,
  // só com dado já carregado (sem inventar correlação).
  const insightText = React.useMemo(() => {
    if (aggregate.length === 0 || !topNetworkType) return null;
    const worst = [...aggregate].sort((a, b) => a.avgScore - b.avgScore)[0];
    if (worst.networkType === topNetworkType.name) {
      return `"${topNetworkType.name}" concentra ${topNetworkType.percentage.toFixed(0)}% dos diagnósticos do período e também tem o pior score médio (${worst.avgScore}/100) — vale investigar antes das outras redes.`;
    }
    return `"${topNetworkType.name}" concentra ${topNetworkType.percentage.toFixed(0)}% dos diagnósticos do período. O pior score médio está em "${worst.networkType}" (${worst.avgScore}/100) — ver detalhe na tabela abaixo.`;
  }, [aggregate, topNetworkType]);

  const handleExportSessions = () => {
    const header = "ID,Dispositivo,Rede,Operadora,Download,Upload,Score,Problemas,Timestamp\r\n";
    const rows = sessions
      .map((s) => [
        s.id, s.deviceModel, s.networkType, s.operator ?? "",
        s.speed.downloadMbps, s.speed.uploadMbps, s.score, s.issues.length, s.timestamp,
      ].join(","))
      .join("\r\n");
    const csvContent = `data:text/csv;charset=utf-8,${header}${rows}`;
    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", `signallq_diagnosticos_${localEnv}_${localPeriod}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const tableColumns = [
    {
      header: "Sessão ID",
      accessor: (row: DiagnosticSession) => (
        <span className="font-mono font-bold text-[var(--text-secondary)]">{(row.id ?? "—").replace("diag_", "")}</span>
      ),
    },
    {
      header: "Dispositivo",
      accessor: (row: DiagnosticSession) => (
        <div>
          <span className="font-sans font-medium text-[var(--text-primary)] block max-w-[140px] truncate">{row.deviceModel}</span>
          <span
            className={`text-[9px] font-mono uppercase tracking-wider font-bold ${
              row.platform === "web" ? "text-[var(--info)]" : "text-[var(--text-tertiary)]"
            }`}
          >
            {row.platform === "web" ? "WebApp" : "Android"}
          </span>
        </div>
      ),
    },
    {
      header: "Rede / Canal",
      accessor: (row: DiagnosticSession) => {
        const details = row.networkStrength?.ssid
          ? `SSID: ${row.networkStrength.ssid}`
          : row.operator || "-";
        return (
          <div>
            <span className="font-sans text-[11px] text-[var(--text-secondary)] block uppercase font-bold">{row.networkType}</span>
            <span className="text-[10px] text-[var(--text-tertiary)] font-mono block truncate max-w-[120px]">{details}</span>
          </div>
        );
      },
    },
    {
      header: "Download / Upload",
      accessor: (row: DiagnosticSession) => (
        <span className="font-mono text-[var(--info)] font-bold">
          {row.speed.downloadMbps} / {row.speed.uploadMbps} <span className="text-[10px] text-[var(--text-tertiary)]">M</span>
        </span>
      ),
    },
    {
      header: "Problemas",
      accessor: (row: DiagnosticSession) => {
        const severity = row.issues.some((i) => i.severity === "critical")
          ? "critical"
          : row.issues.length > 0
          ? "attention"
          : "ok";
        return (
          <StatusBadge
            status={severity}
            customLabel={row.issues.length > 0 ? `${row.issues.length} detectados` : "Limpo"}
          />
        );
      },
    },
  ];

  return (
    <div className="space-y-6">
      {/* 1. Avançados Filtros Integrados */}
      <DiagnosticsFilters
        searchText={searchQuery}
        onSearchChange={setSearchQuery}
        selectedNetwork={selectedNetwork}
        onNetworkChange={setSelectedNetwork}
        selectedOperator={selectedOperator}
        onOperatorChange={setSelectedOperator}
        selectedScore={selectedScore}
        onScoreChange={setSelectedScore}
        selectedIssue={selectedIssue}
        onIssueChange={setSelectedIssue}
        selectedVersion={selectedVersion}
        onVersionChange={setSelectedVersion}
        availableVersions={availableVersions}
        selectedDistChannel={selectedDistChannel}
        onDistChannelChange={setSelectedDistChannel}
        selectedBuildType={selectedBuildType}
        onBuildTypeChange={setSelectedBuildType}
        selectedPlatform={selectedPlatform}
        onPlatformChange={setSelectedPlatform}
        selectedPeriod={localPeriod}
        onPeriodChange={setLocalPeriod}
        selectedEnvironment={localEnv}
        onEnvironmentChange={setLocalEnv}
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing}
      />

      {/* 2. KPIs — 4 cards, com veredito humano (GH#552 Fase 3, substitui grid de 6) */}
      <DiagnosticsMetricGrid environment={localEnv} summary={summary} topNetworkType={topNetworkType} />

      {/* 3. Gráfico principal — o que os usuários estão medindo, por tipo de rede
          (D1 diagnostic_sessions agregado; sem granularidade diária no worker
          hoje, então o gráfico é por composição do período, não série temporal) */}
      <ChartCard
        title="Diagnósticos por tipo de rede"
        description="Volume de sessões e score médio no período selecionado, por tipo de rede."
        id="diagnostics-main-chart"
      >
        {aggregate.length === 0 ? (
          <FeatureComingSoon feature="Distribuição por tipo de rede" reason="Sem sessões suficientes no período selecionado" compact />
        ) : (
          <BarChart
            data={aggregate.map((r) => ({ name: r.networkType, sessões: r.diagnosticsCount, score: r.avgScore }))}
            xAxisKey="name"
            series={[
              { key: "sessões", name: "Sessões", color: "var(--chart-line-primary)" },
              { key: "score", name: "Score médio", color: "var(--chart-line-tertiary)" },
            ]}
          />
        )}
      </ChartCard>

      {/* 4. Bloco de explicação — antes da tabela de investigação */}
      {insightText && <InsightBlock id="diagnostics-insight-block">{insightText}</InsightBlock>}

      {/* Funil de diagnóstico (iniciado → concluído → laudo gerado) pedido pelo
          wireframe não tem contrato de dados real hoje — o worker não rastreia
          estágio de sessão, só o resultado final persistido. Sem número inventado. */}
      <FeatureComingSoon
        feature="Funil de Diagnóstico (iniciado → concluído → laudo gerado)"
        reason="Requer rastreamento de estágio por sessão no worker/app — hoje só o resultado final é persistido"
        compact
      />

      {/* 5. Tabela de investigação — agregado por tipo de rede (drill-down) */}
      <DiagnosticsAggregateTable environment={localEnv} data={aggregate} />

      {/* 6. Tabela de Sessões de Diagnósticos Recentes (Filtros Avançados) */}
      {loading ? (
        <LoadingState message="Acompanhando logs de conectividade (Android e WebApp)..." />
      ) : error ? (
        <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[var(--error)]/20 bg-[var(--error)]/5 rounded-[8px]">
          <h4 className="text-sm font-semibold text-[var(--error)] uppercase tracking-wider font-sans">Erro de Telemetria</h4>
          <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">{error}</p>
          <button
            onClick={() => { setError(null); loadSessionsData(); }}
            className="mt-4 px-4 py-2 text-xs bg-[var(--error)]/10 border border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 transition-all rounded-xl font-sans"
          >
            TENTAR NOVAMENTE
          </button>
        </div>
      ) : sessions.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[var(--border)] bg-[var(--bg-sidebar)] rounded-[8px]">
          <h4 className="text-xs font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-sans">Sem dados</h4>
          <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">Nenhuma sessão de diagnóstico encontrada neste período.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-6">
          {/* Esquerda: Lista de testes físicos */}
          <div className="xl:col-span-7">
            <SectionCard
              title="Sessões de Telemetria de Conectividade"
              description="Histórico bruto das varreduras físicas de rádio e speedtests disparados via SDK móvel de borda."
            >
              <DataTable
                data={sessions}
                columns={tableColumns}
                keyExtractor={(row) => row.id}
                emptyMessage="Nenhum log corresponde aos parâmetros dos filtros avançados configurados."
                rowClassName="cursor-pointer"
                onRowClick={(row) => setSelectedSession(row)}
                id="telemetry-diagnostics-table"
              />
              <div className="mt-4 flex items-center gap-2 text-[10px] text-[var(--text-secondary)] font-sans select-none">
                <Info className="w-4 h-4 text-[var(--text-tertiary)] shrink-0" />
                <span>Dica: Clique em qualquer sessão para carregar o escrutínio térmico detalhado de RF local e laudo de IA à direita.</span>
              </div>
            </SectionCard>
          </div>

          {/* Direita: Inspetor completo da sessão e laudo Gemini */}
          <div className="xl:col-span-5">
            {selectedSession ? (
              <div className="rounded-[8px] p-6 relative overflow-hidden" style={{ background: "var(--bg-surface)", border: "1px solid var(--border)" }}>
                <div className="absolute top-0 right-0 w-32 h-32 bg-[var(--primary)]/5 rounded-full filter blur-2xl flex items-center justify-center pointer-events-none" />

                {/* ID and date details */}
                <div className="flex items-center justify-between border-b border-[var(--border)] pb-4 mb-4 select-none">
                  <div>
                    <span className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider block">ID do laudo técnico</span>
                    <h4 className="text-sm font-bold text-[var(--text-primary)] font-mono">{selectedSession.id}</h4>
                  </div>
                  <div className="text-right">
                    <div className="flex items-center text-xs text-[var(--text-secondary)] font-mono gap-1 justify-end">
                      <Clock className="w-3.5 h-3.5 text-[var(--text-tertiary)] mr-1" />
                      <span>{new Date(selectedSession.timestamp).toLocaleTimeString("pt-BR")}</span>
                    </div>
                    <span className="text-[10px] text-[var(--text-tertiary)] font-mono block mt-0.5">{selectedSession.timestamp.split("T")[0]}</span>
                  </div>
                </div>

                {/* Hardware constraints */}
                <div className="grid grid-cols-2 gap-4 border-b border-[var(--border)] pb-4 mb-4 font-sans text-xs">
                  <div>
                    <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider">
                      Dispositivo de Borda · {selectedSession.platform === "web" ? "WebApp" : "Android"}
                    </div>
                    <p className="font-semibold text-[var(--text-primary)] mt-0.5 flex items-center gap-1.5 leading-none">
                      <Smartphone className="w-3.5 h-3.5 text-[var(--text-secondary)] shrink-0" />
                      <span>{selectedSession.deviceModel}</span>
                    </p>
                  </div>
                  <div>
                    <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider">Arquitetura SO</div>
                    <p className="font-semibold text-[var(--text-secondary)] mt-0.5 flex items-center gap-1 leading-none truncate w-full">
                      <Server className="w-3.5 h-3.5 text-[var(--text-tertiary)] shrink-0" />
                      <span>{selectedSession.osVersion} • app {selectedSession.appVersion}</span>
                    </p>
                  </div>
                  {selectedSession.operator && (
                    <div>
                      <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider">Operadora</div>
                      <p className="font-semibold text-[var(--text-primary)] mt-0.5">{selectedSession.operator}</p>
                    </div>
                  )}
                  <div>
                    <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider">Score da Sessão</div>
                    <p className={`font-semibold mt-0.5 ${selectedSession.score >= 80 ? "text-emerald-400" : selectedSession.score >= 60 ? "text-amber-500" : "text-red-400"}`}>
                      {selectedSession.score}/100
                    </p>
                  </div>
                </div>

                {/* Physical metrics metrics table */}
                <div className="mb-5 select-none">
                  <span className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider block mb-2.5">Medições Físicas (SpeedTest)</span>
                  <div className="grid grid-cols-3 gap-2.5">
                    <div className="bg-[var(--bg-base)] border border-[var(--border)]/40 p-3 rounded-xl">
                      <span className="text-[9px] font-mono text-[var(--text-tertiary)] uppercase">Download</span>
                      <div className="flex items-baseline gap-1 mt-1 text-xs">
                        <span className="text-sm font-bold text-[var(--info)]">{selectedSession.speed.downloadMbps}</span>
                        <span className="text-[10px] text-[var(--text-tertiary)] font-mono font-medium">Mbps</span>
                      </div>
                    </div>

                    <div className="bg-[var(--bg-base)] border border-[var(--border)]/40 p-3 rounded-xl">
                      <span className="text-[9px] font-mono text-[var(--text-tertiary)] uppercase">Upload</span>
                      <div className="flex items-baseline gap-1 mt-1 text-xs">
                        <span className="text-sm font-bold text-indigo-400">{selectedSession.speed.uploadMbps}</span>
                        <span className="text-[10px] text-[var(--text-tertiary)] font-mono font-medium">Mbps</span>
                      </div>
                    </div>

                    <div className="bg-[var(--bg-base)] border border-[var(--border)]/40 p-3 rounded-xl">
                      <span className="text-[9px] font-mono text-[var(--text-tertiary)] uppercase font-semibold">Bufferbloat</span>
                      <div className="flex items-baseline gap-1 mt-1 justify-between text-xs font-mono">
                        <span className={`text-md font-bold ${selectedSession.speed.bufferbloatGrade === "A+" ? "text-emerald-400" : "text-amber-500"}`}>{selectedSession.speed.bufferbloatGrade}</span>
                        <span className="text-[9px] bg-zinc-950 text-[var(--text-tertiary)] px-1 rounded">Grade</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Additional latency metrics */}
                <div className="grid grid-cols-3 gap-3 mb-5 bg-[var(--bg-base)]/40 p-3 rounded-xl border border-[var(--border)]/30 select-none">
                  <div className="text-center">
                    <span className="text-[9px] font-mono text-[var(--text-tertiary)] uppercase">Latência</span>
                    <p className="text-xs font-semibold text-[var(--text-primary)] mt-1">{selectedSession.speed.latencyMs} ms</p>
                  </div>
                  <div className="text-center border-l border-r border-[var(--border)]">
                    <span className="text-[9px] font-mono text-[var(--text-tertiary)] uppercase">Jitter</span>
                    <p className="text-xs font-semibold text-[var(--text-primary)] mt-1">{selectedSession.speed.jitterMs} ms</p>
                  </div>
                  <div className="text-center">
                    <span className="text-[9px] font-mono text-[var(--text-tertiary)] uppercase">Perda de Pacote</span>
                    <p className="text-xs font-semibold text-[var(--text-primary)] mt-1">{selectedSession.speed.packetLossPercentage}%</p>
                  </div>
                </div>

                {/* Network Quality indicators */}
                {selectedSession.networkStrength && (
                  <div className="mb-5 p-3.5 bg-[var(--bg-base)] border border-[var(--border)]/40 rounded-xl space-y-2 select-none">
                    <span className="text-[9px] text-[var(--primary)] font-sans uppercase tracking-wide block font-bold">Interfaces físicas & Rádio</span>
                    <div className="grid grid-cols-2 gap-3 text-xs font-sans">
                      <div>
                        <div className="text-[var(--text-tertiary)] text-[9px] font-sans">Força Sinal (RSSI)</div>
                        <div className="font-semibold text-[var(--text-primary)] mt-0.5">{selectedSession.networkStrength.signalStrengthDbm} dBm</div>
                      </div>
                      <div>
                        <div className="text-[var(--text-tertiary)] text-[9px] font-sans">Qualidade Estimada</div>
                        <div className="font-semibold text-emerald-400 mt-0.5">{selectedSession.networkStrength.signalQualityPercentage}%</div>
                      </div>
                      {selectedSession.networkStrength.ssid && (
                        <div>
                          <div className="text-[var(--text-tertiary)] text-[9px] font-sans">SSID Wi-Fi</div>
                          <div className="font-semibold text-[var(--text-secondary)] mt-0.5 font-mono truncate max-w-[130px]">{selectedSession.networkStrength.ssid}</div>
                        </div>
                      )}
                      {selectedSession.networkStrength.frequencyBandGhz && (
                        <div>
                          <div className="text-[var(--text-tertiary)] text-[9px] font-sans">Banda frequência</div>
                          <div className="font-semibold text-[var(--text-secondary)] mt-0.5">{selectedSession.networkStrength.frequencyBandGhz} GHz</div>
                        </div>
                      )}
                      {selectedSession.networkStrength.wifiStandard && (
                        <div>
                          <div className="text-[var(--text-tertiary)] text-[9px] font-sans">Padrão Wi-Fi</div>
                          <div className="font-semibold text-[var(--text-secondary)] mt-0.5 uppercase">802.11{selectedSession.networkStrength.wifiStandard}</div>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Identified list of problems */}
                <div className="mb-5">
                  <span className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider block mb-2 select-none font-semibold">Anomalias Físicas ({selectedSession.issues.length})</span>
                  {selectedSession.issues.length === 0 ? (
                    <div className="p-3 bg-emerald-950/20 border border-emerald-500/20 rounded-xl text-xs text-emerald-400 flex items-center gap-2 font-sans select-none">
                      <ShieldCheck className="w-4 h-4 shrink-0 text-[var(--success)]" />
                      <span>Todas as interfaces de rádio analisadas operam na estabilidade linear.</span>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {selectedSession.issues.map((issue, index) => (
                        <div
                          key={index}
                          className={`p-3 border rounded-xl flex items-start gap-2.5 text-xs font-sans ${
                            issue.severity === "critical"
                              ? "bg-red-950/10 border-red-500/10 text-zinc-200"
                              : "bg-amber-950/10 border-amber-500/10 text-zinc-200"
                          }`}
                        >
                          <AlertOctagon className={`w-4 h-4 shrink-0 mt-0.5 ${issue.severity === "critical" ? "text-red-400" : "text-amber-500"}`} />
                          <div>
                            <div className="font-semibold font-sans tracking-wider text-[10px] uppercase text-[var(--text-secondary)]">
                              {(issue.issue ?? "").replace(/_/g, " ")}
                            </div>
                            <p className="text-[var(--text-secondary)] mt-1 leading-normal text-[11px] font-sans">{issue.description}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* AI report segment */}
                <div className="border-t border-[var(--border)] pt-5">
                  <div className="flex items-center justify-between pb-3 select-none">
                    <div className="flex items-center gap-1.5">
                      <Sparkles className="w-4 h-4 text-[var(--text-secondary)]" />
                      <span className="text-xs font-bold text-[var(--text-primary)] font-sans">Laudo Preditivo Gemini</span>
                    </div>
                    <button
                      onClick={() => handleTriggerReDiagnosis(selectedSession.id)}
                      disabled={diagnosingId !== null}
                      className="flex items-center gap-1 text-[10px] px-2.5 py-1.5 border border-[var(--border)] hover:border-[var(--primary)]/40 hover:bg-[var(--primary)]/10 text-[var(--primary)] rounded-lg transition-colors font-sans font-bold uppercase select-none cursor-pointer"
                    >
                      <Zap className={`w-3 h-3 ${diagnosingId ? "animate-bounce" : ""}`} />
                      <span>{diagnosingId ? "Roteando..." : "Regenerar IA"}</span>
                    </button>
                  </div>

                  {statusMessage && (
                    <div className="p-2.5 bg-[var(--bg-base)] text-[var(--text-secondary)] text-[10px] font-sans mb-3 rounded-lg border border-[var(--border)] select-none text-center">
                      {statusMessage}
                    </div>
                  )}

                  {selectedSession.aiSummaryReport ? (
                    <div className="bg-[var(--bg-sidebar)] text-[11px] font-sans text-[var(--text-secondary)] leading-relaxed p-4 rounded-xl border border-[var(--border)]/80 max-h-40 overflow-y-auto">
                      {selectedSession.aiSummaryReport}
                    </div>
                  ) : (
                    <div className="text-center py-6 text-xs text-[var(--text-tertiary)] font-sans border border-dashed border-[var(--border)] rounded-xl select-none">
                      Nenhum laudo compilado para esta telemetria. Clique em "REGENERAR IA" para processar no Gemini.
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="py-20 text-center rounded-[8px] p-6 select-none font-sans" style={{ background: "var(--bg-surface)", border: "1px dashed var(--border)" }}>
                <p className="text-xs text-[var(--text-tertiary)]">Selecione algum registro físico de rádio para investigar.</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 7. Ações */}
      <ActionsRow
        id="diagnostics-actions-row"
        actions={[
          { label: "Exportar CSV", onClick: handleExportSessions, variant: "secondary" },
          ...(onNavigate ? [{ label: "Ver Redes & Provedores", onClick: () => onNavigate("/networks") }] : []),
        ]}
      />
    </div>
  );
};
