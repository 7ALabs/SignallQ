import React from "react";

/**
 * Paridade com o mockup de referência do Luiz (signallq-admin-mockup.dc.html):
 * cada uma das 9 telas abre com overline > H1 em forma de pergunta > parágrafo
 * descritivo > linha mono "FONTE(S) · ...". Copy é literal do mockup — não
 * parafrasear. Usado uma vez no topo de cada Page, antes de GlobalFilters/KPIs.
 */
interface SectionIntroProps {
  overline: string;
  question: string;
  description: string;
  /** Ausente apenas em Configurações — o mockup não lista fonte para essa tela. */
  source?: string;
  id?: string;
}

export const SectionIntro: React.FC<SectionIntroProps> = ({
  overline,
  question,
  description,
  source,
  id,
}) => {
  return (
    <div id={id} className="mb-2">
      <div
        className="text-[11px] font-sans font-semibold uppercase tracking-[0.08em]"
        style={{ color: "var(--text-tertiary)" }}
      >
        {overline}
      </div>
      <h1
        className="text-[24px] font-sans font-medium leading-[1.25] tracking-[-0.02em] mt-1.5 mb-1.5"
        style={{ color: "var(--text-primary)" }}
      >
        {question}
      </h1>
      <p className={`text-[13px] leading-relaxed max-w-[640px] ${source ? "mb-1.5" : "mb-4"}`} style={{ color: "var(--text-secondary)" }}>
        {description}
      </p>
      {source && (
        <div
          className="text-[10px] font-sans font-semibold uppercase tracking-[0.04em]"
          style={{ color: "var(--text-tertiary)" }}
        >
          {source}
        </div>
      )}
    </div>
  );
};
