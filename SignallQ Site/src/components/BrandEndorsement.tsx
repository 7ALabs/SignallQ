export type BrandEndorsementVariant = 'text' | 'symbol-text'
export type BrandEndorsementSize = 'compact' | 'default'

interface BrandEndorsementProps {
  /**
   * 'text' — só "by 7A". 'symbol-text' — símbolo 7A + "by 7A".
   * GH#1376: o asset vetorial do símbolo 7A ainda não existe no repo nem em
   * `7ALabs/.github` — enquanto `symbolSrc` não for informado, 'symbol-text'
   * cai automaticamente para 'text' em vez de inventar/recriar o símbolo à
   * mão (mesma regra de `brand/README.md`).
   */
  variant?: BrandEndorsementVariant
  size?: BrandEndorsementSize
  /** Caminho do símbolo 7A vetorial (SVG/PNG), quando existir. Decorativo — nunca leva alt/aria própria. */
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
  symbolSrc,
  className = '',
  id,
}: BrandEndorsementProps) {
  const showSymbol = variant === 'symbol-text' && Boolean(symbolSrc)
  const fontSize = size === 'compact' ? '10px' : '11px'
  const symbolSize = size === 'compact' ? '12px' : '14px'

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
          src={symbolSrc}
          alt=""
          aria-hidden="true"
          className="shrink-0"
          style={{ width: symbolSize, height: symbolSize }}
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
