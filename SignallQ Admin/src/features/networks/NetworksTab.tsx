import React from "react";
import { adminMetricsService, NetworkTypeStat } from "../../services/adminMetricsService";
import { ChartCard } from "../../components/ui/ChartCard";
import { BarChart } from "../../components/charts/BarChart";
import { LoadingState } from "../../components/ui/LoadingState";
import { Wifi, Radio } from "lucide-react";
import { AppEnvironment } from "../../types/admin";

interface NetworksTabProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

// GH#427: cada indicador nesta aba vem de diagnostic_sessions agregada por
// network_type (GET /admin/metrics/network, ver PageHeader "Fonte de dados" e
// data-architecture.md). Não exibir contagem de torres/SSIDs, banda Wi-Fi
// específica (2.4G/5G) ou índice de "interferência" — o Android não coleta
// nenhum desses campos hoje.
export const NetworksTab: React.FC<NetworksTabProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [networkStats, setNetworkStats] = React.useState<NetworkTypeStat[]>([]);

  React.useEffect(() => {
    let active = true;
    async function loadData() {
      setLoading(true);
      try {
        const stats = await adminMetricsService.getNetworkTypeStats({ environment, period });
        if (active) setNetworkStats(stats);
      } catch (e) {
        console.error(e);
      } finally {
        if (active) setLoading(false);
      }
    }
    loadData();
    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  if (loading) {
    return <LoadingState message="Agregando diagnóstico por tipo de rede..." />;
  }

  const wifiStat = networkStats.find((s) => s.name.toLowerCase().includes("wi"));
  const mobileStat = networkStats.find((s) => {
    const n = s.name.toLowerCase();
    return n.includes("móvel") || n.includes("movel") || n.includes("mobile") || n.includes("4g") || n.includes("5g") || n.includes("cellular");
  });

  const fmt = (v: number | null, suffix = "") => (v == null ? "sem dados" : `${v.toLocaleString("pt-BR")}${suffix}`);

  return (
    <div className="space-y-6">
      {/* Cards com métricas reais por tipo de rede — sem dados, mostra "sem dados" */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="p-5 rounded-xl flex items-center gap-4" style={{ background: "var(--bg-surface)", border: "1px solid var(--border)" }}>
          <div className="w-10 h-10 rounded-lg bg-[var(--primary)]/10 flex items-center justify-center border border-[var(--primary)]/20 text-[var(--primary)]">
            <Wifi className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-sans text-[var(--text-tertiary)]">Wi-Fi</span>
            <h4 className="text-sm font-sans font-bold text-[var(--text-primary)] mt-0.5">
              {(wifiStat?.count ?? 0).toLocaleString("pt-BR")} diagnósticos no período
            </h4>
            <span className="text-[10px] text-[var(--text-secondary)] font-sans">
              Score médio {fmt(wifiStat?.avgScore ?? null)} · latência média {fmt(wifiStat?.avgLatencyMs ?? null, " ms")}
            </span>
          </div>
        </div>

        <div className="p-5 rounded-xl flex items-center gap-4" style={{ background: "var(--bg-surface)", border: "1px solid var(--border)" }}>
          <div className="w-10 h-10 rounded-lg bg-[var(--success)]/10 flex items-center justify-center border border-[var(--success)]/20 text-[var(--success)]">
            <Radio className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-sans text-[var(--text-tertiary)] font-semibold">Rede móvel</span>
            <h4 className="text-sm font-sans font-bold text-[var(--text-primary)] mt-0.5">
              {(mobileStat?.count ?? 0).toLocaleString("pt-BR")} diagnósticos no período
            </h4>
            <span className="text-[10px] text-[var(--text-secondary)] font-sans">
              Score médio {fmt(mobileStat?.avgScore ?? null)} · latência média {fmt(mobileStat?.avgLatencyMs ?? null, " ms")}
            </span>
          </div>
        </div>
      </div>

      {/* Latência e perda de pacote por tipo de rede — únicas métricas físicas
          que diagnostic_sessions realmente registra por network_type. */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Latência média por tipo de rede"
          description="Média de latency_ms reportada pelo app, agrupada por network_type, no período selecionado."
        >
          <BarChart
            data={networkStats}
            xAxisKey="name"
            series={[{ key: "avgLatencyMs", name: "Latência média (ms)", color: "var(--info)" }]}
          />
        </ChartCard>

        <ChartCard
          title="Perda de pacote média por tipo de rede"
          description="Média de packet_loss reportada pelo app, agrupada por network_type, no período selecionado."
        >
          <BarChart
            data={networkStats}
            xAxisKey="name"
            series={[{ key: "avgPacketLoss", name: "Perda de pacote (%)", color: "var(--attention)" }]}
          />
        </ChartCard>
      </div>
    </div>
  );
};
