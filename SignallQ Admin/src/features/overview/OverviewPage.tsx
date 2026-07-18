import React from "react";
import { adminMetricsService, NetworkTypeStat } from "../../services/adminMetricsService";
import { aiUsageService } from "../../services/aiUsageService";
import { integrationsService } from "../../integrations/integrationsService";
import { FirebaseAnalyticsSummary, FirebaseCrashlyticsSummary } from "../../integrations/firebase/firebase.types";
import { GooglePlayRatingSummary } from "../../integrations/google-play/googlePlay.types";
import { AppSection } from "./components/AppSection";
import { NetworkOverviewSection } from "./components/NetworkOverviewSection";
import { AiCostSummaryCard } from "./components/AiCostSummaryCard";
import { RecentAlertsPanel } from "./components/RecentAlertsPanel";
import { LoadingState } from "../../components/ui/LoadingState";
import { AppEnvironment, OperatorRecord } from "../../types/admin";
import { OverviewMetricsResponse } from "../../mocks/overview.mock";
import { productAnalyticsService } from "../../services/productAnalyticsService";
import { ScreenNavigationMetric } from "../../types/productAnalytics";
import { computeNetworkOverviewMetrics } from "../networks/networkOverviewMetrics";

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
  const [alerts, setAlerts] = React.useState<any[]>([]);
  const [networkStats, setNetworkStats] = React.useState<NetworkTypeStat[]>([]);
  const [operators, setOperators] = React.useState<OperatorRecord[]>([]);

  // Paridade com o protótipo md3-tobe (Md3DashboardContent.dc.html): os KPIs
  // do Centro de Controle são Usuários Ativos, Sessões (7d), Crash-free rate e
  // Nota na Play Store (seção "App"); Score Médio de Rede, Sessões via Wi-Fi e
  // Operadoras Monitoradas (seção "Rede & Operadora"); Custo de IA isolado.
  // Nota da Play Store hoje não tem dado real disponível (integração ainda
  // mock-only) — "Não disponível" é o estado honesto, não bug. Crash-free rate
  // vem do worker (BigQuery/Crashlytics); "Não disponível" só aparece quando o
  // source não é "bigquery".
  const [firebaseAnalytics, setFirebaseAnalytics] = React.useState<FirebaseAnalyticsSummary | null>(null);
  const [firebaseCrashlytics, setFirebaseCrashlytics] = React.useState<FirebaseCrashlyticsSummary | null>(null);
  const [aiCostMonthLabel, setAiCostMonthLabel] = React.useState<string | null>(null);
  const [playStoreRating, setPlayStoreRating] = React.useState<GooglePlayRatingSummary | null>(null);

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
          alertsRes,
          networkStatsRes,
          operatorsRes,
        ] = await Promise.all([
          adminMetricsService.getOverviewMetrics(filters),
          adminMetricsService.getDiagnosticsTimeline(filters),
          productAnalyticsService.getScreenNavigation({ period: period as any, environment }),
          adminMetricsService.getRecentAlerts(filters),
          adminMetricsService.getNetworkTypeStats(filters),
          adminMetricsService.getOperatorMetrics(filters),
        ]);

        if (active) {
          setMetrics(metricsRes);
          setTimelineData(timelineRes);
          setScreenNavigation(screenNavRes);
          setAlerts(alertsRes);
          setNetworkStats(networkStatsRes);
          setOperators(operatorsRes);
        }

        // KPIs do protótipo — resilientes individualmente (.catch(() => null)): a
        // ausência de um não pode derrubar a tela inteira, e cada um já é
        // honestamente nullable quando a integração real não existe ainda.
        const [fbAnalyticsRes, fbCrashlyticsRes, aiCostMonthRes, gpRatingRes] = await Promise.all([
          integrationsService.getFirebaseAnalytics(filters).catch(() => null),
          integrationsService.getFirebaseCrashlytics(filters).catch(() => null),
          aiUsageService.getAiCostSummary({ ...filters, period: "30d" }).catch(() => null),
          integrationsService.getGooglePlayRatings(filters).catch(() => null),
        ]);

        if (active) {
          setFirebaseAnalytics(fbAnalyticsRes);
          setFirebaseCrashlytics(fbCrashlyticsRes);
          setAiCostMonthLabel(aiCostMonthRes?.totalCostUsd ?? null);
          setPlayStoreRating(gpRatingRes);
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
        <h4 className="text-sm font-semibold text-[var(--error)] uppercase tracking-wider font-sans">Erro de telemetria</h4>
        <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">{error}</p>
        <button
          onClick={() => { setError(null); setRetryCount(c => c + 1); }}
          className="mt-4 px-4 py-2 text-xs bg-[var(--error)]/10 border border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 transition-all rounded-xl font-sans"
        >
          Tentar novamente
        </button>
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[var(--border)] bg-[var(--bg-sidebar)] rounded-[var(--radius-card)]">
        <h4 className="text-xs font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-sans">Nenhum registro encontrado</h4>
        <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">Nossos Workers não catalogaram dados de diagnósticos neste período.</p>
      </div>
    );
  }

  const networkOverviewMetrics = computeNetworkOverviewMetrics(networkStats, operators);

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — título estático do protótipo md3-tobe
          (Md3DashboardContent.dc.html:12-15: overline "CENTRO DE CONTROLE" +
          H1 "Visão geral do SignallQ"), não a pergunta dinâmica do
          SectionIntro (correção 2026-07-16: "o que vale é o que está na spec
          md3-tobe" — reverte a decisão anterior de manter o padrão de
          pergunta-guia nesta tela). SectionIntro continua em uso nas outras
          8 telas do Console. */}
      <div className="mb-2">
        <div
          className="text-[11px] font-sans font-semibold uppercase tracking-[0.08em]"
          style={{ color: "var(--text-tertiary)" }}
        >
          CENTRO DE CONTROLE
        </div>
        <h1
          className="text-[26px] font-sans font-medium leading-[1.25] tracking-[-0.02em] mt-1.5"
          style={{ color: "var(--text-primary)" }}
        >
          Visão geral do SignallQ
        </h1>
      </div>

      {/* Período já é controlado globalmente pelo Topbar (pills PROD/STG +
          período) — duplicar o mesmo filtro aqui era um GlobalFilters
          redundante herdado do SIG-294 Fase 1, antes do Topbar assumir esse
          controle. Removido. */}

      {/* 1. Seção "App" — KPIs + gráfico de sessões + donut de sessões por tela */}
      <AppSection
        activeUsersToday={firebaseAnalytics?.activeUsersToday ?? null}
        sessions7d={firebaseAnalytics?.sessions7d ?? null}
        firebaseCrashlytics={firebaseCrashlytics}
        playStoreRating={playStoreRating}
        timelineData={timelineData}
        screenNavigation={screenNavigation}
      />

      {/* 2. Seção "Rede & Operadora" — mesmo cálculo de NetworksOperatorsPage */}
      <NetworkOverviewSection metrics={networkOverviewMetrics} onNavigate={onNavigate} />

      {/* 3. Custo de IA — card isolado, fora do grid de métricas de produto */}
      <AiCostSummaryCard aiCostMonthLabel={aiCostMonthLabel} onNavigate={onNavigate} />

      {/* 4. Alertas recentes — card full-width, último item da composição fixa
          do protótipo pra sec-overview (header > App > Rede & Operadora >
          Custo de IA > alertas). */}
      <RecentAlertsPanel alerts={alerts} />
    </div>
  );
};
