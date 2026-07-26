// Linha "Rede Cloudflare" — aparece tanto no estado idle quanto running
// (`ScreenHome.dc.html`, dentro do bloco `showDial`, não gated por
// isIdle/isRunning específico). Extraído como componente próprio porque as
// duas telas o compartilham 1:1. Reconstrução v2 (export 26 jul 2026): linha
// centralizada, sem moldura/card — divergência anterior (borda + rótulo
// "Infraestrutura" em duas linhas) corrigida na auditoria 1:1 da Marina.
export function InfraInfoCard() {
  return (
    <div className="flex w-full max-w-[520px]">
      <div className="flex flex-1 items-center justify-center gap-2.5 px-4 py-3">
        <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'var(--text-secondary)' }}>
          dns
        </span>
        <span className="label-large" style={{ color: 'var(--text-primary)' }}>
          Rede Cloudflare
        </span>
      </div>
    </div>
  )
}
