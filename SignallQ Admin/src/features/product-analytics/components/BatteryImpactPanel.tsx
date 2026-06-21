import React from "react";
import { BatteryImpactMetric } from "../../../types/battery";
import { Battery, Zap, Clock, ShieldAlert, Cpu, Network } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";

interface BatteryImpactPanelProps {
  metrics: BatteryImpactMetric[];
}

export const BatteryImpactPanel: React.FC<BatteryImpactPanelProps> = ({ metrics }) => {
  const getImpactBadge = (impact: "low" | "medium" | "high") => {
    switch (impact) {
      case "high":
        return <span className="text-[10px] bg-red-950/40 text-red-400 border border-red-500/10 px-2 py-0.5 rounded font-bold font-mono">ALTO</span>;
      case "medium":
        return <span className="text-[10px] bg-amber-950/30 text-amber-500 border border-amber-500/10 px-2 py-0.5 rounded font-bold font-mono">MÉDIO</span>;
      case "low":
      default:
        return <span className="text-[10px] bg-emerald-950/20 text-emerald-400 border border-emerald-500/10 px-2 py-0.5 rounded font-bold font-mono">BAIXO</span>;
    }
  };

  const getImpactProgressColor = (impact: "low" | "medium" | "high") => {
    switch (impact) {
      case "high": return "bg-red-500";
      case "medium": return "bg-amber-500";
      case "low": default: return "bg-emerald-500";
    }
  };

  return (
    <SectionCard
      title="Impacto Estimado de Bateria e Performance"
      description="Estimativas agregadas de consumo com base no padrão operacional de rádio, ciclos de hardware e rotas nativas (Android Vitals/Custom Traces)."
    >
      <div className="space-y-5">
        
        {/* Main list */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {metrics.map((item) => (
            <div 
              key={item.feature} 
              className="p-4 bg-zinc-950/50 border border-zinc-900 rounded-xl hover:border-zinc-800 transition-colors flex flex-col justify-between"
            >
              <div className="space-y-2.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Battery className={`w-4 h-4 ${
                      item.estimatedImpact === "high" ? "text-red-400 animate-pulse" : item.estimatedImpact === "medium" ? "text-amber-400" : "text-emerald-400"
                    }`} />
                    <span className="text-sm font-bold text-white font-sans">{item.label}</span>
                  </div>
                  {getImpactBadge(item.estimatedImpact)}
                </div>

                <p className="text-[10px] text-zinc-400 leading-relaxed font-sans">{item.notes}</p>
              </div>

              {/* Technical indicators */}
              <div className="pt-3.5 mt-3.5 border-t border-zinc-900 grid grid-cols-3 gap-2 text-[10px] font-mono text-zinc-500">
                <div>
                  <span className="block text-[8px] text-zinc-550 uppercase">Tempo Ativo</span>
                  <strong className="text-zinc-300">
                    {item.avgDurationMs >= 60000 
                      ? `${(item.avgDurationMs / 60000).toFixed(0)}m` 
                      : `${(item.avgDurationMs / 1000).toFixed(1)}s`}
                  </strong>
                </div>
                <div>
                  <span className="block text-[8px] text-zinc-550 uppercase">Background</span>
                  <strong className="text-zinc-300">{item.backgroundExecutionPercent}%</strong>
                </div>
                <div>
                  <span className="block text-[8px] text-zinc-550 uppercase">Avg Networks</span>
                  <strong className="text-zinc-300">{item.networkCallsAvg} reqs</strong>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Custom Traces section mapping Firebase Performance Monitoring */}
        <div className="p-4 bg-[#111111]/30 border border-zinc-900 rounded-xl space-y-3.5">
          <div className="flex items-center gap-2">
            <Cpu className="w-4 h-4 text-purple-400" />
            <span className="text-xs font-bold text-white font-sans">Custom Traces Previstos (Firebase Performance)</span>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 text-[10px] font-mono text-zinc-450 select-none">
            <span className="bg-zinc-950 px-2 py-1.5 rounded border border-zinc-900/60">trace_speedtest_execution</span>
            <span className="bg-zinc-950 px-2 py-1.5 rounded border border-zinc-900/60">trace_diagnosis_pipeline</span>
            <span className="bg-zinc-950 px-2 py-1.5 rounded border border-zinc-900/60">trace_wifi_analysis</span>
            <span className="bg-zinc-950 px-2 py-1.5 rounded border border-zinc-900/60">trace_devices_scan</span>
          </div>
        </div>

        {/* Legal and precision warning footnote */}
        <div className="text-[10px] font-sans text-zinc-500 italic bg-zinc-950/20 px-3 py-2 rounded flex items-start gap-2 border border-zinc-900/40">
          <ShieldAlert className="w-3.5 h-3.5 text-zinc-650 shrink-0 mt-0.5" />
          <span>
            Impacto de bateria é estimado por sinais operacionais e métricas agregadas. Para consumo real do sistema, consultar Android vitals/Play Console.
          </span>
        </div>

      </div>
    </SectionCard>
  );
};
