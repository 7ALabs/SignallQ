import React from "react";
import { ExtendedSettingsPayload } from "../../../services/adminSettingsService";
import { SectionCard } from "../../../components/ui/SectionCard";
import { ShieldAlert } from "lucide-react";

interface CostLimitSettingsProps {
  settings: ExtendedSettingsPayload;
  onChange: (updates: Partial<ExtendedSettingsPayload>) => void;
}

export const CostLimitSettings: React.FC<CostLimitSettingsProps> = ({ settings, onChange }) => {
  return (
    <SectionCard
      title="Limiares de Alerta (GET /admin/metrics/alerts)"
      description="Estes três valores são lidos diretamente pelo worker a cada consulta de alertas — alterar aqui muda o comportamento real do painel de alertas."
      id="cost-limit-settings"
    >
      <div className="space-y-4 font-sans text-xs">
        <div className="space-y-1.5">
          <label className="text-[var(--text-secondary)] font-medium block">Orçamento diário de IA (USD)</label>
          <div className="relative">
            <div className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--text-tertiary)] font-mono font-bold">$</div>
            <input
              type="number"
              step="0.01"
              min="0"
              value={settings.aiDailyBudgetUsd}
              onChange={(e) => onChange({ aiDailyBudgetUsd: Math.max(0, parseFloat(e.target.value) || 0) })}
              className="w-full bg-[var(--bg-base)] border border-[var(--border)] rounded-xl pl-8 pr-3.5 py-2.5 text-[var(--text-primary)] font-mono text-xs focus:outline-none focus:border-[var(--text-tertiary)] transition-colors"
              placeholder="e.g. 1.00"
            />
          </div>
          <span className="text-[10px] text-[var(--text-tertiary)] block font-sans">
            Custo de IA acumulado nas últimas 24h acima deste valor dispara o alerta crítico <code>AI_BUDGET</code>.
          </span>
        </div>

        <div className="space-y-1.5">
          <label className="text-[var(--text-secondary)] font-medium block">Limite de erros por hora</label>
          <input
            type="number"
            min="1"
            step="1"
            value={settings.errorSpikeThreshold}
            onChange={(e) => onChange({ errorSpikeThreshold: Math.max(1, parseInt(e.target.value, 10) || 1) })}
            className="w-full bg-[var(--bg-base)] border border-[var(--border)] rounded-xl px-3.5 py-2.5 text-[var(--text-primary)] font-mono text-xs focus:outline-none focus:border-[var(--text-tertiary)] transition-colors"
            placeholder="e.g. 10"
          />
          <span className="text-[10px] text-[var(--text-tertiary)] block font-sans">
            Erros registrados na última hora acima deste valor disparam o alerta de aviso <code>ERROR_SPIKE</code>.
          </span>
        </div>

        <div className="space-y-1.5">
          {/* #880 (achado 19): rótulo dizia "crítico", mas o alerta correspondente
              (LOW_SCORE, ver texto abaixo) é "warning" no worker — nunca "critical". */}
          <label className="text-[var(--text-secondary)] font-medium block">Score mínimo de alerta (0-100)</label>
          <input
            type="number"
            min="0"
            max="100"
            step="1"
            value={settings.criticalScoreThreshold}
            onChange={(e) => onChange({ criticalScoreThreshold: Math.min(100, Math.max(0, parseInt(e.target.value, 10) || 0)) })}
            className="w-full bg-[var(--bg-base)] border border-[var(--border)] rounded-xl px-3.5 py-2.5 text-[var(--text-primary)] font-mono text-xs focus:outline-none focus:border-[var(--text-tertiary)] transition-colors"
            placeholder="e.g. 50"
          />
          <span className="text-[10px] text-[var(--text-tertiary)] block font-sans">
            Score médio de diagnósticos nas últimas 24h abaixo deste valor dispara o alerta de aviso <code>LOW_SCORE</code>.
          </span>
        </div>

        <div className="p-3 bg-red-950/10 border border-red-500/10 rounded-xl flex items-start gap-2.5">
          <ShieldAlert className="w-4 h-4 text-[var(--error)] shrink-0 mt-0.5" />
          <div className="text-[10.5px] leading-snug text-[var(--text-secondary)]">
            Persistir estes valores altera imediatamente os thresholds usados pelo worker na próxima consulta de alertas — sem deploy necessário.
          </div>
        </div>
      </div>
    </SectionCard>
  );
};
