import React from "react";
import { ContextualAdOpportunity } from "../../../types/ads";
import { Megaphone, Ban, Compass, ShieldOff, AlertCircle } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";

interface AdsOpportunityPanelProps {
  opportunities: ContextualAdOpportunity[];
}

export const AdsOpportunityPanel: React.FC<AdsOpportunityPanelProps> = ({ opportunities }) => {
  const getStatusBadge = (status: string) => {
    switch (status) {
      case "eligible":
        return <span className="text-[10px] bg-emerald-950/30 text-emerald-400 border border-emerald-500/10 px-2 py-0.5 rounded font-bold font-mono">ELEGÍVEL</span>;
      case "blocked_by_privacy":
        return <span className="text-[10px] bg-red-950/40 text-red-500 border border-red-500/10 px-2 py-0.5 rounded font-bold font-mono">BLOQUEADO</span>;
      case "planned":
      default:
        return <span className="text-[10px] bg-zinc-950 text-zinc-400 border border-zinc-850 px-2 py-0.5 rounded font-bold font-mono">PLANEJADO</span>;
    }
  };

  const getSensitivityClass = (sensitivity: "low" | "medium" | "high") => {
    switch (sensitivity) {
      case "high": return "text-red-400";
      case "medium": return "text-amber-500";
      case "low": default: return "text-emerald-400";
    }
  };

  return (
    <SectionCard
      title="Oportunidades de Anúncios Contextuais (Monetização Futura)"
      description="Identificação de demandas e dores operacionais de conectividade do usuário para recomendação dirigida de equipamentos/serviços de rede."
    >
      <div className="space-y-6">
        
        {/* Top summary stats */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 select-none">
          <div className="bg-zinc-950/40 border border-zinc-900 p-4 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Anúncios Ativos</span>
            <div className="text-sm font-bold text-red-500 font-sans mt-1">NÃO</div>
          </div>
          <div className="bg-zinc-950/40 border border-zinc-900 p-4 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Modo de Operação</span>
            <div className="text-sm font-bold text-zinc-400 font-sans mt-1">Planejado</div>
          </div>
          <div className="bg-zinc-950/40 border border-zinc-900 p-4 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Elegibilidade Geral</span>
            <div className="text-sm font-bold text-white font-mono mt-1">12.480 diagnósticos</div>
          </div>
          <div className="bg-zinc-950/40 border border-zinc-900 p-4 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Categoria Relevante</span>
            <div className="text-sm font-bold text-purple-400 font-sans mt-1">Equipamentos Wi-Fi</div>
          </div>
          <div className="bg-zinc-950/40 border border-zinc-900 p-4 rounded-xl">
            <span className="block text-[8px] font-mono text-zinc-500 uppercase">Privacidade / Consentimento</span>
            <div className="text-sm font-bold text-amber-500 font-sans mt-1">Aguardando UMP SDK</div>
          </div>
        </div>

        {/* Opportunities Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-900 text-[10px] font-mono text-zinc-550 uppercase tracking-wider">
                <th className="py-3 px-4 font-normal">Sinal / Problema Detectado</th>
                <th className="py-3 px-4 text-right font-normal">Sessões Elegíveis</th>
                <th className="py-3 px-4 font-normal">Categorias Sugeridas (Contexto)</th>
                <th className="py-3 px-4 text-center font-normal">Sensibilidade</th>
                <th className="py-3 px-4 text-center font-normal">Exige Consentimento</th>
                <th className="py-3 px-3 text-right font-normal">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-900/40 text-[11px] font-sans">
              {opportunities.map((opp) => (
                <tr key={opp.issue} className="hover:bg-zinc-950/20 transition-colors">
                  <td className="py-3.5 px-4 font-bold text-white">
                    {opp.label}
                    <span className="text-[10px] font-mono text-zinc-550 block font-normal mt-0.5">issue_id: {opp.issue}</span>
                  </td>
                  <td className="py-3.5 px-4 text-right font-mono text-zinc-200">
                    {opp.eligibleDiagnostics.toLocaleString("pt-BR")}
                  </td>
                  <td className="py-3.5 px-4">
                    <div className="flex flex-wrap gap-1 max-w-[280px]">
                      {opp.recommendedCategories.map((c, i) => (
                        <span key={i} className="text-[9px] bg-zinc-950 text-zinc-400 px-1.5 py-0.5 rounded border border-zinc-900/60 font-mono">
                          {c}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className={`py-3.5 px-4 text-center font-mono font-bold uppercase ${getSensitivityClass(opp.sensitivity)}`}>
                    {opp.sensitivity}
                  </td>
                  <td className="py-3.5 px-4 text-center">
                    <span className={`text-[10px] font-bold font-mono px-2 py-0.5 rounded ${
                      opp.requiresConsent 
                        ? "text-red-400 bg-red-950/20 border border-red-500/10" 
                        : "text-emerald-400 bg-emerald-950/20 border border-emerald-500/10"
                    }`}>
                      {opp.requiresConsent ? "OBRIGATÓRIO" : "RECOMENDADO"}
                    </span>
                  </td>
                  <td className="py-3.5 px-3 text-right">
                    {getStatusBadge(opp.status)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Warning Policy info */}
        <div className="p-4 bg-zinc-900/10 border border-dashed border-zinc-850 rounded-xl flex items-start gap-3">
          <ShieldOff className="w-4 h-4 text-zinc-450 shrink-0 mt-0.5" />
          <div className="text-[10px] font-mono text-zinc-500 leading-relaxed">
            <span className="text-zinc-300 font-bold block mb-1">Privacidade Garantida e Proteção à Conectividade</span>
            O direcionamento de monetização contextual baseia-se exclusivamente no tipo e criticidade do diagnóstico técnico de rede coletado localmente. 
            É estritamente proibido o uso de SSID completo, BSSID, endereços IP públicos descriptografados, posicionamento geográfico fino ou dados que configurem PII (Personally Identifiable Information) para fins publicitários.
          </div>
        </div>

      </div>
    </SectionCard>
  );
};
