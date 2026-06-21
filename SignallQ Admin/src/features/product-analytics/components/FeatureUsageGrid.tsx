import React from "react";
import { 
  Zap, 
  Layers, 
  TrendingDown, 
  AlertTriangle, 
  Clock, 
  UserCheck, 
  Cpu, 
  Battery 
} from "lucide-react";

interface FeatureUsageGridProps {
  overview: {
    mostUsedFeature: string;
    mostUsedFeatureCount: string;
    topViewedScreen: string;
    topViewedScreenCount: string;
    worstAbandonedFlow: string;
    abandonRate: string;
    mostCrashingFeature: string;
    crashCount: string;
    avgInstalledTime: string;
    d7Retention: string;
    highestAiConsumer: string;
    highestAiConsumerCalls: string;
    batteryHighestFeature: string;
    batteryHighestImpact: string;
  };
}

export const FeatureUsageGrid: React.FC<FeatureUsageGridProps> = ({ overview }) => {
  const cards = [
    {
      title: "Função mais usada",
      value: overview.mostUsedFeature,
      sub: overview.mostUsedFeatureCount,
      icon: <Zap className="w-4 h-4 text-purple-400" />,
      color: "border-purple-500/10 hover:border-purple-500/20 bg-purple-950/5"
    },
    {
      title: "Tela mais acessada",
      value: overview.topViewedScreen,
      sub: overview.topViewedScreenCount,
      icon: <Layers className="w-4 h-4 text-emerald-400" />,
      color: "border-emerald-500/10 hover:border-emerald-500/20 bg-emerald-950/5"
    },
    {
      title: "Maior Abandono",
      value: overview.worstAbandonedFlow,
      sub: `Taxa: ${overview.abandonRate}`,
      icon: <TrendingDown className="w-4 h-4 text-red-400" />,
      color: "border-red-500/10 hover:border-red-500/20 bg-red-950/5"
    },
    {
      title: "Função com mais Crash",
      value: overview.mostCrashingFeature,
      sub: overview.crashCount,
      icon: <AlertTriangle className="w-4 h-4 text-amber-500" />,
      color: "border-amber-500/10 hover:border-amber-500/20 bg-amber-950/5"
    },
    {
      title: "Média Instalado",
      value: overview.avgInstalledTime,
      sub: "Tempo de vida médio",
      icon: <Clock className="w-4 h-4 text-blue-400" />,
      color: "border-blue-500/10 hover:border-blue-500/20 bg-blue-950/5"
    },
    {
      title: "Retenção D7",
      value: overview.d7Retention,
      sub: "Usuários ativos D7",
      icon: <UserCheck className="w-4 h-4 text-teal-400" />,
      color: "border-teal-500/10 hover:border-teal-500/20 bg-teal-950/5"
    },
    {
      title: "Maior Consumo IA",
      value: overview.highestAiConsumer,
      sub: overview.highestAiConsumerCalls,
      icon: <Cpu className="w-4 h-4 text-amber-400" />,
      color: "border-amber-500/10 hover:border-amber-500/20 bg-amber-950/10"
    },
    {
      title: "Impacto Estimado Bateria",
      value: overview.batteryHighestFeature,
      sub: `Impacto: ${overview.batteryHighestImpact}`,
      icon: <Battery className="w-4 h-4 text-pink-400" />,
      color: "border-pink-500/10 hover:border-pink-500/20 bg-pink-950/5"
    }
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 select-none">
      {cards.map((card, i) => (
        <div 
          key={i} 
          className={`relative border rounded-2xl p-4.5 flex flex-col justify-between overflow-hidden transition-all duration-200 ${card.color}`}
        >
          {/* Subtle grid bg effect */}
          <div className="absolute -top-6 -right-6 w-16 h-16 bg-white/[0.01] rounded-full filter blur-md pointer-events-none" />
          
          <div className="flex items-center justify-between mb-2">
            <span className="text-[10px] font-mono text-zinc-400 uppercase tracking-wider">
              {card.title}
            </span>
            {card.icon}
          </div>
          
          <div>
            <div className="text-base font-bold text-white tracking-tight">{card.value}</div>
            <div className="text-[10px] font-mono text-zinc-400/90 mt-0.5">{card.sub}</div>
          </div>
        </div>
      ))}
    </div>
  );
};
