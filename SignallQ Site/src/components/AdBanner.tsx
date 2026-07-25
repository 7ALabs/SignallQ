import { useEffect, useRef, useState } from 'react'
import { ADSENSE_PUBLISHER_ID, ADSENSE_SLOT_RESULT } from '../lib/config'
import { buscarAnunciosLocais, sortearAnuncioLocal, type AnuncioLocal } from '../lib/localAdsClient'

// Nome da CSS var lida pelo `PwaToastStack` pra nunca sobrepor este banner —
// achado do Rhodolfo (QA de PR #1186): o AdBanner entrou depois da spec da
// Lia, que só coordenava a colisão entre os dois toasts entre si.
const AD_BANNER_HEIGHT_VAR = '--ad-banner-height'
const ADSENSE_SCRIPT_ID = 'adsbygoogle-loader'

// Tempo máximo de espera pelo `data-ad-status` real do AdSense (issue #1402/#1403)
// antes de considerar no-fill por segurança — indisponibilidade de rede, bloqueador
// de conteúdo ou lentidão do Google não pode deixar o slot vazio pra sempre.
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

// Slot de anúncio reservado após o resultado do PWA — substitui o antigo
// `AdSlot.tsx` (card com ícone de
// imagem quebrada e botão "Saiba mais" desabilitado com aparência clicável,
// achado da Lia em .claude/design-specs/2026-07-19-site-pwa-redesign/SPEC.md)
// pelo padrão aprovado pelo Luiz no protótipo "SignallQ WebApp.dc.html":
// aviso simples e honesto, sem affordance de clique fantasma. Injeta o
// AdSense real por trás quando ADSENSE_PUBLISHER_ID/ADSENSE_SLOT_RESULT
// estiverem configurados. Desde a issue #1402/#1403, quando o AdSense não
// preenche o slot (no-fill real, via `data-ad-status`, ou timeout) ou nem está
// configurado, o banner sorteia e exibe um anúncio local (house ad) do
// catálogo administrável via Console em vez do placeholder vazio.
//
// PENDENTE (2026-07-25): o protótipo Claude Design da reconstrução completa do
// Site/PWA (coordenado pela Claudete) mostrou que a superfície real é multi-slot —
// AdRail esquerda/direita (desktop, 240px), AdBannerWide (todas as larguras) e um
// card embutido fixo no painel de Resultado — não um único slot como este
// componente assume hoje. A lógica de detecção de no-fill e o catálogo/sorteio
// (`localAdsClient.ts`) continuam valendo para os 3+ slots; só a apresentação
// visual deste componente será substituída/dividida quando a spec de
// reconstrução chegar. Não tratar este `AdBanner` como definitivo.
export function AdBanner() {
  const rootRef = useRef<HTMLDivElement>(null)
  const anuncioRef = useRef<HTMLModElement>(null)
  const anuncioInicializadoRef = useRef(false)
  const adSenseConfigurado = Boolean(ADSENSE_PUBLISHER_ID && ADSENSE_SLOT_RESULT)

  // `true` assim que o AdSense sinaliza no-fill (`data-ad-status="unfilled"`) ou
  // estoura o timeout de segurança sem responder — nesse momento o banner passa
  // a tentar o anúncio local em vez do slot do Google.
  const [caiuParaAnuncioLocal, setCaiuParaAnuncioLocal] = useState(false)
  const [anuncioLocal, setAnuncioLocal] = useState<AnuncioLocal | null>(null)

  // Precisa buscar o catálogo local quando o AdSense nunca esteve configurado
  // (fallback imediato) ou quando ele caiu para no-fill (fallback adiado).
  const precisaDeAnuncioLocal = !adSenseConfigurado || caiuParaAnuncioLocal

  // Em mobile, publica a altura real do banner numa CSS var no <html> pro
  // `PwaToastStack` conseguir se posicionar acima dele. Em desktop o slot é
  // lateral, então o toast continua ancorado no rodapé da viewport.
  useEffect(() => {
    const el = rootRef.current
    if (!el) return

    const publicarAltura = () => {
      const lateral = typeof window.matchMedia === 'function' && window.matchMedia('(min-width: 1024px)').matches
      document.documentElement.style.setProperty(AD_BANNER_HEIGHT_VAR, lateral ? '0px' : `${el.offsetHeight}px`)
    }

    publicarAltura()

    // jsdom (testes) não implementa ResizeObserver — a altura publicada no
    // mount já cobre o caso, só perde reação a resize/orientação em teste.
    const observer = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(publicarAltura) : null
    observer?.observe(el)
    window.addEventListener('resize', publicarAltura)

    return () => {
      observer?.disconnect()
      window.removeEventListener('resize', publicarAltura)
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

  // Observa o `data-ad-status` real que o Google escreve no <ins> (`filled`/`unfilled`)
  // pra distinguir "AdSense configurado e preencheu" de "configurado mas sem anúncio pra
  // entregar" — issue #1402/#1403. Timeout de segurança cobre indisponibilidade de rede,
  // bloqueador de conteúdo ou lentidão do Google, que nunca setam o atributo.
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

  return (
    <aside ref={rootRef} aria-label="Publicidade" className="mt-3 w-full px-5 pb-5 pt-3 box-border lg:mt-0 lg:w-[300px] lg:shrink-0 lg:px-0 lg:pt-2">
      <div className="flex w-full items-center gap-2.5 rounded-xl p-3 box-border lg:min-h-[250px] lg:flex-col lg:items-stretch" style={{ background: 'var(--bg-secondary)' }}>
        <div
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
          style={{ background: 'color-mix(in srgb, var(--accent) 14%, transparent)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--accent)' }}>
            campaign
          </span>
        </div>
        <div className="flex flex-1 flex-col gap-0.5 lg:items-center lg:justify-center lg:text-center">
          {adSenseConfigurado && !caiuParaAnuncioLocal ? (
            <ins
              ref={anuncioRef}
              className="adsbygoogle"
              style={{ display: 'block', width: '100%' }}
              data-ad-client={ADSENSE_PUBLISHER_ID}
              data-ad-slot={ADSENSE_SLOT_RESULT}
              data-ad-format="auto"
              data-full-width-responsive="true"
            />
          ) : anuncioLocal ? (
            // Anúncio local (house ad) — issue #1402/#1403, sorteado do catálogo
            // administrável via Console. Layout mínimo/neutro reaproveitando o card
            // existente de propósito: o Luiz vai enviar um protótipo Claude Design
            // redesenhando a apresentação — não investir em layout final aqui ainda.
            // O link é real (destino configurado no catálogo), por isso é uma âncora
            // de verdade, não uma affordance falsa.
            <a href={anuncioLocal.targetUrl} target="_blank" rel="noopener noreferrer sponsored" className="flex flex-col gap-0.5 no-underline lg:items-center lg:text-center">
              <div className="label-medium" style={{ color: 'var(--text-primary)' }}>
                {anuncioLocal.title}
              </div>
              <div className="label-small" style={{ color: 'var(--text-tertiary)' }}>
                {anuncioLocal.description}
              </div>
              <div className="label-small" style={{ color: 'var(--accent)' }}>
                {anuncioLocal.ctaLabel}
              </div>
            </a>
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
          className="shrink-0 rounded lg:self-start"
          style={{ padding: '2px 6px', color: 'var(--text-tertiary)', background: 'var(--bg-primary)', fontSize: 9, fontWeight: 700, letterSpacing: '0.04em' }}
        >
          PUBLICIDADE
        </span>
      </div>
    </aside>
  )
}
