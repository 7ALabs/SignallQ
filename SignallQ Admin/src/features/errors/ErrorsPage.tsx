import React from "react";
import { errorMetricsService } from "../../services/errorMetricsService";
import { ErrorMetricGrid } from "./components/ErrorMetricGrid";
import { ErrorByEndpointChart } from "./components/ErrorByEndpointChart";
import { ErrorAlertsPanel } from "./components/ErrorAlertsPanel";
import { RecentErrorsTable } from "./components/RecentErrorsTable";
import { LoadingState } from "../../components/ui/LoadingState";
import { AppEnvironment } from "../../types/admin";
import { SystemError } from "../../types/errors";
import {
  AlertTriangle,
  RefreshCw,
  Search,
  CheckCircle,
  Eye,
  Info,
  Layers,
  Terminal,
  Clock,
  UserCheck2,
  Workflow
} from "lucide-react";

interface ErrorsPageProps {
  environment: AppEnvironment;
  period: string;
  onEnvironmentChange: (env: AppEnvironment) => void;
  onPeriodChange: (p: string) => void;
  triggerRefreshCounter: number;
}

export const ErrorsPage: React.FC<ErrorsPageProps> = ({
  environment: propEnv,
  period: propPeriod,
  onEnvironmentChange,
  onPeriodChange,
  triggerRefreshCounter,
}) => {
  const [localEnv, setLocalEnv] = React.useState<AppEnvironment>(propEnv);
  const [localPeriod, setLocalPeriod] = React.useState<string>(propPeriod);
  const [searchQuery, setSearchQuery] = React.useState("");

  const [loading, setLoading] = React.useState(true);
  const [isRefreshing, setIsRefreshing] = React.useState(false);
  const [errors, setErrors] = React.useState<SystemError[]>([]);
  const [selectedError, setSelectedError] = React.useState<SystemError | null>(null);

  // Resolution states
  const [resolvingId, setResolvingId] = React.useState<string | null>(null);
  const [statusMessage, setStatusMessage] = React.useState<string | null>(null);

  // Grouping state for error mapping
  const [activeGroup, setActiveGroup] = React.useState<string>("feature");

  const groupingData = React.useMemo(() => {
    switch (activeGroup) {
      case "feature":
        return [
          { name: "Scan de Dispositivos", count: 31, percentage: 53, subtitle: "Multicast scan overload & network buffer crash" },
          { name: "Análise Móvel", count: 14, percentage: 24, subtitle: "TelephonyManager broadcast timeout exception" },
          { name: "Diagnóstico Completo", count: 9, percentage: 15, subtitle: "API sequential pipeline connection failure" },
          { name: "SpeedTest", count: 4, percentage: 7, subtitle: "Saturação de link / socket connection abort" },
        ];
      case "screen":
        return [
          { name: "Painel de Dispositivos (devices)", count: 28, percentage: 48, subtitle: "Multicast address response parser failure" },
          { name: "Análise Móvel (signal)", count: 18, percentage: 31, subtitle: "Cellular signal strength callback failure" },
          { name: "Laudo Técnico (laudo)", count: 8, percentage: 14, subtitle: "PDF native serialization crash" },
          { name: "Diagnóstico guiado (home)", count: 4, percentage: 7, subtitle: "Guided wizard navigation flow exception" },
        ];
      case "version":
        return [
          { name: "Build 0.18.1 (Critical)", count: 38, percentage: 65, subtitle: "Novo Core Network Multicast Discovery SDK" },
          { name: "Build 0.18.0", count: 16, percentage: 27, subtitle: "Permissões de rede local no Android 13+" },
          { name: "Build 0.17.5", count: 4, percentage: 8, subtitle: "Incompatibilidade SSL Handshake de Legacy OS" },
        ];
      case "network":
        return [
          { name: "Conexões Móveis (4G/5G)", count: 34, percentage: 58, subtitle: "Timeouts de socket do modem de telefonia (Tim/Vivo)" },
          { name: "Wi-Fi (Multicast local)", count: 20, percentage: 34, subtitle: "Local gateway router dropping multicast probes" },
          { name: "Ethernet (Cabada)", count: 4, percentage: 8, subtitle: "Inconsistências de MTU estático em switchboards" },
        ];
      case "provider":
        return [
          { name: "Google Gemini API Gateway", count: 12, percentage: 66, subtitle: "Quota Exceeded / rate limit 429 response" },
          { name: "Fallback Local (On-device)", count: 4, percentage: 22, subtitle: "Regex pattern compilation out of memory" },
          { name: "Workers AI Qwen", count: 2, percentage: 12, subtitle: "Edge gateway connection reset by Cloudflare" },
        ];
      case "endpoint":
      default:
        return [
          { name: "/api/diagnostics/submit", count: 18, percentage: 39, subtitle: "Erro 500 PostgreSQL database connection pool spike" },
          { name: "/api/ai/laudo", count: 12, percentage: 26, subtitle: "Gemini server-side API Gateway timeout" },
          { name: "/api/error/logger", count: 10, percentage: 21, subtitle: "Erro 413 Client Payload Too Large Exception" },
          { name: "/api/auth/session", count: 6, percentage: 14, subtitle: "Invalidação precoce de tokens JWT expirados" },
        ];
    }
  }, [activeGroup]);

  // Sync prop changes
  React.useEffect(() => {
    setLocalEnv(propEnv);
  }, [propEnv]);

  React.useEffect(() => {
    setLocalPeriod(propPeriod);
  }, [propPeriod]);

  const loadErrors = React.useCallback(async () => {
    setLoading(true);
    setStatusMessage(null);
    try {
      const data = await errorMetricsService.getSystemErrors({
        environment: localEnv,
        period: localPeriod,
        search: searchQuery
      });
      setErrors(data);
      if (data.length > 0) {
        setSelectedError(data[0]);
      } else {
        setSelectedError(null);
      }
    } catch (err) {
      console.error("Failed to load logs of system errors", err);
    } finally {
      setLoading(false);
    }
  }, [localEnv, localPeriod, searchQuery, triggerRefreshCounter]);

  React.useEffect(() => {
    loadErrors();
  }, [loadErrors]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await new Promise((resolve) => setTimeout(resolve, 300));
    await loadErrors();
    setIsRefreshing(false);
  };

  const handleResolve = async (id: string) => {
    setResolvingId(id);
    setStatusMessage(null);
    try {
      const res = await errorMetricsService.resolveError(id);
      if (res.success) {
        setStatusMessage(res.message);
        
        // Update local state is resolved
        setErrors(prev => prev.map(e => e.id === id ? { ...e, resolved: true } : e));
        if (selectedError && selectedError.id === id) {
          setSelectedError({ ...selectedError, resolved: true });
        }
      }
    } catch (err) {
      console.error("Failed to resolve error", err);
      setStatusMessage("Falha ao comunicar resolução com o cluster.");
    } finally {
      setResolvingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* 1. Bar of core controls: Search and Filtering */}
      <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 space-y-4 shadow-sm select-none">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Pesquise por mensagem de erro, stack trace, componente ou ID do caso..."
              className="w-full bg-[#18181B] border border-[#262626] rounded-xl pl-10 pr-4 py-2.5 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-[#6C2BFF]/60 focus:ring-1 focus:ring-[#6C2BFF]/30 transition-all font-sans"
            />
          </div>

          <div className="flex items-center gap-3 self-end md:self-auto">
            {/* Env Selector */}
            <div className="flex bg-[#18181B] p-1 border border-[#262626] rounded-xl text-[10px] font-mono">
              <button
                type="button"
                onClick={() => {
                  setLocalEnv("production");
                  onEnvironmentChange("production");
                }}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                  localEnv === "production"
                    ? "bg-[#6C2BFF] text-white shadow-sm"
                    : "text-zinc-400 hover:text-white"
                }`}
              >
                PROD
              </button>
              <button
                type="button"
                onClick={() => {
                  setLocalEnv("staging");
                  onEnvironmentChange("staging");
                }}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                  localEnv === "staging"
                    ? "bg-yellow-550 bg-[#Eab308] text-black shadow-sm"
                    : "text-zinc-400 hover:text-white"
                }`}
              >
                STAGING
              </button>
            </div>

            {/* Period Selector */}
            <select
              value={localPeriod}
              onChange={(e) => {
                const targetVal = e.target.value;
                setLocalPeriod(targetVal);
                onPeriodChange(targetVal);
              }}
              className="bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#6C2BFF] transition-colors cursor-pointer font-mono font-bold"
            >
              <option value="today">HOJE</option>
              <option value="7d">7 DIAS</option>
              <option value="30d">30 DIAS</option>
            </select>

            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="flex items-center justify-center p-2.5 bg-[#18181B] border border-[#262626] hover:border-zinc-700 active:bg-zinc-900 text-zinc-400 hover:text-white transition-all rounded-xl disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? "animate-spin text-purple-400" : ""}`} />
            </button>
          </div>
        </div>
      </div>

      {/* 2. Grid cards (4 stats items) */}
      <ErrorMetricGrid environment={localEnv} />

      {/* 3. Operational grid layouts (Visual chart left + alerts logs right) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in">
        <div className="lg:col-span-2">
          <ErrorByEndpointChart environment={localEnv} />
        </div>
        <div>
          <ErrorAlertsPanel />
        </div>
      </div>

      {/* 3.5. Agrupamentos e Respostas Operacionais */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 animate-fade-in">
        {/* Left column: Dynamic group list */}
        <div className="lg:col-span-7 bg-[#0E0E12] border border-[#262626] rounded-2xl p-6 space-y-5">
          <div className="space-y-1">
            <h4 className="text-xs font-semibold font-mono text-zinc-500 uppercase tracking-widest">Agrupador Inteligente de Falhas</h4>
            <h3 className="text-sm font-bold text-white font-sans">Estatísticas Operacionais Consolidadas</h3>
            <p className="text-[10px] text-zinc-550 font-sans">Agrupe logs de exceção do Worker para localizar pontos fracos de estabilidade no Android.</p>
          </div>

          <div className="flex flex-wrap gap-1.5 p-1 bg-[#15151A] border border-zinc-900 rounded-xl">
            {[
              { key: "feature", label: "Função" },
              { key: "screen", label: "Tela" },
              { key: "version", label: "Versão" },
              { key: "network", label: "Rede" },
              { key: "provider", label: "Provedor IA" },
              { key: "endpoint", label: "Endpoint API" },
            ].map((opt) => (
              <button
                key={opt.key}
                type="button"
                onClick={() => setActiveGroup(opt.key)}
                className={`px-3 py-1.5 rounded-lg text-[10px] font-mono font-bold transition-all ${
                  activeGroup === opt.key
                    ? "bg-[#6C2BFF] text-white"
                    : "text-zinc-400 hover:text-white hover:bg-zinc-900"
                }`}
              >
                {opt.label.toUpperCase()}
              </button>
            ))}
          </div>

          <div className="space-y-4 pt-1">
            {groupingData.map((item, idx) => (
              <div key={idx} className="space-y-1.5">
                <div className="flex justify-between items-end text-xs font-sans">
                  <div>
                    <span className="font-bold text-zinc-200">{item.name}</span>
                    <span className="text-[10px] text-zinc-500 block">{item.subtitle}</span>
                  </div>
                  <span className="font-mono text-zinc-400 font-bold">
                    {item.count} erros <span className="text-zinc-600">({item.percentage}%)</span>
                  </span>
                </div>
                <div className="w-full h-1.5 bg-[#171720] border border-zinc-900 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-purple-500 to-indigo-600 rounded-full"
                    style={{ width: `${item.percentage}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right column: QA answers (Google Vitals FAQ) */}
        <div className="lg:col-span-5 bg-[#0E0E12] border border-[#262626] rounded-2xl p-6 space-y-4">
          <div className="space-y-1">
            <span className="text-[9px] text-[#FF4D4F] font-mono uppercase tracking-widest font-bold">Análise Google Vitals</span>
            <h3 className="text-sm font-bold text-white font-sans">FAQ de Diagnóstico Técnico</h3>
            <p className="text-[10px] text-zinc-550 font-sans">Respostas imediatas inferidas a partir da volumetria ativa de erros.</p>
          </div>

          <div className="space-y-3.5 pt-2">
            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[9px] font-mono text-[#FF4D4F] uppercase tracking-wider block">Qual função mais deu crash?</span>
              <p className="text-xs font-bold text-white font-sans">Scan de Dispositivos (Multicast)</p>
              <p className="text-[10px] text-zinc-400 font-sans">Registrou <strong className="text-red-400">31 crashes</strong> devido a sobrecarga de buffer na leitura de pacotes UDP.</p>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[9px] font-mono text-indigo-400 uppercase tracking-wider block">Qual tela mais concentrou erro?</span>
              <p className="text-xs font-bold text-white font-sans">Painel de Dispositivos (devices)</p>
              <p className="text-[10px] text-zinc-400 font-sans">Concentra <b className="text-[#6C2BFF]">48% dos erros</b> com taxa média de evasão e cancelamento de 55%.</p>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[9px] font-mono text-amber-400 uppercase tracking-wider block">Qual versão mais afetada?</span>
              <p className="text-xs font-bold text-white font-sans">Lançamento v0.18.1</p>
              <p className="text-[10px] text-zinc-400 font-sans">Fator crítico respondendo por <strong className="text-amber-500">65% das falhas</strong> do ecossistema Android.</p>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[9px] font-mono text-emerald-400 uppercase tracking-wider block">Qual erro está ligado ao diagnóstico móvel?</span>
              <p className="text-xs font-bold text-white font-sans">and_tel_mob_analysis_failed</p>
              <p className="text-[10px] text-zinc-400 font-sans">Falha de callback no listener <code className="font-mono text-[9px] text-emerald-400">TelephonyManager</code> por timeout.</p>
            </div>
          </div>
        </div>
      </div>

      {/* 4. Fine-grained logs of system errors */}
      {loading ? (
        <LoadingState message="Acompanhando dumps de erros e crash outputs..." />
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-6">
          <div className="xl:col-span-7">
            <RecentErrorsTable
              errors={errors}
              selectedError={selectedError}
              onSelectError={(row) => setSelectedError(row)}
            />
          </div>

          {/* Right side investigator panel */}
          <div className="xl:col-span-5">
            {selectedError ? (
              <div className="bg-[#0f0f12] border border-[#262626] rounded-2xl p-6 relative overflow-hidden font-sans">
                <div className="absolute top-0 right-0 w-32 h-32 bg-red-500/5 rounded-full filter blur-2xl flex items-center justify-center pointer-events-none" />

                {/* Main title casing */}
                <div className="flex items-start justify-between pb-4 border-b border-[#262626] mb-5 select-none">
                  <div>
                    <span className="text-[9px] text-[#FF4D4F] font-mono uppercase tracking-widest font-bold">Insvestigador de Exceção</span>
                    <h5 className="font-bold text-white text-sm font-mono mt-0.5">{selectedError.id}</h5>
                  </div>
                  <div className="text-right">
                    <span className="font-mono text-[9px] text-zinc-500 block">{new Date(selectedError.timestamp).toLocaleDateString("pt-BR")}</span>
                    <span className="font-mono text-[9px] text-zinc-550 block mt-0.5">{new Date(selectedError.timestamp).toLocaleTimeString("pt-BR")}</span>
                  </div>
                </div>

                {/* Stats layout */}
                <div className="grid grid-cols-3 gap-2 border-b border-[#262626] pb-4 mb-4 select-none">
                  <div className="bg-[#161619]/40 border border-[#2d2d31]/30 rounded-xl p-2.5 text-center">
                    <span className="font-sans text-[8.5px] text-zinc-500 uppercase block">Componente</span>
                    <span className="text-[10px] font-mono text-zinc-350 font-bold uppercase truncate block mt-0.5">{selectedError.source}</span>
                  </div>

                  <div className="bg-[#161619]/40 border border-[#2d2d31]/30 rounded-xl p-2.5 text-center">
                    <span className="font-sans text-[8.5px] text-zinc-500 uppercase block">Dumps brutos</span>
                    <span className="text-xs font-mono text-[#FF4D4F] font-bold block mt-0.5">{selectedError.count}</span>
                  </div>

                  <div className="bg-[#161619]/40 border border-[#2d2d31]/30 rounded-xl p-2.5 text-center">
                    <span className="font-sans text-[8.5px] text-zinc-500 uppercase block">Afetados</span>
                    <span className="text-xs font-mono text-[#Eab308] font-bold block mt-0.5">{selectedError.affectedUserCount}</span>
                  </div>
                </div>

                {/* Error message detailed */}
                <div className="space-y-4 font-sans text-xs">
                  <div className="space-y-1">
                    <div className="text-[9px] text-zinc-550 font-mono uppercase tracking-widest font-bold text-zinc-500">Mensagem do Dump</div>
                    <p className="text-zinc-200 bg-[#161619] border border-[#262626] p-3 rounded-xl font-mono text-[10.5px] leading-snug">
                      {selectedError.message}
                    </p>
                  </div>

                  {/* Active Stack trace */}
                  <div className="space-y-1">
                    <div className="text-[9px] text-zinc-550 font-mono uppercase tracking-widest font-bold text-zinc-500 flex items-center gap-1">
                      <Terminal className="w-3.5 h-3.5 text-[#FF4D4F]" />
                      <span>Trace Back do Sistema</span>
                    </div>
                    <div className="bg-black text-[#FF4D4F] p-4 rounded-xl font-mono text-[9px] leading-relaxed max-h-48 overflow-y-auto whitespace-pre-wrap selection:bg-[#FF4D4F]/35 selection:text-white">
                      {selectedError.stackTrace}
                    </div>
                  </div>

                  {/* Operational resolution actions */}
                  <div className="pt-4 border-t border-[#262626]">
                    <div className="flex items-center justify-between pb-3 select-none">
                      <div className="flex items-center gap-1 text-xs text-zinc-400">
                        <Workflow className="w-4 h-4 text-zinc-500 mr-1" />
                        <span>Resoldor do Caso</span>
                      </div>
                      <button
                        onClick={() => handleResolve(selectedError.id)}
                        disabled={selectedError.resolved || resolvingId !== null}
                        className={`flex items-center gap-1.5 px-3.5 py-2 border rounded-xl font-mono text-[10px] font-bold uppercase transition-all select-none cursor-pointer ${
                          selectedError.resolved
                            ? "bg-emerald-950/20 border-emerald-500/20 text-emerald-400 cursor-not-allowed"
                            : "bg-[#FF4D4F]/10 border-[#FF4D4F]/20 text-[#FF4D4F] hover:bg-[#FF4D4F]/20"
                        }`}
                      >
                        <CheckCircle className="w-3.5 h-3.5" />
                        <span>{selectedError.resolved ? "Resolvido no Cluster" : resolvingId ? "Disparando..." : "Marcar Resolvido"}</span>
                      </button>
                    </div>

                    {statusMessage && (
                      <div className="p-3 bg-zinc-950/80 border border-zinc-850 text-emerald-400 text-[10px] font-mono text-center rounded-xl select-none">
                        {statusMessage}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-20 text-center bg-zinc-950/25 border border-dashed border-[#262626] rounded-2xl p-6 select-none font-sans">
                <p className="text-xs text-neutral-500">Selecione algum dump técnico ativo no console esquerdo para inspecionar.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
