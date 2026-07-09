import React from "react";
import { formatCurrency, parseFormattedCurrencyBrl } from "../../../utils/format";

interface AiBudgetCardProps {
  /** Custo total do período, formatado em BRL (aiUsageService.getAiCostSummary — utils/format.formatCurrency). */
  totalCostUsd: string | null;
  /** Teto de alerta configurado em USD (errorMetricsService.getAiAlerts — mesmo valor usado nos alertas de IA). */
  ceilingUsd: number;
}

// GH#781 (paridade mockup) — "Orçamento mensal de IA" full-width, no topo da
// tela. O teto (ceilingUsd) é o mesmo valor real já usado para disparar
// alertas de custo (getAiAlerts) — não é um número novo inventado para esta
// barra, é o único teto de orçamento que o sistema hoje conhece. Exibição em
// BRL (GH#781 ajuste fino 2) via utils/format.formatCurrency.
export const AiBudgetCard: React.FC<AiBudgetCardProps> = ({ totalCostUsd, ceilingUsd }) => {
  const usedValueBrl = totalCostUsd ? parseFormattedCurrencyBrl(totalCostUsd) : null;
  const ceilingBrlFormatted = formatCurrency(ceilingUsd);
  const ceilingBrlValue = parseFormattedCurrencyBrl(ceilingBrlFormatted);
  const pct = usedValueBrl != null && ceilingBrlValue > 0 ? Math.min(100, Math.round((usedValueBrl / ceilingBrlValue) * 100)) : null;

  return (
    <div
      className="rounded-[var(--radius-card)] p-5 sq-card-hover"
      style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      id="ai-budget-card"
    >
      <div className="flex items-center justify-between gap-4 mb-2.5">
        <h4 className="text-[11px] font-semibold font-sans uppercase tracking-[0.08em]" style={{ color: "var(--text-secondary)" }}>
          Orçamento mensal de IA
        </h4>
        <span className="text-[13px] font-semibold font-sans" style={{ color: "var(--text-primary)" }}>
          {totalCostUsd ?? "—"}
          <span style={{ color: "var(--text-tertiary)", fontWeight: 400 }}> / {ceilingBrlFormatted}</span>
        </span>
      </div>
      <div className="h-2.5 rounded-full overflow-hidden" style={{ backgroundColor: "var(--bg-base)" }}>
        <div
          className="h-full rounded-full transition-all"
          style={{ width: `${pct ?? 0}%`, backgroundColor: pct != null && pct >= 90 ? "var(--error)" : "var(--primary)" }}
        />
      </div>
      <p className="text-[11px] mt-2" style={{ color: "var(--text-tertiary)" }}>
        {pct != null
          ? `${pct}% do orçamento usado no período · teto de alerta configurado no worker`
          : "Sem dado de custo no período selecionado."}
      </p>
    </div>
  );
};
