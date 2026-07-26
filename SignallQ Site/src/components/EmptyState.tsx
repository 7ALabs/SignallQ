import type { CSSProperties, ReactNode } from 'react'

interface EmptyStateProps {
  /** Ícone opcional (algumas variações, ex. o corpo central do 404, não têm ícone). */
  icon?: string
  iconColor?: string
  iconSize?: number
  /** Rótulo curto opcional acima do título (ex.: "Erro 404"). */
  eyebrow?: ReactNode
  eyebrowClassName?: string
  title?: ReactNode
  titleClassName?: string
  /** `h1` para o corpo central do 404 (título real da página); `div` nos demais estados. */
  titleAs?: 'div' | 'h1'
  message: ReactNode
  messageClassName?: string
  messageStyle?: CSSProperties
  /** Nó de ação totalmente customizado (botão ou `Link`) — cada estado mantém seu próprio
   * estilo/ícone de ação, não há um "botão padrão" imposto pelo componente. */
  action?: ReactNode
  className?: string
  style?: CSSProperties
}

// Genérico (ícone/eyebrow opcional + título opcional + mensagem + ação opcional) — unifica
// a estrutura antes duplicada ad-hoc em `HistoricoPage.tsx` (carregando/vazio/indisponível),
// `ProblemPanel.tsx` (6 variações de erro do speedtest) e o corpo central de
// `NotFoundPage.tsx`. Extração estrutural (auditoria 1:1 2026-07-26): cada chamador
// continua controlando className/style de wrapper, mensagem e ação — texto e ícone de
// cada estado preservados 1:1, só a composição (slots) foi unificada, não o visual.
export function EmptyState({
  icon,
  iconColor = 'var(--text-tertiary)',
  iconSize = 32,
  eyebrow,
  eyebrowClassName,
  title,
  titleClassName = 'headline-small',
  titleAs = 'div',
  message,
  messageClassName = 'body-large',
  messageStyle,
  action,
  className,
  style,
}: EmptyStateProps) {
  const TitleTag = titleAs

  return (
    <div className={className} style={style}>
      {icon && (
        <span className="material-symbols-outlined" style={{ fontSize: iconSize, color: iconColor }}>
          {icon}
        </span>
      )}
      {eyebrow && <div className={eyebrowClassName}>{eyebrow}</div>}
      {title && <TitleTag className={titleClassName}>{title}</TitleTag>}
      <div className={messageClassName} style={messageStyle}>
        {message}
      </div>
      {action}
    </div>
  )
}
