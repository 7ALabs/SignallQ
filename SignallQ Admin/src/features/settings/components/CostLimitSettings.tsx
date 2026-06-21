import React from "react";
import { ExtendedSettingsPayload } from "../../../services/adminSettingsService";
import { SectionCard } from "../../../components/ui/SectionCard";
import { ShieldAlert, DollarSign } from "lucide-react";

interface CostLimitSettingsProps {
  settings: ExtendedSettingsPayload;
  onChange: (updates: Partial<ExtendedSettingsPayload>) => void;
}

export const CostLimitSettings: React.FC<CostLimitSettingsProps> = ({ settings, onChange }) => {
  return (
    <SectionCard
      title="Controle de Escalonamento e Orçamentos"
      description="Políticas de interrupção síncronas para mitigar ataques de loop de faturamento e limites estourados."
      id="cost-limit-settings"
    >
      <div className="space-y-4 font-sans text-xs">
        {/* Monthly Budget input */}
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-zinc-400 font-medium block">Teto Mensal Operacional (USD)</label>
            <div className="relative">
              <div className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-550 font-mono font-bold">$</div>
              <input
                type="number"
                value={settings.monthlyBudgetUsd}
                onChange={(e) => onChange({ monthlyBudgetUsd: parseFloat(e.target.value) || 0 })}
                className="w-full bg-[#161619] border border-[#262626] rounded-xl pl-8 pr-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
                placeholder="e.g. 200.00"
              />
            </div>
            <span className="text-[10px] text-zinc-550 block font-sans">
              Orçamento alvo global em dólar abrangendo todas as zonas do Cloudflare Worker API.
            </span>
          </div>

          <div className="space-y-1.5">
            <label className="text-zinc-400 font-medium block">Limite Diário de Tokens de IA por Usuário</label>
            <input
              type="number"
              value={settings.maxAiTokensUserDaily ?? 150000}
              onChange={(e) => onChange({ maxAiTokensUserDaily: parseInt(e.target.value) || 150000 })}
              className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
              placeholder="e.g. 150000"
            />
            <span className="text-[10px] text-zinc-550 block font-sans">
              Evita abusos de quota de inferência de IA (laudos/pareceres) por IP ou dispositivo único nas últimas 24 horas.
            </span>
          </div>
        </div>

        {/* Action Taken on Limit Select */}
        <div className="space-y-1.5">
          <label className="text-zinc-400 font-medium block">Ação do Gateway ao Estourar o Teto</label>
          <div className="relative">
            <select
              value={settings.budgetAction}
              onChange={(e) => onChange({ budgetAction: e.target.value as "block" | "alert" | "throttle" })}
              className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-sans text-xs focus:outline-none focus:border-purple-500 transition-colors cursor-pointer"
            >
              <option value="alert">Apenas alertar via webhook operacional</option>
              <option value="throttle">Gargalar Latência (Atrasa respostas em 2s para desencorajar flood)</option>
              <option value="block">Bloquear síncronamente (Retorna Erro 429 para requisições extras)</option>
            </select>
          </div>
          <span className="text-[10px] text-zinc-550 block font-mono">
            Recomendação: Gargalar / Bloquear em Produção para mitigar custos inesperados do Gemini API ou OpenAI.
          </span>
        </div>

        {/* Tactical indicator widget */}
        <div className="p-3 bg-red-950/10 border border-red-500/10 rounded-xl flex items-start gap-2.5">
          <ShieldAlert className="w-4 h-4 text-[#FF4D4F] shrink-0 mt-0.5" />
          <div className="text-[10.5px] leading-snug text-neutral-400">
            Qualquer alteração acima atualiza síncronamente as tabelas de roteamento e limites de quota em milissegundos nos clusters globais.
          </div>
        </div>
      </div>
    </SectionCard>
  );
};
