import { useEffect, useState } from 'react'
import { buscarAnunciosLocais, sortearAnuncioLocal, type AnuncioLocal } from '../lib/localAdsClient'

// Coluna de anúncio lateral (240px, só desktop) — issue #1402/#1403, reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/AdRail.dc.html`). Diferente do
// `AdBannerWide`, ainda não tem unidade de anúncio AdSense própria configurada — sempre
// mostra o catálogo local (house ad), duas peças empilhadas por coluna. Skin de cor
// roxo (`a`, coluna esquerda) ou azul (`b`, coluna direita), copiada 1:1 do protótipo.

type VarianteCor = 'a' | 'b'

interface AdRailProps {
  variant: VarianteCor
}

const ALTURAS_BARRAS = [34, 52, 40, 74, 96, 62, 84, 46, 30]

export function AdRail({ variant }: AdRailProps) {
  const [anuncios, setAnuncios] = useState<AnuncioLocal[]>([])

  useEffect(() => {
    let cancelado = false
    const controller = typeof AbortController !== 'undefined' ? new AbortController() : undefined

    buscarAnunciosLocais(controller?.signal).then((lista) => {
      if (!cancelado) setAnuncios(lista)
    })

    return () => {
      cancelado = true
      controller?.abort()
    }
  }, [])

  // Achado da Marina (auditoria 1:1 de 2026-07-25): o protótipo sempre mostra a
  // coluna de anúncio, mesmo sem catálogo cadastrado — os cards já sabem lidar
  // com `anuncio: null` (placeholder "Espaço para anúncio" + `aria-disabled`).
  // Abortar aqui fazia a coluna inteira sumir do DOM (`asideCount: 0` confirmado
  // com o catálogo real vazio em produção), divergindo do protótipo, que é
  // conteúdo fixo do design.
  const principal = sortearAnuncioLocal(anuncios)
  const restante = anuncios.filter((a) => a.id !== principal?.id)
  const secundario = sortearAnuncioLocal(restante.length > 0 ? restante : anuncios)

  return (
    <aside aria-label="Publicidade" className="hidden w-[240px] shrink-0 flex-col gap-3 lg:flex">
      {variant === 'a' ? (
        <>
          <CardGrande anuncio={principal} cor="roxo" />
          <CardPequeno anuncio={secundario} cor="roxo" />
        </>
      ) : (
        <>
          <CardGrande anuncio={principal} cor="azul" />
          <CardPequeno anuncio={secundario} cor="azul" />
        </>
      )}
    </aside>
  )
}

