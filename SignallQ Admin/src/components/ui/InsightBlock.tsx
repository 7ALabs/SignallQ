import React from "react";
import { Lightbulb } from "lucide-react";

/**
 * GH#552 (Fase 2) — "bloco de explicação" do padrão obrigatório do wireframe:
 * fica entre o gráfico principal e a tabela de investigação, traduzindo o que
 * o gráfico está dizendo em texto corrido, antes do usuário cair no drill-down.
 * Recebe só texto derivado de dado real já carregado na tela — nunca reinventa
 * métrica aqui dentro (ver `docs_ai/design-system/WIREFRAME_ADMIN_REDESIGN_552.md`).
 */
interface InsightBlockProps {
  children: React.ReactNode;
  id?: string;
}

export const InsightBlock: React.FC<InsightBlockProps> = ({ children, id }) => {
  return (
    <div
      id={id || "insight-block"}
      className="flex items-start gap-3 rounded-[8px] p-4"
      style={{
        backgroundColor: "var(--bg-surface)",
        border: "1px solid var(--border)",
      }}
    >
      <Lightbulb className="w-4 h-4 mt-0.5 shrink-0" style={{ color: "var(--primary)" }} />
      <p className="text-xs leading-relaxed font-sans" style={{ color: "var(--text-secondary)" }}>
        {children}
      </p>
    </div>
  );
};
