import React from "react";
import { adminMetricsService } from "../../services/adminMetricsService";
import { productAnalyticsService } from "../../services/productAnalyticsService";
import { OverviewMetricGrid } from "./components/OverviewMetricGrid";
import { DiagnosticsTimeline } from "./components/DiagnosticsTimeline";
import { NetworkTypeDistribution } from "./components/NetworkTypeDistribution";
import { TopIssuesPanel } from "./components/TopIssuesPanel";
import { RecentAlertsPanel } from "./components/RecentAlertsPanel";
import { AiProviderUsagePanel } from "./components/AiProviderUsagePanel";
import { LoadingState } from "../../components/ui/LoadingState";
import { AppEnvironment } from "../../types/admin";
import { OverviewMetricsResponse } from "../../mocks/overview.mock";
import { Zap, Layers, AlertCircle, Cpu, FileText, Activity, UserCheck, Battery } from "lucide-react";

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
  const [productMetrics, setProductMetrics] = React.useState<any>(null);

  React.useEffect(() => {
    let active = true;

    async function loadDashboardData() {
      setLoading(true);
      setError(null);
      try {
        const filters = { environment, period };
        
        // Parallelized loading using the mock services
        const [
          metricsRes,
          timelineRes,
          networkRes,
          issuesRes,
          alertsRes,
          aiUsageRes,
          productRes,
        ] = await Promise.all([
          adminMetricsService.getOverviewMetrics(filters),
          adminMetricsService.getDiagnosticsTimeline(filters),
          adminMetricsService.getNetworkInsights(filters),
          adminMetricsService.getTopIssues(filters),
          adminMetricsService.getRecentAlerts(filters),
          adminMetricsService.getAiProviderUsage(filters),
          productAnalyticsService.getOverviewCards(filters as any),
        ]);

        if (active) {
          setMetrics(metricsRes);
          setTimelineData(timelineRes);
          setNetworkDistribution(networkRes);
          setTopIssues(issuesRes);
          setAlerts(alertsRes);
          setAiUsage(aiUsageRes);
          setProductMetrics(productRes);
        }
      } catch (err: any) {
        console.error("Failed to load overview telemetry dashboard:", err);
        if (active) {
          const code = err?.code;
          setError(code > 0 ? `Erro: ${code}` : "Sem conexão com o servidor");
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

      {/* 5. Health Section "Saúde do Produto" - Cards menores */}
      {productMetrics && (
        <div className="p-6 bg-[#0E0E12] border border-[#262626] rounded-2xl space-y-4">
          <div className="flex justify-between items-center border-b border-zinc-900 pb-3">
            <div>
              <h3 className="text-sm font-bold text-white font-sans">Saúde e Engajamento do Produto</h3>
              <p className="text-[10px] text-zinc-500 font-sans mt-0.5">Indicadores do comportamento do usuário e estabilidade por funcionalidade.</p>
            </div>
          </div>
          
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3 select-none">
            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Função mais usada</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <Zap className="w-3 h-3 text-purple-400 shrink-0" />
                {productMetrics.mostUsedFeature}
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">{productMetrics.mostUsedFeatureCount} usos</span>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Tela mais acessada</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <Layers className="w-3 h-3 text-emerald-400 shrink-0" />
                {productMetrics.topViewedScreen}
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">{productMetrics.topViewedScreenCount} views</span>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Função c/ mais falhas</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <AlertCircle className="w-3 h-3 text-red-400 shrink-0" />
                {productMetrics.mostCrashingFeature}
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">{productMetrics.crashCount}</span>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Custos IA / Diag.</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <Cpu className="w-3 h-3 text-amber-500 shrink-0" />
                R$ 0,014
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">Médio por sessão</span>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Tokens Hoje</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <FileText className="w-3 h-3 text-zinc-400 shrink-0" />
                26.1M tkn
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">Inferência API</span>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Crash-free Users</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <Activity className="w-3 h-3 text-emerald-400 shrink-0" />
                99,2%
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">Estável</span>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Retenção D7</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <UserCheck className="w-3 h-3 text-indigo-400 shrink-0" />
                {productMetrics.d7Retention}
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">Tempo médio útil</span>
            </div>

            <div className="p-3 bg-[#111115] border border-zinc-900 rounded-xl space-y-1">
              <span className="text-[8px] font-mono text-zinc-550 uppercase block">Bateria / Em Obs.</span>
              <div className="text-[11px] font-bold text-white flex items-center gap-1">
                <Battery className="w-3 h-3 text-pink-400 shrink-0" />
                {productMetrics.batteryHighestFeature}
              </div>
              <span className="text-[9px] font-mono text-zinc-500 block">Impacto: Alto</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
