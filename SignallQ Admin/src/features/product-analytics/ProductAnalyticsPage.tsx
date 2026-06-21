import React from "react";
import { productAnalyticsService } from "../../services/productAnalyticsService";
import { adsIntelligenceService } from "../../services/adsIntelligenceService";
import { batteryInsightsService } from "../../services/batteryInsightsService";
import { FeatureUsageGrid } from "./components/FeatureUsageGrid";
import { MostUsedFeaturesTable } from "./components/MostUsedFeaturesTable";
import { ScreenNavigationPanel } from "./components/ScreenNavigationPanel";
import { FeatureCrashTable } from "./components/FeatureCrashTable";
import { RetentionPanel } from "./components/RetentionPanel";
import { BatteryImpactPanel } from "./components/BatteryImpactPanel";
import { AdsOpportunityPanel } from "./components/AdsOpportunityPanel";
import { FeatureTokenUsagePanel } from "./components/FeatureTokenUsagePanel";
import { LoadingState } from "../../components/ui/LoadingState";
import { AppEnvironment } from "../../types/admin";

interface ProductAnalyticsPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const ProductAnalyticsPage: React.FC<ProductAnalyticsPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState<boolean>(true);
  const [error, setError] = React.useState<string | null>(null);

  // Core product states
  const [overview, setOverview] = React.useState<any>(null);
  const [featureUsage, setFeatureUsage] = React.useState<any[]>([]);
  const [screenNavigation, setScreenNavigation] = React.useState<any[]>([]);
  const [featureCrashes, setFeatureCrashes] = React.useState<any[]>([]);
  const [retention, setRetention] = React.useState<any[]>([]);
  const [featureAiUsage, setFeatureAiUsage] = React.useState<any[]>([]);
  const [adOpportunities, setAdOpportunities] = React.useState<any[]>([]);
  const [batteryImpact, setBatteryImpact] = React.useState<any[]>([]);

  React.useEffect(() => {
    let active = true;

    async function loadProductAnalyticsData() {
      setLoading(true);
      setError(null);
      try {
        const filters = { environment, period: period as "1d" | "7d" | "30d" };

        const [
          overviewRes,
          featureUsageRes,
          screenNavRes,
          crashesRes,
          retentionRes,
          aiUsageRes,
          adsRes,
          batteryRes,
        ] = await Promise.all([
          productAnalyticsService.getOverviewCards(filters),
          productAnalyticsService.getFeatureUsage(filters),
          productAnalyticsService.getScreenNavigation(filters),
          productAnalyticsService.getFeatureCrashes(filters),
          productAnalyticsService.getRetention(filters),
          productAnalyticsService.getFeatureAiUsage(filters),
          adsIntelligenceService.getAdOpportunities(),
          batteryInsightsService.getBatteryImpactMetrics(),
        ]);

        if (active) {
          setOverview(overviewRes);
          setFeatureUsage(featureUsageRes);
          setScreenNavigation(screenNavRes);
          setFeatureCrashes(crashesRes);
          setRetention(retentionRes);
          setFeatureAiUsage(aiUsageRes);
          setAdOpportunities(adsRes);
          setBatteryImpact(batteryRes);
        }
      } catch (err: any) {
        console.error("Failed to load product analytics metrics:", err);
        if (active) {
          setError(err?.message || "Ocorreu um erro ao carregar os dados de comportamento e produto.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadProductAnalyticsData();

    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <LoadingState message="Agregando eventos analíticos de tela, traces de performance e retenção de produto..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-red-500/20 bg-[#FF4D4F]/5 rounded-2xl">
        <h4 className="text-sm font-semibold text-[#FF4D4F] uppercase tracking-wider font-mono">Erro de Produto</h4>
        <p className="text-xs text-neutral-400 mt-2 font-sans">{error}</p>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-12">
      {/* 1. Key Metrics Cards */}
      {overview && <FeatureUsageGrid overview={overview} />}

      {/* 2. Features Usage and App Screens Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <MostUsedFeaturesTable metrics={featureUsage} />
        </div>
        <div>
          <ScreenNavigationPanel metrics={screenNavigation} />
        </div>
      </div>

      {/* 3. Retenção & Instalações */}
      <RetentionPanel metrics={retention} />

      {/* 4. AI Tokens & Resources Consumption */}
      <FeatureTokenUsagePanel metrics={featureAiUsage} />

      {/* 5. Crashes & Battery Impact list */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <FeatureCrashTable metrics={featureCrashes} />
        <BatteryImpactPanel metrics={batteryImpact} />
      </div>

      {/* 6. Future Ad opportunities */}
      <AdsOpportunityPanel opportunities={adOpportunities} />
    </div>
  );
};
