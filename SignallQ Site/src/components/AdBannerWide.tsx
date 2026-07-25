import { useEffect, useRef } from 'react'
import { ADSENSE_PUBLISHER_ID, ADSENSE_SLOT_RESULT } from '../lib/config'
import { useAdFallback } from '../hooks/useAdFallback'

// Nome da CSS var lida pelo `PwaToastStack` pra nunca sobrepor este banner —
// achado do Rhodolfo (QA de PR #1186).
const AD_BANNER_HEIGHT_VAR = '--ad-banner-height'

type VarianteCor = 'a' | 'b'

interface AdBannerWideProps {
  /** Skin de cor do house ad quando cai pro catálogo local — roxo (`a`) ou azul (`b`). */
  variant?: VarianteCor
}

const SKINS: Record<VarianteCor, {
  gradient: string
  glow: string
  glowPos: string
  /** Skin distinta do bloco compacto/mobile — `cBg`/`cGlow` no protótipo (`AdBannerWide.dc.html`),
   * diferente do gradiente/glow do bloco largo/desktop (achado da Marina, auditoria 1:1 de
   * 2026-07-25: o bloco mobile reaproveitava por engano o skin do desktop). */
  compactGradient: string
  compactGlow: string
  badgeBg: string
  badgeFg: string
  ctaBg: string
  ctaFg: string
  barMuted: string
  barAccent: string
}> = {
  a: {
    gradient: 'linear-gradient(100deg, #1B1533 0%, #241C42 46%, #0E0D14 100%)',
    glow: 'radial-gradient(circle, rgba(124,77,255,.45), rgba(124,77,255,0) 70%)',
    glowPos: 'left: 180px; top: -60px;',
    compactGradient: 'linear-gradient(120deg, #1B1533 0%, #241C42 50%, #0E0D14 100%)',
    compactGlow: 'radial-gradient(circle, rgba(124,77,255,.45), rgba(124,77,255,0) 70%)',
    badgeBg: '#C9F26B',
    badgeFg: '#0E0D14',
    ctaBg: 'linear-gradient(135deg, #7C4DFF, #5B21D6)',
    ctaFg: '#fff',
    barMuted: 'rgba(124,77,255,.6)',
    barAccent: '#C9F26B',
  },
  b: {
    gradient: 'linear-gradient(100deg, #0B1533 0%, #131A3C 52%, #0E0D14 100%)',
    glow: 'radial-gradient(circle, rgba(40,81,184,.5), rgba(40,81,184,0) 70%)',
    glowPos: 'right: 160px; bottom: -90px;',
    compactGradient: 'linear-gradient(120deg, #0B1533 0%, #131A3C 55%, #0E0D14 100%)',
    compactGlow: 'radial-gradient(circle, rgba(40,81,184,.5), rgba(40,81,184,0) 70%)',
    badgeBg: '#7FD1FF',
    badgeFg: '#0B1533',
    ctaBg: '#7FD1FF',
    ctaFg: '#0B1533',
    barMuted: 'rgba(40,81,184,.8)',
    barAccent: '#7FD1FF',
  },
}

const ALTURAS_BARRAS = [40, 62, 48, 88, 100, 70, 54]

/**
 * Banner horizontal universal (todas as larguras) — issue #1402/#1403, redesenhado na
 * reconstrução v2 (`.claude/design-specs/2026-07-25-site-webapp-v2/AdBannerWide.dc.html`).
 * Aparece em toda tela, embaixo do conteúdo, antes do `SiteFooter` — substitui o antigo
 * `AdBanner.tsx` de slot único. Mecanismo de no-fill/catálogo local vem de
 * `useAdFallback` (extraído do `AdBanner.tsx` original, não muda de comportamento).
 */
