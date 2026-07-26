import { useId } from 'react'
import { useAdSlotItems, useAlwaysLocalAdSlot } from '../AdSlotsProvider'

// 4º espaço de anúncio (achado 1 do README de design-specs): embutido dentro do próprio card
// de resultado do PWA — issue #1402/#1405. Peça dedicada, distinta do `AdBannerWide` genérico
// (achado da auditoria 1:1 de 2026-07-25, Marina: o `ResultPanel` reaproveitava
// `<AdBannerWide variant="a" />` aqui, divergindo do protótipo, que embute uma peça própria
// com copy fixa de "Teste fechado" — `ScreenHome.dc.html`, linhas ~289-308).
//
// Nunca tenta AdSense real — é sempre catálogo local (mesmo padrão do `AdRail`), sem troca de
// headline (o protótipo não anima este card; usa só `sq-ad-bar`/`sq-ad-sweep`/`sq-ad-glow-a`,
// já existentes em `src/index.css` e reaproveitados aqui como pedido, sem recriar).
export function ResultAdCard() {
  const uid = useId()
  useAlwaysLocalAdSlot(uid)
  const [anuncio] = useAdSlotItems(`result-ad-${uid}`, 1)

  return (
    <a
      href={anuncio?.targetUrl ?? '#'}
      target="_blank"
      rel="noopener noreferrer sponsored"
      aria-disabled={!anuncio}
      aria-label="Publicidade"
      className={`relative box-border flex w-full flex-wrap items-center gap-3 overflow-hidden rounded-2xl p-3.5 no-underline ${!anuncio ? 'pointer-events-none' : ''}`}
      style={{ minHeight: 84, background: 'linear-gradient(105deg, #241A45 0%, #17132B 55%, #0E0D14 100%)' }}
    >
      <div
        className="sq-ad-glow-a pointer-events-none absolute h-[220px] w-[220px] rounded-full"
        style={{ left: '40%', top: -60, background: 'radial-gradient(circle, rgba(124,77,255,.4), rgba(124,77,255,0) 70%)' }}
      />

      <div className="relative flex h-[34px] shrink-0 items-end gap-1">
        {[14, 22, 32, 20, 12].map((h, i) => (
          <div
            key={i}
            className="sq-ad-bar w-1.5 rounded-sm"
            style={{ height: h, background: i === 2 ? '#C9F26B' : i % 2 === 1 ? '#7C4DFF' : 'rgba(255,255,255,.28)', animationDelay: `${i * 0.12}s` }}
          />
        ))}
      </div>

      <div className="relative flex min-w-[180px] flex-1 flex-col gap-0.5">
        <span
          className="w-fit self-start rounded-full px-2 py-0.5 text-[8px] font-bold uppercase tracking-wider"
          style={{ background: '#C9F26B', color: '#0E0D14' }}
        >
          Teste fechado
        </span>
        <div className="text-[17px] font-bold leading-tight tracking-tight text-white">{anuncio?.title ?? 'O Wi-Fi some no quarto?'}</div>
        <div className="text-[11px] leading-tight" style={{ color: 'rgba(255,255,255,.62)' }}>
          {anuncio?.description ?? 'O app SignallQ mede cômodo a cômodo e mostra onde o sinal morre.'}
        </div>
      </div>

      <div
        className="relative flex h-[38px] shrink-0 items-center overflow-hidden rounded-full px-5"
        style={{ background: 'linear-gradient(135deg, #7C4DFF, #5B21D6)' }}
      >
        <span className="relative text-[13px] font-semibold text-white">{anuncio?.ctaLabel ?? 'Entrar na lista de teste'}</span>
        <span
          className="sq-ad-sweep pointer-events-none absolute inset-y-0 left-0 w-[34px]"
          style={{ background: 'linear-gradient(100deg, transparent, rgba(255,255,255,.4), transparent)' }}
        />
      </div>

      <span className="absolute right-3 top-1.5 text-[8px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,.3)' }}>
        PUBLICIDADE
      </span>
    </a>
  )
}
