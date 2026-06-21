import React from "react";
import { ExtendedSettingsPayload } from "../../../services/adminSettingsService";
import { SectionCard } from "../../../components/ui/SectionCard";
import { Coins } from "lucide-react";

interface MonetizationSettingsProps {
  settings: ExtendedSettingsPayload;
  onChange: (updates: Partial<ExtendedSettingsPayload>) => void;
}

const CATEGORY_OPTIONS = [
  { key: "telefonia", name: "Serviços e Planos de Telefonia" },
  { key: "hardware", name: "Roteadores e Certificadoras de Hardware" },
  { key: "streaming", name: "Servidores e Assinaturas de Streaming de Mídia" },
  { key: "jogos", name: "Aceleradores de Jogos e VPNs móveis" },
  { key: "operadoras", name: "Upgrade para Fibra de Banda Larga Fixa" },
];

export const MonetizationSettings: React.FC<MonetizationSettingsProps> = ({
  settings,
  onChange,
}) => {
  const activeCategories = settings.contextualAdsCategories || [];

  const handleToggleCategory = (categoryKey: string) => {
    let updated: string[];
    if (activeCategories.includes(categoryKey)) {
      updated = activeCategories.filter((c) => c !== categoryKey);
    } else {
      updated = [...activeCategories, categoryKey];
    }
    onChange({ contextualAdsCategories: updated });
  };

  return (
    <SectionCard
      title="Monetização e Plataforma Ad Tech"
      description="Políticas experimentais para monetização contextual inteligente sem agredir a privacidade do usuário."
      id="monetization-ads-settings"
    >
      <div className="space-y-4 font-sans text-xs">
        {/* Toggle contextual ads checkbox */}
        <div className="flex items-start gap-4 p-4 bg-zinc-950 border border-zinc-900 rounded-xl select-none">
          <input
            type="checkbox"
            id="contextualAdsEnabled"
            checked={settings.contextualAdsEnabled ?? false}
            onChange={(e) => onChange({ contextualAdsEnabled: e.target.checked })}
            className="mt-0.5 rounded text-[#6C2BFF] bg-[#161619] border-[#262626] focus:ring-[#6C2BFF] cursor-pointer h-4 w-4 shrink-0"
          />
          <div className="space-y-0.5">
            <label
              htmlFor="contextualAdsEnabled"
              className="text-white font-bold cursor-pointer flex items-center gap-1.5"
            >
              <Coins className="w-4 h-4 text-amber-500 shrink-0" />
              <span>Habilitar Veiculação de Anúncios Contextuais (Futuros)</span>
            </label>
            <span className="text-[10.5px] text-zinc-400 block leading-relaxed">
              Ativa o escopo de preparação Ad-Hub para que o app envie bandeiras de problemas de rádio no Android e receba ofertas locais (ex: roteador novo quando Wi-Fi acusa canal saturado).
            </span>
          </div>
        </div>

        {/* Categories selector if ads is enabled */}
        <div className="space-y-2">
          <label className="text-zinc-400 font-bold block">Categorias Permitidas para Monetização</label>
          <p className="text-[10px] text-zinc-550 leading-relaxed block">
            Selecione quais segmentos de mercado podem oferecer recomendações aos usuários com base em falhas detectadas síncronas.
          </p>

          <div className="space-y-1.5 pt-1">
            {CATEGORY_OPTIONS.map((cat) => {
              const isChecked = activeCategories.includes(cat.key);
              return (
                <label
                  key={cat.key}
                  className="flex items-center gap-3 p-3 bg-[#111115] border border-zinc-900 rounded-xl hover:border-zinc-800 transition-all cursor-pointer select-none"
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    disabled={!(settings.contextualAdsEnabled)}
                    onChange={() => handleToggleCategory(cat.key)}
                    className="rounded text-purple-600 bg-zinc-950 border-zinc-800 focus:ring-purple-500 h-3.5 w-3.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                  />
                  <span className={`text-[11px] font-medium font-sans ${isChecked && settings.contextualAdsEnabled ? "text-purple-300 font-bold" : "text-zinc-400"}`}>
                    {cat.name} <span className="font-mono text-[9px] text-zinc-600 font-normal">({cat.key})</span>
                  </span>
                </label>
              );
            })}
          </div>
        </div>
      </div>
    </SectionCard>
  );
};
