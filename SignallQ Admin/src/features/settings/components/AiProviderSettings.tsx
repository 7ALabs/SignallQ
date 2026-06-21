import React from "react";
import { ExtendedSettingsPayload } from "../../../services/adminSettingsService";
import { SectionCard } from "../../../components/ui/SectionCard";
import { Sparkles, HelpCircle, HardDrive, ShieldAlert } from "lucide-react";
import { AiProvider } from "../../../types/ai";

interface AiProviderSettingsProps {
  settings: ExtendedSettingsPayload;
  onChange: (updates: Partial<ExtendedSettingsPayload>) => void;
}

export const AiProviderSettings: React.FC<AiProviderSettingsProps> = ({ settings, onChange }) => {
  return (
    <SectionCard
      title="Roteamento de Provedores de IA"
      description="Orquestração inteligente de LLMs para pareces consultivos e triagem técnica na borda."
      id="ai-provider-settings"
    >
      <div className="space-y-4 font-sans text-xs">
        {/* Model Selector */}
        <div className="space-y-1.5ClassName">
          <label className="text-zinc-400 font-medium block">Modelo Principal de Linguagem</label>
          <div className="relative">
            <select
              value={settings.selectedDefaultAiModel}
              onChange={(e) => onChange({ selectedDefaultAiModel: e.target.value as AiProvider })}
              className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors cursor-pointer"
            >
              <option value="gemini_flash">Google Gemini 1.5 Flash (Altamente Recomendado)</option>
              <option value="cloudflare_qwen">Qwen 2.5 on Workers AI (Open-source Latência Média)</option>
              <option value="openai">OpenAI GPT-4o Mini (Faturamento síncrono complementar)</option>
              <option value="local_fallback">Offline Local Fallback (Regras Estáticas)</option>
            </select>
          </div>
          <span className="text-[10px] text-zinc-550 block font-mono">
            O Gemini 1.5 Flash opera com fração de centavo e entrega maior correlação para diagnósticos de RF.
          </span>
        </div>

        {/* Max Tokens input */}
        <div className="space-y-1.5">
          <label className="text-zinc-400 font-medium block">Limite de Tokens de Resposta por Laudo</label>
          <input
            type="number"
            value={settings.maxTokensPerDiagnostic}
            onChange={(e) => onChange({ maxTokensPerDiagnostic: parseInt(e.target.value) || 2048 })}
            className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
            placeholder="e.g. 4096"
          />
          <span className="text-[10px] text-zinc-550 block font-sans">
            Limita o tamanho máximo do laudo para evitar desperdício financeiro de buffers longos.
          </span>
        </div>

        {/* Fallback Checkbox */}
        <div className="flex items-start gap-3 p-3.5 bg-zinc-950 border border-[#232326] rounded-xl select-none">
          <input
            type="checkbox"
            id="aiFallbackEnabled"
            checked={settings.aiFallbackEnabled}
            onChange={(e) => onChange({ aiFallbackEnabled: e.target.checked })}
            className="mt-0.5 rounded text-purple-600 bg-[#161619] border-[#262626] focus:ring-purple-500 cursor-pointer h-4 w-4"
          />
          <div>
            <label htmlFor="aiFallbackEnabled" className="text-white font-semibold cursor-pointer block select-none">
              Ativar Fallback Inteligente de DNS
            </label>
            <span className="text-[10.5px] text-zinc-400 block mt-0.5 leading-snug">
              Caso o cluster do provedor principal registre erro ou timeout, o SDK Android migra síncronamente para o próximo modelo disponível para evitar falha ao usuário final.
            </span>
          </div>
        </div>
      </div>
    </SectionCard>
  );
};