function CardGrande({ anuncio, cor }: { anuncio: AnuncioLocal | null; cor: 'roxo' | 'azul' }) {
  const roxo = cor === 'roxo'
  return (
    <a
      href={anuncio?.targetUrl ?? '#'}
      target="_blank"
      rel="noopener noreferrer sponsored"
      aria-disabled={!anuncio}
      className={`relative box-border flex h-[600px] w-[240px] flex-col justify-between overflow-hidden rounded-lg p-5 no-underline ${!anuncio ? 'pointer-events-none' : ''}`}
      style={{
        background: roxo
          ? 'linear-gradient(168deg, #241C42 0%, #17132B 46%, #0E0D14 100%)'
          : 'linear-gradient(200deg, #0B1533 0%, #131A3C 40%, #0E0D14 100%)',
      }}
    >
      <div
        className="pointer-events-none absolute h-[300px] w-[300px] rounded-full"
        style={{
          left: roxo ? -70 : undefined,
          top: roxo ? 168 : undefined,
          background: roxo
            ? 'radial-gradient(circle, rgba(91,33,214,.55), rgba(91,33,214,0) 68%)'
            : 'radial-gradient(circle, rgba(40,81,184,.5), rgba(40,81,184,0) 70%)',
        }}
      />

      <div className="relative flex flex-col gap-4">
        <span
          className="w-fit rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider"
          style={{ background: roxo ? '#C9F26B' : '#7FD1FF', color: roxo ? '#0E0D14' : '#0B1533' }}
        >
          Teste fechado
        </span>
        <div className="text-[26px] font-bold leading-tight tracking-tight text-white">
          {anuncio?.title ?? 'Espaço para anúncio'}
        </div>
        <div className="text-[13px] leading-relaxed" style={{ color: 'rgba(255,255,255,.72)' }}>
          {anuncio?.description ?? 'Banner simulado 240×600'}
        </div>
      </div>

      {roxo ? (
        <div className="relative flex h-24 items-end gap-[7px] overflow-hidden px-0.5">
          {ALTURAS_BARRAS.map((h, i) => (
            <div
              key={i}
              className="flex-1 rounded-sm"
              style={{ height: `${h}%`, background: i === 4 ? '#C9F26B' : i % 2 === 0 ? 'rgba(255,255,255,.16)' : 'rgba(124,77,255,.55)' }}
            />
          ))}
        </div>
      ) : (
        <div className="relative flex h-[150px] items-center justify-center">
          <span
            className="flex h-[46px] w-[46px] items-center justify-center rounded-full"
            style={{ background: 'rgba(127,209,255,.16)' }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 24, color: '#7FD1FF' }}>
              router
            </span>
          </span>
        </div>
      )}

      <div className="relative flex flex-col gap-2.5">
        <div
          className="flex h-11 items-center justify-center rounded-full text-[14px] font-semibold"
          style={{ background: roxo ? 'linear-gradient(135deg, #7C4DFF, #5B21D6)' : '#7FD1FF', color: roxo ? '#fff' : '#0B1533' }}
        >
          {anuncio?.ctaLabel ?? 'Saiba mais'}
        </div>
        <div className="flex items-center justify-center gap-1.5 text-[11px]" style={{ color: 'rgba(255,255,255,.6)' }}>
          <span className="h-1.5 w-1.5 rounded-full" style={{ background: roxo ? '#C9F26B' : '#7FD1FF' }} />
          App em desenvolvimento
        </div>
      </div>

      <span className="absolute right-2.5 top-2 text-[8px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,.35)' }}>
        PUBLICIDADE
      </span>
    </a>
  )
}

function CardPequeno({ anuncio, cor }: { anuncio: AnuncioLocal | null; cor: 'roxo' | 'azul' }) {
  const roxo = cor === 'roxo'
  return (
    <a
      href={anuncio?.targetUrl ?? '#'}
      target="_blank"
      rel="noopener noreferrer sponsored"
      aria-disabled={!anuncio}
      className={`relative box-border flex h-[230px] w-[240px] flex-col justify-between overflow-hidden rounded-lg p-4.5 no-underline ${!anuncio ? 'pointer-events-none' : ''}`}
      style={{
        background: roxo
          ? 'linear-gradient(155deg, #2A1F4D 0%, #17132B 60%, #0E0D14 100%)'
          : 'linear-gradient(150deg, #17132B, #0E0D14)',
      }}
    >
      <div
        className="pointer-events-none absolute h-[180px] w-[180px] rounded-full"
        style={{
          right: roxo ? -60 : undefined,
          top: roxo ? -40 : undefined,
          left: !roxo ? -40 : undefined,
          bottom: !roxo ? -50 : undefined,
          background: roxo
            ? 'radial-gradient(circle, rgba(124,77,255,.42), rgba(124,77,255,0) 70%)'
            : 'radial-gradient(circle, rgba(201,242,107,.18), rgba(201,242,107,0) 70%)',
        }}
      />

      <div className="relative flex flex-col gap-2">
        <span
          className="w-fit rounded-full px-2.5 py-1 text-[9px] font-bold uppercase tracking-wider"
          style={{ background: roxo ? '#C9F26B' : '#0E0D14', color: roxo ? '#0E0D14' : '#C9F26B' }}
        >
          {roxo ? 'Teste fechado' : 'Rápido'}
        </span>
        <div className="text-[19px] font-bold leading-tight tracking-tight text-white">
          {anuncio?.title ?? 'Espaço para anúncio'}
        </div>
        <div className="text-[12px] leading-snug" style={{ color: 'rgba(255,255,255,.68)' }}>
          {anuncio?.description ?? 'Banner simulado 240×230'}
        </div>
      </div>

      <div className="relative flex items-center justify-between gap-2">
        <span className="text-[12px] font-semibold" style={{ color: roxo ? '#fff' : '#C9F26B' }}>
          {anuncio?.ctaLabel ?? 'Saiba mais'}
        </span>
      </div>

      <span className="absolute left-2.5 top-2 text-[8px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,.3)' }}>
        PUBLICIDADE
      </span>
    </a>
  )
}
