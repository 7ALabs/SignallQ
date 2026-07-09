import React from "react";
import { adminMetricsService } from "../../services/adminMetricsService";
import { OverviewMetricGrid } from "./components/OverviewMetricGrid";
import { DiagnosticsTimeline } from "./components/DiagnosticsTimeline";
import { ScreenSessionsDonut } from "./components/ScreenSessionsDonut";
import { TopIssuesPanel } from "./components/TopIssuesPanel";
import { RecentAlertsPanel } from "./components/RecentAlertsPanel";
import { AiProviderUsagePanel } from "./components/AiProviderUsagePanel";
import { LoadingState } from "../../components/ui/LoadingState";
import { AppEnvironment } from "../../types/admin";
import { OverviewMetricsResponse } from "../../mocks/overview.mock";
import { productAnalyticsService } from "../../services/productAnalyticsService";
import { ScreenNavigationMetric } from "../../types/productAnalytics";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { GlobalFilters } from "../../components/ui/GlobalFilters";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
import { PERIOD_FILTERS } from "../../config/constants";

interface OverviewPageProps {
  environment: AppEnvironment;
  period: string;
  onPeriodChange: (p: string) => void;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

export const OverviewPage: React.FC<OverviewPageProps> = ({
  environment,
  period,
  onPeriodChange,
  onNavigate,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState<boolean>(true);
  const [error, setError] = React.useState<string | null>(null);
  const [retryCount, setRetryCount] = React.useState(0);

  // Dash states
  const [metrics, setMetrics] = React.useState<OverviewMetricsResponse | null>(null);
  const [timelineData, setTimelineData] = React.useState<any[]>([]);
  const [screenNavigation, setScreenNavigation] = React.useState<ScreenNavigationMetric[]>([]);
  const [topIssues, setTopIssues] = React.useState<any[]>([]);
  const [alerts, setAlerts] = React.useState<any[]>([]);
  const [aiUsage, setAiUsage] = React.useState<any[]>([]);

  React.useEffect(() => {
    let active = true;

    async function loadDashboardData() {
      setLoading(true);
      setError(null);
      try {
        const filters = { environment, period };

        // Parallelized loading
        const [
          metricsRes,
          timelineRes,
          screenNavRes,
          issuesRes,
          alertsRes,
          aiUsageRes,
        ] = await Promise.all([
          adminMetricsService.getOverviewMetrics(filters),
          adminMetricsService.getDiagnosticsTimeline(filters),
          productAnalyticsService.getScreenNavigation({ period: period as any, environment }),
          adminMetricsService.getTopIssues(filters),
          adminMetricsService.getRecentAlerts(filters),
          adminMetricsService.getAiProviderUsage(filters),
        ]);

        if (active) {
          setMetrics(metricsRes);
          setTimelineData(timelineRes);
          setScreenNavigation(screenNavRes);
          setTopIssues(issuesRes);
          setAlerts(alertsRes);
          setAiUsage(aiUsageRes);
        }
      } catch (err: any) {
        console.error("Failed to load overview telemetry dashboard:", err);
        if (active) {
          const code = err?.code;
          if (code === 401) {
            setError("Erro de autenticação — verifique a configuração do token");
          } else {
            setError(code > 0 ? `Erro HTTP ${code} — worker indisponível` : "Sem conexão com o worker");
          }
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadDashboardData();

    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter, retryCount]);

  // GH#552 (Fase 2) — síntese textual derivada só de valores já carregados nesta
  // tela (metrics + timelineData), sem inventar correlação que os dados não
  // sustentam. Se algum valor de referência faltar, cai para uma frase neutra.
  //
  // Precisa ficar antes dos early-returns abaixo (loading/error/!metrics): um
  // hook chamado condicionalmente muda de ordem entre renders e quebra a tela
  // inteira com "Rendered more hooks than during the previous render" (GH#753).
  const insightText = React.useMemo(() => {
    if (!metrics) return null;
    const diagCount = metrics.diagnosticsCount?.value;
    const successRate = metrics.successRate?.value;
    const aiCost = metrics.aiCost?.value;
    const peak = timelineData.reduce(
      (max, point: any) => (point.completedDiagnostics > (max?.completedDiagnostics ?? -1) ? point : max),
      null as any
    );
    const parts: string[] = [];
    if (diagCount != null) {
      parts.push(`${diagCount} diagnósticos executados no período selecionado`);
    }
    if (successRate != null) {
      // GH#766 — successRate já vem formatado como string com "%" (ex: "63.0%");
      // concatenar outro "%" aqui duplicava o símbolo ("0.0%%").
      parts.push(`taxa de sucesso de ${successRate}`);
    }
    if (peak?.timestamp) {
      parts.push(`pico de volume em ${peak.timestamp} (${peak.completedDiagnostics} diagnósticos)`);
    }
    if (aiCost != null) {
      parts.push(`custo de IA acumulado de ${aiCost}`);
    }
    if (parts.length === 0) return null;
    return `${parts.join(", ")}.`;
  }, [metrics, timelineData]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <LoadingState message="Acompanhando uso, diagnósticos, qualidade da rede e custo de IA..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[var(--error)]/20 bg-[var(--error)]/5 rounded-[var(--radius-card)]">
        <h4 className="text-sm font-semibold text-[var(--error)] uppercase tracking-wider font-sans">Erro de Telemetria</h4>
        <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">{error}</p>
        <button
          onClick={() => { setError(null); setRetryCount(c => c + 1); }}
          className="mt-4 px-4 py-2 text-xs bg-[var(--error)]/10 border border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 transition-all rounded-xl font-sans"
        >
          TENTAR NOVAMENTE
        </button>
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[var(--border)] bg-[var(--bg-sidebar)] rounded-[var(--radius-card)]">
        <h4 className="text-xs font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-sans">Nenhum Registro Encontrado</h4>
        <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">Nossos Workers não catalogaram dados de diagnósticos neste período.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz (signallq-admin-mockup.dc.html) */}
      <SectionIntro
        id="overview-section-intro"
        hero={{
          iconSrc: `${import.meta.env.BASE_URL}icon-192.png`,
          title: "SignallQ — Diagnóstico de Rede",
          subtitle: `io.signallq.app · Conta técnica · Atualizado em ${new Date().toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" })}`,
        }}
        overline="CENTRO DE CONTROLE"
        question="O SignallQ está saudável agora?"
        description="Visão consolidada de uso, estabilidade, custo de IA e performance do app — para decidir onde agir sem cruzar painéis manualmente."
        source="FONTES · FIREBASE ANALYTICS · PLAY CONSOLE · CLOUDFLARE WORKERS · CRASHLYTICS"
      />

      {/* 1. Filtros globais — período (env já é global via Topbar) */}
      <GlobalFilters
        filters={[
          {
            key: "period",
            label: "Período",
            value: period,
            onChange: onPeriodChange,
            options: PERIOD_FILTERS.filter((o) => o.value !== "custom"),
          },
        ]}
      />

      {/* 2. KPIs — grid de cards principais, cada um com veredito/tendência */}
      <OverviewMetricGrid metrics={metrics} />

      {/* 3. Gráfico principal — volume de diagnósticos x dispositivos ativos,
          com a composição de sessões por tela ao lado (paridade mockup). */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <DiagnosticsTimeline timelineData={timelineData} period={period} />
        </div>
        <div>
          <ScreenSessionsDonut screens={screenNavigation} />
        </div>
      </div>

      {/* 4. Alertas recentes — card full-width, posição fixa no mockup (item 3
          logo abaixo do par gráfico/donut). */}
      <RecentAlertsPanel alerts={alerts} />

      {/* 5. Bloco de explicação — antes da tabela de investigação */}
      {insightText && <InsightBlock id="overview-insight-block">{insightText}</InsightBlock>}

      {/* 6. Drill-down secundário — problemas recorrentes e uso de IA por
          provedor (contexto adicional, fora da composição fixa do mockup). */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-fade-in">
        <div>
          <TopIssuesPanel issues={topIssues} />
        </div>
        <div>
          <AiProviderUsagePanel usage={aiUsage} />
        </div>
      </div>

      {/* Saúde de produto — sem dado real ainda (requer Firebase Analytics/Crashlytics) */}
      <FeatureComingSoon
        feature="Saúde e Engajamento do Produto"
        reason="Requer Firebase Analytics, Crashlytics e Product Analytics no worker"
      />

      {/* 6. Ações — navegação direta pras telas que aprofundam cada eixo do semáforo */}
      <ActionsRow
        id="overview-actions-row"
        actions={[
          { label: "Ver Problemas & Incidentes", onClick: () => onNavigate("/errors") },
          { label: "Ver IA & Custos", onClick: () => onNavigate("/ai-cost"), variant: "secondary" },
        ]}
      />
    </div>
  );
};
