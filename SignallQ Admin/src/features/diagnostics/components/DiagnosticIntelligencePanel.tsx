import React from "react";
import { Sparkles, Activity, AlertTriangle, Clock, ArrowUpRight, ArrowDownRight } from "lucide-react";
import { diagnosticsService } from "../../../services/diagnosticsService";
import { IntelligenceItem } from "../../../mocks/diagnostics.mock";
import { DashboardFilters } from "../../../services/adminMetricsService";

interface DiagnosticIntelligencePanelProps {
  onSelectIssue?: (issueName: string) => void;
  filters?: DashboardFilters;
}

export const DiagnosticIntelligencePanel: React.FC<DiagnosticIntelligencePanelProps> = ({ onSelectIssue, filters }) => {
  const [items, setItems] = React.useState<IntelligenceItem[]>([]);

  React.useEffect(() => {
    let active = true;
    diagnosticsService.getDiagnosticIntelligence(filters ?? {}).then((data) => {
      if (active) setItems(data);
    });
    return () => { active = false; };
  }, [filters]);

  return (
    <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between pb-4 border-b border-[var(--border)] mb-5 select-none">
        <div>
          <h4 className="text-xs font-semibold font-sans uppercase tracking-wider text-[var(--text-secondary)] flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-purple-400 animate-pulse" />
            Diagnostic Intelligence (IA Co-Pilot)
          </h4>
          <p className="text-[11px] text-[var(--text-tertiary)] font-sans mt-0.5">
            Mapeamento analítico de regressão identificando anomalias crônicas no parque de dispositivos.
          </p>
        </div>
      </div>

      {items.length === 0 ? (
        <p className="text-xs text-[var(--text-tertiary)] font-sans py-4 text-center">Análise de inteligência indisponível neste ambiente.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          {items.map((item) => {
            const isUp = item.variationType === "up";
            const isWifi = item.problem.toLowerCase().includes("wi-fi");
            const isMobile = item.problem.toLowerCase().includes("móvel") || item.problem.toLowerCase().includes("celular");

            return (
              <div
                key={item.id}
                onClick={() => onSelectIssue && onSelectIssue(item.problem)}
                className="bg-[var(--bg-base)] border border-[var(--border)] hover:border-zinc-700 p-4 rounded-xl flex flex-col justify-between transition-all duration-200 cursor-pointer group"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="p-1.5 rounded-lg bg-[var(--bg-surface)] border border-[var(--border)]">
                      {isWifi ? (
                        <Activity className="w-3.5 h-3.5 text-purple-400 animate-pulse" />
                      ) : isMobile ? (
                        <AlertTriangle className="w-3.5 h-3.5 text-yellow-500" />
                      ) : (
                        <Clock className="w-3.5 h-3.5 text-blue-400" />
                      )}
                    </span>
                    <div className="flex items-center gap-1 font-mono text-[9px] font-bold">
                      <span className={isUp ? "text-[var(--error)]" : "text-[var(--success)]"}>
                        {item.variation}
                      </span>
                      {isUp ? (
                        <ArrowUpRight className="w-3 h-3 text-[var(--error)]" />
                      ) : (
                        <ArrowDownRight className="w-3 h-3 text-[var(--success)]" />
                      )}
                    </div>
                  </div>

                  <div>
                    <h5 className="font-semibold text-[var(--text-primary)] text-xs group-hover:text-purple-400 transition-colors font-sans">
                      {item.problem}
                    </h5>
                    <div className="flex items-baseline gap-1.5 mt-1 select-none">
                      <span className="text-xl font-bold font-mono text-[var(--text-primary)] leading-none">
                        {item.occurrence}%
                      </span>
                      <span className="text-[9px] font-mono text-[var(--text-tertiary)] block">ocorrência</span>
                    </div>
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-[var(--border)]/50 space-y-2 select-none">
                  <div className="flex justify-between items-center text-[10px] font-mono">
                    <span className="text-[var(--text-tertiary)]">Score Afetado:</span>
                    <span className="text-[var(--error)] font-bold bg-[var(--error)]/10 px-1.5 py-0.5 rounded border border-[var(--error)]/20">
                      {item.scoreAffected}/100
                    </span>
                  </div>
                  <p className="text-[10px] text-[var(--text-secondary)] font-sans leading-snug line-clamp-2">
                    {item.impact}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
