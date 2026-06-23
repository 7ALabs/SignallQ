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
      <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 shadow-sm h-full flex items-center justify-center">
        <span className="text-xs text-zinc-500 font-mono">Carregando análise...</span>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 shadow-sm h-full flex items-center justify-center">
        <span className="text-xs text-zinc-500 font-mono">Análise indisponível neste ambiente.</span>
      </div>
    );
  }

  return (
    <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 shadow-sm h-full flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between pb-4 border-b border-[#262626] mb-5 select-none">
          <div className="flex items-center gap-2">
            <span className="p-1 px-2 rounded-md bg-[#FF4D4F]/10 border border-[#FF4D4F]/20 text-[#FF4D4F] font-mono text-[10px] uppercase font-bold">
              Diagnóstico Fatores
            </span>
            <h4 className="text-xs font-semibold font-mono uppercase tracking-wider text-zinc-400">
              Escrutínio Operacional
            </h4>
          </div>
          {selectedIssueName && (
            <button
              onClick={onClear}
              className="text-[10px] text-zinc-500 hover:text-white uppercase transition-colors font-mono"
            >
              Limpar seleção [x]
            </button>
          )}
        </div>

        <div className="space-y-4 font-sans text-xs">
          <div className="p-3 bg-[#18181B] border border-[#262626] rounded-xl relative overflow-hidden select-none">
            <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/5 rounded-full filter blur-xl pointer-events-none" />
            <div className="text-[10px] text-[#FF4D4F] font-mono uppercase font-bold">ALERTA OPERACIONAL AUTOMÁTICO</div>
            <h5 className="font-semibold text-white text-sm font-sans mt-0.5">{detail.title}</h5>
            <span className="text-[10px] font-mono text-zinc-400 block mt-1">Impacto: {detail.probability}</span>
          </div>

          <div className="space-y-1 select-none">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-zinc-500" />
              <span>Causa Raiz Física Identificada</span>
            </div>
            <p className="text-zinc-350 leading-relaxed text-[11px] font-sans">
              {detail.technicalCause}
            </p>
          </div>

          <div className="space-y-2 select-none">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold">
              Impacto Direto de Desempenho
            </div>
            <div className="grid grid-cols-3 gap-2.5">
              {detail.impactMetrics.map((met, idx) => (
                <div key={idx} className="bg-[#161619] border border-[#2d2d31]/50 p-2.5 rounded-lg text-center">
                  <span className="text-[9px] text-zinc-500 font-sans block truncate">{met.key}</span>
                  <span className="text-xs font-bold font-mono text-[#FF4D4F] block mt-0.5">{met.val}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-1">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1.5 select-none">
              <Cpu className="w-3.5 h-3.5 text-purple-400" />
              <span>Rotina do Gateway (Cloudflare Edge)</span>
            </div>
            <div className="p-3 bg-[#0a0a0c] border border-[#262626] rounded-xl font-mono text-[10px] text-zinc-400 leading-relaxed max-h-24 overflow-y-auto">
              {detail.cloudflareEdgeWorkflow}
            </div>
          </div>

          <div className="space-y-1">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1.5 select-none">
              <Terminal className="w-3.5 h-3.5 text-[#22C55E]" />
              <span>Diretriz de Mitigação (App Android)</span>
            </div>
            <div className="p-3 bg-[#0a0a0c] border border-dashed border-[#22C55E]/20 text-[#22C55E] rounded-xl font-sans text-[11px] leading-relaxed">
              {detail.remediationRecipeAndroid}
            </div>
          </div>
        </div>
      </div>

      <div className="mt-5 pt-4 border-t border-[#262626] flex items-center justify-between text-[10px] font-mono text-zinc-540 select-none">
        <span>Sincronizado via laudos Gemini</span>
        <span className="flex items-center gap-1 text-purple-400 font-semibold cursor-pointer hover:underline">
          Abrir documentação <ArrowRight className="w-3 h-3" />
        </span>
      </div>
    </div>
  );
};
