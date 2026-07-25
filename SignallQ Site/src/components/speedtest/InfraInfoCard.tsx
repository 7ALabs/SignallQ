// Card "Infraestrutura: Rede Cloudflare" — aparece tanto no estado idle quanto
// running (`ScreenHome.dc.html`, dentro do bloco `showDial`, não gated por
// isIdle/isRunning específico). Extraído como componente próprio porque as
// duas telas o compartilham 1:1.
export function InfraInfoCard() {
  return (
    <div
      className="flex w-full max-w-[520px] overflow-hidden rounded-2xl border"
      style={{ borderColor: 'color-mix(in srgb, var(--border) 22%, transparent)' }}
    >
      <div className="flex flex-1 items-center gap-2.5 px-4 py-3">
        <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'var(--text-secondary)' }}>
          dns
        </span>
        <div className="flex flex-col">
          <span className="overline">Infraestrutura</span>
          <span className="label-large" style={{ color: 'var(--text-primary)' }}>
            Rede Cloudflare
          </span>
        </div>
      </div>
    </div>
  )
}
