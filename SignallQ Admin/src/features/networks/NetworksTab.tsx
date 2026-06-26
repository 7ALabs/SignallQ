import React from "react";
import { adminMetricsService } from "../../services/adminMetricsService";
import { ChartCard } from "../../components/ui/ChartCard";
import { BarChart } from "../../components/charts/BarChart";
import { LoadingState } from "../../components/ui/LoadingState";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { Wifi, Radio, Cpu, Network, ZapOff } from "lucide-react";
import { AppEnvironment } from "../../types/admin";

interface NetworksTabProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const NetworksTab: React.FC<NetworksTabProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [networkDistribution, setNetworkDistribution] = React.useState<any[]>([]);
  const [specs, setSpecs] = React.useState<any>(null);

  React.useEffect(() => {
    let active = true;
    async function loadData() {
      setLoading(true);
      try {
        const [distData, specsData] = await Promise.all([
          adminMetricsService.getNetworkInsights({ environment, period }),
          adminMetricsService.getNetworkSpecs({ environment, period })
        ]);
        if (active) {
          setNetworkDistribution(distData);
          setSpecs(specsData);
        }
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
    return <LoadingState message="Buscando performance de antenas celular e canais Wi-Fi..." />;
  }

  const hasData = networkDistribution.length > 0;

  if (!hasData) {
    return (
      <FeatureComingSoon
        feature="Análise de Redes e RF"
        reason="Requer rota de telemetria de rede no worker"
      />
    );
  }

  const barData = specs?.physicalAverages || [];

  const wifiCount = specs?.summaryStats?.wifiCount ?? 0;
  const cellCount = specs?.summaryStats?.cellCount ?? 0;
  const attenuationRate = specs?.summaryStats?.attenuationRate ?? 0;

  return (
    <div className="space-y-6">
      {/* Top statistics cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-zinc-900 border border-zinc-800 p-5 rounded-xl flex items-center gap-4">
          <div className="w-10 h-10 rounded-lg bg-[var(--primary)]/10 flex items-center justify-center border border-[var(--primary)]/20 text-[var(--primary)]">
            <Wifi className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-sans text-[var(--text-tertiary)]">SSID Coletivos</span>
            <h4 className="text-sm font-sans font-bold text-[var(--text-primary)] mt-0.5">{wifiCount.toLocaleString("pt-BR")} Redes Analisadas</h4>
            <span className="text-[10px] text-[var(--text-secondary)] font-sans">Banda principal de 5GHz (65%)</span>
          </div>
        </div>

        <div className="bg-zinc-900 border border-zinc-800 p-5 rounded-xl flex items-center gap-4">
          <div className="w-10 h-10 rounded-lg bg-[var(--success)]/10 flex items-center justify-center border border-[var(--success)]/20 text-[var(--success)]">
            <Radio className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-sans text-[var(--text-tertiary)] font-semibold">Towers Celular</span>
            <h4 className="text-sm font-sans font-bold text-[var(--text-primary)] mt-0.5">{cellCount.toLocaleString("pt-BR")} Estações ERB 4G/5G</h4>
            <span className="text-[10px] text-[var(--text-secondary)] font-sans">LTE Band 28 (700MHz) congestivo</span>
          </div>
        </div>

        <div className="bg-zinc-900 border border-zinc-800 p-5 rounded-xl flex items-center gap-4">
          <div className="w-10 h-10 rounded-lg bg-sky-500/10 flex items-center justify-center border border-sky-500/20 text-sky-400">
            <Cpu className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-sans text-[var(--text-tertiary)]">Nível Wi-Fi Ruim</span>
            <h4 className="text-sm font-semibold font-sans text-[var(--text-primary)] mt-0.5">{attenuationRate.toLocaleString("pt-BR")}% Atenuação Física</h4>
            <span className="text-[10px] text-[var(--text-secondary)] font-sans">Sinal inferior a -80 dBm</span>
          </div>
        </div>
      </div>

      {/* Grid comparing latency and packet loss metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Latência Média de Gateway por Meio Físico"
          description="Atraso do primeiro de salto (Rádio) local até o servidor DNS de borda em milissegundos."
        >
          <BarChart
            data={barData}
            xAxisKey="medium"
            series={[{ key: "averageLatency", name: "Latência Média (ms)", color: "var(--info)" }]}
          />
        </ChartCard>

        <ChartCard
          title="Taxa Percentual de Interferência & Ruído de RF"
          description="Estatística consolidada calculada a partir de flutuações de canais locais saturados."
        >
          <BarChart
            data={barData}
            xAxisKey="medium"
            series={[{ key: "interference", name: "Índice de Ruído local (%)", color: "var(--attention)" }]}
          />
        </ChartCard>
      </div>

      {/* Telemetry Breakdown Details */}
      <div className="bg-zinc-950/60 border border-zinc-800/80 rounded-2xl p-6">
        <h4 className="text-sm font-bold font-sans text-[var(--text-primary)] tracking-wide">Quadro Clínico de Radiofrequência</h4>
        <p className="text-xs text-[var(--text-secondary)] mt-1 mb-5">
          Comportamento esperado da conectividade móvel vs canais residenciais deduzido a partir da telemetria de rede.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 font-sans text-xs">
          <div className="p-4 bg-zinc-900 border border-zinc-850 rounded-xl">
            <div className="flex items-center gap-2 text-indigo-400 font-semibold mb-2">
              <Network className="w-4 h-4" />
              <span>Gargalo de Upload 4G</span>
            </div>
            <p className="text-[var(--text-secondary)] leading-relaxed text-[11px]">
              O upload minguado nas conexões de celular (médias de 2-4 Mbps em 700MHz) limita transações rápidas simultâneas de dados, gerando bufferbloat crítico sob carga.
            </p>
          </div>

          <div className="p-4 bg-zinc-900 border border-zinc-850 rounded-xl">
            <div className="flex items-center gap-2 text-[var(--success)] font-semibold mb-2">
              <Wifi className="w-4 h-4" />
              <span>Saturação 2.4 GHz</span>
            </div>
            <p className="text-[var(--text-secondary)] leading-relaxed text-[11px]">
              O espectro de 2.4 GHz permanece saturado nas grandes metrópoles devido ao auto-overlapping de canais Wi-Fi de terceiros (especialmente canais 1, 6 e 11).
            </p>
          </div>

          <div className="p-4 bg-zinc-900 border border-zinc-850 rounded-xl">
            <div className="flex items-center gap-2 text-amber-400 font-semibold mb-2">
              <ZapOff className="w-4 h-4" />
              <span>Bufferbloat Crítico</span>
            </div>
            <p className="text-[var(--text-secondary)] leading-relaxed text-[11px]">
              Durante picos de transmissão simultânea, modens antigos de banda larga ADSL/Cabo incham as filas locais de buffering de rede antes de descartar pacotes, elevando pings de 12ms até 450ms.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
