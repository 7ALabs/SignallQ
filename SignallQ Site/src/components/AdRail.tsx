import { useId } from 'react'
import { useAdSlotItems, useAlwaysLocalAdSlot } from './AdSlotsProvider'
import type { AnuncioLocal } from '../lib/localAdsClient'

// Coluna de anúncio lateral (240px, só desktop) — issue #1402/#1403/#1405, reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/AdRail.dc.html`). Nunca tenta AdSense real
// (sem unidade própria configurada) — sempre mostra o catálogo local, coordenado a nível de
// página via `AdSlotsProvider` (achado da auditoria 1:1 de 2026-07-25, Marina): antes desta
// versão, cada `AdRail` sorteava o catálogo sozinho e podia repetir o mesmo anúncio nas duas
// colunas ou no `AdBannerWide` da mesma página.
//
// Animações portadas 1:1 do protótipo (antes zero das keyframes existiam no componente):
// glow drift, badge rise-in, barras/equalizador com pulso, sweep no CTA, dot piscando, scan
// line (card grande variant a), anéis saindo (card grande variant b), contador com pulso e
// ticker vertical (card pequeno variant b). Ver `.sq-rail-*` em `src/index.css`.
//
// Decisão do Luiz (2026-07-25, Opção B): os cards grandes têm troca de headline/subtítulo
// animada (`sq-rail-head-a/b`, `sq-rail-room-a/b`) — em vez de texto fixo do protótipo, a
// troca cross-fade entre 2 itens REAIS do catálogo (`useAdSlotItems(slotId, 2)`), preservando
// a animação exata mas com conteúdo administrável de verdade.

type VarianteCor = 'a' | 'b'

interface AdRailProps {
  variant: VarianteCor
}

const ALTURAS_BARRAS = [34, 52, 40, 74, 96, 62, 84, 46, 30]
const DELAY_BARRA = (i: number) => `${(i * 0.13).toFixed(2)}s`

export function AdRail({ variant }: AdRailProps) {
  const uid = useId()
  useAlwaysLocalAdSlot(uid)
  const [principal, secundario] = useAdSlotItems(`${uid}-grande`, 2)
  const [pequeno] = useAdSlotItems(`${uid}-pequeno`, 1)

  return (
    <aside aria-label="Publicidade" className="hidden w-[240px] shrink-0 flex-col gap-3 lg:flex">
      {variant === 'a' ? (
        <>
          <CardGrandeA principal={principal ?? null} secundario={secundario ?? null} />
          <CardPequenoA anuncio={pequeno ?? null} />
        </>
      ) : (
        <>
          <CardGrandeB principal={principal ?? null} secundario={secundario ?? null} />
          <CardPequenoB anuncio={pequeno ?? null} />
        </>
      )}
    </aside>
  )
}

// Card grande, variant "a" (roxo) — headline cross-fade entre `principal`/`secundario`,
// equalizador com scan line, CTA com sweep. 1:1 com o bloco `isA` de `AdRail.dc.html`.
function CardGrandeA({ principal, secundario }: { principal: AnuncioLocal | null; secundario: AnuncioLocal | null }) {
  const temAnuncio = Boolean(principal)
  return (
    <a
      href={principal?.targetUrl ?? '#'}
      target="_blank"
      rel="noopener noreferrer sponsored"
      aria-disabled={!temAnuncio}
      className={`relative box-border flex h-[600px] w-[240px] flex-col justify-between overflow-hidden rounded-lg p-5 no-underline ${!temAnuncio ? 'pointer-events-none' : ''}`}
      style={{ background: 'linear-gradient(168deg, #241C42 0%, #17132B 46%, #0E0D14 100%)' }}
    >
      <div
        className="sq-rail-glow-a pointer-events-none absolute h-[300px] w-[300px] rounded-full"
        style={{ left: -70, top: 168, background: 'radial-gradient(circle, rgba(91,33,214,.55), rgba(91,33,214,0) 68%)' }}
      />
      <div
        className="sq-rail-glow-b pointer-events-none absolute h-[260px] w-[260px] rounded-full"
        style={{ right: -90, bottom: 40, background: 'radial-gradient(circle, rgba(40,81,184,.45), rgba(40,81,184,0) 70%)' }}
      />

      <div className="relative flex flex-col gap-4">
        <span
          className="sq-rail-rise w-fit rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider"
          style={{ background: '#C9F26B', color: '#0E0D14' }}
        >
          Teste fechado
        </span>
        <div className="relative h-[66px]">
          <div className="sq-rail-head-a absolute inset-0 text-[26px] font-bold leading-[1.08] tracking-tight text-white">
            {principal?.title ?? 'Espaço para anúncio'}
          </div>
          <div className="sq-rail-head-b absolute inset-0 text-[26px] font-bold leading-[1.08] tracking-tight text-white">
            {secundario?.title ?? principal?.title ?? 'Espaço para anúncio'}
          </div>
        </div>
        <div className="sq-rail-rise text-[13px] leading-relaxed" style={{ color: 'rgba(255,255,255,.72)', animationDelay: '.35s' }}>
          {principal?.description ?? 'Banner simulado 240×600'}
        </div>
      </div>

      <div className="relative flex h-24 items-end gap-[7px] overflow-hidden px-0.5">
        {ALTURAS_BARRAS.map((h, i) => (
          <div
            key={i}
            className="sq-rail-bar flex-1 rounded-sm"
            style={{ height: `${h}%`, background: i === 4 ? '#C9F26B' : i % 2 === 0 ? 'rgba(255,255,255,.16)' : 'rgba(124,77,255,.55)', animationDelay: DELAY_BARRA(i) }}
          />
        ))}
        <div
          className="sq-rail-scan pointer-events-none absolute inset-x-0 top-0 h-px"
          style={{ background: 'linear-gradient(to right, transparent, rgba(201,242,107,.85), transparent)' }}
        />
      </div>

      <div className="relative flex flex-col gap-2.5">
        <div
          className="relative flex h-11 items-center justify-center overflow-hidden rounded-full text-[14px] font-semibold"
          style={{ background: 'linear-gradient(135deg, #7C4DFF, #5B21D6)', boxShadow: '0 10px 26px rgba(91,33,214,.45)', color: '#fff' }}
        >
          <span className="relative">{principal?.ctaLabel ?? 'Saiba mais'}</span>
          <span
            className="sq-rail-sweep pointer-events-none absolute inset-y-0 left-0 w-10"
            style={{ background: 'linear-gradient(100deg, transparent, rgba(255,255,255,.34), transparent)' }}
          />
        </div>
        <div className="flex items-center justify-center gap-1.5 text-[11px]" style={{ color: 'rgba(255,255,255,.6)' }}>
          <span className="sq-rail-dot h-1.5 w-1.5 rounded-full" style={{ background: '#C9F26B' }} />
          App em desenvolvimento
        </div>
      </div>

      <span className="absolute right-2.5 top-2 text-[8px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,.35)' }}>
        PUBLICIDADE
      </span>
    </a>
  )
}

