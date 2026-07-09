import React from "react";
import { RecentAlertItem } from "../../../mocks/overview.mock";
import { AlertOctagon, AlertCircle, Info } from "lucide-react";

interface RecentAlertsPanelProps {
  alerts: RecentAlertItem[];
}

function severityFor(alert: RecentAlertItem & { _severity?: string }) {
  return alert._severity ?? alert.severity;
}

export const RecentAlertsPanel: React.FC<RecentAlertsPanelProps> = ({ alerts }) => {
  return (
    <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[var(--radius-card)] p-5 hover:border-[var(--bg-card-hover)] transition-all duration-200 flex flex-col justify-between h-full">
      <div>
        <div className="flex items-center justify-between gap-4 pb-4 border-b border-[var(--border)] mb-5 select-none">
          <div>
            <h4 className="text-xs font-semibold font-sans uppercase tracking-wider text-[var(--text-secondary)]">
              Alertas Recentes
            </h4>
            <p className="text-[11px] text-[var(--text-tertiary)] font-sans mt-0.5">
              Anomalias operacionais e picos de falha de rádio disparados nas últimas horas.
            </p>
          </div>
          <AlertOctagon className="w-5 h-5 text-[var(--error)] shrink-0" />
        </div>

        <div className="max-h-[290px] overflow-y-auto pr-1 space-y-1">
          {alerts.length === 0 ? (
            <div className="py-8 text-center text-xs text-[var(--text-tertiary)] font-sans border border-dashed border-[var(--border)] rounded-[var(--radius-card)]">
              Sem alertas ativos
            </div>
          ) : (
            <div className="space-y-3">
              {(alerts as Array<RecentAlertItem & { _severity?: string }>).map((alert) => {
                const sev = severityFor(alert);
                const isCritical = sev === "critical";
                const isInfo     = sev === "info";
                const borderClass = isCritical
                  ? "border-red-950/70 bg-red-950/10 hover:border-red-900/60"
                  : isInfo
                  ? "border-blue-950/70 bg-blue-950/10 hover:border-blue-900/60"
                  : "border-amber-950/70 bg-amber-950/10 hover:border-amber-900/60";
                const iconColor = isCritical ? "text-red-400" : isInfo ? "text-blue-400" : "text-amber-400";
                const Icon = isCritical ? AlertOctagon : isInfo ? Info : AlertCircle;

                return (
                  <div
                    key={alert.id}
                    className={`flex items-start justify-between p-4 rounded-[var(--radius-card)] border transition-all duration-150 ${borderClass}`}
                  >
                    <div className="flex items-start gap-3">
                      <Icon className={`w-4 h-4 mt-0.5 shrink-0 ${iconColor}`} />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-semibold text-[var(--text-primary)] font-sans">{alert.source}</span>
                          <span className="text-[10px] px-1.5 py-0.5 bg-zinc-900 border border-zinc-800 rounded text-[var(--text-secondary)] font-mono">
                            {alert.count}x
                          </span>
                        </div>
                        <p className="text-xs text-[var(--text-secondary)] mt-1 font-sans leading-relaxed">
                          {alert.message}
                        </p>
                        <span className="text-[10px] text-[var(--text-tertiary)] mt-2 block font-mono">
                          {new Date(alert.timestamp).toLocaleString("pt-BR")}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
