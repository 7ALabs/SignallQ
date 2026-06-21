import React from "react";
import { FeatureCrashMetric } from "../../../types/productAnalytics";
import { AlertOctagon, ShieldAlert, CheckCircle, Smartphone } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";

interface FeatureCrashTableProps {
  metrics: FeatureCrashMetric[];
}

export const FeatureCrashTable: React.FC<FeatureCrashTableProps> = ({ metrics }) => {
  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case "critical":
        return (
          <span className="inline-flex items-center gap-1 text-[10px] bg-red-950/50 text-red-400 border border-red-500/20 px-2 py-0.5 rounded-full font-bold font-sans">
            <AlertOctagon className="w-3 h-3 text-red-400 shrink-0" />
            CRÍTICO
          </span>
        );
      case "attention":
        return (
          <span className="inline-flex items-center gap-1 text-[10px] bg-amber-950/40 text-amber-500 border border-amber-550/20 px-2 py-0.5 rounded-full font-bold font-sans">
            <ShieldAlert className="w-3 h-3 text-amber-500 shrink-0" />
            ATENÇÃO
          </span>
        );
      case "ok":
      default:
        return (
          <span className="inline-flex items-center gap-1 text-[10px] bg-emerald-950/30 text-emerald-400 border border-emerald-555/20 px-2 py-0.5 rounded-full font-bold font-sans">
            <CheckCircle className="w-3 h-3 text-emerald-400 shrink-0" />
            CONFIÁVEL
          </span>
        );
    }
  };

  return (
    <SectionCard
      title="Crashes, Erros e ANRs por Funcionalidade"
      description="Relação sistemática de anomalias detectadas pelo SDK do Firebase Crashlytics e Android Vitals por segmento operacional."
    >
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-zinc-900 text-[10px] font-mono text-zinc-550 uppercase tracking-wider">
              <th className="py-3 px-4 font-normal">Função Impactada</th>
              <th className="py-3 px-4 text-right font-normal">Crashes</th>
              <th className="py-3 px-4 text-right font-normal">Não Fatais (Non-Fatals)</th>
              <th className="py-3 px-4 text-right font-normal">ANRs</th>
              <th className="py-3 px-4 text-right font-normal">Taxa de Falha</th>
              <th className="py-3 px-4 text-right font-normal">Versões Afetadas</th>
              <th className="py-3 px-4 text-right font-normal">Severidade</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-900/40 text-[11px] font-sans">
            {metrics.map((item) => (
              <tr key={item.feature} className="hover:bg-zinc-950/20 transition-colors">
                <td className="py-3.5 px-4 font-bold text-white">
                  {item.label}
                  <span className="text-[10px] font-mono text-zinc-550 block uppercase font-normal mt-0.5">key: {item.feature}</span>
                </td>
                <td className="py-3.5 px-4 text-right font-mono font-bold text-red-400">
                  {item.crashes}
                </td>
                <td className="py-3.5 px-4 text-right font-mono text-amber-500">
                  {item.nonFatalErrors}
                </td>
                <td className="py-3.5 px-4 text-right font-mono text-yellow-600">
                  {item.anrs}
                </td>
                <td className="py-3.5 px-4 text-right font-mono text-zinc-350">
                  {(item.crashRate * 100).toFixed(2)}%
                </td>
                <td className="py-3.5 px-4 text-right font-mono text-zinc-450 text-[10px]">
                  <div className="flex gap-1 justify-end flex-wrap">
                    {item.affectedVersions.map(v => (
                      <span key={v} className="bg-zinc-950 px-1.5 py-0.5 rounded border border-zinc-900/60 flex items-center gap-0.5">
                        <Smartphone className="w-2.5 h-2.5 text-zinc-500" />
                        v{v}
                      </span>
                    ))}
                  </div>
                </td>
                <td className="py-3.5 px-4 text-right">
                  {getSeverityBadge(item.severity)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </SectionCard>
  );
};
