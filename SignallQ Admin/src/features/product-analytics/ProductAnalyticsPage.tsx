import React, { useEffect, useState, useCallback } from "react";
import { AppEnvironment } from "../../types/admin";
import { productAnalyticsService, DashboardFilters } from "../../services/productAnalyticsService";
import { FeatureUsageMetric, ScreenNavigationMetric, FeatureCrashMetric, RetentionMetric } from "../../types/productAnalytics";
import { FeatureUsageGrid } from "./components/FeatureUsageGrid";
import { MostUsedFeaturesTable } from "./components/MostUsedFeaturesTable";
import { ScreenNavigationPanel } from "./components/ScreenNavigationPanel";
import { FeatureCrashTable } from "./components/FeatureCrashTable";
import { RetentionPanel } from "./components/RetentionPanel";
import { FeatureRankingBars } from "./components/FeatureRankingBars";
import { RetentionBars } from "./components/RetentionBars";
import { LoadingState } from "../../components/ui/LoadingState";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
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
}

export const ProductAnalyticsPage: React.FC<ProductAnalyticsPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
  onNavigate,
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
      const [featureUsage, screenNavigation, featureCrashes, retention, sessionDuration] =
        await Promise.all([
          productAnalyticsService.getFeatureUsage(filters),
          productAnalyticsService.getScreenNavigation(filters),
          productAnalyticsService.getFeatureCrashes(filters),
          productAnalyticsService.getRetention(filters),
          productAnalyticsService.getSessionDuration(filters),
        ]);
      setData({ featureUsage, screenNavigation, featureCrashes, retention, sessionDuration });
    } catch (e) {
      setError("Falha ao carregar dados de produto.");
    } finally {
      setLoading(false);
    }
  }, [environment, period, triggerRefreshCounter]);

  useEffect(() => { load(); }, [load]);

  const handleExportFeatureUsage = () => {
    if (!data) return;
    const header = "Feature,Usos,Usuários únicos,Taxa conclusão,Taxa falha,Duração média (s),Tendência\r\n";
    const rows = data.featureUsage
      .map((f) => [
        f.feature, f.usageCount, f.uniqueUsers,
        `${(f.completionRate * 100).toFixed(1)}%`, `${(f.failureRate * 100).toFixed(1)}%`,
        (f.avgDurationMs / 1000).toFixed(1), `${f.trendPercent}%`,
      ].join(","))
      .join("\r\n");
    const csvContent = `data:text/csv;charset=utf-8,${header}${rows}`;
    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", `signallq_uso_do_app_${environment}_${period}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // GH#552 (Fase 3) — bloco de explicação: compara retenção com benchmark e
  // aponta a feature com maior abandono (menor completionRate), sem inventar
  // correlação além do que os dois conjuntos de dados já carregados mostram.
  const insightText = React.useMemo(() => {
    if (!data) return null;
    const cohort = data.retention[0];
    const worstFeature = [...data.featureUsage].sort((a, b) => a.completionRate - b.completionRate)[0];
    const parts: string[] = [];
    if (cohort?.day1 != null) {
      const inRange = cohort.day1 >= 25 && cohort.day1 <= 40;
      parts.push(
        inRange
          ? `Retenção D1 de ${cohort.day1.toFixed(0)}% está na faixa saudável de mercado para utilitários (25-40%).`
          : `Retenção D1 de ${cohort.day1.toFixed(0)}% está ${cohort.day1 < 25 ? "abaixo" : "acima"} da faixa saudável de mercado (25-40%).`
      );
    }
    if (worstFeature) {
      parts.push(
        `Maior abandono está em "${worstFeature.label}" (${(worstFeature.completionRate * 100).toFixed(0)}% de conclusão) — investigar se é timeout de IA (ver IA & Custos).`
      );
    }
    return parts.length > 0 ? parts.join(" ") : null;
  }, [data]);

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

      {/* DAU/MAU pedido pelo wireframe não tem endpoint de usuários ativos únicos
          por dia/mês hoje — só uniqueUsers por feature/tela. Sem número inventado. */}
      <FeatureComingSoon feature="DAU/MAU" reason="Requer contagem de usuários ativos únicos por dia/mês no worker" compact />

      {/* 2. Composição principal — paridade mockup: funcionalidade mais usada
          (ranking real) + funil de teste de velocidade (sem contrato de dado
          hoje, ver FeatureComingSoon abaixo) */}
      <div className="grid grid-cols-1 lg:grid-cols-[1.3fr_1fr] gap-6">
        {data!.featureUsage.length > 0 ? (
          <FeatureRankingBars metrics={data!.featureUsage} />
        ) : (
          <ChartCard title="Funcionalidade mais usada" id="feature-ranking-bars-card">
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
        <ChartCard title="Dispositivos mais ativos" id="most-active-devices-card">
          <FeatureComingSoon
            feature="Dispositivos mais ativos"
            reason="Métrica ainda não disponível — aguardando exposição no worker (requer breakdown de usuários ativos por modelo/versão de Android)"
          />
        </ChartCard>
      </div>

      {/* 3. Bloco de explicação */}
      {insightText && <InsightBlock id="product-analytics-insight-block">{insightText}</InsightBlock>}

      {/* 4. Tabela de investigação — engajamento por função (drill-down primário) */}
      {data!.featureUsage.length > 0 && <MostUsedFeaturesTable metrics={data!.featureUsage} />}

      {/* Drill-down secundário — navegação de telas e crashes por função */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {data!.screenNavigation.length > 0 && <ScreenNavigationPanel metrics={data!.screenNavigation} />}
        {data!.featureCrashes.length > 0 && <FeatureCrashTable metrics={data!.featureCrashes} />}
      </div>

      {(data!.retention.length > 0 || data!.sessionDuration) && (
        <RetentionPanel metrics={data!.retention} sessionDuration={data!.sessionDuration} />
      )}

      {/* 5. Ações */}
      <ActionsRow
        id="product-analytics-actions-row"
        actions={[
          { label: "Exportar CSV", onClick: handleExportFeatureUsage, variant: "secondary" },
          ...(onNavigate ? [{ label: "Ver IA & Custos", onClick: () => onNavigate("/ai-cost") }] : []),
        ]}
      />
    </div>
  );
};
