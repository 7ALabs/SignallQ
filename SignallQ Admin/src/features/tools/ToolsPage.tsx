import React from "react";
import { Save, CheckCircle2, AlertTriangle } from "lucide-react";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { SectionCard } from "../../components/ui/SectionCard";
import { LoadingState } from "../../components/ui/LoadingState";
import { DataTable } from "../../components/ui/DataTable";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { alpha } from "../../utils/color";
import { AppEnvironment } from "../../types/admin";

// --- Diagnósticos — sessões individuais ---
import { diagnosticsService } from "../../services/diagnosticsService";
import { DiagnosticsFilters } from "../diagnostics/components/DiagnosticsFilters";
import { DiagnosticSession, DistChannel, BuildType, DataPlatform } from "../../types/diagnostics";

// --- Erros — lista detalhada ---
import { errorMetricsService } from "../../services/errorMetricsService";
import { RecentErrorsTable } from "../errors/components/RecentErrorsTable";
import { SystemError } from "../../types/errors";

// --- IA & Custos — detalhamento ---
import { aiUsageService } from "../../services/aiUsageService";
import { ProviderCostTable } from "../ai-cost/components/ProviderCostTable";
import { AiCostTimeline } from "../ai-cost/components/AiCostTimeline";
import { AiAlertsPanel } from "../ai-cost/components/AiAlertsPanel";
import { AiModelInsights, AiDailyUsage } from "../../types/ai";

// --- Uso do App — detalhamento ---
import { productAnalyticsService, DashboardFilters } from "../../services/productAnalyticsService";
import { MostUsedFeaturesTable } from "../product-analytics/components/MostUsedFeaturesTable";
import { ScreenNavigationPanel } from "../product-analytics/components/ScreenNavigationPanel";
import { FeatureCrashTable } from "../product-analytics/components/FeatureCrashTable";
import { RetentionPanel } from "../product-analytics/components/RetentionPanel";
import { FeatureUsageMetric, ScreenNavigationMetric, FeatureCrashMetric, RetentionMetric } from "../../types/productAnalytics";

// --- Saúde do Sistema — detalhamento de infraestrutura ---
import { systemHealthService, SystemHealthResponse } from "../../services/systemHealthService";
import { D1StatusCard } from "../system-health/components/D1StatusCard";
import { IntegrationCheckRow } from "../system-health/components/IntegrationCheckRow";
import { LastEventsCard } from "../system-health/components/LastEventsCard";

// --- Configurações — integrações e limites ---
import { adminSettingsService, ExtendedSettingsPayload } from "../../services/adminSettingsService";
import { IntegrationsSettings } from "../settings/components/IntegrationsSettings";
import { CostLimitSettings } from "../settings/components/CostLimitSettings";

interface ToolsPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

/** Cabeçalho leve de subseção — a "chrome" pesada (SectionCard) já vive dentro
 * de cada componente restaurado; aqui só identificamos o bloco. */
const ToolSection: React.FC<{ title: string; description: string; children: React.ReactNode }> = ({
  title,
  description,
  children,
}) => (
  <div className="space-y-4">
    <div>
      <h2 className="text-[13px] font-sans font-bold uppercase tracking-[0.06em]" style={{ color: "var(--text-primary)" }}>
        {title}
      </h2>
      <p className="text-xs font-sans mt-1" style={{ color: "var(--text-secondary)" }}>
        {description}
      </p>
    </div>
    {children}
  </div>
);

const SectionError: React.FC<{ message: string }> = ({ message }) => (
  <div className="p-4 rounded-xl text-xs bg-[var(--error)]/8 border border-[var(--error)]/25 text-[var(--error)] font-sans">
    {message}
  </div>
);

