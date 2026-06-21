import React from "react";
import { Sparkles, Wifi, Signal, Clock, Activity, AlertTriangle, ArrowUpRight, ArrowDownRight, RefreshCw, Zap } from "lucide-react";

export interface IntelligenceItem {
  id: string;
  problem: string;
  occurrence: number;
  variation: string;
  variationType: "up" | "down";
  scoreAffected: number;
  impact: string;
}

export const diagnosticIntelligenceData: IntelligenceItem[] = [
  {
    id: "intel_1",
    problem: "Wi-Fi fraco",
    occurrence: 31,
    variation: "+8% vs semana anterior",
    variationType: "up",
    scoreAffected: 62,
    impact: "Chamadas, jogos e vídeo instáveis"
  },
  {
    id: "intel_2",
    problem: "Bufferbloat upload",
    occurrence: 18,
    variation: "-2%",
    variationType: "down",
    scoreAffected: 64,
    impact: "Travamentos durante upload, videochamadas e jogos"
  },
  {
    id: "intel_3",
    problem: "DNS lento",
    occurrence: 14,
    variation: "+4%",
    variationType: "up",
    scoreAffected: 71,
    impact: "Demora para abrir sites e apps"
  },
  {
    id: "intel_4",
    problem: "Rede móvel congestionada",
    occurrence: 11,
    variation: "+12%",
    variationType: "up",
    scoreAffected: 58,
    impact: "Internet lenta mesmo com sinal bom"
  },
  {
    id: "intel_5",
    problem: "Gateway lento",
    occurrence: 7,
    variation: "+1%",
    variationType: "up",
    scoreAffected: 69,
    impact: "Lentidão local entre dispositivo e roteador"
  }
];

interface DiagnosticIntelligencePanelProps {
  onSelectIssue?: (issueName: string) => void;
}

export const DiagnosticIntelligencePanel: React.FC<DiagnosticIntelligencePanelProps> = ({ onSelectIssue }) => {
  return (
    <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between pb-4 border-b border-[#262626] mb-5 select-none">
        <div>
          <h4 className="text-xs font-semibold font-mono uppercase tracking-wider text-zinc-400 flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-purple-400 animate-pulse" />
            Diagnostic Intelligence (IA Co-Pilot)
          </h4>
          <p className="text-[11px] text-zinc-500 font-sans mt-0.5">
            Mapeamento analítico de regressão identificando anomalias crônicas no parque de dispositivos.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        {diagnosticIntelligenceData.map((item) => {
          const isUp = item.variationType === "up";
          const isWifi = item.problem.toLowerCase().includes("wi-fi");
          const isMobile = item.problem.toLowerCase().includes("móvel") || item.problem.toLowerCase().includes("celular");

          return (
            <div
              key={item.id}
              onClick={() => onSelectIssue && onSelectIssue(item.problem)}
              className="bg-[#161619] border border-[#262626] hover:border-zinc-700 p-4 rounded-xl flex flex-col justify-between transition-all duration-200 cursor-pointer group"
            >
              {/* Top info and badge */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  {/* Small icon representing problem */}
                  <span className="p-1.5 rounded-lg bg-[#212124] border border-[#2d2d31]">
                    {isWifi ? (
                      <Activity className="w-3.5 h-3.5 text-purple-400 animate-pulse" />
                    ) : isMobile ? (
                      <AlertTriangle className="w-3.5 h-3.5 text-yellow-500" />
                    ) : (
                      <Clock className="w-3.5 h-3.5 text-blue-400" />
                    )}
                  </span>
                  <div className="flex items-center gap-1 font-mono text-[9px] font-bold">
                    <span className={isUp ? "text-[#FF4D4F]" : "text-[#22C55E]"}>
                      {item.variation}
                    </span>
                    {isUp ? (
                      <ArrowUpRight className="w-3 h-3 text-[#FF4D4F]" />
                    ) : (
                      <ArrowDownRight className="w-3 h-3 text-[#22C55E]" />
                    )}
                  </div>
                </div>

                {/* Problem Name and occurrence */}
                <div>
                  <h5 className="font-semibold text-white text-xs group-hover:text-purple-400 transition-colors font-sans">
                    {item.problem}
                  </h5>
                  <div className="flex items-baseline gap-1.5 mt-1 select-none">
                    <span className="text-xl font-bold font-mono text-white leading-none">
                      {item.occurrence}%
                    </span>
                    <span className="text-[9px] font-mono text-zinc-500 block">ocorrência</span>
                  </div>
                </div>
              </div>

              {/* Impact description & score */}
              <div className="mt-4 pt-3 border-t border-[#262626]/50 space-y-2 select-none">
                <div className="flex justify-between items-center text-[10px] font-mono">
                  <span className="text-zinc-500">Score Afetado:</span>
                  <span className="text-[#FF4D4F] font-bold bg-[#FF4D4F]/10 px-1.5 py-0.2 rounded border border-[#FF4D4F]/20">
                    {item.scoreAffected}/100
                  </span>
                </div>
                <p className="text-[10px] text-zinc-400 font-sans leading-snug line-clamp-2">
                  {item.impact}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
