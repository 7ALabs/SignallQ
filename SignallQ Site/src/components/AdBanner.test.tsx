import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AdBanner } from './AdBanner'

describe('AdBanner', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
    document.getElementById('adsbygoogle-loader')?.remove()
    delete (window as Window & { adsbygoogle?: unknown }).adsbygoogle
  })

  it('sem AdSense configurado -> mostra aviso honesto de espaço reservado, sem affordance falsa', () => {
    render(<AdBanner />)
    expect(screen.getByText('Espaço para anúncio')).toBeInTheDocument()
    expect(screen.getByText('PUBLICIDADE')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
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
})
