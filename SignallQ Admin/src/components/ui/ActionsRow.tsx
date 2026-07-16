import React from "react";
import { ArrowRight } from "lucide-react";

export interface ActionItem {
  label: string;
  onClick: () => void;
  variant?: "primary" | "secondary";
}

/**
 * GH#552 (Fase 2) — passo final do padrão obrigatório do wireframe: "nunca tela
 * sem ação". Lista curta de próximos passos concretos (navegação ou operação),
 * nunca decorativa.
 */
interface ActionsRowProps {
  actions: ActionItem[];
  id?: string;
}

export const ActionsRow: React.FC<ActionsRowProps> = ({ actions, id }) => {
  if (actions.length === 0) return null;

  return (
    <div id={id || "actions-row"} className="flex flex-wrap items-center gap-3">
      {actions.map((action, idx) => (
        <button
          key={idx}
          type="button"
          onClick={action.onClick}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-sans font-semibold transition-all cursor-pointer"
          style={
            action.variant === "secondary"
              ? {
                  backgroundColor: "var(--bg-surface)",
                  border: "1px solid var(--border)",
                  color: "var(--text-secondary)",
                }
              : {
                  backgroundColor: "var(--primary)",
                  color: "var(--on-primary)",
                }
          }
        >
          <span>{action.label}</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      ))}
    </div>
  );
};
