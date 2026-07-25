import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { SiteFooter } from './SiteFooter'
import { SIGNALLQ_TEST_GROUP_URL } from '../lib/config'

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

    expect(screen.getAllByText('Beta').length).toBeGreaterThanOrEqual(2)
  })

  it('coluna "Baixe o app" (full) é o card de anúncio 1:1 do protótipo, com direcionamento real pro grupo de teste fechado', () => {
    render(
      <MemoryRouter>
        <SiteFooter />
      </MemoryRouter>
    )

    expect(screen.getByText('Baixe o app SignallQ')).toBeInTheDocument()
    expect(screen.getByText('Mede o Wi-Fi de cada cômodo e mostra onde o sinal morre.')).toBeInTheDocument()
    expect(screen.getByText('Teste fechado · Android')).toBeInTheDocument()

    const cta = screen.getByRole('link', { name: 'Entrar na lista de teste' })
    expect(cta).toHaveAttribute('href', SIGNALLQ_TEST_GROUP_URL)
    expect(cta).toHaveAttribute('target', '_blank')
    expect(cta).toHaveAttribute('rel', 'noopener noreferrer')
  })
})
