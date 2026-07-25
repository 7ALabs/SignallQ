import { useEffect, useRef, useState } from 'react'
import { ADSENSE_PUBLISHER_ID } from '../lib/config'
import { buscarAnunciosLocais, sortearAnuncioLocal, type AnuncioLocal } from '../lib/localAdsClient'

// Mecanismo de fallback AdSense → anúncio local, extraído do `AdBanner.tsx` original
// (issue #1402/#1403) para ser reaproveitado pelos 3 espaços de anúncio da
// reconstrução v2 (`AdBannerWide`, `AdRail`) — ver
// `.claude/design-specs/2026-07-25-site-webapp-v2/README.md`, achado 1. A lógica de
// detecção de no-fill e catálogo/sorteio não muda entre slots; só a apresentação
// visual (cada componente decide o que fazer com `mostrarAdsense`/`anuncioLocal`).

const ADSENSE_SCRIPT_ID = 'adsbygoogle-loader'

// Tempo máximo de espera pelo `data-ad-status` real do AdSense antes de considerar
// no-fill por segurança — indisponibilidade de rede, bloqueador de conteúdo ou
// lentidão do Google não pode deixar o slot vazio pra sempre.
const AD_NO_FILL_TIMEOUT_MS = 4000

type JanelaComAdSense = Window & { adsbygoogle?: Array<Record<string, never>> }

let carregamentoAdSense: Promise<void> | undefined

function carregarScriptAdSense(publisherId: string): Promise<void> {
  if (carregamentoAdSense) return carregamentoAdSense

  const existente = document.getElementById(ADSENSE_SCRIPT_ID) as HTMLScriptElement | null
  if (existente) {
    carregamentoAdSense = new Promise((resolve, reject) => {
      if ((window as JanelaComAdSense).adsbygoogle) {
        resolve()
        return
      }

      existente.addEventListener('load', () => resolve(), { once: true })
      existente.addEventListener('error', () => reject(new Error('Não foi possível carregar o AdSense.')), { once: true })
    })
    return carregamentoAdSense
  }

  carregamentoAdSense = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.id = ADSENSE_SCRIPT_ID
    script.async = true
    script.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${publisherId}`
    script.crossOrigin = 'anonymous'
    script.addEventListener('load', () => resolve(), { once: true })
    script.addEventListener('error', () => reject(new Error('Não foi possível carregar o AdSense.')), { once: true })
    document.head.appendChild(script)
  })

  return carregamentoAdSense
}

export interface UseAdFallbackResult {
  /** Ref pra atachar no `<ins class="adsbygoogle">` quando `mostrarAdsense` for `true`. */
  anuncioRef: React.RefObject<HTMLModElement | null>
  /** `true` enquanto o slot deve tentar o AdSense real (configurado e ainda sem no-fill). */
  mostrarAdsense: boolean
  /** Item sorteado do catálogo local — só populado quando precisa dele (no-fill/não configurado). */
  anuncioLocal: AnuncioLocal | null
  adSenseConfigurado: boolean
}

/**
 * Hook de fallback de anúncio: tenta o AdSense real (quando `adSenseSlotId` é passado e
 * `ADSENSE_PUBLISHER_ID` está configurada) e cai para um item sorteado do catálogo local
 * quando o Google sinaliza no-fill (`data-ad-status="unfilled"`) ou estoura o timeout de
 * segurança. Sem `adSenseSlotId`, pula direto pro catálogo local (caso do `AdRail`, que
 * ainda não tem unidade de anúncio própria configurada).
 */
export function useAdFallback(adSenseSlotId: string | undefined): UseAdFallbackResult {
  const anuncioRef = useRef<HTMLModElement>(null)
  const anuncioInicializadoRef = useRef(false)
  const adSenseConfigurado = Boolean(ADSENSE_PUBLISHER_ID && adSenseSlotId)

  const [caiuParaAnuncioLocal, setCaiuParaAnuncioLocal] = useState(false)
  const [anuncioLocal, setAnuncioLocal] = useState<AnuncioLocal | null>(null)

  const precisaDeAnuncioLocal = !adSenseConfigurado || caiuParaAnuncioLocal

  useEffect(() => {
    if (!adSenseConfigurado || !anuncioRef.current || anuncioInicializadoRef.current) return

    let cancelado = false

    void carregarScriptAdSense(ADSENSE_PUBLISHER_ID)
      .then(() => {
        if (cancelado || anuncioInicializadoRef.current) return

        const adSense = (window as JanelaComAdSense).adsbygoogle
        if (!adSense) return

        adSense.push({})
        anuncioInicializadoRef.current = true
      })
      .catch(() => {
        // Falha de rede, bloqueador de conteúdo ou indisponibilidade do Google não pode derrubar o site.
      })

    return () => {
      cancelado = true
    }
  }, [adSenseConfigurado])

  // Observa o `data-ad-status` real que o Google escreve no <ins> (`filled`/`unfilled`)
  // pra distinguir "AdSense configurado e preencheu" de "configurado mas sem anúncio pra
  // entregar". Timeout de segurança cobre indisponibilidade de rede, bloqueador de
  // conteúdo ou lentidão do Google, que nunca setam o atributo.
  useEffect(() => {
    if (!adSenseConfigurado) return
    const elemento = anuncioRef.current
    if (!elemento) return

    let resolvido = false

    const cairParaLocal = () => {
      if (resolvido) return
      resolvido = true
      setCaiuParaAnuncioLocal(true)
    }

    const observer =
      typeof MutationObserver !== 'undefined'
        ? new MutationObserver(() => {
            const status = elemento.getAttribute('data-ad-status')
            if (status === 'filled') resolvido = true
            else if (status === 'unfilled') cairParaLocal()
          })
        : null
    observer?.observe(elemento, { attributes: true, attributeFilter: ['data-ad-status'] })

    const timeoutId = window.setTimeout(cairParaLocal, AD_NO_FILL_TIMEOUT_MS)

    return () => {
      observer?.disconnect()
      window.clearTimeout(timeoutId)
    }
  }, [adSenseConfigurado])

  // Busca o catálogo local só quando de fato precisa dele — evita chamada de rede
  // desnecessária enquanto o AdSense ainda está preenchendo normalmente.
  useEffect(() => {
    if (!precisaDeAnuncioLocal) return

    let cancelado = false
    const controller = typeof AbortController !== 'undefined' ? new AbortController() : undefined

    buscarAnunciosLocais(controller?.signal).then((anuncios) => {
      if (!cancelado) setAnuncioLocal(sortearAnuncioLocal(anuncios))
    })

    return () => {
      cancelado = true
      controller?.abort()
    }
  }, [precisaDeAnuncioLocal])

  return {
    anuncioRef,
    mostrarAdsense: adSenseConfigurado && !caiuParaAnuncioLocal,
    anuncioLocal,
    adSenseConfigurado,
  }
}
