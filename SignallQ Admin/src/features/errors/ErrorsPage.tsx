import React from "react";
import { errorMetricsService } from "../../services/errorMetricsService";
import { RecentErrorsTable } from "./components/RecentErrorsTable";
import { ErrorMetricGrid } from "./components/ErrorMetricGrid";
import { ErrorByEndpointChart } from "./components/ErrorByEndpointChart";
import { ErrorAlertsPanel } from "./components/ErrorAlertsPanel";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { GlobalFilters } from "../../components/ui/GlobalFilters";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
import { ChartCard } from "../../components/ui/ChartCard";
import { AppEnvironment } from "../../types/admin";
import { SystemError, SystemErrorCategory } from "../../types/errors";
import {
  Search,
  CheckCircle,
  Terminal,
  Workflow
} from "lucide-react";
import { alpha } from "../../utils/color";

interface ErrorsPageProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

const CATEGORY_LABEL: Record<SystemErrorCategory, string> = {
  app: "App",
  backend: "Backend",
  ia: "IA",
  integration: "Integração",
};

export const ErrorsPage: React.FC<ErrorsPageProps> = ({
  environment,
  period,
  onNavigate,
  triggerRefreshCounter,
}) => {
  const [searchQuery, setSearchQuery] = React.useState("");
  const [categoryFilter, setCategoryFilter] = React.useState<string>("all");

  const [loading, setLoading] = React.useState(true);
  const [errors, setErrors] = React.useState<SystemError[]>([]);
  const [selectedError, setSelectedError] = React.useState<SystemError | null>(null);

  // Resolution states
  const [resolvingId, setResolvingId] = React.useState<string | null>(null);
  const [statusMessage, setStatusMessage] = React.useState<{ text: string; success: boolean } | null>(null);
  const [resolutionNoteDraft, setResolutionNoteDraft] = React.useState("");

  const loadErrors = React.useCallback(async () => {
    setLoading(true);
    setStatusMessage(null);
    try {
      const data = await errorMetricsService.getSystemErrors({
        environment,
        period,
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
  }, [environment, period, searchQuery, triggerRefreshCounter]);

  React.useEffect(() => {
    loadErrors();
  }, [loadErrors]);

  const handleResolve = async (id: string) => {
    setResolvingId(id);
    setStatusMessage(null);
    try {
      const res = await errorMetricsService.resolveError(id, resolutionNoteDraft);
      setStatusMessage({ text: res.message, success: res.success });

      if (res.success) {
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
      }
    } catch (err) {
      console.error("Failed to resolve error", err);
      setStatusMessage({ text: "Falha ao comunicar resolução com o cluster.", success: false });
    } finally {
      setResolvingId(null);
    }
  };

  // Ação irreversível na sessão (o erro some da lista assim que resolvido) —
  // exige confirmação explícita para não perder um caso por misclique durante
  // triagem rápida, mesmo padrão já usado no reset de Configurações.
  const handleResolveClick = (id: string) => {
    if (!window.confirm(`Marcar o erro ${id} como resolvido? Ele sai da lista de erros ativos.`)) return;
    handleResolve(id);
  };

  const filteredErrors = React.useMemo(() => {
    if (categoryFilter === "all") return errors;
    return errors.filter((e) => (e.category ?? "backend") === categoryFilter);
  }, [errors, categoryFilter]);

  const handleExportErrors = () => {
    const header = "ID,Fonte,Categoria,Mensagem,Ocorrencias,Afetados,Resolvido,Timestamp\r\n";
    const rows = filteredErrors
      .map((e) => [
        e.id, e.source, e.category ?? "backend",
        `"${e.message.replace(/"/g, '""')}"`,
        e.count, e.affectedUserCount, e.resolved ? "sim" : "nao", e.timestamp,
      ].join(","))
      .join("\r\n");
    const csvContent = `data:text/csv;charset=utf-8,${header}${rows}`;
    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", `signallq_erros_${environment}_${period}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // GH#552 (Fase 2) — síntese derivada só do que já está carregado (fonte
  // principal por contagem de eventos). Sem inventar taxa de crash: esta tela
  // reporta erros de sistema (worker/app/IA/integração), não crash rate de app.
  const insightText = React.useMemo(() => {
    if (filteredErrors.length === 0) return null;
    const bySource = new Map<string, number>();
    filteredErrors.forEach((e) => bySource.set(e.source, (bySource.get(e.source) ?? 0) + e.count));
    const top = [...bySource.entries()].sort((a, b) => b[1] - a[1])[0];
    const activeCount = filteredErrors.filter((e) => !e.resolved).length;
    if (!top) return null;
    return `${activeCount} erro(s) ativo(s) no período. Maior concentração de eventos vem de "${top[0]}" (${top[1]} ocorrências) — comece a investigação por ali.`;
  }, [filteredErrors]);

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="errors-section-intro"
        overline="PROBLEMAS & INCIDENTES"
        question="O app está falhando em algum lugar?"
        description="Crashes, ANRs e erros — priorizados por impacto em usuários, não por volume bruto."
        source="FONTE · FIREBASE CRASHLYTICS"
      />

      {/* 1. Busca livre — free-text não cabe no idioma de GlobalFilters (lista de
          opções), então fica isolada. Env/período já são globais via Topbar
          (SIG#552 Fase 1) — não reimplementar aqui. */}
      <div className="relative">
        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--text-tertiary)]" />
        <input
          type="text"
          aria-label="Pesquisar erros por mensagem, stack trace, componente ou ID do caso"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Pesquise por mensagem de erro, stack trace, componente ou ID do caso..."
          className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl pl-10 pr-4 py-2.5 text-xs text-[var(--text-primary)] placeholder-[var(--text-tertiary)] focus:outline-none focus:border-[var(--primary)]/60 focus:ring-1 focus:ring-[var(--primary)]/30 transition-all font-sans"
        />
      </div>

      {/* 1.5. Filtros globais — único idioma de filtro da tela (categoria real do
          dado, SystemError.category) */}
      <GlobalFilters
        id="errors-global-filters"
        filters={[
          {
            key: "category",
            label: "Categoria",
            value: categoryFilter,
            onChange: setCategoryFilter,
            options: [
              { label: "Todas", value: "all" },
              ...Object.entries(CATEGORY_LABEL).map(([value, label]) => ({ value, label })),
            ],
          },
        ]}
      />

      {/* 2. KPIs — grid de métricas de erros */}
      <ErrorMetricGrid environment={environment} />

      {/* 3. Composição paridade mockup — taxa de erro · 14 dias (sem série
          temporal real hoje) + volume por fonte técnica (real, mais próximo
          disponível de "erros por tela": SystemError não carrega o campo
          tela/screen, só a fonte técnica que originou o erro). */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in">
        <div className="lg:col-span-2">
          <ChartCard title="Taxa de erro · 14 dias" id="error-rate-timeline-card">
            <FeatureComingSoon
              feature="Taxa de erro · série temporal"
              reason="Métrica ainda não disponível — aguardando exposição no worker (sem série temporal de erros hoje)"
            />
          </ChartCard>
        </div>
        <div className="lg:col-span-1">
          <ErrorByEndpointChart environment={environment} />
        </div>
      </div>

      {/* 4. Bloco de explicação — antes da tabela de investigação */}
      {insightText && <InsightBlock id="errors-insight-block">{insightText}</InsightBlock>}

      {/* Alertas de infraestrutura — drill-down secundário, fora da composição
          fixa do mockup (que aqui prioriza a tabela de investigação abaixo). */}
      <ErrorAlertsPanel />

      {/* Funcionalidades ainda sem rota de agregação no worker — reunidas numa
          faixa compacta em vez de dois cards vazios grandes (menos ruído visual). */}
      <FeatureComingSoon
        feature="Agrupador de Falhas por Função/Tela/Versão · Análise Automática de Vitals"
        reason="Requer rota de agregação de erros e integração com Firebase Crashlytics/Google Play Vitals"
        compact
      />

      {/* 5. Tabela de investigação — drill-down por caso, com ação de resolução */}
      {loading ? (
        <LoadingState message="Carregando erros do sistema..." />
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-6">
          <div className="xl:col-span-7">
            <RecentErrorsTable
              errors={filteredErrors}
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
              <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[var(--radius-card)] p-6 font-sans">
                {/* Main title casing */}
                <div className="flex items-start justify-between pb-4 border-b border-[var(--border)] mb-5 select-none">
                  <div>
                    <span className="text-[9px] text-[var(--error)] font-sans uppercase tracking-widest font-bold">Detalhes do erro</span>
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
                      <span>Stack trace</span>
                    </div>
                    {/* Terminal técnico deliberadamente sempre escuro, independente do tema
                        do app (mesmo princípio da superfície SignallQ/IA) — cor de erro
                        fixa em vez de var(--error) porque o tom claro do tema light
                        (#D93025) não garante contraste AA sobre preto. */}
                    <div
                      className="p-4 rounded-xl font-mono text-[9px] leading-relaxed max-h-48 overflow-y-auto whitespace-pre-wrap"
                      style={{ backgroundColor: "#000000", color: "#FF4D4F" }}
                    >
                      {selectedError.stackTrace}
                    </div>
                  </div>

                  {/* Operational resolution actions */}
                  <div className="pt-4 border-t border-[var(--border)]">
                    <div className="flex items-center gap-1 text-xs text-[var(--text-secondary)] pb-3 select-none">
                      <Workflow className="w-4 h-4 text-[var(--text-tertiary)] mr-1" />
                      <span>Resolução</span>
                    </div>

                    {selectedError.resolved ? (
                      <div
                        className="p-3 rounded-xl space-y-1.5"
                        style={{ backgroundColor: alpha("var(--success)", 15), border: `1px solid ${alpha("var(--success)", 20)}` }}
                      >
                        <div
                          className="flex items-center gap-1.5 text-[10px] font-sans font-bold uppercase"
                          style={{ color: "var(--success)" }}
                        >
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
                          onClick={() => handleResolveClick(selectedError.id)}
                          disabled={resolvingId !== null}
                          className="w-full flex items-center justify-center gap-1.5 px-3.5 py-2 border rounded-xl font-sans text-[10px] font-bold uppercase transition-all select-none cursor-pointer bg-[var(--error)]/10 border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <CheckCircle className="w-3.5 h-3.5" />
                          <span>{resolvingId ? "Disparando..." : "Marcar Resolvido"}</span>
                        </button>
                      </div>
                    )}

                    {statusMessage && (
                      <div
                        className="mt-3 p-3 text-[10px] font-sans text-center rounded-xl select-none"
                        style={{
                          backgroundColor: alpha(statusMessage.success ? "var(--success)" : "var(--error)", 10),
                          border: `1px solid ${alpha(statusMessage.success ? "var(--success)" : "var(--error)", 20)}`,
                          color: statusMessage.success ? "var(--success)" : "var(--error)",
                        }}
                      >
                        {statusMessage.text}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-20 text-center rounded-[var(--radius-card)] p-6 select-none font-sans" style={{ background: "var(--bg-surface)", border: "1px dashed var(--border)" }}>
                <p className="text-xs text-[var(--text-tertiary)]">Selecione um erro na lista à esquerda para investigar.</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 6. Ações */}
      <ActionsRow
        id="errors-actions-row"
        actions={[
          { label: "Ver erros por versão", onClick: () => onNavigate("/app-versions") },
          { label: "Exportar CSV", onClick: handleExportErrors, variant: "secondary" },
        ]}
      />
    </div>
  );
};
