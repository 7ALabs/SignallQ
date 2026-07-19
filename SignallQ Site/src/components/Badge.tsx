import type { ReactNode } from 'react'

interface BadgeProps {
  children: ReactNode
}

// Badge simples (pill) no acento da marca — usado para o rótulo "Beta".
export function Badge({ children }: BadgeProps) {
  return (
    <span
      className="rounded-full px-2.5 py-0.5 text-xs font-medium text-white"
      style={{ background: 'var(--accent)' }}
    >
      {children}
    </span>
  )
}
