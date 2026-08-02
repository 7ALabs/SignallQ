import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrandEndorsement } from './BrandEndorsement'

describe('BrandEndorsement', () => {
  it("renderiza o texto 'by Buildea' na variante padrão", () => {
    render(<BrandEndorsement />)
    expect(screen.getByText('by')).toBeInTheDocument()
    expect(screen.getByText('Buildea')).toBeInTheDocument()
  })

  it('resolve o símbolo oficial da Buildea quando variant=symbol-text, sem exigir symbolSrc', () => {
    const { container } = render(<BrandEndorsement variant="symbol-text" />)
    const img = container.querySelector('img')
    expect(img).not.toBeNull()
    expect(img).toHaveAttribute('src', '/brand/buildea-symbol.png')
  })

  it('usa o mesmo símbolo independente de isDark (já vem com fundo próprio embutido)', () => {
    const { container: darkContainer } = render(<BrandEndorsement variant="symbol-text" isDark />)
    const { container: lightContainer } = render(<BrandEndorsement variant="symbol-text" isDark={false} />)
    expect(darkContainer.querySelector('img')).toHaveAttribute('src', '/brand/buildea-symbol.png')
    expect(lightContainer.querySelector('img')).toHaveAttribute('src', '/brand/buildea-symbol.png')
  })

  it('mantém a proporção real do símbolo (praticamente quadrado, 408x408) via altura fixa e largura auto', () => {
    const { container } = render(<BrandEndorsement variant="symbol-text" isDark />)
    const img = container.querySelector('img') as HTMLImageElement
    expect(img.style.height).toBe('16px')
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
