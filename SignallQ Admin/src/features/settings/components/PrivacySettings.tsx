import React from "react";
import { ExtendedSettingsPayload } from "../../../services/adminSettingsService";
import { SectionCard } from "../../../components/ui/SectionCard";
import { ShieldAlert, Eye, Bell } from "lucide-react";

interface PrivacySettingsProps {
  settings: ExtendedSettingsPayload;
  onChange: (updates: Partial<ExtendedSettingsPayload>) => void;
}

export const PrivacySettings: React.FC<PrivacySettingsProps> = ({ settings, onChange }) => {
  return (
    <SectionCard
      title="Privacidade e alertas operacionais"
      description="Políticas de retenção, anonimização e webhooks operacionais da futura Admin API."
      id="privacy-noc-settings"
    >
      <div className="space-y-4 font-sans text-xs">
        {/* Retention days of logs */}
        <div className="space-y-1.5">
          <label className="text-zinc-400 font-medium block">Dias de retenção de telemetria</label>
          <input
            type="number"
            value={settings.retentionDays}
            onChange={(e) => onChange({ retentionDays: parseInt(e.target.value) || 30 })}
            className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
            placeholder="e.g. 30"
          />
          <span className="text-[10px] text-zinc-550 block font-sans">
            Define por quanto tempo eventos brutos de telemetria ficam disponíveis antes de serem agregados ou removidos.
          </span>
        </div>

        {/* Alert webhook URLs (Staging and Prod) */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <label className="text-zinc-400 font-medium block">Webhook de alerta (Staging)</label>
            <div className="relative">
              <Bell className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-500" />
              <input
                type="text"
                value={settings.stagingAlertWebhookUrl}
                onChange={(e) => onChange({ stagingAlertWebhookUrl: e.target.value })}
                className="w-full bg-[#161619] border border-[#262626] rounded-xl pl-8 pr-3 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
                placeholder="https://hooks.example.invalid/staging"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-zinc-400 font-medium block">Webhook de alerta (Produção)</label>
            <div className="relative">
              <Bell className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-500" />
              <input
                type="text"
                value={settings.productionAlertWebhookUrl}
                onChange={(e) => onChange({ productionAlertWebhookUrl: e.target.value })}
                className="w-full bg-[#161619] border border-[#262626] rounded-xl pl-8 pr-3 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
                placeholder="https://hooks.example.invalid/production"
              />
            </div>
          </div>
        </div>

        {/* Anonymize User IP checkboxes */}
        <div className="space-y-3">
          <div className="flex items-start gap-3 p-3.5 bg-zinc-950 border border-[#232326] rounded-xl select-none">
            <input
              type="checkbox"
              id="anonymizeIp"
              checked={settings.anonymizeIp}
              onChange={(e) => onChange({ anonymizeIp: e.target.checked })}
              className="mt-0.5 rounded text-purple-600 bg-[#161619] border-[#262626] focus:ring-purple-500 cursor-pointer h-4 w-4"
            />
            <div>
              <label htmlFor="anonymizeIp" className="text-white font-semibold cursor-pointer block select-none">
                Anonimizar Endereços IP dos Dispositivos
              </label>
              <span className="text-[10.5px] text-zinc-400 block mt-0.5 leading-snug font-sans">
                Remove granularidade desnecessária de IP antes de persistir eventos técnicos agregados.
              </span>
            </div>
          </div>

          <div className="flex items-start gap-3 p-3.5 bg-zinc-950 border border-[#232326] rounded-xl select-none">
            <input
              type="checkbox"
              id="firebaseAnalyticsEnabled"
              checked={settings.firebaseAnalyticsEnabled ?? true}
              onChange={(e) => onChange({ firebaseAnalyticsEnabled: e.target.checked })}
              className="mt-0.5 rounded text-purple-600 bg-[#161619] border-[#262626] focus:ring-purple-500 cursor-pointer h-4 w-4"
            />
            <div>
              <label htmlFor="firebaseAnalyticsEnabled" className="text-white font-semibold cursor-pointer block select-none">
                Enviar Analytics de Comportamento para Firebase
              </label>
              <span className="text-[10.5px] text-zinc-400 block mt-0.5 leading-snug font-sans">
                Permite eventos agregados de navegação, uso e engajamento para Product Analytics, sem dados pessoais identificáveis.
              </span>
            </div>
          </div>
        </div>
      </div>
    </SectionCard>
  );
};
