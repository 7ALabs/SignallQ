import React from "react";
import { diagnosticsService } from "../../services/diagnosticsService";
import { DiagnosticsFilters } from "./components/DiagnosticsFilters";
import { DiagnosticsMetricGrid } from "./components/DiagnosticsMetricGrid";
import { DiagnosticIntelligencePanel } from "./components/DiagnosticIntelligencePanel";
import { DiagnosticsAggregateTable } from "./components/DiagnosticsAggregateTable";
import { IssueDetailPanel } from "./components/IssueDetailPanel";
import { DataTable } from "../../components/ui/DataTable";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { LoadingState } from "../../components/ui/LoadingState";
import { DiagnosticSession, DiagnosticsSummary } from "../../types/diagnostics";
import { AppEnvironment } from "../../types/admin";
import { Smartphone, Clock, Server, Sparkles, Zap, Info, ShieldCheck, AlertOctagon } from "lucide-react";

interface DiagnosticsPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const DiagnosticsPage: React.FC<DiagnosticsPageProps> = ({
  environment: propEnv,
  period: propPeriod,
  triggerRefreshCounter,
}) => {
  // Advanced filter states
  const [localEnv, setLocalEnv] = React.useState<AppEnvironment>(propEnv);
  const [localPeriod, setLocalPeriod] = React.useState<string>(propPeriod);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [selectedNetwork, setSelectedNetwork] = React.useState("all");
  const [selectedOperator, setSelectedOperator] = React.useState("all");
  const [selectedScore, setSelectedScore] = React.useState("all");
  const [selectedIssue, setSelectedIssue] = React.useState("all");
  const [selectedAiProvider, setSelectedAiProvider] = React.useState("all");

  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = React.useState(false);
  const [sessions, setSessions] = React.useState<DiagnosticSession[]>([]);
  const [selectedSession, setSelectedSession] = React.useState<DiagnosticSession | null>(null);
  const [summary, setSummary] = React.useState<DiagnosticsSummary | null>(null);
  
  // Highlighted Intelligence problem
  const [selectedIntelIssue, setSelectedIntelIssue] = React.useState<string | null>(null);

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
          search: searchQuery
        }),
        diagnosticsService.getDiagnosticsSummary({
          environment: localEnv,
          period: localPeriod
        })
      ]);

      setSummary(summaryResponse);

      // Filter with local parameters
      let filtered = sessionsData;

      if (selectedNetwork !== "all") {
        filtered = filtered.filter(s => s.networkType === selectedNetwork);
      }

      if (selectedOperator !== "all") {
        filtered = filtered.filter(s => 
          s.networkStrength?.carrierName?.toLowerCase().includes(selectedOperator.toLowerCase())
        );
      }

      if (selectedScore !== "all") {
        filtered = filtered.filter(s => {
          const score = s.networkStrength?.signalQualityPercentage || 80; // defaults to 80
          if (selectedScore === "poor") return score < 60;
          if (selectedScore === "medium") return score >= 60 && score <= 80;
          return score > 80;
        });
      }

      if (selectedIssue !== "all") {
        filtered = filtered.filter(s => 
          s.issues.some(iss => iss.issue === selectedIssue)
        );
      }

      if (selectedAiProvider !== "all") {
        // Mock matching provider based on status
        filtered = filtered.filter(s => {
          if (selectedAiProvider === "gemini_flash") return s.aiStatus === "completed";
          if (selectedAiProvider === "local_fallback") return s.aiStatus === "failed";
          return true;
        });
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
  }, [localEnv, localPeriod, searchQuery, selectedNetwork, selectedOperator, selectedScore, selectedIssue, selectedAiProvider, triggerRefreshCounter]);

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
      if (res.success) {
        setStatusMessage(res.message);
        
        // Simutate updating report in local UI state
        if (selectedSession && selectedSession.id === id) {
          setSelectedSession({
            ...selectedSession,
            aiStatus: "completed",
            aiSummaryReport: "Ajuste e recalculado efetuado com sucesso via Gemini 1.5 Flash. O robô reavaliou que as flutuações eletromagnéticas de radiofrequência local foram dirimidas de forma parcial, mantendo apenas latência leve no DNS local.",
            issues: selectedSession.issues.map(i => ({ ...i, severity: "attention" })),
          });
        }
      }
    } catch (e) {
      console.error("Critical connection failure with remote router", e);
      setStatusMessage("Erro crítico de conexão com o Cloudflare Workers Gateway.");
    } finally {
      setDiagnosingId(null);
    }
  };

  const tableColumns = [
    {
      header: "Sessão ID",
      accessor: (row: DiagnosticSession) => (
        <span className="font-mono font-bold text-zinc-400">{row.id.replace("diag_", "")}</span>
      ),
    },
    {
      header: "Dispositivo",
      accessor: (row: DiagnosticSession) => (
        <span className="font-sans font-medium text-white block max-w-[140px] truncate">{row.deviceModel}</span>
      ),
    },
    {
      header: "Rede / Canal",
      accessor: (row: DiagnosticSession) => {
        const details = row.networkStrength?.ssid 
          ? `SSID: ${row.networkStrength.ssid}` 
          : row.networkStrength?.carrierName || "-";
        return (
          <div>
            <span className="font-mono text-[11px] text-zinc-350 block uppercase font-bold">{row.networkType}</span>
            <span className="text-[10px] text-zinc-500 font-mono block truncate max-w-[120px]">{details}</span>
          </div>
        );
      },
    },
    {
      header: "Download / Upload",
      accessor: (row: DiagnosticSession) => (
        <span className="font-mono text-[#38BDF8] font-bold">
          {row.speed.downloadMbps} / {row.speed.uploadMbps} <span className="text-[10px] text-zinc-500">M</span>
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
        selectedAiProvider={selectedAiProvider}
        onAiProviderChange={setSelectedAiProvider}
        selectedPeriod={localPeriod}
        onPeriodChange={setLocalPeriod}
        selectedEnvironment={localEnv}
        onEnvironmentChange={setLocalEnv}
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing}
      />

      {/* 2. Grid de cards principais - 6 cards */}
      <DiagnosticsMetricGrid environment={localEnv} summary={summary} />

      {/* 3. Diagnostic Intelligence Panel (5 cards cognitivos) */}
      <DiagnosticIntelligencePanel onSelectIssue={(name) => setSelectedIntelIssue(name)} />

      {/* 4. Linha dividida - tabela agregada (esquerda) ou detalhe de problema (direita) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <DiagnosticsAggregateTable environment={localEnv} />
        </div>
        <div>
          <IssueDetailPanel
            selectedIssueName={selectedIntelIssue}
            onClear={() => setSelectedIntelIssue(null)}
          />
        </div>
      </div>

      {/* 5. Tabela de Sessões de Diagnósticos Recentes (Filtros Avançados) */}
      {loading ? (
        <LoadingState message="Acompanhando logs de conectividade do app Android..." />
      ) : error ? (
        <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-red-500/20 bg-[#FF4D4F]/5 rounded-2xl">
          <h4 className="text-sm font-semibold text-[#FF4D4F] uppercase tracking-wider font-mono">Erro de Telemetria</h4>
          <p className="text-xs text-neutral-400 mt-2 font-sans">{error}</p>
          <button
            onClick={() => { setError(null); loadSessionsData(); }}
            className="mt-4 px-4 py-2 text-xs bg-[#FF4D4F]/10 border border-[#FF4D4F]/20 text-[#FF4D4F] hover:bg-[#FF4D4F]/20 transition-all rounded-xl font-mono"
          >
            TENTAR NOVAMENTE
          </button>
        </div>
      ) : sessions.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[#262626] bg-[#111111] rounded-2xl">
          <h4 className="text-xs font-semibold text-[#9CA3AF] uppercase tracking-widest font-mono">Sem dados</h4>
          <p className="text-xs text-[#9CA3AF] mt-2 font-sans">Nenhuma sessão de diagnóstico encontrada neste período.</p>
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
              <div className="mt-4 flex items-center gap-2 text-[10px] text-[#9CA3AF] font-mono select-none">
                <Info className="w-4 h-4 text-zinc-550 shrink-0" />
                <span>Dica: Clique em qualquer sessão para carregar o escrutínio térmico detalhado de RF local e laudo de IA à direita.</span>
              </div>
            </SectionCard>
          </div>

          {/* Direita: Inspetor completo da sessão e laudo Gemini */}
          <div className="xl:col-span-5">
            {selectedSession ? (
              <div className="bg-zinc-950/60 border border-[#262626] rounded-2xl p-6 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-32 h-32 bg-[#6C2BFF]/5 rounded-full filter blur-2xl flex items-center justify-center pointer-events-none" />

                {/* ID and date details */}
                <div className="flex items-center justify-between border-b border-[#262626] pb-4 mb-4 select-none">
                  <div>
                    <span className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider block">ID do laudo técnico</span>
                    <h4 className="text-sm font-bold text-white font-mono">{selectedSession.id}</h4>
                  </div>
                  <div className="text-right">
                    <div className="flex items-center text-xs text-zinc-400 font-mono gap-1 justify-end">
                      <Clock className="w-3.5 h-3.5 text-zinc-500 mr-1" />
                      <span>{new Date(selectedSession.timestamp).toLocaleTimeString("pt-BR")}</span>
                    </div>
                    <span className="text-[10px] text-zinc-500 font-mono block mt-0.5">{selectedSession.timestamp.split("T")[0]}</span>
                  </div>
                </div>

                {/* Hardware constraints */}
                <div className="grid grid-cols-2 gap-4 border-b border-[#262626] pb-4 mb-4 font-sans text-xs">
                  <div>
                    <div className="text-[10px] text-zinc-500 font-mono uppercase tracking-wider">Dispositivo de Borda</div>
                    <p className="font-semibold text-white mt-0.5 flex items-center gap-1.5 leading-none">
                      <Smartphone className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                      <span>{selectedSession.deviceModel}</span>
                    </p>
                  </div>
                  <div>
                    <div className="text-[10px] text-zinc-500 font-mono uppercase tracking-wider">Arquitetura SO</div>
                    <p className="font-semibold text-zinc-450 mt-0.5 flex items-center gap-1 leading-none truncate w-full">
                      <Server className="w-3.5 h-3.5 text-zinc-650 shrink-0" />
                      <span>{selectedSession.osVersion} • app {selectedSession.appVersion}</span>
                    </p>
                  </div>
                </div>

                {/* Physical metrics metrics table */}
                <div className="mb-5 select-none">
                  <span className="text-[10px] text-zinc-500 font-mono uppercase tracking-wider block mb-2.5">Medições Físicas (SpeedTest)</span>
                  <div className="grid grid-cols-3 gap-2.5">
                    <div className="bg-[#161619] border border-[#2d2d31]/40 p-3 rounded-xl">
                      <span className="text-[9px] font-mono text-zinc-500 uppercase">Download</span>
                      <div className="flex items-baseline gap-1 mt-1 text-xs">
                        <span className="text-sm font-bold text-[#38BDF8]">{selectedSession.speed.downloadMbps}</span>
                        <span className="text-[10px] text-zinc-500 font-mono font-medium">Mbps</span>
                      </div>
                    </div>

                    <div className="bg-[#161619] border border-[#2d2d31]/40 p-3 rounded-xl">
                      <span className="text-[9px] font-mono text-zinc-500 uppercase">Upload</span>
                      <div className="flex items-baseline gap-1 mt-1 text-xs">
                        <span className="text-sm font-bold text-indigo-400">{selectedSession.speed.uploadMbps}</span>
                        <span className="text-[10px] text-zinc-500 font-mono font-medium">Mbps</span>
                      </div>
                    </div>

                    <div className="bg-[#161619] border border-[#2d2d31]/40 p-3 rounded-xl">
                      <span className="text-[9px] font-mono text-zinc-500 uppercase font-semibold">Bufferbloat</span>
                      <div className="flex items-baseline gap-1 mt-1 justify-between text-xs font-mono">
                        <span className={`text-md font-bold ${selectedSession.speed.bufferbloatGrade === "A+" ? "text-emerald-400" : "text-amber-500"}`}>{selectedSession.speed.bufferbloatGrade}</span>
                        <span className="text-[9px] bg-zinc-950 text-zinc-500 px-1 rounded">Grade</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Additional latency metrics */}
                <div className="grid grid-cols-3 gap-3 mb-5 bg-[#161619]/40 p-3 rounded-xl border border-[#2d2d31]/30 select-none">
                  <div className="text-center">
                    <span className="text-[9px] font-mono text-zinc-500 uppercase">Latência</span>
                    <p className="text-xs font-semibold text-white mt-1">{selectedSession.speed.latencyMs} ms</p>
                  </div>
                  <div className="text-center border-l border-r border-[#262626]">
                    <span className="text-[9px] font-mono text-zinc-500 uppercase">Jitter</span>
                    <p className="text-xs font-semibold text-white mt-1">{selectedSession.speed.jitterMs} ms</p>
                  </div>
                  <div className="text-center">
                    <span className="text-[9px] font-mono text-zinc-500 uppercase">Perda de Pacote</span>
                    <p className="text-xs font-semibold text-white mt-1">{selectedSession.speed.packetLossPercentage}%</p>
                  </div>
                </div>

                {/* Network Quality indicators */}
                {selectedSession.networkStrength && (
                  <div className="mb-5 p-3.5 bg-[#161619] border border-[#2d2d31]/40 rounded-xl space-y-2 select-none">
                    <span className="text-[9px] text-[#6C2BFF] font-mono uppercase tracking-wide block font-bold">Interfaces físicas & Rádio</span>
                    <div className="grid grid-cols-2 gap-3 text-xs font-sans">
                      <div>
                        <div className="text-zinc-550 text-[9px] font-mono">Força Sinal (RSSI)</div>
                        <div className="font-semibold text-white mt-0.5">{selectedSession.networkStrength.signalStrengthDbm} dBm</div>
                      </div>
                      <div>
                        <div className="text-zinc-550 text-[9px] font-mono">Qualidade Estimada</div>
                        <div className="font-semibold text-emerald-450 mt-0.5">{selectedSession.networkStrength.signalQualityPercentage}%</div>
                      </div>
                      {selectedSession.networkStrength.ssid && (
                        <div>
                          <div className="text-zinc-550 text-[9px] font-mono">SSID Wi-Fi</div>
                          <div className="font-semibold text-zinc-350 mt-0.5 font-mono truncate max-w-[130px]">{selectedSession.networkStrength.ssid}</div>
                        </div>
                      )}
                      {selectedSession.networkStrength.frequencyBandGhz && (
                        <div>
                          <div className="text-zinc-550 text-[9px] font-mono">Banda frequência</div>
                          <div className="font-semibold text-zinc-350 mt-0.5">{selectedSession.networkStrength.frequencyBandGhz} GHz</div>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Identified list of problems */}
                <div className="mb-5">
                  <span className="text-[10px] text-zinc-500 font-mono uppercase tracking-wider block mb-2 select-none font-semibold">Anomalias Físicas ({selectedSession.issues.length})</span>
                  {selectedSession.issues.length === 0 ? (
                    <div className="p-3 bg-emerald-950/20 border border-emerald-500/20 rounded-xl text-xs text-emerald-400 flex items-center gap-2 font-sans select-none">
                      <ShieldCheck className="w-4 h-4 shrink-0 text-[#22C55E]" />
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
                            <div className="font-semibold font-mono tracking-wider text-[10px] uppercase text-zinc-300">
                              {issue.issue.replace(/_/g, " ")}
                            </div>
                            <p className="text-zinc-400 mt-1 leading-normal text-[11px] font-sans">{issue.description}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* AI report segment */}
                <div className="border-t border-[#262626] pt-5">
                  <div className="flex items-center justify-between pb-3 select-none">
                    <div className="flex items-center gap-1.5">
                      <Sparkles className="w-4 h-4 text-purple-400" />
                      <span className="text-xs font-bold text-white font-sans">Laudo Preditivo Gemini</span>
                    </div>
                    <button
                      onClick={() => handleTriggerReDiagnosis(selectedSession.id)}
                      disabled={diagnosingId !== null}
                      className="flex items-center gap-1 text-[10px] px-2.5 py-1.5 border border-[#262626] hover:border-[#6C2BFF]/40 hover:bg-[#6C2BFF]/10 text-[#6C2BFF] rounded-lg transition-colors font-mono font-bold uppercase select-none cursor-pointer"
                    >
                      <Zap className={`w-3 h-3 ${diagnosingId ? "animate-bounce" : ""}`} />
                      <span>{diagnosingId ? "Roteando..." : "Regenerar IA"}</span>
                    </button>
                  </div>

                  {statusMessage && (
                    <div className="p-2.5 bg-[#161619] text-purple-400 text-[10px] font-mono mb-3 rounded-lg border border-[#262626] select-none text-center">
                      {statusMessage}
                    </div>
                  )}

                  {selectedSession.aiSummaryReport ? (
                    <div className="bg-[#0b0b0d] text-[11px] font-sans text-zinc-350 leading-relaxed p-4 rounded-xl border border-[#262626]/80 max-h-40 overflow-y-auto">
                      {selectedSession.aiSummaryReport}
                    </div>
                  ) : (
                    <div className="text-center py-6 text-xs text-neutral-500 font-sans border border-dashed border-[#262626] rounded-xl select-none">
                      Nenhum laudo compilado para esta telemetria. Clique em "REGENERAR IA" para processar no Gemini.
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="py-20 text-center bg-zinc-950/20 border border-dashed border-[#262626] rounded-2xl p-6 select-none font-sans">
                <p className="text-xs text-neutral-500">Selecione algum registro físico de rádio para investigar.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