// ---------------------------------------------------------------------------
// 1. Diagnósticos — sessões individuais
// ---------------------------------------------------------------------------
const diagnosticsTableColumns = [
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
        <span className={`text-[9px] font-mono uppercase tracking-wider font-bold ${row.platform === "web" ? "text-[var(--info)]" : "text-[var(--text-tertiary)]"}`}>
          {row.platform === "web" ? "WebApp" : "Android"}
        </span>
      </div>
    ),
  },
  {
    header: "Rede / Canal",
    accessor: (row: DiagnosticSession) => {
      // Carrier/SSID crus do dispositivo às vezes vêm com espaço/caractere de
      // padding sobrando (ex: "NC BRASIL TELECOM _") — sanitiza só a exibição,
      // sem alterar o dado de origem.
      const rawDetails = row.networkStrength?.ssid ? `SSID: ${row.networkStrength.ssid}` : row.operator || "-";
      const details = rawDetails.trim().replace(/[\s_]+$/, "");
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
        {row.speed.downloadMbps.toFixed(1)} / {row.speed.uploadMbps.toFixed(1)}{" "}
        <span className="text-[10px] text-[var(--text-tertiary)]">Mbps</span>
      </span>
    ),
  },
  {
    header: "Problemas",
    accessor: (row: DiagnosticSession) => {
      const count = row.issues.length;
      const severity = row.issues.some((i) => i.severity === "critical") ? "critical" : count > 0 ? "attention" : "ok";
      const label = count === 0 ? "Limpo" : count === 1 ? "1 problema detectado" : `${count} problemas detectados`;
      return <StatusBadge status={severity} customLabel={label} />;
    },
  },
];

const DiagnosticsSessionsSection: React.FC<{ environment: AppEnvironment; period: string; triggerRefreshCounter: number }> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [localEnv, setLocalEnv] = React.useState<AppEnvironment>(environment);
  const [localPeriod, setLocalPeriod] = React.useState<string>(period);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [selectedNetwork, setSelectedNetwork] = React.useState("all");
  const [selectedOperator, setSelectedOperator] = React.useState("all");
  const [selectedScore, setSelectedScore] = React.useState("all");
  const [selectedIssue, setSelectedIssue] = React.useState("all");
  const [selectedVersion, setSelectedVersion] = React.useState("all");
  const [availableVersions, setAvailableVersions] = React.useState<string[]>([]);
  const [selectedDistChannel, setSelectedDistChannel] = React.useState<DistChannel | "">("");
  const [selectedBuildType, setSelectedBuildType] = React.useState<BuildType | "">("");
  const [selectedPlatform, setSelectedPlatform] = React.useState<DataPlatform | "">("");

  const [loading, setLoading] = React.useState(true);
  const [isRefreshing, setIsRefreshing] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [sessions, setSessions] = React.useState<DiagnosticSession[]>([]);

  React.useEffect(() => {
    setLocalEnv(environment);
    setLocalPeriod(period);
  }, [environment, period]);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const sessionsData = await diagnosticsService.getDiagnosticSessions({
        environment: localEnv,
        period: localPeriod,
        search: searchQuery,
        platform: selectedPlatform || undefined,
      });

      const versions = Array.from(new Set(sessionsData.map((s) => s.appVersion).filter(Boolean))).sort();
      setAvailableVersions(versions);

      let filtered = sessionsData;
      if (selectedNetwork !== "all") filtered = filtered.filter((s) => s.networkType === selectedNetwork);
      if (selectedOperator !== "all") filtered = filtered.filter((s) => s.operator?.toLowerCase().includes(selectedOperator.toLowerCase()));
      if (selectedScore !== "all") {
        filtered = filtered.filter((s) => {
          if (selectedScore === "poor") return s.score < 60;
          if (selectedScore === "medium") return s.score >= 60 && s.score <= 80;
          return s.score > 80;
        });
      }
      if (selectedIssue !== "all") filtered = filtered.filter((s) => s.issues.some((iss) => iss.issue === selectedIssue));
      if (selectedVersion !== "all") filtered = filtered.filter((s) => s.appVersion === selectedVersion);
      if (selectedDistChannel !== "") filtered = filtered.filter((s) => s.distChannel === selectedDistChannel);
      if (selectedBuildType !== "") filtered = filtered.filter((s) => s.buildType === selectedBuildType);

      setSessions(filtered);
    } catch (err) {
      console.error("[Ferramentas] Falha ao buscar sessões de diagnóstico:", err);
      setError("Falha ao carregar sessões de diagnóstico.");
    } finally {
      setLoading(false);
    }
  }, [
    localEnv,
    localPeriod,
    searchQuery,
    selectedNetwork,
    selectedOperator,
    selectedScore,
    selectedIssue,
    selectedVersion,
    selectedDistChannel,
    selectedBuildType,
    selectedPlatform,
    triggerRefreshCounter,
  ]);

  React.useEffect(() => {
    load();
  }, [load]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await load();
    setIsRefreshing(false);
  };

  return (
    <ToolSection
      title="Diagnósticos — sessões individuais"
      description="Filtros avançados por rede, operadora, score, problema, versão e canal de distribuição — drill-down sessão a sessão."
    >
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

      {error ? (
        <SectionError message={error} />
      ) : loading ? (
        <LoadingState message="Acompanhando logs de conectividade..." />
      ) : (
        <SectionCard
          title="Sessões de telemetria de conectividade"
          description="Histórico bruto das varreduras físicas de rádio e speedtests disparados via SDK móvel."
          id="tools-diagnostics-sessions-card"
        >
          <DataTable
            data={sessions}
            columns={diagnosticsTableColumns}
            keyExtractor={(row) => row.id}
            emptyMessage="Nenhum log corresponde aos parâmetros dos filtros avançados configurados."
            id="tools-telemetry-diagnostics-table"
          />
        </SectionCard>
      )}
    </ToolSection>
  );
};

