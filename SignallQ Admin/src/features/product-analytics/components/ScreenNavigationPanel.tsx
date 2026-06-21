import React from "react";
import { ScreenNavigationMetric } from "../../../types/productAnalytics";
import { Compass, Eye, Users, Timer, LogOut, ArrowRight } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";

interface ScreenNavigationPanelProps {
  metrics: ScreenNavigationMetric[];
}

export const ScreenNavigationPanel: React.FC<ScreenNavigationPanelProps> = ({ metrics }) => {
  return (
    <SectionCard
      title="Navegação e Fluxo de Telas"
      description="Views totais, usuários únicos e taxa de evasão de cada tela visualizada pelo aplicativo móvel."
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {metrics.map((item, index) => (
          <div 
            key={item.screen}
            className="p-4 bg-[#111111]/30 border border-[#262626]/80 hover:border-zinc-800 rounded-xl transition-all flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-3.5">
                <div className="flex items-center gap-2">
                  <div className="w-5 h-5 flex items-center justify-center rounded bg-zinc-900 border border-zinc-850 font-mono text-[10px] text-zinc-400 font-bold">
                    {index + 1}
                  </div>
                  <span className="text-sm font-bold text-white font-sans">{item.label}</span>
                </div>
                <span className="text-[9px] font-mono text-zinc-550 uppercase">screen_key: {item.screen}</span>
              </div>

              {/* Progress visual indicator of views */}
              <div className="h-1 bg-zinc-90 w-full rounded overflow-hidden mb-4">
                <div 
                  className="bg-purple-500 h-full rounded"
                  style={{ width: `${Math.min(100, (item.views / (metrics[0]?.views || 1)) * 100)}%` }}
                />
              </div>

              <div className="grid grid-cols-2 gap-y-3 gap-x-2 text-[10px] font-mono text-zinc-400 mb-4">
                <div className="flex items-center gap-1.5">
                  <Eye className="w-3.5 h-3.5 text-purple-400" />
                  <span>Views: <strong className="text-zinc-200">{item.views.toLocaleString("pt-BR")}</strong></span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-zinc-400" />
                  <span>Ativos: <strong className="text-zinc-200">{item.uniqueUsers.toLocaleString("pt-BR")}</strong></span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Timer className="w-3.5 h-3.5 text-zinc-400" />
                  <span>Duração: <strong className="text-zinc-200">{item.avgTimeOnScreenSec}s</strong></span>
                </div>
                <div className="flex items-center gap-1.5">
                  <LogOut className="w-3.5 h-3.5 text-red-500/80" />
                  <span>Taxa Saída: <strong className="text-red-400">{(item.exitRate * 100).toFixed(0)}%</strong></span>
                </div>
              </div>
            </div>

            {item.nextMostCommonScreen && (
              <div className="pt-2.5 border-t border-zinc-950/60 flex items-center gap-1 text-[9px] font-mono text-zinc-500">
                <Compass className="w-3 h-3 text-zinc-450 shrink-0" />
                <span>Próxima tela mais comum:</span>
                <span className="text-indigo-400 text-[10px] font-semibold flex items-center gap-0.5 ml-1">
                  {item.nextMostCommonScreen.toUpperCase()}
                  <ArrowRight className="w-2.5 h-2.5" />
                </span>
              </div>
            )}
          </div>
        ))}
      </div>
    </SectionCard>
  );
};
