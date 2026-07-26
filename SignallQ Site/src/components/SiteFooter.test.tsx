import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { SiteFooter } from './SiteFooter'
import { SIGNALLQ_TEST_GROUP_URL } from '../lib/config'

// Mock mínimo de IntersectionObserver (jsdom não implementa) — captura o
// callback pra disparar manualmente `isIntersecting` nos testes abaixo.
class MockIntersectionObserver {
  static instances: MockIntersectionObserver[] = []
  callback: IntersectionObserverCallback
  constructor(callback: IntersectionObserverCallback) {
    this.callback = callback
    MockIntersectionObserver.instances.push(this)
  }
  observe = vi.fn()
  disconnect = vi.fn()
  unobserve = vi.fn()
  trigger(isIntersecting: boolean) {
    this.callback([{ isIntersecting } as IntersectionObserverEntry], this as unknown as IntersectionObserver)
  }
}

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

    // Copyright aparece em mais de uma densidade — texto agora é composto
    // (prefixo + BrandEndorsement + sufixo), então cada fragmento é checado
    // separadamente em vez do texto corrido de antes.
    expect(screen.getAllByText('© 2026 SignallQ ·').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByText('. Produto em fase Beta.').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByText('by').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByText('7A').length).toBeGreaterThanOrEqual(2)

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

  describe('classe sq-footer-visible (fix da sobreposição com o PwaToastStack)', () => {
    afterEach(() => {
      document.documentElement.classList.remove('sq-footer-visible')
      MockIntersectionObserver.instances = []
    })

    it('publica sq-footer-visible no <html> quando o rodapé entra na viewport, e remove quando sai', () => {
      vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)

      const { unmount } = render(
        <MemoryRouter>
          <SiteFooter />
        </MemoryRouter>
      )

      expect(document.documentElement.classList.contains('sq-footer-visible')).toBe(false)

      const observer = MockIntersectionObserver.instances[0]
      observer.trigger(true)
      expect(document.documentElement.classList.contains('sq-footer-visible')).toBe(true)

      observer.trigger(false)
      expect(document.documentElement.classList.contains('sq-footer-visible')).toBe(false)

      observer.trigger(true)
      expect(document.documentElement.classList.contains('sq-footer-visible')).toBe(true)

      unmount()
      expect(document.documentElement.classList.contains('sq-footer-visible')).toBe(false)
    })
  })
})
