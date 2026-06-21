import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { AlertCircle, AlertTriangle, ShieldCheck, Sparkles, Clock, ArrowRight } from "lucide-react";

interface AiAlert {
  id: string;
  type: "critical" | "warning" | "info";
  title: string;
  description: string;
  timestamp: string;
}

const mockAiAlerts: AiAlert[] = [
  {
    id: "al_ai_01",
    type: "warning",
    title: "Limite de Custo Mensal 80% Atingido",
    description: "Os dispêndios agregados das APIs de laudos atingiram $158,40 contra o limite teto operacional de $200,00.",
    timestamp: "Há 10 min"
  },
  {
    id: "al_ai_02",
    type: "critical",
    title: "Anomalia de Latência no Qwen 2.5 Engine",
    description: "Workers AI registrou pico de 2.4 segundos na resolução de pareces térmicos na região de Campinas.",
    timestamp: "Há 1 hora"
  },
  {
    id: "al_ai_03",
    type: "info",
    title: "Handover preventivo de DNS ativado",
    description: "SDK Android migrou de canal local de IA para Gemini 1.5 Flash de forma bem-sucedida após erro de timeout local.",
    timestamp: "Há 3 horas"
  }
];

export const AiAlertsPanel: React.FC = () => {
  return (
    <SectionCard
      title="Alertas Críticos de Orçamento & IA"
      description="Gatilhos automatizados do model manager capturando gargalos ou perigos de estouro de custos."
      id="ai-alerts-card"
    >
      <div className="space-y-3 font-sans text-xs">
        {mockAiAlerts.map((alert) => {
          let icon = <InfoClass className="w-4 h-4 text-purple-400" />;
          let containerClass = "bg-[#161619] border-[#262626]";
          let titleColor = "text-white";

          if (alert.type === "critical") {
            icon = <AlertTriangle className="w-4 h-4 text-[#FF4D4F]" />;
            containerClass = "bg-red-950/10 border-red-500/10";
            titleColor = "text-red-400";
          } else if (alert.type === "warning") {
            icon = <AlertCircle className="w-4 h-4 text-[#Eab308]" />;
            containerClass = "bg-amber-950/10 border-amber-500/10";
            titleColor = "text-[#Eab308]";
          } else {
            icon = <Sparkles className="w-4 h-4 text-purple-400 animate-pulse" />;
          }

          return (
            <div
              key={alert.id}
              className={`p-3.5 border rounded-xl flex items-start gap-3 transition-colors hover:bg-zinc-900/40 ${containerClass}`}
            >
              <span className="p-1.5 bg-zinc-950/40 border border-[#2d2d31] rounded-lg shrink-0">
                {icon}
              </span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2 select-none">
                  <span className={`font-semibold text-xs truncate ${titleColor}`}>
                    {alert.title}
                  </span>
                  <span className="font-mono text-[9px] text-zinc-500 whitespace-nowrap shrink-0 flex items-center gap-1">
                    <Clock className="w-3 h-3 text-zinc-550" />
                    {alert.timestamp}
                  </span>
                </div>
                <p className="text-zinc-400 leading-snug mt-1 text-[11px]">
                  {alert.description}
                </p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-4 pt-3.5 border-t border-dashed border-[#262626] flex items-center justify-between text-[10px] font-mono text-zinc-550 select-none">
        <span>Teto Operacional: $200.00 / Mês</span>
        <span className="flex items-center gap-0.5 text-zinc-400 hover:text-white cursor-pointer transition-colors font-bold uppercase">
          Ajustar Limites <ArrowRight className="w-3" />
        </span>
      </div>
    </SectionCard>
  );
};

// Help helper for React Scope
const InfoClass = Sparkles;
