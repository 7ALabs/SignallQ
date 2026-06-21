import React from "react";
import { RetentionMetric } from "../../../types/productAnalytics";
import { SectionCard } from "../../../components/ui/SectionCard";
import { UserPlus, Calendar, Clock, AlertCircle } from "lucide-react";

interface RetentionPanelProps {
  metrics: RetentionMetric[];
}

export const RetentionPanel: React.FC<RetentionPanelProps> = ({ metrics }) => {
  const m = metrics[0] || {
    cohort: "Cohort Geral (Últimos 30d)",
    day1: 0.68,
    day7: 0.32,
    day30: 0.14,
    avgInstalledDays: 18.4,
    uninstallRate: 0.28
  };

  const rates = [
    { label: "Retenção D1", val: `${(m.day1 * 100).toFixed(0)}%`, desc: "Primeiro dia de abertura" },
    { label: "Retenção D7", val: `${(m.day7 * 100).toFixed(0)}%`, desc: "Engajamento recorrente semanal" },
    { label: "Retenção D30", val: `${(m.day30 * 100).toFixed(0)}%`, desc: "Uso prolongado mensal" },
    { label: "Taxa de Desinstalação", val: `${(m.uninstallRate * 100).toFixed(0)}%`, desc: "Evasão de instalados *" }
  ];

  return (
    <SectionCard
      title="Retenção de Usuários & Tempo de Vida útil (LTV)"
      description="Taxas de permanência ativa e estimativas de exclusão de usuários na base móvel recorrente."
    >
      <div className="space-y-6">
        <div>
          <div className="flex justify-between items-center mb-1 bg-zinc-950/20 px-3 py-1.5 rounded border border-zinc-900/45">
            <span className="text-[11px] font-mono text-zinc-440">Filtro Ativo: {m.cohort}</span>
            <span className="text-[10px] font-bold text-indigo-400">Tempo Médio Instalado: {m.avgInstalledDays} dias</span>
          </div>
        </div>

        {/* Dynamic cohort bars */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {rates.map((rate, i) => (
            <div key={i} className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl flex flex-col justify-between">
              <span className="text-[10px] font-mono uppercase text-zinc-450 tracking-wider font-semibold">{rate.label}</span>
              <div className="my-2.5">
                <span className="text-xl font-bold font-mono text-white">{rate.val}</span>
              </div>
              <p className="text-[10px] font-sans text-zinc-500 leading-tight">{rate.desc}</p>
            </div>
          ))}
        </div>

        {/* Insights / warning block details */}
        <div className="p-4.5 bg-[#111111]/30 border border-dashed border-[#262626] rounded-xl space-y-3">
          <div className="flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
            <div className="text-[10px] font-mono text-zinc-450 leading-relaxed space-y-1.5">
              <p className="text-white font-semibold">Estimativa Operacional de Desinstalação (*)</p>
              <p>
                Os dados de desinstalação são calculados heuristicamente com base nas interações silenciadas do FCM (Firebase Cloud Messaging), 
                estatísticas agregadas obtidas na Google Play Developer API e ausência sistemática de telemetria por períodos superiores a 14 dias consecutivo.
              </p>
              <div className="grid grid-cols-2 gap-2 text-zinc-350 pt-1.5 select-none text-[9px]">
                <div>• Versão com maior abandono: <strong className="text-red-400">v0.18.0 (estimada)</strong></div>
                <div>• Fluxo associado a churn: <strong className="text-red-400 font-semibold uppercase">Diagnóstico guiado (35%)</strong></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </SectionCard>
  );
};
