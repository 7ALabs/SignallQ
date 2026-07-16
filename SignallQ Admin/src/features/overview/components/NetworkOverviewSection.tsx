import React from "react";
import { SectionGroupHeader } from "./SectionGroupHeader";
import { MetricCard } from "../../../components/ui/MetricCard";
import { NetworkOverviewMetrics, scoreVerdict } from "../../networks/networkOverviewMetrics";

interface NetworkOverviewSectionProps {
  metrics: NetworkOverviewMetrics;
  onNavigate: (path: string) => void;
}

// Seção "Rede & Operadora" (secundária) do Centro de Controle — spec Lia,
// Md3DashboardContent.dc.html:61-76. KPIs reaproveitam o cálculo já existente
// em NetworksOperatorsPage.tsx (ver networkOverviewMetrics.ts) — não duplica
// lógica. No mobile, "Operadoras Monitoradas" e o CTA somem (grid 2×1 em vez
// de 3 colunas + CTA, Md3DashboardContentMobile.dc.html:58-61).
export const NetworkOverviewSection: React.FC<NetworkOverviewSectionProps> = ({ metrics, onNavigate }) => {
  const { avgNetworkScore, wifiSessionsPct, operatorsMonitored } = metrics;

  return (
    <div className="flex flex-col gap-3.5">
      <SectionGroupHeader label="Rede & Operadora" dotColor="var(--info)" id="overview-section-network" />

      <div className="grid grid-cols-2 lg:grid-cols-[repeat(3,1fr)_auto] gap-4 items-stretch">
        <MetricCard
          label="Score Médio de Rede"
          value={avgNetworkScore != null ? avgNetworkScore : "Não disponível"}
          verdict={avgNetworkScore != null ? scoreVerdict(avgNetworkScore) : undefined}
          verdictNote="score 0-100, calculado no dispositivo, ponderado por sessões"
          id="metric-overview-avg-network-score"
        />
        <MetricCard
          label="Sessões via Wi-Fi"
          value={wifiSessionsPct != null ? `${wifiSessionsPct}%` : "Não disponível"}
          verdictNote="participação de Wi-Fi no total de sessões Wi-Fi + rede móvel"
          id="metric-overview-wifi-sessions-pct"
        />
        <MetricCard
          label="Operadoras Monitoradas"
          value={operatorsMonitored}
          className="hidden lg:block"
          id="metric-overview-operators-monitored"
        />
        <button
          type="button"
          onClick={() => onNavigate("/networks")}
          className="hidden lg:flex items-center min-h-[44px] rounded-[var(--radius-card)] px-5 sq-card-hover text-left"
          style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
        >
          <span className="text-xs font-sans font-semibold whitespace-nowrap" style={{ color: "var(--primary)" }}>
            Ver Redes &amp; Provedores →
          </span>
        </button>
      </div>
    </div>
  );
};
