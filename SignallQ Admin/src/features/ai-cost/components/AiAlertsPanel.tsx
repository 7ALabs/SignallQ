import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { AlertCircle, AlertTriangle, Bell, Clock, ArrowRight } from "lucide-react";
import { errorMetricsService } from "../../../services/errorMetricsService";
import { AiAlert } from "../../../mocks/errors.mock";
import { formatCurrency } from "../../../utils/format";

export const AiAlertsPanel: React.FC = () => {
  const [alerts, setAlerts] = React.useState<AiAlert[]>([]);
  const [aiCostCeiling, setAiCostCeiling] = React.useState<number>(200);

  React.useEffect(() => {
    let active = true;
    errorMetricsService.getAiAlerts().then(({ alerts: data, aiCostCeiling: ceiling }) => {
      if (active) {
        setAlerts(data);
        setAiCostCeiling(ceiling);
      }
    });
    return () => { active = false; };
  }, []);

  return (
    <SectionCard
      title="Alertas de orçamento e IA"
      description="Avisos automáticos quando o custo ou o uso de IA sai do esperado."
      id="ai-alerts-card"
    >
      {alerts.length === 0 ? (
        <p className="text-xs text-[var(--text-tertiary)] font-sans py-4 text-center">Nenhum alerta ativo no momento.</p>
      ) : (
        <div className="space-y-3 font-sans text-xs">
          {alerts.map((alert) => {
            let icon = <Bell className="w-4 h-4 text-[var(--text-secondary)]" />;
            let containerClass = "bg-[var(--bg-base)] border-[var(--border)]";
            let titleColor = "text-[var(--text-primary)]";

            if (alert.type === "critical") {
              icon = <AlertTriangle className="w-4 h-4 text-[var(--error)]" />;
              containerClass = "bg-red-950/10 border-red-500/10";
              titleColor = "text-red-400";
            } else if (alert.type === "warning") {
              icon = <AlertCircle className="w-4 h-4 text-[var(--attention)]" />;
              containerClass = "bg-amber-950/10 border-amber-500/10";
              titleColor = "text-[var(--attention)]";
            }

            return (
              <div
                key={alert.id}
                className={`p-3.5 border rounded-xl flex items-start gap-3 transition-colors hover:bg-zinc-900/40 ${containerClass}`}
              >
                <span className="p-1.5 bg-zinc-950/40 border border-[var(--border)] rounded-lg shrink-0">
                  {icon}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2 select-none">
                    <span className={`font-semibold text-xs truncate ${titleColor}`}>
                      {alert.title}
                    </span>
                    <span className="font-mono text-[9px] text-[var(--text-tertiary)] whitespace-nowrap shrink-0 flex items-center gap-1">
                      <Clock className="w-3 h-3 text-[var(--text-tertiary)]" />
                      {alert.timestamp}
                    </span>
                  </div>
                  <p className="text-[var(--text-secondary)] leading-snug mt-1 text-[11px]">
                    {alert.description}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <div className="mt-4 pt-3.5 border-t border-dashed border-[var(--border)] flex items-center justify-between text-[10px] font-mono text-[var(--text-tertiary)] select-none">
        <span>Teto Operacional: {formatCurrency(aiCostCeiling)} / Mês</span>
        <span className="flex items-center gap-0.5 text-[var(--text-secondary)] hover:text-[var(--text-primary)] cursor-pointer transition-colors font-bold uppercase">
          Ajustar Limites <ArrowRight className="w-3" />
        </span>
      </div>
    </SectionCard>
  );
};
