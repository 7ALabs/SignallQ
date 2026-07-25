import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AdBanner } from './AdBanner'

const ANUNCIO_LOCAL_MOCK = {
  id: 'house-1',
  title: 'Teste sua velocidade com o app SignallQ',
  description: 'Diagnóstico completo de rede, grátis.',
  ctaLabel: 'Baixar agora',
  targetUrl: 'https://signallq.pages.dev',
}

describe('AdBanner', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
    vi.restoreAllMocks()
    document.getElementById('adsbygoogle-loader')?.remove()
    delete (window as Window & { adsbygoogle?: unknown }).adsbygoogle
  })

  it('sem AdSense configurado -> mostra aviso honesto de espaço reservado antes do catálogo local resolver, sem affordance falsa', () => {
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {})) // nunca resolve nesta asserção síncrona
    render(<AdBanner />)
    expect(screen.getByText('Espaço para anúncio')).toBeInTheDocument()
    expect(screen.getByText('PUBLICIDADE')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('sem AdSense configurado e catálogo local disponível -> sorteia e exibe o anúncio local', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ads: [ANUNCIO_LOCAL_MOCK] }), { status: 200 })
    )

    render(<AdBanner />)

    await waitFor(() => expect(screen.getByText(ANUNCIO_LOCAL_MOCK.title)).toBeInTheDocument())
    expect(screen.getByText(ANUNCIO_LOCAL_MOCK.ctaLabel)).toBeInTheDocument()
    expect(screen.getByText('PUBLICIDADE')).toBeInTheDocument()
    expect(screen.queryByText('Espaço para anúncio')).not.toBeInTheDocument()
  })

  it('com AdSense configurado mantém o anúncio sob controle do React e inicializa apenas uma vez', async () => {
    vi.stubEnv('VITE_ADSENSE_PUBLISHER_ID', 'ca-pub-1234567890123456')
    vi.stubEnv('VITE_ADSENSE_SLOT_RESULT', '1234567890')

    const script = document.createElement('script')
    script.id = 'adsbygoogle-loader'
    document.head.appendChild(script)
    const fila: Array<Record<string, never>> = []
    ;(window as Window & { adsbygoogle?: Array<Record<string, never>> }).adsbygoogle = fila

    const { AdBanner: BannerComAdSense } = await import('./AdBanner')
    render(<BannerComAdSense />)

    const anuncio = document.querySelector('ins.adsbygoogle')
    expect(anuncio).toHaveAttribute('data-ad-client', 'ca-pub-1234567890123456')
    expect(anuncio).toHaveAttribute('data-ad-slot', '1234567890')
    expect(document.querySelector('[data-adsbygoogle-loader]')).toBeNull()

    await waitFor(() => expect(fila).toHaveLength(1))
  })

  it('com AdSense configurado mas em no-fill (data-ad-status=unfilled) -> cai para o anúncio local', async () => {
    vi.stubEnv('VITE_ADSENSE_PUBLISHER_ID', 'ca-pub-1234567890123456')
    vi.stubEnv('VITE_ADSENSE_SLOT_RESULT', '1234567890')
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ads: [ANUNCIO_LOCAL_MOCK] }), { status: 200 })
    )

    const script = document.createElement('script')
    script.id = 'adsbygoogle-loader'
    document.head.appendChild(script)
    ;(window as Window & { adsbygoogle?: Array<Record<string, never>> }).adsbygoogle = []

    const { AdBanner: BannerComAdSense } = await import('./AdBanner')
    render(<BannerComAdSense />)

    const anuncio = document.querySelector('ins.adsbygoogle')
    expect(anuncio).not.toBeNull()
    anuncio?.setAttribute('data-ad-status', 'unfilled')

    await waitFor(() => expect(screen.getByText(ANUNCIO_LOCAL_MOCK.title)).toBeInTheDocument())
    expect(document.querySelector('ins.adsbygoogle')).not.toBeInTheDocument()
  })
})