export function AdBannerWide({ variant = 'a' }: AdBannerWideProps) {
  const rootRef = useRef<HTMLDivElement>(null)
  const { anuncioRef, mostrarAdsense, anuncioLocal } = useAdFallback(ADSENSE_SLOT_RESULT || undefined)
  const skin = SKINS[variant]

  // Em mobile, publica a altura real do banner numa CSS var no <html> pro
  // `PwaToastStack` conseguir se posicionar acima dele. Em desktop o slot some
  // dessa preocupação por já não colidir com o toast (ancorado no rodapé).
  useEffect(() => {
    const el = rootRef.current
    if (!el) return

    const publicarAltura = () => {
      document.documentElement.style.setProperty(AD_BANNER_HEIGHT_VAR, `${el.offsetHeight}px`)
    }

    publicarAltura()

    const observer = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(publicarAltura) : null
    observer?.observe(el)
    window.addEventListener('resize', publicarAltura)

    return () => {
      observer?.disconnect()
      window.removeEventListener('resize', publicarAltura)
      document.documentElement.style.setProperty(AD_BANNER_HEIGHT_VAR, '0px')
    }
  }, [])

  return (
    <div ref={rootRef} aria-label="Publicidade" className="w-full px-5 py-3 box-border lg:px-0">
      <div className="mx-auto w-full max-w-[1280px]">
        {mostrarAdsense ? (
          <ins
            ref={anuncioRef}
            className="adsbygoogle"
            style={{ display: 'block', width: '100%', minHeight: 90 }}
            data-ad-client={ADSENSE_PUBLISHER_ID}
            data-ad-slot={ADSENSE_SLOT_RESULT}
            data-ad-format="auto"
            data-full-width-responsive="true"
          />
        ) : (
          <a
            href={anuncioLocal?.targetUrl ?? '#'}
            target="_blank"
            rel="noopener noreferrer sponsored"
            aria-disabled={!anuncioLocal}
            className={`relative block w-full overflow-hidden rounded-xl no-underline box-border ${!anuncioLocal ? 'pointer-events-none' : ''}`}
          >
            {/* Fundo por breakpoint — skin `compact`/`cBg` (mobile) é distinto do skin largo/
                desktop no protótipo (`AdBannerWide.dc.html`), não o mesmo gradiente reaproveitado. */}
            <div className="pointer-events-none absolute inset-0 lg:hidden" style={{ background: skin.compactGradient }} />
            <div className="pointer-events-none absolute inset-0 hidden lg:block" style={{ background: skin.gradient }} />

            <div
              className="pointer-events-none absolute h-[200px] w-[200px] rounded-full lg:hidden"
              style={{ background: skin.compactGlow, right: -60, top: -80 }}
            />
            <div
              className="pointer-events-none absolute hidden h-[260px] w-[260px] rounded-full lg:block"
              style={{ background: skin.glow, ...parseInlinePos(skin.glowPos) }}
            />

            {/* Compacto (mobile): empilhado vertical. */}
            <div className="relative flex flex-col gap-3 p-4 lg:hidden">
              <div className="flex items-center gap-3">
                <div className="flex h-[34px] w-[38px] shrink-0 items-end gap-[3px]">
                  {[40, 68, 100, 56].map((h, i) => (
                    <div key={i} className="flex-1 rounded-sm" style={{ height: `${h}%`, background: i === 2 ? skin.barAccent : skin.barMuted }} />
                  ))}
                </div>
                <div className="flex min-w-0 flex-col gap-0.5">
                  <span
                    className="w-fit rounded-full px-2 py-0.5 text-[8px] font-bold uppercase tracking-wider"
                    style={{ background: skin.badgeBg, color: skin.badgeFg }}
                  >
                    Teste fechado
                  </span>
                  <div className="truncate text-[16px] font-bold leading-tight text-white">
                    {anuncioLocal?.title ?? 'Espaço para anúncio'}
                  </div>
                </div>
              </div>
              <div className="text-[12px] leading-tight" style={{ color: 'rgba(255,255,255,.66)' }}>
                {anuncioLocal?.description ?? 'Banner simulado 320×50'}
              </div>
              <div
                className="flex h-11 items-center justify-center rounded-full text-[14px] font-semibold"
                style={{ background: skin.ctaBg, color: skin.ctaFg }}
              >
                {anuncioLocal?.ctaLabel ?? 'Saiba mais'}
              </div>
            </div>

            {/* Largo (desktop): barras + texto + CTA lado a lado. */}
            <div className="relative hidden items-center gap-6 p-5 lg:flex lg:h-[140px]">
              <div className="flex h-[68px] w-[96px] items-end gap-[5px]">
                {ALTURAS_BARRAS.map((h, i) => (
                  <div key={i} className="flex-1 rounded-sm" style={{ height: `${h}%`, background: i === 4 ? skin.barAccent : skin.barMuted }} />
                ))}
              </div>
              <div className="flex min-w-0 flex-1 flex-col gap-1.5">
                <span
                  className="w-fit rounded-full px-2.5 py-0.5 text-[9px] font-bold uppercase tracking-wider"
                  style={{ background: skin.badgeBg, color: skin.badgeFg }}
                >
                  Teste fechado
                </span>
                <div className="text-[24px] font-bold leading-tight tracking-tight text-white">
                  {anuncioLocal?.title ?? 'Espaço para anúncio'}
                </div>
                <div className="text-[13px]" style={{ color: 'rgba(255,255,255,.7)' }}>
                  {anuncioLocal?.description ?? 'Banner simulado 320×50'}
                </div>
              </div>
              <div className="flex shrink-0 flex-col items-center gap-2">
                <div
                  className="flex h-11 items-center rounded-full px-[22px] text-[14px] font-semibold"
                  style={{ background: skin.ctaBg, color: skin.ctaFg }}
                >
                  {anuncioLocal?.ctaLabel ?? 'Saiba mais'}
                </div>
              </div>
            </div>

            <span
              className="absolute right-3 top-2 text-[8px] font-bold uppercase tracking-wider"
              style={{ color: 'rgba(255,255,255,.3)' }}
            >
              PUBLICIDADE
            </span>
          </a>
        )}
      </div>
    </div>
  )
}

// Auxiliar pra converter "left: 180px; top: -60px;" (posição do glow por variante,
// copiada 1:1 do protótipo) num objeto de style React.
function parseInlinePos(css: string): Record<string, string> {
  const out: Record<string, string> = {}
  css.split(';').forEach((decl) => {
    const [prop, value] = decl.split(':').map((s) => s?.trim())
    if (prop && value) out[prop] = value
  })
  return out
}
