import { useEffect, useId, useRef } from 'react'
import { ADSENSE_PUBLISHER_ID, ADSENSE_SLOT_RESULT } from '../lib/config'
import { useAdFallback } from '../hooks/useAdFallback'
import { useAdSlotItems } from './AdSlotsProvider'

// Nome da CSS var lida pelo `PwaToastStack` pra nunca sobrepor este banner —
// achado do Rhodolfo (QA de PR #1186).
const AD_BANNER_HEIGHT_VAR = '--ad-banner-height'

type VarianteCor = 'a' | 'b'

interface AdBannerWideProps {
  /** Skin de cor do house ad quando cai pro catálogo local — roxo (`a`) ou azul (`b`). */
  variant?: VarianteCor
}

const ALTURAS_BARRAS_DESKTOP = [40, 62, 48, 88, 100, 70, 54]
const DELAY_BARRA_DESKTOP = (i: number) => `${(i * 0.12).toFixed(2)}s`
const ALTURAS_BARRAS_COMPACT = [40, 68, 100, 56]
const DELAY_BARRA_COMPACT = (i: number) => `${(i * 0.14).toFixed(2)}s`

/**
 * Banner horizontal universal (todas as larguras) — issue #1402/#1403/#1405, redesenhado na
 * reconstrução v2 (`.claude/design-specs/2026-07-25-site-webapp-v2/AdBannerWide.dc.html`).
 * Aparece em toda tela, embaixo do conteúdo, antes do `SiteFooter` — substitui o antigo
 * `AdBanner.tsx` de slot único. Mecanismo de no-fill/catálogo local vem de `useAdFallback`,
 * coordenado a nível de página via `AdSlotsProvider` (achado da auditoria 1:1 de 2026-07-25,
 * Marina): antes desta versão, cada instância sorteava o catálogo sozinha, sem coordenação
 * com o `AdRail` da mesma página nem com a regra de "pelo menos 1 espaço interno mesmo com
 * AdSense preenchendo tudo".
 *
 * Animações portadas 1:1 do protótipo (antes zero das keyframes existiam no componente):
 * glow drift, barras com pulso, sweep no CTA, dot piscando (variant a), anéis saindo do ícone
 * de roteador (variant b). Ver `.sq-wide-*` em `src/index.css`.
 *
 * Decisão do Luiz (2026-07-25, Opção B): o bloco desktop tem troca de headline (variant a) ou
 * subtítulo (variant b) animada (`sq-wide-a`/`sq-wide-b`) — em vez de texto fixo do protótipo,
 * a troca cross-fade entre 2 itens REAIS do catálogo (`useAdSlotItems(slotId, 2)`),
 * preservando a animação exata mas com conteúdo administrável de verdade. O bloco compacto
 * (mobile) não tem troca no protótipo — usa só o primeiro item.
 */
