import React from "react";

export type BrandEndorsementVariant = "text" | "symbol-text";
export type BrandEndorsementSize = "compact" | "default";
export type BrandEndorsementTheme = "dark" | "light";

// GH#1376: símbolo 7A vetorial recebido do Luiz (recortado das paths originais, viewBox
// `267 164 763 653` — não quadrado, ~1.17:1). Fonte: `brand/7alabs-symbol-{dark,light}.svg`,
// copiado para `public/brand/7a/` (mesmo padrão de subpasta por marca que `public/brand/signallq/`
// já usa). Resolução automática por tema — só passe `symbolSrc` para forçar um caminho específico.
const SYMBOL_SRC_BY_THEME: Record<BrandEndorsementTheme, string> = {
  dark: `${import.meta.env.BASE_URL}brand/7a/symbol-dark.svg`,
  light: `${import.meta.env.BASE_URL}brand/7a/symbol-light.svg`,
};

function detectTheme(): BrandEndorsementTheme {
  if (typeof document !== "undefined") {
    const attr = document.documentElement.getAttribute("data-theme");
    if (attr === "light" || attr === "dark") return attr;
  }
  if (typeof window !== "undefined" && window.matchMedia) {
    return window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
  }
  return "dark";
}

interface BrandEndorsementProps {
  /** "text" — só "by 7A". "symbol-text" — símbolo 7A + "by 7A". */
  variant?: BrandEndorsementVariant;
  size?: BrandEndorsementSize;
  /**
   * Tema ativo do Console (`"dark" | "light"`), mesma convenção já usada por
   * `Sidebar`/`NavRail`/`BottomNav`/`Topbar` (prop `theme` vinda de `useTheme()` em `App.tsx`).
   * Quando omitido, o componente detecta sozinho via `data-theme` do documento (aplicado por
   * `useTheme()`) ou, na ausência dele, `prefers-color-scheme`.
   */
  theme?: BrandEndorsementTheme;
  /** Override do caminho do símbolo 7A. Sem isso, resolve sozinho por tema — não precisa ser passado manualmente. */
  symbolSrc?: string;
  className?: string;
  id?: string;
}

/**
 * Assinatura institucional "by 7A" (GH#1376, decisão de marca aprovada —
 * ver `7ALabs/.github/BRAND.md`). Endosso discreto, nunca lockup completo
 * "7A Labs" em tela operacional.
 *
 * Uso: superfícies institucionais (login, rodapé, Sobre/versão) — nunca em
 * dashboards, tabelas ou telas operacionais repetidas.
 */
export const BrandEndorsement: React.FC<BrandEndorsementProps> = ({
  variant = "text",
  size = "default",
  theme,
  symbolSrc,
  className = "",
  id,
}) => {
  const showSymbol = variant === "symbol-text";
  const resolvedTheme = theme ?? detectTheme();
  const resolvedSymbolSrc = symbolSrc ?? SYMBOL_SRC_BY_THEME[resolvedTheme];
  const fontSize = size === "compact" ? "10px" : "11px";
  // Símbolo não é quadrado (viewBox 763x653, ~1.17:1) — altura fixa por tamanho, largura
  // livre (`auto`) para o navegador preservar a proporção intrínseca do SVG.
  const symbolHeight = size === "compact" ? "12px" : "14px";

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
          style={{ height: symbolHeight, width: "auto" }}
          draggable={false}
        />
      )}
      <span style={{ fontSize }}>
        <span style={{ fontWeight: 400 }}>by</span>{" "}
        <span style={{ fontWeight: 700, letterSpacing: "0.02em" }}>7A</span>
      </span>
    </span>
  );
};
