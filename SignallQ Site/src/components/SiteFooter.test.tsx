import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { SiteFooter } from './SiteFooter'

// Reconstrução v2 — 3 densidades responsivas (SiteFooter.dc.html). jsdom não aplica
// media query real, então as 3 densidades coexistem no DOM (visibilidade é só CSS) —
// o teste garante que o conteúdo de cada uma está presente, não a visibilidade.
describe('SiteFooter', () => {
  it('renderiza os links das 3 densidades (mobile/compact/full) e o copyright', () => {
    render(
      <MemoryRouter>
        <SiteFooter />
      </MemoryRouter>
    )

    // Copyright aparece em mais de uma densidade.
    expect(screen.getAllByText('© 2026 SignallQ · by 7A. Produto em fase Beta.').length).toBeGreaterThanOrEqual(2)

    // Link presente em todas as densidades (mobile+compact+full).
    expect(screen.getAllByText('Teste de velocidade').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByText('Política de Privacidade').length).toBeGreaterThanOrEqual(2)

    // CTA real de download (decisão da Lia — não é o card de anúncio animado do protótipo).
    expect(screen.getAllByText('Beta').length).toBeGreaterThanOrEqual(2)
  })
})