// ---------------------------------------------------------------------------
// 2. Erros — lista detalhada
// ---------------------------------------------------------------------------
const ErrorsDetailSection: React.FC<{ environment: AppEnvironment; period: string; triggerRefreshCounter: number }> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [errors, setErrors] = React.useState<SystemError[]>([]);
  const [selectedError, setSelectedError] = React.useState<SystemError | null>(null);
  const [resolvingId, setResolvingId] = React.useState<string | null>(null);
  const [statusMessage, setStatusMessage] = React.useState<{ text: string; success: boolean } | null>(null);
  const [resolutionNoteDraft, setResolutionNoteDraft] = React.useState("");

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    setStatusMessage(null);
    try {
      const data = await errorMetricsService.getSystemErrors({ environment, period });
      setErrors(data);
      setSelectedError(data.length > 0 ? data[0] : null);
    } catch (err) {
      console.error("[Ferramentas] Falha ao buscar erros do sistema:", err);
      setError("Falha ao carregar a lista detalhada de erros.");
    } finally {
      setLoading(false);
    }
  }, [environment, period, triggerRefreshCounter]);

  React.useEffect(() => {
    load();
  }, [load]);

  const handleResolveClick = async (id: string) => {
    if (!window.confirm(`Marcar o erro ${id} como resolvido? Ele sai da lista de erros ativos.`)) return;
    setResolvingId(id);
    setStatusMessage(null);
    try {
      const res = await errorMetricsService.resolveError(id, resolutionNoteDraft);
      setStatusMessage({ text: res.message, success: res.success });
      if (res.success) {
        setErrors((prev) => prev.filter((e) => e.id !== id));
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
      console.error("[Ferramentas] Falha ao resolver erro:", err);
      setStatusMessage({ text: "Falha ao comunicar resolução com o worker.", success: false });
    } finally {
      setResolvingId(null);
    }
  };

  if (error) return <ToolSection title="Erros — lista detalhada" description="Drill-down por caso, com ação de resolução."><SectionError message={error} /></ToolSection>;

  return (
    <ToolSection title="Erros — lista detalhada" description="Drill-down por caso individual, com stack trace e ação de resolução manual.">
      {loading ? (
        <LoadingState message="Carregando erros do sistema..." />
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
          <div className="xl:col-span-5">
            {selectedError ? (
              <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[var(--radius-card)] p-5 font-sans text-xs space-y-3">
                <div className="flex items-center justify-between pb-3 border-b border-[var(--border)]">
                  <span className="font-bold text-[var(--text-primary)] font-mono">{selectedError.id}</span>
                  <span className="text-[10px] text-[var(--text-tertiary)] font-mono">
                    {new Date(selectedError.timestamp).toLocaleString("pt-BR")}
                  </span>
                </div>
                <p className="text-[var(--text-primary)] bg-[var(--bg-base)] border border-[var(--border)] p-3 rounded-xl font-mono text-[10.5px] leading-snug">
                  {selectedError.message}
                </p>
                <div
                  className="p-3 rounded-xl font-mono text-[9px] leading-relaxed max-h-40 overflow-y-auto whitespace-pre-wrap"
                  style={{ backgroundColor: "#000000", color: "#FF4D4F" }}
                >
                  {selectedError.stackTrace}
                </div>

                {selectedError.resolved ? (
                  <div
                    className="p-3 rounded-xl space-y-1"
                    style={{ backgroundColor: alpha("var(--success)", 15), border: `1px solid ${alpha("var(--success)", 20)}` }}
                  >
                    <span className="font-bold uppercase text-[10px]" style={{ color: "var(--success)" }}>Resolvido</span>
                    <p className="text-[10px] text-[var(--text-secondary)]">
                      Responsável: <strong className="text-[var(--text-primary)]">{selectedError.resolvedBy || "—"}</strong>
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <textarea
                      value={resolutionNoteDraft}
                      onChange={(e) => setResolutionNoteDraft(e.target.value)}
                      placeholder="Observação da resolução (opcional)."
                      rows={2}
                      className="w-full bg-[var(--bg-base)] border border-[var(--border)] rounded-xl p-2.5 text-[10.5px] text-[var(--text-primary)] placeholder-[var(--text-tertiary)] focus:outline-none focus:border-[var(--primary)]/60 resize-none font-sans"
                    />
                    <button
                      onClick={() => handleResolveClick(selectedError.id)}
                      disabled={resolvingId !== null}
                      className="w-full px-3.5 py-2 border rounded-xl font-sans text-[10px] font-bold uppercase transition-all cursor-pointer bg-[var(--error)]/10 border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 disabled:opacity-50"
                    >
                      {resolvingId ? "Disparando..." : "Marcar resolvido"}
                    </button>
                  </div>
                )}

                {statusMessage && (
                  <div
                    className="p-2.5 text-[10px] text-center rounded-xl"
                    style={{
                      backgroundColor: alpha(statusMessage.success ? "var(--success)" : "var(--error)", 10),
                      color: statusMessage.success ? "var(--success)" : "var(--error)",
                    }}
                  >
                    {statusMessage.text}
                  </div>
                )}
              </div>
            ) : (
              <div className="py-16 text-center rounded-[var(--radius-card)] p-6 font-sans" style={{ background: "var(--bg-surface)", border: "1px dashed var(--border)" }}>
                <p className="text-xs text-[var(--text-tertiary)]">Selecione um erro na lista à esquerda para investigar.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </ToolSection>
  );
};

// ---------------------------------------------------------------------------
// 3. IA & Custos — detalhamento
// ---------------------------------------------------------------------------
const AiCostDetailSection: React.FC<{ environment: AppEnvironment; period: string; triggerRefreshCounter: number }> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [modelInsights, setModelInsights] = React.useState<AiModelInsights[]>([]);
  const [timelineData, setTimelineData] = React.useState<AiDailyUsage[]>([]);

  React.useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    const filters = { environment, period };
    Promise.all([aiUsageService.getAiUsageMetrics(filters), aiUsageService.getAiUsageTimeSeries(filters)])
      .then(([insights, timeline]) => {
        if (!active) return;
        setModelInsights(insights ?? []);
        setTimelineData(timeline);
      })
      .catch((err) => {
        console.error("[Ferramentas] Falha ao buscar detalhamento de IA & Custos:", err);
        if (active) setError("Falha ao carregar o detalhamento de IA & Custos.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  return (
    <ToolSection title="IA & Custos — detalhamento" description="Custo e confiabilidade por provedor, série diária de tokens e alertas de orçamento.">
      {error ? (
        <SectionError message={error} />
      ) : loading ? (
        <LoadingState message="Recuperando telemetria de tokens e faturas..." />
      ) : (
        <div className="space-y-6">
          <ProviderCostTable insights={modelInsights} />
          <AiCostTimeline timelineData={timelineData} />
          <AiAlertsPanel />
        </div>
      )}
    </ToolSection>
  );
};

// ---------------------------------------------------------------------------
// 4. Uso do App — detalhamento
// ---------------------------------------------------------------------------
const ProductAnalyticsDetailSection: React.FC<{ environment: AppEnvironment; period: string; triggerRefreshCounter: number }> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [featureUsage, setFeatureUsage] = React.useState<FeatureUsageMetric[]>([]);
  const [screenNavigation, setScreenNavigation] = React.useState<ScreenNavigationMetric[]>([]);
  const [featureCrashes, setFeatureCrashes] = React.useState<FeatureCrashMetric[]>([]);
  const [retention, setRetention] = React.useState<RetentionMetric[]>([]);
  const [sessionDuration, setSessionDuration] = React.useState<{ avgDurationMs: number | null; sessionCount: number } | null>(null);

  React.useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    const filters: DashboardFilters = {
      period: period as DashboardFilters["period"],
      environment: environment === "all" ? undefined : environment,
    };
    Promise.all([
      productAnalyticsService.getFeatureUsage(filters),
      productAnalyticsService.getScreenNavigation(filters),
      productAnalyticsService.getFeatureCrashes(filters),
      productAnalyticsService.getRetention(filters),
      productAnalyticsService.getSessionDuration(filters),
    ])
      .then(([usage, navigation, crashes, retentionData, duration]) => {
        if (!active) return;
        setFeatureUsage(usage);
        setScreenNavigation(navigation);
        setFeatureCrashes(crashes);
        setRetention(retentionData);
        setSessionDuration(duration);
      })
      .catch((err) => {
        console.error("[Ferramentas] Falha ao buscar detalhamento de uso do app:", err);
        if (active) setError("Falha ao carregar o detalhamento de uso do app.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  return (
    <ToolSection title="Uso do App — detalhamento" description="Engajamento por funcionalidade, navegação entre telas, crashes e contexto de retenção.">
      {error ? (
        <SectionError message={error} />
      ) : loading ? (
        <LoadingState message="Carregando dados de produto..." />
      ) : (
        <div className="space-y-6">
          <MostUsedFeaturesTable metrics={featureUsage} />
          <ScreenNavigationPanel metrics={screenNavigation} />
          <FeatureCrashTable metrics={featureCrashes} />
          <RetentionPanel metrics={retention} sessionDuration={sessionDuration} />
        </div>
      )}
    </ToolSection>
  );
};

// ---------------------------------------------------------------------------
// 5. Saúde do Sistema — detalhamento de infraestrutura
// ---------------------------------------------------------------------------
const SystemHealthDetailSection: React.FC<{ triggerRefreshCounter: number }> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [healthError, setHealthError] = React.useState<string | null>(null);
  const [health, setHealth] = React.useState<SystemHealthResponse | null>(null);

  React.useEffect(() => {
    let active = true;
    setLoading(true);
    setHealthError(null);
    systemHealthService
      .getSystemHealth()
      .then(({ data }) => {
        if (active) setHealth(data);
      })
      .catch((e) => {
        console.error("[Ferramentas] Falha ao consultar /admin/system-health:", e);
        if (active) {
          setHealth(null);
          setHealthError(e instanceof Error ? e.message : "Falha ao consultar /admin/system-health");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [triggerRefreshCounter]);

  const d1Check = health?.checks.d1;
  const ingestCheck = health?.checks.ingest;

  return (
    <ToolSection
      title="Saúde do Sistema — detalhamento de infraestrutura"
      description="Status bruto do D1, credenciais Firebase, acesso BigQuery, ingest e últimos eventos registrados."
    >
      {healthError && <SectionError message={`Não foi possível consultar /admin/system-health: ${healthError}`} />}

      <D1StatusCard
        status={!d1Check ? "error" : d1Check.status === "ok" ? "connected" : "error"}
        lastQuery={health?.timestamp ?? null}
        loading={loading}
      />

      {loading ? (
        <LoadingState message="Consultando integrações externas..." rows={3} />
      ) : (
        <SectionCard
          title="Integrações externas"
          description="Credenciais Firebase, acesso ao BigQuery (Crashlytics export) e configuração do ingest do app."
          id="tools-integrations-status-card"
        >
          <div className="space-y-3">
            <IntegrationCheckRow
              label="Credenciais Firebase"
              status={health?.checks.firebaseCredentials.status ?? "error"}
              detail={health?.checks.firebaseCredentials.latencyMs != null ? `${health.checks.firebaseCredentials.latencyMs} ms` : undefined}
              message={health?.checks.firebaseCredentials.message}
            />
            <IntegrationCheckRow
              label="Acesso BigQuery"
              status={health?.checks.bigQuery.status ?? "error"}
              detail={health?.checks.bigQuery.latencyMs != null ? `${health.checks.bigQuery.latencyMs} ms` : undefined}
              message={health?.checks.bigQuery.message}
            />
            <IntegrationCheckRow
              label="Ingest configurado"
              status={ingestCheck?.status ?? "error"}
              detail={ingestCheck?.lastSuccessAt ? `Último ingest: ${new Date(ingestCheck.lastSuccessAt).toLocaleString("pt-BR")}` : "Sem ingest registrado"}
              message={ingestCheck?.message}
            />
          </div>
        </SectionCard>
      )}

      <LastEventsCard lastFailure={health?.lastFailure ?? null} lastSuccess={health?.lastSuccess ?? null} loading={loading} />
    </ToolSection>
  );
};

// ---------------------------------------------------------------------------
// 6. Configurações — integrações e limites
// ---------------------------------------------------------------------------
const SettingsDetailSection: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [settings, setSettings] = React.useState<ExtendedSettingsPayload | null>(null);
  const [loadError, setLoadError] = React.useState<string | null>(null);
  const [saveStatus, setSaveStatus] = React.useState<string | null>(null);
  const [saveError, setSaveError] = React.useState<string | null>(null);

  const loadSettings = React.useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const payload = await adminSettingsService.getSettings();
      setSettings(payload);
    } catch (e) {
      console.error("[Ferramentas] Falha ao buscar configurações:", e);
      setSettings(null);
      setLoadError(e instanceof Error ? e.message : "Não foi possível carregar as configurações da Admin API.");
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  const handleUpdate = (updates: Partial<ExtendedSettingsPayload>) => {
    setSaveStatus(null);
    setSaveError(null);
    setSettings((prev) => (prev ? { ...prev, ...updates } : null));
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!settings) return;
    setSaving(true);
    setSaveStatus(null);
    setSaveError(null);
    try {
      const res = await adminSettingsService.saveSettings(settings);
      if (res.success) setSaveStatus(res.message);
    } catch (err) {
      console.error("[Ferramentas] Falha ao salvar configurações:", err);
      setSaveError(err instanceof Error ? err.message : "Falha ao salvar configurações. Tente novamente.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <ToolSection title="Configurações — integrações e limites" description="Status das integrações externas e thresholds reais consumidos pelo worker de alertas.">
      {loading ? (
        <LoadingState message="Estruturando painéis de controle e chaves de segurança..." />
      ) : !settings ? (
        <SectionError message={loadError ?? "Sem dados: não foi possível carregar as configurações."} />
      ) : (
        <form onSubmit={handleSave} className="space-y-4 font-sans text-xs">
          {saveStatus && (
            <div
              className="p-3 rounded-xl flex items-center justify-center gap-2 text-xs"
              style={{ backgroundColor: alpha("var(--success)", 20), color: "var(--success)" }}
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>{saveStatus}</span>
            </div>
          )}
          {saveError && (
            <div
              className="p-3 rounded-xl flex items-center justify-center gap-2 text-xs"
              style={{ backgroundColor: alpha("var(--error)", 20), color: "var(--error)" }}
            >
              <AlertTriangle className="w-4 h-4" />
              <span>{saveError}</span>
            </div>
          )}

          <IntegrationsSettings />
          <CostLimitSettings settings={settings} onChange={handleUpdate} />

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="flex items-center gap-1.5 px-4 py-2.5 bg-[var(--primary)] hover:opacity-90 disabled:opacity-40 font-sans text-xs font-semibold text-white rounded-xl transition-all cursor-pointer"
            >
              <Save className={`w-3.5 h-3.5 ${saving ? "animate-spin" : ""}`} />
              <span>{saving ? "Salvando..." : "Salvar alterações"}</span>
            </button>
          </div>
        </form>
      )}
    </ToolSection>
  );
};

// ---------------------------------------------------------------------------
// Página principal
// ---------------------------------------------------------------------------
export const ToolsPage: React.FC<ToolsPageProps> = ({ environment, period, triggerRefreshCounter }) => {
  return (
    <div className="space-y-10">
      <SectionIntro
        id="tools-section-intro"
        overline="FERRAMENTAS"
        question="O que mais dá pra investigar e configurar?"
        description="Ferramentas operacionais que não fazem parte da narrativa principal das outras 9 telas, mas têm dado real por trás — drill-down avançado, resolução manual e configuração de integrações."
      />

      <DiagnosticsSessionsSection environment={environment} period={period} triggerRefreshCounter={triggerRefreshCounter} />
      <ErrorsDetailSection environment={environment} period={period} triggerRefreshCounter={triggerRefreshCounter} />
      <AiCostDetailSection environment={environment} period={period} triggerRefreshCounter={triggerRefreshCounter} />
      <ProductAnalyticsDetailSection environment={environment} period={period} triggerRefreshCounter={triggerRefreshCounter} />
      <SystemHealthDetailSection triggerRefreshCounter={triggerRefreshCounter} />
      <SettingsDetailSection />
    </div>
  );
};
