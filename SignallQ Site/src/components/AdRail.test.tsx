import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AdRail } from './AdRail'

const ANUNCIOS_MOCK = [
  {
    id: 'house-1',
    title: 'A internet caiu de novo?',
    description: 'Mede o Wi-Fi de cada cômodo.',
    ctaLabel: 'Entrar na lista de teste',
    targetUrl: 'https://signallq.pages.dev',
  },
  {
    id: 'house-2',
    title: 'Todo Wi-Fi tem um ponto cego.',
    description: 'O SignallQ acha o seu em minutos.',
    ctaLabel: 'Quero testar',
    targetUrl: 'https://signallq.pages.dev/pro',
  },
]

describe('AdRail', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('não renderiza nada enquanto o catálogo local não resolveu (sem flash de placeholder)', () => {
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
    const { container } = render(<AdRail variant="a" />)
    expect(container).toBeEmptyDOMElement()
  })

  it('não renderiza nada quando o catálogo local está vazio', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads: [] }), { status: 200 }))
    const { container } = render(<AdRail variant="a" />)
    await waitFor(() => expect(container).toBeEmptyDOMElement())
  })

  it('variant "a" mostra os dois cards (grande + pequeno) sorteados do catálogo, com skin roxo', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads: ANUNCIOS_MOCK }), { status: 200 }))
    render(<AdRail variant="a" />)

    await waitFor(() => expect(screen.getAllByText('PUBLICIDADE').length).toBe(2))
    // Os dois títulos do catálogo aparecem em algum dos dois cards.
    const titulos = ANUNCIOS_MOCK.map((a) => a.title)
    const presentes = titulos.filter((t) => screen.queryByText(t))
    expect(presentes.length).toBeGreaterThan(0)
  })

  it('variant "b" renderiza sem quebrar (skin azul, ícone de roteador no card grande)', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads: ANUNCIOS_MOCK }), { status: 200 }))
    render(<AdRail variant="b" />)

    await waitFor(() => expect(screen.getAllByText('PUBLICIDADE').length).toBe(2))
    expect(screen.getByText('router')).toBeInTheDocument()
  })

  it('com catálogo de 1 item só, reaproveita o mesmo anúncio nos dois cards em vez de quebrar', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads: [ANUNCIOS_MOCK[0]] }), { status: 200 }))
    render(<AdRail variant="a" />)

    await waitFor(() => expect(screen.getAllByText('PUBLICIDADE').length).toBe(2))
    expect(screen.getAllByText(ANUNCIOS_MOCK[0].title).length).toBe(2)
  })
})