// Card grande, variant "b" (azul) — headline fixo, subtítulo cross-fade entre
// `principal`/`secundario`, anéis saindo do ícone de roteador. 1:1 com o bloco `isB`.
function CardGrandeB({ principal, secundario }: { principal: AnuncioLocal | null; secundario: AnuncioLocal | null }) {
  const temAnuncio = Boolean(principal)
  return (
    <a
      href={principal?.targetUrl ?? '#'}
      target="_blank"
      rel="noopener noreferrer sponsored"
      aria-disabled={!temAnuncio}
      className={`relative box-border flex h-[600px] w-[240px] flex-col justify-between overflow-hidden rounded-lg p-5 no-underline ${!temAnuncio ? 'pointer-events-none' : ''}`}
      style={{ background: 'linear-gradient(200deg, #0B1533 0%, #131A3C 40%, #0E0D14 100%)' }}
    >
      <div
        className="sq-rail-glow-b pointer-events-none absolute h-[280px] w-[280px] rounded-full"
        style={{ left: -60, bottom: 120, background: 'radial-gradient(circle, rgba(40,81,184,.5), rgba(40,81,184,0) 70%)' }}
      />

      <div className="relative flex flex-col gap-4">
        <span
          className="sq-rail-rise w-fit rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider"
          style={{ background: '#7FD1FF', color: '#0B1533' }}
        >
          Teste fechado
        </span>
        <div className="text-[26px] font-bold leading-[1.1] tracking-tight text-white">{principal?.title ?? 'Espaço para anúncio'}</div>
        <div className="relative h-10">
          <div className="sq-rail-room-a absolute inset-0 text-[13px] leading-relaxed" style={{ color: 'rgba(255,255,255,.72)' }}>
            {principal?.description ?? 'Banner simulado 240×600'}
          </div>
          <div className="sq-rail-room-b absolute inset-0 text-[13px] leading-relaxed" style={{ color: 'rgba(255,255,255,.72)' }}>
            {secundario?.description ?? principal?.description ?? 'Banner simulado 240×600'}
          </div>
        </div>
      </div>

      <div className="relative flex h-[150px] items-center justify-center">
        {['0s', '1.1s', '2.2s'].map((delay) => (
          <span
            key={delay}
            className="sq-rail-ring absolute h-[150px] w-[150px] rounded-full"
            style={{ border: '1px solid rgba(127,209,255,.55)', animationDelay: delay }}
          />
        ))}
        <span
          className="relative flex h-[46px] w-[46px] items-center justify-center rounded-full"
          style={{ background: 'rgba(127,209,255,.16)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 24, color: '#7FD1FF' }}>
            router
          </span>
        </span>
      </div>

      <div className="relative flex flex-col gap-2.5">
        <div
          className="relative flex h-11 items-center justify-center overflow-hidden rounded-full text-[14px] font-semibold"
          style={{ background: '#7FD1FF', boxShadow: '0 10px 26px rgba(40,81,184,.4)', color: '#0B1533' }}
        >
          <span className="relative">{principal?.ctaLabel ?? 'Saiba mais'}</span>
          <span
            className="sq-rail-sweep pointer-events-none absolute inset-y-0 left-0 w-10"
            style={{ background: 'linear-gradient(100deg, transparent, rgba(255,255,255,.6), transparent)' }}
          />
        </div>
        <div className="flex items-center justify-center gap-1.5 text-[11px]" style={{ color: 'rgba(255,255,255,.6)' }}>
          <span className="sq-rail-dot h-1.5 w-1.5 rounded-full" style={{ background: '#7FD1FF' }} />
          App em desenvolvimento
        </div>
      </div>

      <span className="absolute right-2.5 top-2 text-[8px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,.35)' }}>
        PUBLICIDADE
      </span>
    </a>
  )
}

// Card pequeno, variant "a" — estático (sem troca), badge com rise-in. 1:1 com o 2º bloco `isA`.
function CardPequenoA({ anuncio }: { anuncio: AnuncioLocal | null }) {
  return (
    <a
      href={anuncio?.targetUrl ?? '#'}
      target="_blank"
      rel="noopener noreferrer sponsored"
      aria-disabled={!anuncio}
      className={`relative box-border flex h-[230px] w-[240px] flex-col justify-between overflow-hidden rounded-lg p-4.5 no-underline ${!anuncio ? 'pointer-events-none' : ''}`}
      style={{ background: 'linear-gradient(155deg, #2A1F4D 0%, #17132B 60%, #0E0D14 100%)' }}
    >
      <div
        className="sq-rail-glow-a pointer-events-none absolute h-[180px] w-[180px] rounded-full"
        style={{ right: -60, top: -40, background: 'radial-gradient(circle, rgba(124,77,255,.42), rgba(124,77,255,0) 70%)' }}
      />
      <div className="relative flex flex-col gap-2">
        <span
          className="sq-rail-rise w-fit rounded-full px-2.5 py-1 text-[9px] font-bold uppercase tracking-wider"
          style={{ background: '#C9F26B', color: '#0E0D14' }}
        >
          Teste fechado
        </span>
        <div className="text-[19px] font-bold leading-tight tracking-tight text-white">{anuncio?.title ?? 'Espaço para anúncio'}</div>
        <div className="text-[12px] leading-snug" style={{ color: 'rgba(255,255,255,.68)' }}>
          {anuncio?.description ?? 'Banner simulado 240×230'}
        </div>
      </div>
      <div className="relative flex items-center justify-between gap-2">
        <span className="text-[12px] font-semibold text-white">{anuncio?.ctaLabel ?? 'Saiba mais'}</span>
      </div>
      <span className="absolute left-2.5 top-2 text-[8px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,.3)' }}>
        PUBLICIDADE
      </span>
    </a>
  )
}

// Card pequeno, variant "b" — contador "3 minutos" com pulso + ticker vertical entre 2 linhas
// fixas do protótipo (widget numérico, não um par de headlines administráveis — ver decisão
// registrada no handoff da Marina). CTA/link vêm do catálogo. 1:1 com o 2º bloco `isB`.
function CardPequenoB({ anuncio }: { anuncio: AnuncioLocal | null }) {
  return (
    <a
      href={anuncio?.targetUrl ?? '#'}
      target="_blank"
      rel="noopener noreferrer sponsored"
      aria-disabled={!anuncio}
      className={`relative box-border flex h-[230px] w-[240px] flex-col justify-between overflow-hidden rounded-lg p-4.5 no-underline ${!anuncio ? 'pointer-events-none' : ''}`}
      style={{ background: 'linear-gradient(150deg, #17132B, #0E0D14)' }}
    >
      <div
        className="sq-rail-glow-b pointer-events-none absolute h-[180px] w-[180px] rounded-full"
        style={{ left: -40, bottom: -50, background: 'radial-gradient(circle, rgba(201,242,107,.18), rgba(201,242,107,0) 70%)' }}
      />
      <div className="relative flex items-baseline gap-2">
        <span className="sq-rail-count inline-block text-[44px] font-bold leading-none tracking-tight" style={{ color: '#C9F26B' }}>
          3
        </span>
        <span className="text-[14px] font-medium" style={{ color: 'rgba(255,255,255,.8)' }}>
          minutos
        </span>
      </div>
      <div className="relative h-11 overflow-hidden">
        <div className="sq-rail-ticker flex flex-col gap-0">
          <span className="h-[22px] text-[13px] leading-[22px]" style={{ color: 'rgba(255,255,255,.75)' }}>
            para mapear a casa inteira.
          </span>
          <span className="h-[22px] text-[13px] leading-[22px]" style={{ color: 'rgba(255,255,255,.75)' }}>
            e você sabe onde trocar o roteador.
          </span>
        </div>
      </div>
      <div className="relative flex items-center justify-between gap-2">
        <span className="text-[12px] font-semibold" style={{ color: '#C9F26B' }}>
          {anuncio?.ctaLabel ?? 'Saiba mais'} →
        </span>
      </div>
      <span className="absolute right-2.5 top-2 text-[8px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,.3)' }}>
        PUBLICIDADE
      </span>
    </a>
  )
}
