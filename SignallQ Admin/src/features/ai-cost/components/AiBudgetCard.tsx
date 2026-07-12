import React from "react";
import { formatCurrency, parseFormattedCurrencyBrl } from "../../../utils/format";

interface AiBudgetCardProps {
  /** Custo total do período selecionado, formatado em BRL (aiUsageService.getAiCostSummary). */
  totalCostUsd: string | null;
  /** Teto de alerta configurado em USD, DIÁRIO (errorMetricsService.getAiAlerts — mesmo aiDailyBudgetUsd usado pelo worker pro alerta AI_BUDGET). */
  ceilingUsd: number;
  /** Período selecionado no filtro global — só faz sentido comparar contra o teto diário quando period é "hoje"/"1d". */
  period: string;
}

// GH#781 (paridade mockup) — "Orçamento diário de IA" full-width, no topo da
// tela. O teto (ceilingUsd) é o mesmo valor real já usado para disparar
// alertas de custo (getAiAlerts) — não é um número novo inventado para esta
// barra, é o único teto de orçamento que o sistema hoje conhece (aiDailyBudgetUsd,
// diário — não mensal; #880 achado 5, corrigido o hardcode e o rótulo juntos).
// #880 achado 5 (parte 2): totalCostUsd reflete o período GLOBAL selecionado
// (7d/30d/etc.), não só hoje — comparar um total de 30 dias contra um teto
// diário fabricaria uma % sempre estourada e sem sentido. A barra de progresso
// só aparece quando period é "today"/"1d" (a única janela onde a comparação é
// honesta); nos demais períodos mostra o teto e o custo do período lado a lado,
// sem % inventada. Exibição em BRL (GH#781 ajuste fino 2) via utils/format.formatCurrency.
export const AiBudgetCard: React.FC<AiBudgetCardProps> = ({ totalCostUsd, ceilingUsd, period }) => {
  const isDailyWindow = period === "today" || period === "1d";
  const usedValueBrl = totalCostUsd ? parseFormattedCurrencyBrl(totalCostUsd) : null;
  const ceilingBrlFormatted = formatCurrency(ceilingUsd);
  const ceilingBrlValue = parseFormattedCurrencyBrl(ceilingBrlFormatted);
  const pct =
    isDailyWindow && usedValueBrl != null && ceilingBrlValue > 0
      ? Math.min(100, Math.round((usedValueBrl / ceilingBrlValue) * 100))
      : null;

  return (
    <div
      className="rounded-[var(--radius-card)] p-5 sq-card-hover"
      style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      id="ai-budget-card"
    >
      <div className="flex items-center justify-between gap-4 mb-2.5">
        <h4 className="text-[11px] font-semibold font-sans uppercase tracking-[0.08em]" style={{ color: "var(--text-secondary)" }}>
          Orçamento diário de IA
        </h4>
        <span className="text-[13px] font-semibold font-sans" style={{ color: "var(--text-primary)" }}>
          {totalCostUsd ?? "—"}
          <span style={{ color: "var(--text-tertiary)", fontWeight: 400 }}> / {ceilingBrlFormatted} por dia</span>
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
          ? `${pct}% do orçamento de hoje usado`
          : usedValueBrl != null
            ? "Teto é diário — selecione o período \"Hoje\" para comparar com o custo do dia."
            : "Sem dado de custo no período selecionado."}
      </p>
    </div>
  );
};
