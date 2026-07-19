import { Link } from 'react-router-dom'

// Topo mínimo do fluxo do PWA (telas Velocidade/Resultado/Histórico) — sem o
// SiteNav institucional completo. Protótipo "SignallQ WebApp.dc.html" do Luiz
// (GH#1186): "Sem barra de navegação inferior — o PWA é enxuto: teste,
// resultado e histórico". As páginas institucionais (/pro, /quem-somos etc.)
// continuam usando SiteNav/SiteFooter normalmente — não são tocadas aqui.

interface FlowTopBarProps {
  onHistoryClick: () => void
}

export function FlowTopBar({ onHistoryClick }: FlowTopBarProps) {
  return (
    <div className="flex w-full items-center justify-between px-5 py-3.5 box-border">
      <Link to="/" className="flex items-center" aria-label="SignallQ">
        <img src="/signallq-symbol.png" alt="" height={26} style={{ height: 26, width: 'auto', display: 'block' }} />
      </Link>
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
