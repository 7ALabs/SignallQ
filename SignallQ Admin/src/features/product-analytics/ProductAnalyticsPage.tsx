import React, { useEffect, useState, useCallback } from "react";
import { AppEnvironment } from "../../types/admin";
import { productAnalyticsService, DashboardFilters } from "../../services/productAnalyticsService";
import { FeatureUsageMetric, ScreenNavigationMetric, FeatureCrashMetric, RetentionMetric, DeviceBreakdownMetric } from "../../types/productAnalytics";
import { FeatureUsageGrid } from "./components/FeatureUsageGrid";
import { FeatureRankingBars } from "./components/FeatureRankingBars";
import { RetentionBars } from "./components/RetentionBars";
import { DeviceBreakdownList } from "./components/DeviceBreakdownList";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { ChartCard } from "../../components/ui/ChartCard";
import { SectionIntro } from "../../components/ui/SectionIntro";

interface ProductAnalyticsPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
  onNavigate?: (path: string) => void;
}

interface PageData {
  featureUsage: FeatureUsageMetric[];
  screenNavigation: ScreenNavigationMetric[];
  featureCrashes: FeatureCrashMetric[];
  retention: RetentionMetric[];
  sessionDuration: Awaited<ReturnType<typeof productAnalyticsService.getSessionDuration>>;
  deviceBreakdown: DeviceBreakdownMetric[];
}

export const ProductAnalyticsPage: React.FC<ProductAnalyticsPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [data, setData] = useState<PageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    const filters: DashboardFilters = {
      period: period as DashboardFilters["period"],
      environment: environment === "all" ? undefined : environment,
    };
    try {
      const [featureUsage, screenNavigation, featureCrashes, retention, sessionDuration, deviceBreakdown] =
        await Promise.all([
          productAnalyticsService.getFeatureUsage(filters),
          productAnalyticsService.getScreenNavigation(filters),
          productAnalyticsService.getFeatureCrashes(filters),
          productAnalyticsService.getRetention(filters),
          productAnalyticsService.getSessionDuration(filters),
          productAnalyticsService.getDeviceBreakdown(filters),
        ]);
      setData({ featureUsage, screenNavigation, featureCrashes, retention, sessionDuration, deviceBreakdown });
    } catch (e) {
      setError("Falha ao carregar dados de produto.");
    } finally {
      setLoading(false);
    }
  }, [environment, period, triggerRefreshCounter]);

  useEffect(() => { load(); }, [load]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingState message="Carregando dados de produto..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64 text-sm text-[var(--text-secondary)]">
        {error}
      </div>
    );
  }

  const noData =
    !data ||
    (data.featureUsage.length === 0 &&
      data.screenNavigation.length === 0 &&
      data.featureCrashes.length === 0);

  if (noData) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-2 text-center">
        <p className="text-sm font-semibold text-[var(--text-primary)]">Sem dados no período selecionado</p>
        <p className="text-xs text-[var(--text-secondary)] max-w-sm">
          Os eventos de produto são coletados via SDK do app. Verifique se o app está enviando
          eventos para o worker ou altere o período.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="product-analytics-section-intro"
        overline="USO DO APP"
        question="As pessoas estão usando o app como esperado?"
        description="Engajamento, retenção, funcionalidades mais usadas e dispositivos — via SDK de analytics do app."
        source="FONTE · FIREBASE ANALYTICS (ANALYTICS_EVENTS)"
      />

      {/* 1. KPIs — 4 cards, com veredito vs. benchmark de mercado (GH#552 Fase 3) */}
      <FeatureUsageGrid retention={data!.retention} sessionDuration={data!.sessionDuration} />

      {/* 2. Composição principal — paridade mockup: funcionalidade mais usada
          (ranking real) + funil de teste de velocidade (sem contrato de dado
          hoje, ver FeatureComingSoon abaixo) */}
      <div className="grid grid-cols-1 lg:grid-cols-[1.3fr_1fr] gap-6">
        {data!.featureUsage.length > 0 ? (
          <FeatureRankingBars metrics={data!.featureUsage} />
        ) : (
          <ChartCard title="Funcionalidade mais usada · Sessões 7 dias" id="feature-ranking-bars-card">
            <FeatureComingSoon feature="Funcionalidade mais usada" reason="Métrica ainda não disponível — aguardando exposição no worker" />
          </ChartCard>
        )}
        <ChartCard
          title="Funil · teste de velocidade"
          description="Abriu o app → iniciou o teste → completou o teste → compartilhou o resultado."
          id="speedtest-funnel-card"
        >
          <FeatureComingSoon
            feature="Funil de teste de velocidade"
            reason="Métrica ainda não disponível — aguardando exposição no worker (requer rastreamento de estágio por sessão)"
          />
        </ChartCard>
      </div>

      {/* Segunda composição — paridade mockup: retenção D1/D7/D30 (real) +
          dispositivos mais ativos (sem contrato de dado hoje) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RetentionBars metrics={data!.retention} />
        <ChartCard
          title="Dispositivos mais ativos"
          description="Modelo, versão Android e % de sessões — a partir dos diagnósticos rodados."
          id="most-active-devices-card"
        >
          {data!.deviceBreakdown.length > 0 ? (
            <DeviceBreakdownList metrics={data!.deviceBreakdown} />
          ) : (
            <FeatureComingSoon
              feature="Dispositivos mais ativos"
              reason="Sem diagnósticos com device_model no período selecionado"
            />
          )}
        </ChartCard>
      </div>
    </div>
  );
};
