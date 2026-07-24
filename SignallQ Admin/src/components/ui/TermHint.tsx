import React from "react";
import { HelpCircle } from "lucide-react";
import { TERM_GLOSSARY } from "../../config/termGlossary";

interface TermHintProps {
  /** Chave em `termGlossary.ts` — mesmo termo usado em telas diferentes aponta pro mesmo verbete. */
  term: keyof typeof TERM_GLOSSARY | string;
  id?: string;
}

/**
 * GH#1341 — item 2.2.2 do plano de UX: ícone "?" pequeno ao lado de rótulo de termo técnico.
 * Ao interagir (hover/foco), mostra descrição funcional curta, sem jargão do próprio termo.
 */
export const TermHint: React.FC<TermHintProps> = ({ term, id }) => {
  const description = TERM_GLOSSARY[term];
  if (!description) return null;

  return (
    <span id={id} className="relative inline-flex items-center group/hint ml-1 align-middle" tabIndex={0}>
      <HelpCircle
        className="w-3 h-3 cursor-help"
        style={{ color: "var(--text-tertiary)" }}
        aria-hidden="true"
      />
      <span className="sr-only">{description}</span>
      <span
        role="tooltip"
        className="pointer-events-none absolute z-20 hidden group-hover/hint:block group-focus/hint:block bottom-full left-1/2 -translate-x-1/2 mb-1.5 w-56 text-[11px] leading-snug font-sans normal-case font-normal p-2.5 rounded-[var(--radius-button)] shadow-lg"
        style={{
          backgroundColor: "var(--bg-surface)",
          border: "1px solid var(--border)",
          color: "var(--text-secondary)",
        }}
      >
        {description}
      </span>
    </span>
  );
};
