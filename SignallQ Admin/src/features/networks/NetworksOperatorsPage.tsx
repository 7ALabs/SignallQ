import React from "react";
import { adminMetricsService, NetworkTypeStat } from "../../services/adminMetricsService";
import { ChartCard } from "../../components/ui/ChartCard";
import { LoadingState } from "../../components/ui/LoadingState";
import { MetricCard } from "../../components/ui/MetricCard";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { OperatorSessionsList } from "./components/OperatorSessionsList";
import { OperatorRecord, AppEnvironment } from "../../types/admin";
import { computeNetworkOverviewMetrics, scoreVerdict } from "./networkOverviewMetrics";

interface NetworksOperatorsPageProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

// GH#552 (Fase 2) — fusão de `networks/` + `operators/` (wireframe "Redes & Provedores").
// Continua sem coluna de região/UF: diagnostic_sessions não coleta esse campo hoje —
// não inventar dado que o Android não envia (ver comentário original em NetworksTab).
export const NetworksOperatorsPage: React.FC<NetworksOperatorsPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [networkStats, setNetworkStats] = React.useState<NetworkTypeStat[]>([]);
  const [operators, setOperators] = React.useState<OperatorRecord[]>([]);

  const loadData = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [netStats, ops] = await Promise.all([
        adminMetricsService.getNetworkTypeStats({ environment, period }),
        adminMetricsService.getOperatorMetrics({ environment, period }),
      ]);
      setNetworkStats(netStats);
      setOperators(ops);
    } catch (e) {
      console.error("Failed to load network/operator metrics", e);
      setError(e instanceof Error ? e.message : "Não foi possível carregar as métricas de rede.");
    } finally {
      setLoading(false);
    }
  }, [environment, period]);

  React.useEffect(() => {
    loadData();
  }, [loadData, triggerRefreshCounter]);

  if (loading) {
    return <LoadingState message="Agregando qualidade de rede por tipo e operadora..." />;
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-[var(--error)]/20 bg-[var(--error)]/5 rounded-[var(--radius-card)]">
        <h4 className="text-sm font-semibold text-[var(--error)] uppercase tracking-wider font-sans">Erro ao carregar rede</h4>
        <p className="text-xs text-[var(--text-secondary)] mt-2 font-sans">{error}</p>
        <button
          onClick={loadData}
          className="mt-4 px-4 py-2 text-xs bg-[var(--error)]/10 border border-[var(--error)]/20 text-[var(--error)] hover:bg-[var(--error)]/20 transition-all rounded-xl font-sans"
        >
          Tentar novamente
        </button>
      </div>
    );
  }

  const wifiStat = networkStats.find((s) => s.name.toLowerCase().includes("wi"));
  const mobileStat = networkStats.find((s) => {
    const n = s.name.toLowerCase();
    return n.includes("móvel") || n.includes("movel") || n.includes("mobile") || n.includes("4g") || n.includes("5g") || n.includes("cellular");
  });

  // KPIs — paridade com mockup (networkKpis): score médio de rede, sessões via
  // Wi-Fi e operadoras monitoradas usam dado real; "Regiões cobertas" segue
  // indisponível porque diagnostic_sessions não coleta UF/região hoje.
  // Cálculo compartilhado com a seção "Rede & Operadora" do Centro de Controle
  // (Overview) — ver networkOverviewMetrics.ts.
  const { avgNetworkScore, wifiSessionsPct } = computeNetworkOverviewMetrics(networkStats, operators);

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="networks-section-intro"
        overline="REDES & PROVEDORES"
        question="Onde e em que tipo de rede o app é mais usado?"
        description="Contexto de uso por tipo de conexão, operadora e região — para entender a base instalada, não para avaliar a qualidade da rede do usuário."
        source="FONTE · SIGNALLQ ANALYTICS (SESSÕES AGREGADAS)"
      />

      {/* 1. KPIs — paridade com mockup (networkKpis) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Score médio de rede"
          value={avgNetworkScore != null ? avgNetworkScore : "Não disponível"}
          verdict={avgNetworkScore != null ? scoreVerdict(avgNetworkScore) : undefined}
          verdictNote="score 0-100, calculado no dispositivo, ponderado por sessões"
          id="metric-avg-network-score"
        />
        <MetricCard
          label="Sessões via Wi-Fi"
          value={wifiSessionsPct != null ? `${wifiSessionsPct}%` : "Não disponível"}
          verdictNote="participação de Wi-Fi no total de sessões Wi-Fi + rede móvel"
          id="metric-wifi-sessions-pct"
        />
        <MetricCard
          label="Operadoras monitoradas"
          value={operators.length}
          id="metric-operators-monitored"
        />
        <MetricCard
          label="Regiões cobertas"
          value="Não disponível"
          verdictNote="diagnostic_sessions não coleta UF/região hoje"
          source="não implementado"
          id="metric-regions-covered"
        />
      </div>

      {/* 2. Composição paridade mockup — onde o app é mais usado (mapa por UF,
          sem coluna de região no worker hoje) + sessões por operadora (real). */}
      <div className="grid grid-cols-1 lg:grid-cols-[1.2fr_1fr] gap-6">
        <ChartCard
          title="Onde o app é mais usado"
          description="Volume de sessões por estado — não é indicador de qualidade de rede."
          id="regions-map-card"
        >
          <FeatureComingSoon
            feature="Mapa de sessões por UF"
            reason="Métrica ainda não disponível — aguardando exposição no worker (diagnostic_sessions não coleta região/UF hoje)"
          />
        </ChartCard>
        <OperatorSessionsList operators={operators} wifiStat={wifiStat} mobileStat={mobileStat} />
      </div>
    </div>
  );
};
