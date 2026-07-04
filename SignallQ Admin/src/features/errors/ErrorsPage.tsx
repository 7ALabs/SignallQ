import React from "react";
import { errorMetricsService } from "../../services/errorMetricsService";
import { RecentErrorsTable } from "./components/RecentErrorsTable";
import { ErrorMetricGrid } from "./components/ErrorMetricGrid";
import { ErrorByEndpointChart } from "./components/ErrorByEndpointChart";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { AppEnvironment } from "../../types/admin";
import { SystemError } from "../../types/errors";
import {
  RefreshCw,
  Search,
  CheckCircle,
  Terminal,
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
  const [resolutionNoteDraft, setResolutionNoteDraft] = React.useState("");

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
      const res = await errorMetricsService.resolveError(id, resolutionNoteDraft);
      if (res.success) {
        setStatusMessage(res.message);

        // Atualiza estado local: erro resolvido some da lista de ativos
        // (mesmo comportamento do worker, que já filtra resolved=0 por padrão).
        setErrors(prev => prev.filter(e => e.id !== id));
        if (selectedError && selectedError.id === id) {
          setSelectedError({
            ...selectedError,
            resolved: true,
            resolvedBy: res.resolvedBy ?? selectedError.resolvedBy,
            resolvedAt: res.resolvedAt ?? selectedError.resolvedAt,
            resolutionNote: resolutionNoteDraft,
          });
        }
        setResolutionNoteDraft("");
      } else {
        setStatusMessage(res.message);
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
      <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[8px] p-5 space-y-4 select-none">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--text-tertiary)]" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Pesquise por mensagem de erro, stack trace, componente ou ID do caso..."
              className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl pl-10 pr-4 py-2.5 text-xs text-[var(--text-primary)] placeholder-[var(--text-tertiary)] focus:outline-none focus:border-[var(--primary)]/60 focus:ring-1 focus:ring-[var(--primary)]/30 transition-all font-sans"
            />
          </div>

          <div className="flex items-center gap-3 self-end md:self-auto">
            {/* Env Selector */}
            <div className="flex bg-[var(--bg-surface)] p-1 border border-[var(--border)] rounded-xl text-[10px] font-sans">
              <button
                type="button"
                onClick={() => {
                  setLocalEnv("production");
                  onEnvironmentChange("production");
                }}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                  localEnv === "production"
                    ? "bg-[var(--primary)] text-white shadow-sm"
                    : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
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
                    ? "bg-[var(--attention)] text-black shadow-sm"
                    : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
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
              className="bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-primary)] focus:outline-none focus:border-[var(--primary)] transition-colors cursor-pointer font-sans font-bold"
            >
              <option value="today">HOJE</option>
              <option value="7d">7 DIAS</option>
              <option value="30d">30 DIAS</option>
            </select>

            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="flex items-center justify-center p-2.5 bg-[var(--bg-surface)] border border-[var(--border)] hover:border-zinc-700 active:bg-zinc-900 text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-all rounded-xl disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? "animate-spin text-[var(--text-secondary)]" : ""}`} />
            </button>
          </div>
        </div>
      </div>

      {/* 2. Grid de métricas de erros */}
      <ErrorMetricGrid environment={localEnv} />

      {/* 3. Operational grid layouts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in">
        <div className="lg:col-span-2">
          <ErrorByEndpointChart environment={localEnv} />
        </div>
        <div>
          <FeatureComingSoon
            feature="Alertas Críticos de Infraestrutura"
            reason="Sistema de alertas não implementado"
          />
        </div>
      </div>

      {/* 3.5. Agrupamentos e Respostas Operacionais */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 animate-fade-in">
        <div className="lg:col-span-7">
          <FeatureComingSoon
            feature="Agrupador de Falhas por Função / Tela / Versão"
            reason="Requer rota de agregação de erros no worker"
          />
        </div>
        <div className="lg:col-span-5">
          <FeatureComingSoon
            feature="Análise Automática de Vitals"
            reason="Requer integração com Firebase Crashlytics e Google Play Vitals"
          />
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
              onSelectError={(row) => {
                setSelectedError(row);
                setResolutionNoteDraft("");
                setStatusMessage(null);
              }}
            />
          </div>

          {/* Right side investigator panel */}
          <div className="xl:col-span-5">
            {selectedError ? (
              <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[8px] p-6 relative overflow-hidden font-sans">
                <div className="absolute top-0 right-0 w-32 h-32 bg-red-500/5 rounded-full filter blur-2xl flex items-center justify-center pointer-events-none" />

                {/* Main title casing */}
                <div className="flex items-start justify-between pb-4 border-b border-[var(--border)] mb-5 select-none">
                  <div>
                    <span className="text-[9px] text-[var(--error)] font-sans uppercase tracking-widest font-bold">Investigador de Exceção</span>
                    <h5 className="font-bold text-[var(--text-primary)] text-sm font-mono mt-0.5">{selectedError.id}</h5>
                  </div>
                  <div className="text-right">
                    <span className="font-mono text-[9px] text-[var(--text-tertiary)] block">{new Date(selectedError.timestamp).toLocaleDateString("pt-BR")}</span>
                    <span className="font-mono text-[9px] text-[var(--text-tertiary)] block mt-0.5">{new Date(selectedError.timestamp).toLocaleTimeString("pt-BR")}</span>
                  </div>
                </div>

                {/* Stats layout */}
                <div className="grid grid-cols-4 gap-2 border-b border-[var(--border)] pb-4 mb-4 select-none">
                  <div className="bg-[var(--bg-base)]/40 border border-[var(--border)]/30 rounded-xl p-2.5 text-center">
                    <span className="font-sans text-[8.5px] text-[var(--text-tertiary)] uppercase block">Componente</span>
                    <span className="text-[10px] font-mono text-[var(--text-secondary)] font-bold uppercase truncate block mt-0.5">{selectedError.source}</span>
                  </div>

                  <div className="bg-[var(--bg-base)]/40 border border-[var(--border)]/30 rounded-xl p-2.5 text-center">
                    <span className="font-sans text-[8.5px] text-[var(--text-tertiary)] uppercase block">Categoria</span>
                    <span className="text-[10px] font-mono text-[var(--text-secondary)] font-bold uppercase truncate block mt-0.5">{selectedError.category ?? "backend"}</span>
                  </div>

                  <div className="bg-[var(--bg-base)]/40 border border-[var(--border)]/30 rounded-xl p-2.5 text-center">
                    <span className="font-sans text-[8.5px] text-[var(--text-tertiary)] uppercase block">Dumps brutos</span>
                    <span className="text-xs font-mono text-[var(--error)] font-bold block mt-0.5">{selectedError.count}</span>
                  </div>

                  <div className="bg-[var(--bg-base)]/40 border border-[var(--border)]/30 rounded-xl p-2.5 text-center">
                    <span className="font-sans text-[8.5px] text-[var(--text-tertiary)] uppercase block">Afetados</span>
                    <span className="text-xs font-mono text-[var(--attention)] font-bold block mt-0.5">{selectedError.affectedUserCount}</span>
                  </div>
                </div>

                {/* Error message detailed */}
                <div className="space-y-4 font-sans text-xs">
                  <div className="space-y-1">
                    <div className="text-[9px] text-[var(--text-tertiary)] font-sans uppercase tracking-widest font-bold">Mensagem do Dump</div>
                    <p className="text-[var(--text-primary)] bg-[var(--bg-base)] border border-[var(--border)] p-3 rounded-xl font-mono text-[10.5px] leading-snug">
                      {selectedError.message}
                    </p>
                  </div>

                  {/* Active Stack trace */}
                  <div className="space-y-1">
                    <div className="text-[9px] text-[var(--text-tertiary)] font-sans uppercase tracking-widest font-bold flex items-center gap-1">
                      <Terminal className="w-3.5 h-3.5 text-[var(--error)]" />
                      <span>Trace Back do Sistema</span>
                    </div>
                    <div className="bg-black text-[var(--error)] p-4 rounded-xl font-mono text-[9px] leading-relaxed max-h-48 overflow-y-auto whitespace-pre-wrap selection:bg-[var(--error)]/35 selection:text-white">
                      {selectedError.stackTrace}
                    </div>
                  </div>

                  {/* Operational resolution actions */}
                  <div className="pt-4 border-t border-[var(--border)]">
                    <div className="flex items-center gap-1 text-xs text-[var(--text-secondary)] pb-3 select-none">
                      <Workflow className="w-4 h-4 text-[var(--text-tertiary)] mr-1" />
                      <span>Resolvedor do Caso</span>
                    </div>

                    {selectedError.resolved ? (
                      <div className="p-3 bg-emerald-950/15 border border-emerald-500/20 rounded-xl space-y-1.5">
                        <div className="flex items-center gap-1.5 text-emerald-400 text-[10px] font-sans font-bold uppercase">
                          <CheckCircle className="w-3.5 h-3.5" />
                          <span>Resolvido</span>
                        </div>
                        <p className="text-[10px] font-sans text-[var(--text-secondary)]">
                          Responsável: <span className="font-bold text-[var(--text-primary)]">{selectedError.resolvedBy || "—"}</span>
                          {selectedError.resolvedAt && (
                            <> · {new Date(selectedError.resolvedAt).toLocaleString("pt-BR")}</>
                          )}
                        </p>
                        {selectedError.resolutionNote && (
                          <p className="text-[10px] font-sans text-[var(--text-tertiary)] italic">
                            "{selectedError.resolutionNote}"
                          </p>
                        )}
                      </div>
                    ) : (
                      <div className="space-y-2">
                        <textarea
                          value={resolutionNoteDraft}
                          onChange={(e) => setResolutionNoteDraft(e.target.value)}
                          placeholder="Observação da resolução (opcional) — o que foi feito para tratar este erro."
                          rows={2}
                          className="w-full bg-[var(--bg-base)] border border-[var(--border)] rounded-xl p-2.5 text-[10.5px] text-[var(--text-primary)] placeholder-[var(--text-tertiary)] focus:outline-none focus:border-[var(--primary)]/60 resize-none font-sans"
                        />
                        <button
                          onClick={() => handleResolve(selectedError.id)}
                          disabled={resolvingId !== null}
                          className="w-full flex items-center justify-center gap-1.5 px-3.5 py-2 border rounded-xl font-sans text-[10px] font-bold uppercase transition-all select-none cursor-pointer bg-[var(--error)]/10 border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <CheckCircle className="w-3.5 h-3.5" />
                          <span>{resolvingId ? "Disparando..." : "Marcar Resolvido"}</span>
                        </button>
                      </div>
                    )}

                    {statusMessage && (
                      <div className="mt-3 p-3 bg-zinc-950/80 border border-zinc-850 text-emerald-400 text-[10px] font-sans text-center rounded-xl select-none">
                        {statusMessage}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-20 text-center rounded-[8px] p-6 select-none font-sans" style={{ background: "var(--bg-surface)", border: "1px dashed var(--border)" }}>
                <p className="text-xs text-[var(--text-tertiary)]">Selecione algum dump técnico ativo no console esquerdo para inspecionar.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
