import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrandEndorsement } from './BrandEndorsement'

describe('BrandEndorsement', () => {
  it("renderiza o texto 'by 7A' na variante padrão", () => {
    render(<BrandEndorsement />)
    expect(screen.getByText('by')).toBeInTheDocument()
    expect(screen.getByText('7A')).toBeInTheDocument()
  })

  it('resolve o símbolo real por tema quando variant=symbol-text, sem exigir symbolSrc', () => {
    const { container } = render(<BrandEndorsement variant="symbol-text" isDark />)
    const img = container.querySelector('img')
    expect(img).not.toBeNull()
    expect(img).toHaveAttribute('src', '/brand/7alabs-symbol-dark.svg')
  })

  it("troca o símbolo para a variante clara quando isDark=false", () => {
    const { container } = render(<BrandEndorsement variant="symbol-text" isDark={false} />)
    const img = container.querySelector('img')
    expect(img).toHaveAttribute('src', '/brand/7alabs-symbol-light.svg')
  })

  it('mantém a proporção real do símbolo (763x653, não quadrado) via altura fixa e largura auto', () => {
    const { container } = render(<BrandEndorsement variant="symbol-text" isDark />)
    const img = container.querySelector('img') as HTMLImageElement
    expect(img.style.height).toBe('14px')
    expect(img.style.width).toBe('auto')
  })

  it('renderiza símbolo decorativo (alt vazio, aria-hidden)', () => {
    const { container } = render(<BrandEndorsement variant="symbol-text" isDark />)
    const img = container.querySelector('img')
    expect(img).not.toBeNull()
    expect(img).toHaveAttribute('alt', '')
    expect(img).toHaveAttribute('aria-hidden', 'true')
  })

  it('aceita symbolSrc como override explícito do caminho padrão', () => {
    const { container } = render(
      <BrandEndorsement variant="symbol-text" symbolSrc="/brand/7a/custom.svg" />
    )
    const img = container.querySelector('img')
    expect(img).toHaveAttribute('src', '/brand/7a/custom.svg')
  })

  it('aceita id e className customizados', () => {
    const { container } = render(<BrandEndorsement id="footer-brand" className="mt-2" />)
    expect(container.querySelector('#footer-brand')).toHaveClass('mt-2')
  })
})
