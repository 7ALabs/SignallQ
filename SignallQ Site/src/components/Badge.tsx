import type { ReactNode } from 'react'

interface BadgeProps {
  children: ReactNode
}

// Badge simples (pill) no acento da marca — usado para o rótulo "Beta".
export function Badge({ children }: BadgeProps) {
  return (
    <span
      className="rounded-full px-2.5 py-0.5 text-xs font-medium"
      style={{ background: 'var(--accent)', color: 'var(--on-accent)' }}
    >
      {children}
    </span>
  )
}
