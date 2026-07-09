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
  /** Bloco de identidade do produto — só usado no Centro de Controle (Overview). */
  hero?: {
    iconSrc: string;
    title: string;
    subtitle: string;
  };
  id?: string;
}

export const SectionIntro: React.FC<SectionIntroProps> = ({
  overline,
  question,
  description,
  source,
  hero,
  id,
}) => {
  return (
    <div id={id} className="mb-2">
      {hero && (
        <div className="flex items-center gap-4 mb-5">
          <img
            src={hero.iconSrc}
            alt=""
            className="w-14 h-14 rounded-2xl shrink-0 object-cover"
            draggable={false}
          />
          <div>
            <div className="text-[22px] font-sans font-bold leading-tight" style={{ color: "var(--text-primary)" }}>
              {hero.title}
            </div>
            <div className="text-[13px] mt-0.5" style={{ color: "var(--text-secondary)" }}>
              {hero.subtitle}
            </div>
          </div>
        </div>
      )}

      <div
        className="text-[11px] font-sans font-semibold uppercase tracking-[0.08em]"
        style={{ color: "var(--text-tertiary)" }}
      >
        {overline}
      </div>
      <h1
        className="text-[26px] font-sans font-bold leading-[1.25] tracking-[-0.02em] mt-1.5 mb-1.5"
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
