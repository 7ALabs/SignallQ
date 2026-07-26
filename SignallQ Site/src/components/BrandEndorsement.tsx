export type BrandEndorsementVariant = 'text' | 'symbol-text'
export type BrandEndorsementSize = 'compact' | 'default'

// GH#1376: símbolo 7A vetorial recebido do Luiz (recortado das paths originais, viewBox
// `267 164 763 653` — não quadrado, ~1.17:1). Fonte: `brand/7alabs-symbol-{dark,light}.svg`,
// copiado para `public/brand/` (mesma pasta flat que já serve `signallq-lockup-*-bg.png`, ver
// `Logo.tsx`). Resolução automática por tema — só passe `symbolSrc` para forçar um caminho específico.
const SYMBOL_SRC_BY_THEME = {
  dark: '/brand/7alabs-symbol-dark.svg',
  light: '/brand/7alabs-symbol-light.svg',
} as const

interface BrandEndorsementProps {
  /** 'text' — só "by 7A". 'symbol-text' — símbolo 7A + "by 7A". */
  variant?: BrandEndorsementVariant
  size?: BrandEndorsementSize
  /**
   * Tema ativo do Site (`isDark` do `useSystemTheme()`), mesma convenção já usada por `Logo`
   * (recebe o booleano já resolvido do chamador em vez de detectar sozinho — evita duplicar o
   * listener de `matchMedia` que `useSystemTheme()` já registra). Default `false` (claro), igual
   * ao default de `Logo`.
   */
  isDark?: boolean
  /** Override do caminho do símbolo 7A. Sem isso, resolve sozinho por tema — não precisa ser passado manualmente. */
  symbolSrc?: string
  className?: string
  id?: string
}

/**
 * Assinatura institucional "by 7A" (GH#1376, decisão de marca aprovada —
 * ver `7ALabs/.github/BRAND.md`). Endosso discreto, nunca lockup completo
 * "7A Labs" em tela operacional.
 *
 * Mesmo contrato/props do `BrandEndorsement` do SignallQ Admin
 * (`SignallQ Admin/src/components/ui/BrandEndorsement.tsx`), reimplementado
 * aqui em vez de compartilhado via pacote — o Site já decidiu consumir o
 * design system via CSS puro (tokens.css), não via componentes React de
 * outro app (ver `SignallQ Site/CLAUDE.md`, "Decisões técnicas relevantes").
 *
 * Uso: superfícies institucionais (rodapé, páginas Quem somos/Privacidade/
 * Termos) — nunca repetida em todas as telas.
 */
export function BrandEndorsement({
  variant = 'text',
  size = 'default',
  isDark = false,
  symbolSrc,
  className = '',
  id,
}: BrandEndorsementProps) {
  const showSymbol = variant === 'symbol-text'
  const resolvedSymbolSrc = symbolSrc ?? SYMBOL_SRC_BY_THEME[isDark ? 'dark' : 'light']
  const fontSize = size === 'compact' ? '10px' : '11px'
  // Símbolo não é quadrado (viewBox 763x653, ~1.17:1) — altura fixa por tamanho, largura
  // livre (`auto`) para o navegador preservar a proporção intrínseca do SVG.
  const symbolHeight = size === 'compact' ? '12px' : '14px'

  return (
    <span
      id={id}
      className={`inline-flex items-center gap-1 select-none leading-none ${className}`}
      style={{ color: 'var(--text-tertiary)', fontFamily: 'var(--font-sans)' }}
    >
      {showSymbol && (
        // Decorativo — o texto ao lado já carrega o significado, então o
        // símbolo fica oculto de leitor de tela (alt vazio + aria-hidden).
        <img
          src={resolvedSymbolSrc}
          alt=""
          aria-hidden="true"
          className="shrink-0"
          style={{ height: symbolHeight, width: 'auto' }}
          draggable={false}
        />
      )}
      <span style={{ fontSize }}>
        <span style={{ fontWeight: 400 }}>by</span>{' '}
        <span style={{ fontWeight: 700, letterSpacing: '0.02em' }}>7A</span>
      </span>
    </span>
  )
}
