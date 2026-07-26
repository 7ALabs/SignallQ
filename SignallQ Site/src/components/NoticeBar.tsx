export type NoticeSeverity = 'warning' | 'error' | 'success' | 'info'

interface NoticeBarProps {
  severity: NoticeSeverity
  icon?: string
  message: React.ReactNode
}

const SEVERITY_VAR: Record<NoticeSeverity, string> = {
  warning: 'var(--warning)',
  error: 'var(--error)',
  success: 'var(--success)',
  info: 'var(--accent)',
}

const SEVERITY_ICON: Record<NoticeSeverity, string> = {
  warning: 'warning',
  error: 'error',
  success: 'check_circle',
  info: 'info',
}

// Extraído de `ResultPanel.tsx` (auditoria 1:1 2026-07-26) — a faixa de aviso de
// resultado parcial era específica de speedtest; vira componente genérico
// (`severity`/`message` como props) para reaproveitar em qualquer outro aviso do site.
export function NoticeBar({ severity, icon, message }: NoticeBarProps) {
  const color = SEVERITY_VAR[severity]
  return (
    <div className="flex items-start gap-2 rounded-xl" style={{ background: `color-mix(in srgb, ${color} 10%, transparent)`, padding: '12px 14px' }}>
      <span className="material-symbols-outlined" style={{ fontSize: 16, color }}>
        {icon ?? SEVERITY_ICON[severity]}
      </span>
      <div className="body-small">{message}</div>
    </div>
  )
}
