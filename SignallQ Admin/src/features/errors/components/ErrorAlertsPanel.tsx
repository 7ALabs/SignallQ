import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { Cpu, Database, WifiOff } from "lucide-react";
import { errorMetricsService } from "../../../services/errorMetricsService";
import { InfraAlert } from "../../../mocks/errors.mock";

export const ErrorAlertsPanel: React.FC = () => {
  const [alerts, setAlerts] = React.useState<InfraAlert[]>([]);

  React.useEffect(() => {
    let active = true;
    errorMetricsService.getInfraAlerts().then((data) => {
      if (active) setAlerts(data);
    });
    return () => { active = false; };
  }, []);

  return (
    <SectionCard
      title="Alertas Críticos de Infraestrutura"
      description="Gargalos ativos em nossos servidores, bancos SQL e APIs capturados pelo robô de auditorias."
      id="infra-alerts-card"
    >
      {alerts.length === 0 ? (
        <p className="text-xs text-[var(--text-tertiary)] font-sans py-4 text-center">Nenhum alerta ativo no momento.</p>
      ) : (
        <div className="space-y-3 font-sans text-xs">
          {alerts.map((alert) => {
            let icon = <Cpu className="w-4 h-4 text-[var(--text-secondary)]" />;
            let containerClass = "bg-[var(--bg-base)] border-[var(--border)]";
            let labelColor = "text-[var(--text-primary)]";

            if (alert.severity === "high") {
              icon = <Database className="w-4 h-4 text-[var(--error)] animate-pulse" />;
              containerClass = "bg-red-950/15 border-red-500/10";
              labelColor = "text-red-400";
            } else if (alert.severity === "medium") {
              icon = <WifiOff className="w-4 h-4 text-[var(--attention)]" />;
              containerClass = "bg-amber-950/10 border-amber-500/10";
              labelColor = "text-[var(--attention)]";
            }

            return (
              <div
                key={alert.id}
                className={`p-3 border rounded-xl flex items-start gap-2.5 hover:bg-zinc-900/50 transition-colors ${containerClass}`}
              >
                <span className="p-1 px-1.5 bg-zinc-950/40 border border-[var(--border)] rounded-lg shrink-0 mt-0.5">
                  {icon}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1.5 select-none">
                    <span className={`font-sans text-[9px] uppercase tracking-wider font-bold block ${labelColor}`}>
                      {alert.source}
                    </span>
                    <span className="font-mono text-[9px] text-[var(--text-tertiary)] block shrink-0">{alert.timestamp}</span>
                  </div>
                  <p className="text-[var(--text-secondary)] text-[10.5px] leading-snug mt-1 font-sans">
                    {alert.message}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </SectionCard>
  );
};