export function AdBannerWide({ variant = 'a' }: AdBannerWideProps) {
  const rootRef = useRef<HTMLDivElement>(null)
  const uid = useId()
  const slotId = `ad-banner-wide-${uid}`
  const { anuncioRef, mostrarAdsense } = useAdFallback(slotId, ADSENSE_SLOT_RESULT || undefined)
  const [principal, secundario] = useAdSlotItems(slotId, 2)

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
            href={principal?.targetUrl ?? '#'}
            target="_blank"
            rel="noopener noreferrer sponsored"
            aria-disabled={!principal}
            className={`relative block w-full overflow-hidden rounded-xl no-underline box-border ${!principal ? 'pointer-events-none' : ''}`}
          >
            {variant === 'a' ? (
              <BlocoDesktopA principal={principal ?? null} secundario={secundario ?? null} />
            ) : (
              <BlocoDesktopB principal={principal ?? null} secundario={secundario ?? null} />
            )}
            <BlocoCompacto variant={variant} anuncio={principal ?? null} />

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

// Bloco largo desktop, variant "a" (roxo) — headline cross-fade entre `principal`/
// `secundario`, barras com pulso, sweep no CTA. 1:1 com o bloco `isA` do protótipo.
function BlocoDesktopA({ principal, secundario }: { principal: import('../lib/localAdsClient').AnuncioLocal | null; secundario: import('../lib/localAdsClient').AnuncioLocal | null }) {
  return (
    <div
      className="relative hidden h-[172px] items-center gap-6 px-6 py-5 lg:flex"
      style={{ background: 'linear-gradient(100deg, #1B1533 0%, #241C42 46%, #0E0D14 100%)' }}
    >
      <div
        className="sq-wide-glow pointer-events-none absolute h-[260px] w-[260px] rounded-full"
        style={{ left: 180, top: -60, background: 'radial-gradient(circle, rgba(124,77,255,.45), rgba(124,77,255,0) 70%)' }}
      />

      <div className="relative flex h-[68px] w-[96px] items-end gap-[5px]">
        {ALTURAS_BARRAS_DESKTOP.map((h, i) => (
          <div
            key={i}
            className="sq-wide-bar flex-1 rounded-sm"
            style={{ height: `${h}%`, background: i === 4 ? '#C9F26B' : 'rgba(124,77,255,.6)', animationDelay: DELAY_BARRA_DESKTOP(i) }}
          />
        ))}
      </div>

      <div className="relative flex min-w-0 flex-1 flex-col gap-1.5">
        <span
          className="w-fit rounded-full px-2.5 py-0.5 text-[9px] font-bold uppercase tracking-wider"
          style={{ background: '#C9F26B', color: '#0E0D14' }}
        >
          Teste fechado
        </span>
        <div className="relative h-[30px]">
          <div className="sq-wide-a absolute inset-0 text-[24px] font-bold leading-[1.15] tracking-tight text-white">
            {principal?.title ?? 'Espaço para anúncio'}
          </div>
          <div className="sq-wide-b absolute inset-0 text-[24px] font-bold leading-[1.15] tracking-tight text-white">
            {secundario?.title ?? principal?.title ?? 'Espaço para anúncio'}
          </div>
        </div>
        <div className="text-[13px]" style={{ color: 'rgba(255,255,255,.7)' }}>
          {principal?.description ?? 'Banner simulado 320×50'}
        </div>
      </div>

      <div className="relative flex shrink-0 flex-col items-center gap-2">
        <div
          className="relative flex h-11 items-center overflow-hidden rounded-full px-[22px] text-[14px] font-semibold"
          style={{ background: 'linear-gradient(135deg, #7C4DFF, #5B21D6)', boxShadow: '0 10px 24px rgba(91,33,214,.45)', color: '#fff' }}
        >
          <span className="relative">{principal?.ctaLabel ?? 'Saiba mais'}</span>
          <span
            className="sq-wide-sweep pointer-events-none absolute inset-y-0 left-0 w-10"
            style={{ background: 'linear-gradient(100deg, transparent, rgba(255,255,255,.34), transparent)' }}
          />
        </div>
        <div className="flex items-center gap-1.5">
          <span className="sq-wide-dot h-1.5 w-1.5 rounded-full" style={{ background: '#C9F26B' }} />
          <span className="text-[11px]" style={{ color: 'rgba(255,255,255,.6)' }}>
            App em desenvolvimento
          </span>
        </div>
      </div>
    </div>
  )
}

// Bloco largo desktop, variant "b" (azul) — headline fixo, subtítulo cross-fade entre
// `principal`/`secundario`, anéis saindo do ícone de roteador. 1:1 com o bloco `isB`.
function BlocoDesktopB({ principal, secundario }: { principal: import('../lib/localAdsClient').AnuncioLocal | null; secundario: import('../lib/localAdsClient').AnuncioLocal | null }) {
  return (
    <div
      className="relative hidden h-[172px] items-center gap-6 px-6 py-5 lg:flex"
      style={{ background: 'linear-gradient(100deg, #0B1533 0%, #131A3C 52%, #0E0D14 100%)' }}
    >
      <div
        className="sq-wide-glow pointer-events-none absolute h-[260px] w-[260px] rounded-full"
        style={{ right: 160, bottom: -90, background: 'radial-gradient(circle, rgba(40,81,184,.5), rgba(40,81,184,0) 70%)' }}
      />

      <div className="relative flex h-24 w-24 shrink-0 items-center justify-center">
        {['0s', '1.05s', '2.1s'].map((delay) => (
          <span
            key={delay}
            className="sq-wide-ping absolute h-[92px] w-[92px] rounded-full"
            style={{ border: '1px solid rgba(127,209,255,.5)', animationDelay: delay }}
          />
        ))}
        <span
          className="relative flex h-[42px] w-[42px] items-center justify-center rounded-full"
          style={{ background: 'rgba(127,209,255,.16)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 22, color: '#7FD1FF' }}>
            router
          </span>
        </span>
      </div>

      <div className="relative flex min-w-0 flex-1 flex-col gap-1.5">
        <span
          className="w-fit rounded-full px-2.5 py-0.5 text-[9px] font-bold uppercase tracking-wider"
          style={{ background: '#7FD1FF', color: '#0B1533' }}
        >
          Teste fechado
        </span>
        <div className="text-[24px] font-bold leading-[1.15] tracking-tight text-white">{principal?.title ?? 'Espaço para anúncio'}</div>
        <div className="relative h-5">
          <div className="sq-wide-a absolute inset-0 text-[13px]" style={{ color: 'rgba(255,255,255,.7)' }}>
            {principal?.description ?? 'Banner simulado 320×50'}
          </div>
          <div className="sq-wide-b absolute inset-0 text-[13px]" style={{ color: 'rgba(255,255,255,.7)' }}>
            {secundario?.description ?? principal?.description ?? 'Banner simulado 320×50'}
          </div>
        </div>
      </div>

      <div className="relative flex shrink-0 flex-col items-center gap-2">
        <div
          className="relative flex h-11 items-center overflow-hidden rounded-full px-[22px] text-[14px] font-semibold"
          style={{ background: '#7FD1FF', boxShadow: '0 10px 24px rgba(40,81,184,.42)', color: '#0B1533' }}
        >
          <span className="relative">{principal?.ctaLabel ?? 'Saiba mais'}</span>
          <span
            className="sq-wide-sweep pointer-events-none absolute inset-y-0 left-0 w-10"
            style={{ background: 'linear-gradient(100deg, transparent, rgba(255,255,255,.6), transparent)' }}
          />
        </div>
        <div className="flex items-center gap-1.5">
          <span className="sq-wide-dot h-1.5 w-1.5 rounded-full" style={{ background: '#7FD1FF' }} />
          <span className="text-[11px]" style={{ color: 'rgba(255,255,255,.6)' }}>
            App em desenvolvimento
          </span>
        </div>
      </div>
    </div>
  )
}

// Bloco compacto (mobile, empilhado) — estático, um único item, sem troca (o protótipo não
// anima o compacto). Skin distinta do bloco desktop (achado da Marina, auditoria anterior).
function BlocoCompacto({ variant, anuncio }: { variant: VarianteCor; anuncio: import('../lib/localAdsClient').AnuncioLocal | null }) {
  const roxo = variant === 'a'
  const badgeBg = roxo ? '#C9F26B' : '#7FD1FF'
  const badgeFg = roxo ? '#0E0D14' : '#0B1533'
  const ctaBg = roxo ? 'linear-gradient(135deg, #7C4DFF, #5B21D6)' : '#7FD1FF'
  const ctaFg = roxo ? '#fff' : '#0B1533'
  const barAccent = roxo ? '#C9F26B' : '#7FD1FF'
  const barMuted = roxo ? 'rgba(124,77,255,.6)' : 'rgba(40,81,184,.8)'
  const bg = roxo ? 'linear-gradient(120deg, #1B1533 0%, #241C42 50%, #0E0D14 100%)' : 'linear-gradient(120deg, #0B1533 0%, #131A3C 55%, #0E0D14 100%)'
  const glow = roxo
    ? 'radial-gradient(circle, rgba(124,77,255,.45), rgba(124,77,255,0) 70%)'
    : 'radial-gradient(circle, rgba(40,81,184,.5), rgba(40,81,184,0) 70%)'

  return (
    <div className="relative flex flex-col gap-3 p-4 lg:hidden" style={{ background: bg }}>
      <div className="sq-wide-glow pointer-events-none absolute h-[200px] w-[200px] rounded-full" style={{ right: -60, top: -80, background: glow }} />

      <div className="relative flex items-center gap-3">
        <div className="flex h-[34px] w-[38px] shrink-0 items-end gap-[3px]">
          {ALTURAS_BARRAS_COMPACT.map((h, i) => (
            <div
              key={i}
              className="sq-wide-bar flex-1 rounded-sm"
              style={{ height: `${h}%`, background: i === 2 ? barAccent : barMuted, animationDelay: DELAY_BARRA_COMPACT(i) }}
            />
          ))}
        </div>
        <div className="flex min-w-0 flex-col gap-0.5">
          <span className="w-fit rounded-full px-2 py-0.5 text-[8px] font-bold uppercase tracking-wider" style={{ background: badgeBg, color: badgeFg }}>
            Teste fechado
          </span>
          <div className="truncate text-[16px] font-bold leading-tight text-white">{anuncio?.title ?? 'Espaço para anúncio'}</div>
        </div>
      </div>

      <div className="relative text-[12px] leading-tight" style={{ color: 'rgba(255,255,255,.66)' }}>
        {anuncio?.description ?? 'Banner simulado 320×50'}
      </div>

      <div className="relative flex h-11 items-center justify-center rounded-full text-[14px] font-semibold" style={{ background: ctaBg, color: ctaFg }}>
        {anuncio?.ctaLabel ?? 'Saiba mais'}
      </div>
    </div>
  )
}
