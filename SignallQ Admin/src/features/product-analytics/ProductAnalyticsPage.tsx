import React, { useEffect, useState, useCallback } from "react";
import { AppEnvironment } from "../../types/admin";
import { productAnalyticsService, DashboardFilters } from "../../services/productAnalyticsService";
import { FeatureUsageMetric, ScreenNavigationMetric, FeatureCrashMetric, RetentionMetric, FeatureAiUsageMetric } from "../../types/productAnalytics";
import { FeatureUsageGrid } from "./components/FeatureUsageGrid";
import { MostUsedFeaturesTable } from "./components/MostUsedFeaturesTable";
import { ScreenNavigationPanel } from "./components/ScreenNavigationPanel";
import { FeatureCrashTable } from "./components/FeatureCrashTable";
import { RetentionPanel } from "./components/RetentionPanel";
import { FeatureTokenUsagePanel } from "./components/FeatureTokenUsagePanel";
import { LoadingState } from "../../components/ui/LoadingState";

interface ProductAnalyticsPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

interface PageData {
  overview: Awaited<ReturnType<typeof productAnalyticsService.getOverviewCards>>;
  featureUsage: FeatureUsageMetric[];
  screenNavigation: ScreenNavigationMetric[];
  featureCrashes: FeatureCrashMetric[];
  retention: RetentionMetric[];
  aiUsage: FeatureAiUsageMetric[];
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
      const [overview, featureUsage, screenNavigation, featureCrashes, retention, aiUsage] =
        await Promise.all([
          productAnalyticsService.getOverviewCards(filters),
          productAnalyticsService.getFeatureUsage(filters),
          productAnalyticsService.getScreenNavigation(filters),
          productAnalyticsService.getFeatureCrashes(filters),
          productAnalyticsService.getRetention(filters),
          productAnalyticsService.getFeatureAiUsage(filters),
        ]);
      setData({ overview, featureUsage, screenNavigation, featureCrashes, retention, aiUsage });
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
      {data?.overview && (
        <FeatureUsageGrid overview={data.overview} />
      )}

      {data && data.featureUsage.length > 0 && (
        <MostUsedFeaturesTable metrics={data.featureUsage} />
      )}

      {data && data.screenNavigation.length > 0 && (
        <ScreenNavigationPanel metrics={data.screenNavigation} />
      )}

      {data && data.featureCrashes.length > 0 && (
        <FeatureCrashTable metrics={data.featureCrashes} />
      )}

      {data && data.retention.length > 0 && (
        <RetentionPanel metrics={data.retention} />
      )}

      {data && data.aiUsage.length > 0 && (
        <FeatureTokenUsagePanel metrics={data.aiUsage} period={period} />
      )}
    </div>
  );
};
