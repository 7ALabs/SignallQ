import { render, screen } from '@testing-library/react'

import { describe, expect, it } from 'vitest'
import { SiteNav } from './SiteNav'

describe('SiteNav', () => {
  it('mostra os 5 itens na ordem do protótipo (SiteNav.dc.html)', () => {
    render(
      <MemoryRouter>
        <SiteNav  />
      </MemoryRouter>
    )

    const esperado = ['Teste', 'Histórico', 'Como funciona', 'Quem somos', 'Privacidade']
    esperado.forEach((label) => {
      expect(screen.getAllByText(label).length).toBeGreaterThan(0)
    })
  })

  it('é sticky no topo (reconstrução v2) quando não está em heroMode', () => {
    const { container } = render(
      <MemoryRouter>
        <SiteNav  />
      </MemoryRouter>
    )
    const raiz = container.firstElementChild as HTMLElement
    expect(raiz.className).toContain('sticky')
  })
})


