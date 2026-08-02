import Link from 'next/link'

// Topo mínimo do fluxo do PWA (telas Velocidade/Resultado/Histórico) — sem o
// SiteNav institucional completo. Protótipo "SignallQ WebApp.dc.html" do Luiz
// (GH#1186): "Sem barra de navegação inferior — o PWA é enxuto: teste,
// resultado e histórico". As páginas institucionais (/quem-somos, /termos etc.)
// continuam usando SiteNav/SiteFooter normalmente — não são tocadas aqui.

interface FlowTopBarProps {
  onHistoryClick: () => void
}

export function FlowTopBar({ onHistoryClick }: FlowTopBarProps) {
  return (
    <div className="flex w-full items-center justify-between px-5 py-3.5 box-border">
      <Link href="/" className="flex items-center" aria-label="SignallQ">
        <img src="/signallq-symbol.png" alt="" height={26} style={{ height: 26, width: 'auto', display: 'block' }} />
      </Link>
      <nav className="hidden items-center gap-5 sm:flex" aria-label="Navegação principal">
        <Link href="/" className="label-medium no-underline" style={{ color: 'var(--text-primary)' }}>Teste</Link>
        <Link href="/como-medimos" className="label-medium no-underline" style={{ color: 'var(--text-primary)' }}>Como funciona</Link>
        <a href="https://play.google.com/store/apps/details?id=io.signallq.app" className="label-medium no-underline" style={{ color: 'var(--text-primary)' }}>Aplicativo</a>
      </nav>
      <button
        onClick={onHistoryClick}
        aria-label="Ver histórico"
        className="flex h-10 w-10 items-center justify-center rounded-full border-none bg-transparent"
      >
        <span className="material-symbols-outlined" style={{ color: 'var(--text-primary)' }}>
          history
        </span>
      </button>
    </div>
  )
}

interface DetailTopBarProps {
  title: string
  onBack: () => void
  rightIcon: string
  rightLabel: string
  onRightClick: () => void
}

export function DetailTopBar({ title, onBack, rightIcon, rightLabel, onRightClick }: DetailTopBarProps) {
  return (
    <div className="flex w-full items-center justify-between px-2 py-3.5 box-border">
      <button onClick={onBack} aria-label="Voltar" className="flex h-10 w-10 items-center justify-center rounded-full border-none bg-transparent">
        <span className="material-symbols-outlined" style={{ color: 'var(--text-primary)' }}>
          arrow_back
        </span>
      </button>
      <div className="title-medium flex-1 text-center">{title}</div>
      <button onClick={onRightClick} aria-label={rightLabel} className="flex h-10 w-10 items-center justify-center rounded-full border-none bg-transparent">
        <span className="material-symbols-outlined" style={{ color: 'var(--text-primary)' }}>
          {rightIcon}
        </span>
      </button>
    </div>
  )
}


