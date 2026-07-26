import { useEffect, useRef, useState } from 'react'
import { ADSENSE_PUBLISHER_ID } from '../lib/config'
import { useAdSenseCoordination } from '../components/AdSlotsProvider'

// Mecanismo de fallback AdSense → catálogo local, extraído do `AdBanner.tsx` original
// (issue #1402/#1403) e coordenado a nível de página desde a issue #1402 (fase 2, achado
// de auditoria de 2026-07-25 — ver `AdSlotsProvider.tsx`). Este hook decide só se o AdSense
// real preencheu ou não; o conteúdo do catálogo local (quando não preenche) é
// responsabilidade de `useAdSlotItems`, chamado à parte pelo componente consumidor — separar
// as duas coisas evita repetir o mesmo anúncio em 2 espaços da mesma página.

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
  /** `true` enquanto o slot deve tentar o AdSense real (configurado, sem no-fill e não
   * forçado pro catálogo interno pela regra 4 do `AdSlotsProvider`). */
  mostrarAdsense: boolean
  adSenseConfigurado: boolean
}

/**
 * Hook de decisão de fallback de anúncio: tenta o AdSense real (quando `adSenseSlotId` é
 * passado e `ADSENSE_PUBLISHER_ID` está configurada) e reporta o resultado pro
 * `AdSlotsProvider` — que decide, a nível de página, se algum slot precisa ser forçado pro
 * catálogo interno mesmo com AdSense preenchendo tudo (regra 4). Sem `adSenseSlotId`, nunca
 * tenta AdSense e já reporta `local` — o componente consumidor sempre renderiza o item do
 * catálogo (via `useAdSlotItems`, chamado à parte) nesse caso.
 */
export function useAdFallback(slotId: string, adSenseSlotId: string | undefined): UseAdFallbackResult {
  const anuncioRef = useRef<HTMLModElement>(null)
  const anuncioInicializadoRef = useRef(false)
  const { forcedLocal, reportFillStatus } = useAdSenseCoordination(slotId)

  const adSenseConfigurado = Boolean(ADSENSE_PUBLISHER_ID && adSenseSlotId) && !forcedLocal
  const [caiuParaAnuncioLocal, setCaiuParaAnuncioLocal] = useState(false)

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

  // Observa o `data-ad-status` real que o Google escreve no <ins> (`filled`/`unfilled`) pra
  // distinguir "AdSense configurado e preencheu" de "configurado mas sem anúncio pra
  // entregar", e reporta o resultado pro provider de página. Timeout de segurança cobre
  // indisponibilidade de rede, bloqueador de conteúdo ou lentidão do Google, que nunca setam
  // o atributo.
  useEffect(() => {
    if (!adSenseConfigurado) return
    const elemento = anuncioRef.current
    if (!elemento) return

    let resolvido = false

    const cairParaLocal = () => {
      if (resolvido) return
      resolvido = true
      setCaiuParaAnuncioLocal(true)
      reportFillStatus('local')
    }

    const observer =
      typeof MutationObserver !== 'undefined'
        ? new MutationObserver(() => {
            const status = elemento.getAttribute('data-ad-status')
            if (status === 'filled') {
              resolvido = true
              reportFillStatus('adsense')
            } else if (status === 'unfilled') {
              cairParaLocal()
            }
          })
        : null
    observer?.observe(elemento, { attributes: true, attributeFilter: ['data-ad-status'] })

    const timeoutId = window.setTimeout(cairParaLocal, AD_NO_FILL_TIMEOUT_MS)

    return () => {
      observer?.disconnect()
      window.clearTimeout(timeoutId)
    }
  }, [adSenseConfigurado, reportFillStatus])

  // Sem AdSense configurado (ou forçado pro catálogo pela regra 4): nunca tenta de verdade,
  // reporta `local` direto em vez de esperar o timeout de no-fill acima.
  useEffect(() => {
    if (!adSenseConfigurado) reportFillStatus('local')
  }, [adSenseConfigurado, reportFillStatus])

  return {
    anuncioRef,
    mostrarAdsense: adSenseConfigurado && !caiuParaAnuncioLocal,
    adSenseConfigurado,
  }
}
