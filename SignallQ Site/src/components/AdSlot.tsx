import { useEffect, useRef, useState } from 'react'
import { ADSENSE_PUBLISHER_ID, ADSENSE_SLOT_RESULT } from '../lib/config'

interface AdSlotProps {
  format?: 'horizontal' | 'square'
  slot?: string
}

// Único slot de anúncio do site (pedido explícito do Luiz: sem popup/banner
// extra) — reserva espaço fixo (sem CLS) e só injeta o script real do AdSense
// quando ADSENSE_PUBLISHER_ID estiver configurado. Sem os IDs, mostra
// placeholder "ainda não configurado" — nunca um anúncio vazio ou quebrado.
export function AdSlot({ format = 'horizontal', slot }: AdSlotProps) {
  const hostRef = useRef<HTMLDivElement>(null)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    if (mounted || !hostRef.current) return
    const effectiveSlot = slot || ADSENSE_SLOT_RESULT
    if (!ADSENSE_PUBLISHER_ID || !effectiveSlot) return
    setMounted(true)

    const ins = document.createElement('ins')
    ins.className = 'adsbygoogle'
    ins.style.display = 'block'
    ins.style.width = '100%'
    ins.setAttribute('data-ad-client', ADSENSE_PUBLISHER_ID)
    ins.setAttribute('data-ad-slot', effectiveSlot)
    ins.setAttribute('data-ad-format', 'auto')
    ins.setAttribute('data-full-width-responsive', 'true')
    hostRef.current.innerHTML = ''
    hostRef.current.appendChild(ins)

    if (!document.querySelector('script[data-adsbygoogle-loader]')) {
      const s = document.createElement('script')
      s.async = true
      s.setAttribute('data-adsbygoogle-loader', '1')
      s.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_PUBLISHER_ID}`
      s.crossOrigin = 'anonymous'
      document.head.appendChild(s)
    }
    ;(window as unknown as { adsbygoogle: unknown[] }).adsbygoogle = (window as unknown as { adsbygoogle?: unknown[] }).adsbygoogle || []
    ;(window as unknown as { adsbygoogle: unknown[] }).adsbygoogle.push({})
  }, [mounted, slot])

  return (
    <div
      className="flex w-full flex-col items-center justify-center gap-1.5 rounded-2xl border border-dashed p-3 box-border"
      style={{
        minHeight: format === 'square' ? 250 : 100,
        borderColor: 'color-mix(in srgb, var(--border) 45%, transparent)',
        background: 'var(--bg-secondary)',
      }}
    >
      <div className="overline">Publicidade</div>
      <div ref={hostRef} className="flex w-full justify-center">
        {!mounted && <div className="body-small">Espaço reservado — AdSense ainda não configurado</div>}
      </div>
    </div>
  )
}
