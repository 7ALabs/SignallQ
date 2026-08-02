import React from "react";

export type BrandEndorsementVariant = "text" | "symbol-text";
export type BrandEndorsementSize = "compact" | "default";
export type BrandEndorsementTheme = "dark" | "light";

// GH#1376 (símbolo original "7A", legado) + rebrand institucional 7A Labs → Buildea
// (2026-07-29, ver `_workspace/docs/decisions/DECISAO_REBRAND_BUILDEA_2026-07-29.md`).
// Símbolo oficial da Buildea (monograma "iB", preto/branco/amarelo) extraído do avatar
// real da org no GitHub (`buildea-labs`, fonte: `brand/buildea-symbol.png` na raiz do
// monorepo) — já vem com fundo preto opaco embutido (não é um traço vetorial soltável
// como o símbolo "7A" antigo), por isso um único asset serve pra tema claro e escuro
// do Console (o próprio símbolo carrega seu fundo, não depende do fundo da tela).
const BUILDEA_SYMBOL_SRC = `${import.meta.env.BASE_URL}brand/buildea/symbol.png`;

interface BrandEndorsementProps {
  /** "text" — só "by Buildea". "symbol-text" — símbolo (via `symbolSrc`) + "by Buildea". */
  variant?: BrandEndorsementVariant;
  size?: BrandEndorsementSize;
  /**
   * Tema ativo do Console (`"dark" | "light"`), mesma convenção já usada por
   * `Sidebar`/`NavRail`/`BottomNav`/`Topbar` (prop `theme` vinda de `useTheme()` em `App.tsx`).
   * Quando omitido, o componente detecta sozinho via `data-theme` do documento (aplicado por
   * `useTheme()`) ou, na ausência dele, `prefers-color-scheme`.
   */
  theme?: BrandEndorsementTheme;
  /** Override do caminho do símbolo. Sem isso, usa o símbolo oficial da Buildea. */
  symbolSrc?: string;
  className?: string;
  id?: string;
}

/**
 * Assinatura institucional "by Buildea" (rebrand 2026-07-29, sucede o "by 7A"
 * de GH#1376 — ver `_workspace/docs/decisions/DECISAO_REBRAND_BUILDEA_2026-07-29.md`).
 * Endosso discreto, nunca lockup completo "Buildea" em tela operacional.
 *
 * Uso: superfícies institucionais (login, rodapé, Sobre/versão) — nunca em
 * dashboards, tabelas ou telas operacionais repetidas.
 */
export const BrandEndorsement: React.FC<BrandEndorsementProps> = ({
  variant = "text",
  size = "default",
  symbolSrc,
  className = "",
  id,
}) => {
  const showSymbol = variant === "symbol-text";
  const resolvedSymbolSrc = symbolSrc ?? BUILDEA_SYMBOL_SRC;
  const fontSize = size === "compact" ? "10px" : "11px";
  // Símbolo é praticamente quadrado (408x408) — altura fixa por tamanho, largura
  // livre (`auto`) para o navegador preservar a proporção intrínseca do PNG.
  const symbolHeight = size === "compact" ? "14px" : "16px";

  return (
    <span
      id={id}
      className={`inline-flex items-center gap-1 select-none font-sans leading-none ${className}`}
      style={{ color: "var(--sq-text-tertiary)" }}
    >
      {showSymbol && (
        // Decorativo — o texto ao lado já carrega o significado, então o
        // símbolo fica oculto de leitor de tela (alt vazio + aria-hidden).
        <img
          src={resolvedSymbolSrc}
          alt=""
          aria-hidden="true"
          className="shrink-0"
          style={{ height: symbolHeight, width: "auto", borderRadius: "3px" }}
          draggable={false}
        />
      )}
      <span style={{ fontSize }}>
        <span style={{ fontWeight: 400 }}>by</span>{" "}
        <span style={{ fontWeight: 700, letterSpacing: "0.02em" }}>Buildea</span>
      </span>
    </span>
  );
};
