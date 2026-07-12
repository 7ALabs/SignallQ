import React from "react";
import { aiUsageService } from "../../services/aiUsageService";
import { errorMetricsService } from "../../services/errorMetricsService";
import { alpha } from "../../utils/color";
import { AiCostMetricGrid } from "./components/AiCostMetricGrid";
import { AiBudgetCard } from "./components/AiBudgetCard";
import { ProviderCostDonut } from "./components/ProviderCostDonut";
import { SectionCard } from "../../components/ui/SectionCard";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { AppEnvironment } from "../../types/admin";
import { AiModelInsights } from "../../types/ai";

interface AiCostPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
  onNavigate?: (path: string) => void;
}

export const AiCostPage: React.FC<AiCostPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [modelInsights, setModelInsights] = React.useState<AiModelInsights[]>([]);
  const [costSummary, setCostSummary] = React.useState<{ totalCostUsd: string; reliabilityPercentage: number | null } | null>(null);
  // Default provisório até o fetch real — mesmo fallback de errorMetricsService (aiDailyBudgetUsd).
  const [aiCostCeiling, setAiCostCeiling] = React.useState<number>(1.0);

  const loadAiStats = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters = { environment, period };
      const [insights, summary, aiAlerts] = await Promise.all([
        aiUsageService.getAiUsageMetrics(filters),
        aiUsageService.getAiCostSummary(filters),
        errorMetricsService.getAiAlerts(filters),
      ]);

      setModelInsights(insights ?? []);
      setCostSummary(summary);
      setAiCostCeiling(aiAlerts.aiCostCeiling);
    } catch (e: any) {
      console.error("Failed to sync AI usage insights", e);
      const code = e?.code;
      setError(code > 0 ? `Erro: ${code}` : "Sem conexão com o servidor");
    } finally {
      setLoading(false);
    }
  }, [environment, period, triggerRefreshCounter]);

  React.useEffect(() => {
    loadAiStats();
  }, [loadAiStats]);

  if (loading) {
    return <LoadingState message="Recuperando telemetria de tokens, faturas e laudos..." />;
  }

  if (error) {
    return (
      <div
        className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 rounded-[var(--radius-card)]"
        style={{
          border: `1px solid ${alpha("var(--sq-error)", 20)}`,
          backgroundColor: alpha("var(--sq-error)", 5),
        }}
      >
        <h4 className="text-sm font-semibold uppercase tracking-wider font-mono" style={{ color: "var(--sq-error)" }}>
          Erro de Telemetria
        </h4>
        <p className="text-xs mt-2" style={{ color: "var(--sq-text-secondary)" }}>{error}</p>
        <button
          onClick={() => { setError(null); loadAiStats(); }}
          className="mt-4 px-4 py-2 text-xs transition-all rounded-xl font-mono"
          style={{
            backgroundColor: alpha("var(--sq-error)", 10),
            border: `1px solid ${alpha("var(--sq-error)", 20)}`,
            color: "var(--sq-error)",
          }}
        >
          TENTAR NOVAMENTE
        </button>
      </div>
    );
  }

  if (modelInsights.length === 0) {
    return (
      <div
        className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 rounded-[var(--radius-card)]"
        style={{
          border: "1px solid var(--sq-border)",
          backgroundColor: "var(--sq-bg-card)",
        }}
      >
        <h4 className="text-xs font-semibold uppercase tracking-widest font-mono" style={{ color: "var(--sq-text-secondary)" }}>
          Sem dados
        </h4>
        <p className="text-xs mt-2" style={{ color: "var(--sq-text-secondary)" }}>
          Nenhuma inferência de IA registrada neste período.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="ai-cost-section-intro"
        overline="IA & CUSTOS"
        question="A IA está entregando valor com custo controlado?"
        description="Custo por provedor e por funcionalidade, latência, taxa de fallback e orçamento mensal."
        source="FONTE · CLOUDFLARE WORKERS AI (AI_USAGE)"
      />

      {/* 1. KPIs — 4 cards, com veredito humano (GH#552 Fase 3, substitui grid de 7) */}
      <AiCostMetricGrid costSummary={costSummary} modelInsights={modelInsights} />

      {/* 2. Orçamento mensal de IA — full-width, paridade mockup (teto real de alerta) */}
      <AiBudgetCard totalCostUsd={costSummary?.totalCostUsd ?? null} ceilingUsd={aiCostCeiling} period={period} />

      {/* 3. Composição paridade mockup — custo por provedor (donut real) +
          faturamento por função (sem contrato de dado hoje) */}
      <div className="grid grid-cols-1 lg:grid-cols-[1fr_1.3fr] gap-6">
        <ProviderCostDonut insights={modelInsights} />
        <SectionCard
          title="Custo por funcionalidade"
          description="Mapeamento econômico de processamento de tokens por finalidade de IA."
        >
          <FeatureComingSoon
            feature="Custo por funcionalidade"
            reason="Métrica ainda não disponível — aguardando exposição no worker (session_id de ai_usage e de analytics_events não são a mesma entidade hoje)"
          />
        </SectionCard>
      </div>
    </div>
  );
};
