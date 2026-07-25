import { useEffect, useRef } from 'react'
import { ADSENSE_PUBLISHER_ID, ADSENSE_SLOT_RESULT } from '../lib/config'

// Nome da CSS var lida pelo `PwaToastStack` pra nunca sobrepor este banner —
// achado do Rhodolfo (QA de PR #1186): o AdBanner entrou depois da spec da
// Lia, que só coordenava a colisão entre os dois toasts entre si.
const AD_BANNER_HEIGHT_VAR = '--ad-banner-height'
const ADSENSE_SCRIPT_ID = 'adsbygoogle-loader'

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

// Rodapé de anúncio simulado das 3 telas do fluxo do PWA (Velocidade,
// Resultado, Histórico) — substitui o antigo `AdSlot.tsx` (card com ícone de
// imagem quebrada e botão "Saiba mais" desabilitado com aparência clicável,
// achado da Lia em .claude/design-specs/2026-07-19-site-pwa-redesign/SPEC.md)
// pelo padrão aprovado pelo Luiz no protótipo "SignallQ WebApp.dc.html":
// aviso simples e honesto, sem affordance de clique fantasma. Injeta o
// AdSense real por trás quando ADSENSE_PUBLISHER_ID/ADSENSE_SLOT_RESULT
// estiverem configurados — só a apresentação do placeholder muda.
export function AdBanner() {
  const rootRef = useRef<HTMLDivElement>(null)
  const anuncioRef = useRef<HTMLModElement>(null)
  const anuncioInicializadoRef = useRef(false)
  const adSenseConfigurado = Boolean(ADSENSE_PUBLISHER_ID && ADSENSE_SLOT_RESULT)

  // Publica a altura real do banner numa CSS var no <html> pro
  // `PwaToastStack` conseguir se posicionar acima dele, e não por cima.
  // Zera ao desmontar (troca de rota) pra não vazar altura pra páginas sem banner.
  useEffect(() => {
    const el = rootRef.current
    if (!el) return

    const publicarAltura = () => {
      document.documentElement.style.setProperty(AD_BANNER_HEIGHT_VAR, `${el.offsetHeight}px`)
    }

    publicarAltura()

    // jsdom (testes) não implementa ResizeObserver — a altura publicada no
    // mount já cobre o caso, só perde reação a resize/orientação em teste.
    const observer = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(publicarAltura) : null
    observer?.observe(el)

    return () => {
      observer?.disconnect()
      document.documentElement.style.setProperty(AD_BANNER_HEIGHT_VAR, '0px')
    }
  }, [])

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
        // Falha de rede, bloqueador de conteúdo ou indisponibilidade do Google não pode derrubar o PWA.
      })

    return () => {
      cancelado = true
    }
  }, [adSenseConfigurado])

  return (
    <div ref={rootRef} className="mt-auto w-full px-5 pb-5 pt-3 box-border">
      <div className="flex w-full items-center gap-2.5 rounded-xl p-3 box-border" style={{ background: 'var(--bg-secondary)' }}>
        <div
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
          style={{ background: 'color-mix(in srgb, var(--accent) 14%, transparent)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--accent)' }}>
            campaign
          </span>
        </div>
        <div className="flex flex-1 flex-col gap-0.5">
          {adSenseConfigurado ? (
            <ins
              ref={anuncioRef}
              className="adsbygoogle"
              style={{ display: 'block', width: '100%' }}
              data-ad-client={ADSENSE_PUBLISHER_ID}
              data-ad-slot={ADSENSE_SLOT_RESULT}
              data-ad-format="auto"
              data-full-width-responsive="true"
            />
          ) : (
            <>
              <div className="label-medium">Espaço para anúncio</div>
              <div className="label-small" style={{ color: 'var(--text-tertiary)' }}>
                Banner simulado 320×50
              </div>
            </>
          )}
        </div>
        <span
          className="shrink-0 rounded"
          style={{ padding: '2px 6px', color: 'var(--text-tertiary)', background: 'var(--bg-primary)', fontSize: 9, fontWeight: 700, letterSpacing: '0.04em' }}
        >
          PUBLICIDADE
        </span>
      </div>
    </div>
  )
}
