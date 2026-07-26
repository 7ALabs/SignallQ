import type { ReactNode } from 'react'

export interface KeyValueItem {
  key: string
  label: ReactNode
  value: ReactNode
}

interface KeyValueListProps {
  items: KeyValueItem[]
  className?: string
}

// Extraído de `ResultPanel.tsx` (auditoria 1:1 2026-07-26) — as tabelas "Contexto da
// execução" e "Detalhes técnicos" renderizavam suas listas chave-valor ad-hoc com a
// mesma marcação (`dl`/`dt`/`dd` em grid 2 colunas); vira componente genérico.
export function KeyValueList({ items, className }: KeyValueListProps) {
  return (
    <dl className={`grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5 body-small ${className ?? ''}`}>
      {items.map((item) => (
        <div key={item.key} style={{ display: 'contents' }}>
          <dt>{item.label}</dt>
          <dd className="text-right">{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}
