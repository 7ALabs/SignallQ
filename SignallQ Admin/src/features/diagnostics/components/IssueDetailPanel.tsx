import React from "react";
import { Terminal, ShieldAlert, Cpu } from "lucide-react";
import { ArrowRight } from "lucide-react";
import { diagnosticsService } from "../../../services/diagnosticsService";
import { IssueDetail } from "../../../mocks/diagnostics.mock";

interface IssueDetailPanelProps {
  selectedIssueName: string | null;
  onClear: () => void;
}

const DEFAULT_ISSUE = "Wi-Fi fraco";

export const IssueDetailPanel: React.FC<IssueDetailPanelProps> = ({ selectedIssueName, onClear }) => {
  const [detail, setDetail] = React.useState<IssueDetail | null>(null);
  const [loading, setLoading] = React.useState(true);

  const issueName = selectedIssueName || DEFAULT_ISSUE;

  React.useEffect(() => {
    let active = true;
    setLoading(true);
    diagnosticsService.getIssueDetail(issueName).then((data) => {
      if (active) {
        setDetail(data);
        setLoading(false);
      }
    });
    return () => { active = false; };
  }, [issueName]);

  if (loading) {
    return (
      <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-2xl p-5 shadow-sm h-full flex items-center justify-center">
        <span className="text-xs text-[var(--text-tertiary)] font-sans">Carregando análise...</span>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-2xl p-5 shadow-sm h-full flex items-center justify-center">
        <span className="text-xs text-[var(--text-tertiary)] font-sans">Análise indisponível neste ambiente.</span>
      </div>
    );
  }

  return (
    <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-2xl p-5 shadow-sm h-full flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between pb-4 border-b border-[var(--border)] mb-5 select-none">
          <div className="flex items-center gap-2">
            <span className="p-1 px-2 rounded-md bg-[var(--error)]/10 border border-[var(--error)]/20 text-[var(--error)] font-sans text-[10px] uppercase font-bold">
              Diagnóstico Fatores
            </span>
            <h4 className="text-xs font-semibold font-sans uppercase tracking-wider text-[var(--text-secondary)]">
              Escrutínio Operacional
            </h4>
          </div>
          {selectedIssueName && (
            <button
              onClick={onClear}
              className="text-[10px] text-[var(--text-tertiary)] hover:text-[var(--text-primary)] uppercase transition-colors font-sans"
            >
              Limpar seleção [x]
            </button>
          )}
        </div>

        <div className="space-y-4 font-sans text-xs">
          <div className="p-3 bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl relative overflow-hidden select-none">
            <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/5 rounded-full filter blur-xl pointer-events-none" />
            <div className="text-[10px] text-[var(--error)] font-sans uppercase font-bold">ALERTA OPERACIONAL AUTOMÁTICO</div>
            <h5 className="font-semibold text-[var(--text-primary)] text-sm font-sans mt-0.5">{detail.title}</h5>
            <span className="text-[10px] font-mono text-[var(--text-secondary)] block mt-1">Impacto: {detail.probability}</span>
          </div>

          <div className="space-y-1 select-none">
            <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider font-bold flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-[var(--text-tertiary)]" />
              <span>Causa Raiz Física Identificada</span>
            </div>
            <p className="text-[var(--text-secondary)] leading-relaxed text-[11px] font-sans">
              {detail.technicalCause}
            </p>
          </div>

          <div className="space-y-2 select-none">
            <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider font-bold">
              Impacto Direto de Desempenho
            </div>
            <div className="grid grid-cols-3 gap-2.5">
              {detail.impactMetrics.map((met, idx) => (
                <div key={idx} className="bg-[var(--bg-base)] border border-[var(--border)]/50 p-2.5 rounded-lg text-center">
                  <span className="text-[9px] text-[var(--text-tertiary)] font-sans block truncate">{met.key}</span>
                  <span className="text-xs font-bold font-mono text-[var(--error)] block mt-0.5">{met.val}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-1">
            <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider font-bold flex items-center gap-1.5 select-none">
              <Cpu className="w-3.5 h-3.5 text-purple-400" />
              <span>Rotina do Gateway (Cloudflare Edge)</span>
            </div>
            <div className="p-3 bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-xl font-mono text-[10px] text-[var(--text-secondary)] leading-relaxed max-h-24 overflow-y-auto">
              {detail.cloudflareEdgeWorkflow}
            </div>
          </div>

          <div className="space-y-1">
            <div className="text-[10px] text-[var(--text-tertiary)] font-sans uppercase tracking-wider font-bold flex items-center gap-1.5 select-none">
              <Terminal className="w-3.5 h-3.5 text-[var(--success)]" />
              <span>Diretriz de Mitigação (App Android)</span>
            </div>
            <div className="p-3 bg-[var(--bg-sidebar)] border border-dashed border-[var(--success)]/20 text-[var(--success)] rounded-xl font-sans text-[11px] leading-relaxed">
              {detail.remediationRecipeAndroid}
            </div>
          </div>
        </div>
      </div>

      <div className="mt-5 pt-4 border-t border-[var(--border)] flex items-center justify-between text-[10px] font-mono text-[var(--text-tertiary)] select-none">
        <span>Sincronizado via laudos Gemini</span>
        <span className="flex items-center gap-1 text-purple-400 font-semibold cursor-pointer hover:underline">
          Abrir documentação <ArrowRight className="w-3 h-3" />
        </span>
      </div>
    </div>
  );
};
