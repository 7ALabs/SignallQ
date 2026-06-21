import React from "react";
import { AlertCircle, AlertOctagon, RefreshCw } from "lucide-react";
import { Severity } from "../../types/diagnostics";

export interface AlertItem {
  id: string;
  source: string;
  message: string;
  severity: Severity;
  timestamp: string;
  count: number;
}

interface AlertListProps {
  alerts: AlertItem[];
  onResolve?: (id: string) => void;
  id?: string;
}

export const AlertList: React.FC<AlertListProps> = ({ alerts, onResolve, id }) => {
  return (
    <div id={id} className="space-y-3">
      {alerts.length === 0 ? (
        <div className="py-6 text-center text-xs text-neutral-500 font-sans border border-dashed border-zinc-800 rounded-xl">
          Nenhum alerta crítico ativo atualmente.
        </div>
      ) : (
        alerts.map((alert) => {
          const isCritical = alert.severity === "critical";
          const borderClass = isCritical
            ? "border-red-950/70 bg-red-950/10 hover:border-red-900/60"
            : "border-amber-950/70 bg-amber-950/10 hover:border-amber-900/60";
          const iconColor = isCritical ? "text-red-400" : "text-amber-400";
          
          return (
            <div
              key={alert.id}
              className={`flex items-start justify-between p-4 rounded-xl border transition-all duration-150 ${borderClass}`}
            >
              <div className="flex items-start gap-3">
                {isCritical ? (
                  <AlertOctagon className={`w-4 h-4 mt-0.5 shrink-0 ${iconColor}`} />
                ) : (
                  <AlertCircle className={`w-4 h-4 mt-0.5 shrink-0 ${iconColor}`} />
                )}
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-white font-sans">{alert.source}</span>
                    <span className="text-[10px] px-1.5 py-0.2 bg-zinc-900 border border-zinc-800 rounded text-neutral-400 font-mono">
                      Ocorrências: {alert.count}
                    </span>
                  </div>
                  <p className="text-xs text-neutral-350 mt-1 font-sans leading-relaxed">
                    {alert.message}
                  </p>
                  <span className="text-[10px] text-zinc-500 mt-2 block font-mono">
                    {new Date(alert.timestamp).toLocaleTimeString("pt-BR")} — {alert.timestamp.split("T")[0]}
                  </span>
                </div>
              </div>

              {onResolve && (
                <button
                  onClick={() => onResolve(alert.id)}
                  className="flex items-center gap-1.5 px-3 py-1 bg-zinc-950 hover:bg-neutral-900 text-[10px] font-mono tracking-wider border border-zinc-800 hover:border-neutral-700 text-neutral-300 rounded-lg transition-colors select-none"
                >
                  <RefreshCw className="w-2.5 h-2.5 text-neutral-400" />
                  LIMPAR
                </button>
              )}
            </div>
          );
        })
      )}
    </div>
  );
};
