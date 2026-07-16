import React from "react";
import { Brain } from "lucide-react";

interface AiCostSummaryCardProps {
  aiCostMonthLabel: string | null;
  onNavigate: (path: string) => void;
}

// Card isolado "Custo de IA · mês" — spec Lia, Md3DashboardContent.dc.html:78-86.
// Fica fora do grid de KPIs da seção "App" (decisão do protótipo: "não
// misturado com métricas de produto"). Ícone "psychology" do protótipo (Material
// Symbols) vira Brain (lucide-react) — resto do Console usa Lucide, Material
// Symbols é a exceção já sinalizada como bug em Sidebar.tsx (achado #5 da
// auditoria), não repetir aqui. No mobile o CTA vira texto informativo (sem
// link) — Md3DashboardContentMobile.dc.html:63-70 não tem "Ver IA & Custos →".
export const AiCostSummaryCard: React.FC<AiCostSummaryCardProps> = ({ aiCostMonthLabel, onNavigate }) => {
  return (
    <div
      className="rounded-[var(--radius-card)] px-5 py-4 flex items-center gap-5"
      style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      id="overview-ai-cost-summary-card"
    >
      <Brain className="w-5 h-5 shrink-0" style={{ color: "var(--attention)" }} />
      <div className="flex-1 min-w-0">
        <div
          className="text-[11px] font-sans font-semibold uppercase tracking-[0.08em]"
          style={{ color: "var(--text-secondary)" }}
        >
          Custo de IA · mês
        </div>
        <div className="text-xl font-sans font-medium mt-0.5" style={{ color: "var(--text-primary)" }}>
          {aiCostMonthLabel ?? "Não disponível"}
        </div>
      </div>
      <button
        type="button"
        onClick={() => onNavigate("/ai-cost")}
        className="hidden lg:inline-flex items-center min-h-[44px] text-xs font-sans font-semibold whitespace-nowrap"
        style={{ color: "var(--primary)" }}
      >
        Ver IA &amp; Custos →
      </button>
    </div>
  );
};
