import React from "react";
import { adminMetricsService } from "../../services/adminMetricsService";
import { OverviewMetricGrid } from "./components/OverviewMetricGrid";
import { DiagnosticsTimeline } from "./components/DiagnosticsTimeline";
import { NetworkTypeDistribution } from "./components/NetworkTypeDistribution";
import { TopIssuesPanel } from "./components/TopIssuesPanel";
import { RecentAlertsPanel } from "./components/RecentAlertsPanel";
import { AiProviderUsagePanel } from "./components/AiProviderUsagePanel";
import { LoadingState } from "../../components/ui/LoadingState";
import { AppEnvironment } from "../../types/admin";
import { OverviewMetricsResponse } from "../../mocks/overview.mock";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";

interface OverviewPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const OverviewPage: React.FC<OverviewPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState<boolean>(true);
  const [error, setError] = React.useState<string | null>(null);
  const [retryCount, setRetryCount] = React.useState(0);

  // Dash states
  const [metrics, setMetrics] = React.useState<OverviewMetricsResponse | null>(null);
  const [timelineData, setTimelineData] = React.useState<any[]>([]);
  const [networkDistribution, setNetworkDistribution] = React.useState<any[]>([]);
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
          networkRes,
          issuesRes,
          alertsRes,
          aiUsageRes,
        ] = await Promise.all([
          adminMetricsService.getOverviewMetrics(filters),
          adminMetricsService.getDiagnosticsTimeline(filters),
          adminMetricsService.getNetworkInsights(filters),
          adminMetricsService.getTopIssues(filters),
          adminMetricsService.getRecentAlerts(filters),
          adminMetricsService.getAiProviderUsage(filters),
        ]);

        if (active) {
          setMetrics(metricsRes);
          setTimelineData(timelineRes);
          setNetworkDistribution(networkRes);
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

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <LoadingState message="Acompanhando uso, diagnósticos, qualidade da rede e custo de IA..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-red-500/20 bg-[#FF4D4F]/5 rounded-2xl">
        <h4 className="text-sm font-semibold text-[#FF4D4F] uppercase tracking-wider font-mono">Erro de Telemetria</h4>
        <p className="text-xs text-neutral-400 mt-2 font-sans">{error}</p>
        <button
          onClick={() => { setError(null); setRetryCount(c => c + 1); }}
          className="mt-4 px-4 py-2 text-xs bg-[#FF4D4F]/10 border border-[#FF4D4F]/20 text-[#FF4D4F] hover:bg-[#FF4D4F]/20 transition-all rounded-xl font-mono"
        >
          TENTAR NOVAMENTE
        </button>
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[#262626] bg-[#111111] rounded-2xl">
        <h4 className="text-xs font-semibold text-[#9CA3AF] uppercase tracking-widest font-mono">Nenhum Registro Encontrado</h4>
        <p className="text-xs text-[#9CA3AF] mt-2 font-sans">Nossos Workers não catalogaram dados de diagnósticos neste período.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 2. Grid de cards principais - 6 cards */}
      <OverviewMetricGrid metrics={metrics} />

      {/* 3. Linha principal (gráfico por hora + tipo de rede) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <DiagnosticsTimeline timelineData={timelineData} period={period} />
        </div>
        <div>
          <NetworkTypeDistribution networkData={networkDistribution} />
        </div>
      </div>

      {/* 4. Linha secundária (problemas mais comuns + alertas recentes + uso de IA) */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-fade-in">
        <div>
          <TopIssuesPanel issues={topIssues} />
        </div>
        <div>
          <RecentAlertsPanel alerts={alerts} />
        </div>
        <div>
          <AiProviderUsagePanel usage={aiUsage} />
        </div>
      </div>

      {/* 5. Health Section "Saúde do Produto" — dados de produto sem fonte real */}
      <FeatureComingSoon
        feature="Saúde e Engajamento do Produto"
        reason="Requer Firebase Analytics, Crashlytics e Product Analytics no worker"
      />
    </div>
  );
};
