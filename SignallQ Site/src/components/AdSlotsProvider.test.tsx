import { act, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AdSlotsProvider, useAdSenseCoordination, useAdSlotItems, useAlwaysLocalAdSlot } from './AdSlotsProvider'

const CATALOGO_MOCK = [
  { id: 'a1', title: 'Título A', description: 'Desc A', ctaLabel: 'CTA A', targetUrl: 'https://a.example' },
  { id: 'a2', title: 'Título B', description: 'Desc B', ctaLabel: 'CTA B', targetUrl: 'https://b.example' },
  { id: 'a3', title: 'Título C', description: 'Desc C', ctaLabel: 'CTA C', targetUrl: 'https://c.example' },
]

function mockCatalogo(ads: typeof CATALOGO_MOCK) {
  vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads }), { status: 200 }))
}

// Componente de teste: 1 slot que pede N itens do catálogo e lista os títulos.
function SlotDeItens({ slotId, count }: { slotId: string; count: number }) {
  const itens = useAdSlotItems(slotId, count)
  return (
    <ul aria-label={slotId}>
      {itens.map((item) => (
        <li key={item.id}>{item.title}</li>
      ))}
    </ul>
  )
}

// Componente de teste: slot que nunca tenta AdSense (equivalente a AdRail/ResultAdCard).
function SlotSempreLocal({ slotId }: { slotId: string }) {
  useAlwaysLocalAdSlot(slotId)
  return <div data-testid={slotId}>local</div>
}

// Componente de teste: slot capaz de AdSense, controlado manualmente pelo teste via
// `reportFillStatus` — simula o que `useAdFallback` faria de verdade.
function SlotAdSense({ slotId, resultado }: { slotId: string; resultado: 'adsense' | 'local' }) {
  const { forcedLocal, reportFillStatus } = useAdSenseCoordination(slotId)
  return (
    <div data-testid={slotId} data-forced={forcedLocal ? 'true' : 'false'}>
      <button onClick={() => reportFillStatus(resultado)}>reportar {slotId}</button>
    </div>
  )
}

describe('AdSlotsProvider', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('distribui itens distintos entre slots (sem repetir na mesma página) enquanto o catálogo tiver itens suficientes', async () => {
    mockCatalogo(CATALOGO_MOCK)

    render(
      <AdSlotsProvider>
        <SlotDeItens slotId="slot-1" count={1} />
        <SlotDeItens slotId="slot-2" count={1} />
        <SlotDeItens slotId="slot-3" count={1} />
      </AdSlotsProvider>
    )

    await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(3))

    const titulos = screen.getAllByRole('listitem').map((el) => el.textContent)
    expect(new Set(titulos).size).toBe(3)
  })

  it('só repete item quando o catálogo (descontados os já entregues) não tem mais itens distintos', async () => {
    mockCatalogo([CATALOGO_MOCK[0]]) // catálogo com 1 item só

    render(
      <AdSlotsProvider>
        <SlotDeItens slotId="slot-1" count={1} />
        <SlotDeItens slotId="slot-2" count={1} />
      </AdSlotsProvider>
    )

    await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(2))
    const titulos = screen.getAllByRole('listitem').map((el) => el.textContent)
    // Único item disponível — repete nos 2 slots (caso extremo previsto), não quebra.
    expect(titulos).toEqual([CATALOGO_MOCK[0].title, CATALOGO_MOCK[0].title])
  })

  it('mesma identidade de slot sempre recebe a mesma atribuição (não resorteia a cada render)', async () => {
    mockCatalogo(CATALOGO_MOCK)

    const { rerender } = render(
      <AdSlotsProvider>
        <SlotDeItens slotId="slot-estavel" count={1} />
      </AdSlotsProvider>
    )

    await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(1))
    const primeiro = screen.getByRole('listitem').textContent

    rerender(
      <AdSlotsProvider>
        <SlotDeItens slotId="slot-estavel" count={1} />
      </AdSlotsProvider>
    )

    expect(screen.getByRole('listitem').textContent).toBe(primeiro)
  })

  it('regra 4: nenhum slot capaz de AdSense reporta local -> um deles é forçado pro catálogo interno', async () => {
    mockCatalogo(CATALOGO_MOCK)

    render(
      <AdSlotsProvider>
        <SlotAdSense slotId="banner-1" resultado="adsense" />
      </AdSlotsProvider>
    )

    await act(async () => {
      screen.getByText('reportar banner-1').click()
    })

    // Único slot capaz e ele reportou 'adsense' de verdade — sem nenhum outro slot no pool
    // (nem `SlotSempreLocal`), a regra 4 força ESSE mesmo slot a virar catálogo interno.
    await waitFor(() => expect(screen.getByTestId('banner-1')).toHaveAttribute('data-forced', 'true'))
  })

  it('regra 4 não força nada quando já existe um espaço sempre-local na página (ex.: AdRail)', async () => {
    mockCatalogo(CATALOGO_MOCK)

    render(
      <AdSlotsProvider>
        <SlotSempreLocal slotId="rail-a" />
        <SlotAdSense slotId="banner-1" resultado="adsense" />
      </AdSlotsProvider>
    )

    await act(async () => {
      screen.getByText('reportar banner-1').click()
    })

    // `SlotSempreLocal` já reportou 'local' assim que montou — a página já tem >= 1 espaço
    // interno organicamente, então a regra 4 não precisa forçar o `banner-1` (que pode
    // seguir mostrando AdSense real).
    await waitFor(() => expect(screen.getByTestId('banner-1')).toHaveAttribute('data-forced', 'false'))
  })

  it('regra 4 não força nada quando algum slot capaz de AdSense já caiu organicamente pro local', async () => {
    mockCatalogo(CATALOGO_MOCK)

    render(
      <AdSlotsProvider>
        <SlotAdSense slotId="banner-1" resultado="adsense" />
        <SlotAdSense slotId="banner-2" resultado="local" />
      </AdSlotsProvider>
    )

    await act(async () => {
      screen.getByText('reportar banner-1').click()
      screen.getByText('reportar banner-2').click()
    })

    await waitFor(() => expect(screen.getByTestId('banner-2')).toHaveAttribute('data-forced', 'false'))
    expect(screen.getByTestId('banner-1')).toHaveAttribute('data-forced', 'false')
  })
})
