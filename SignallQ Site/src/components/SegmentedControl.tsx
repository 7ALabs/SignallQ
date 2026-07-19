interface SegmentedControlOption<T extends string> {
  value: T
  label: string
}

interface SegmentedControlProps<T extends string> {
  options: Array<SegmentedControlOption<T>>
  value: T
  onChange: (value: T) => void
}

// Réplica visual em Tailwind/tokens CSS do SegmentedControl do design system
// (packages/design-system/src/controls/SegmentedControl.tsx) — o Site usa os
// tokens CSS diretamente em vez do pacote React (decisão de arquitetura já
// registrada, ver CLAUDE.md do Site), então replica o visual em vez de
// importar o componente React do pacote.
export function SegmentedControl<T extends string>({ options, value, onChange }: SegmentedControlProps<T>) {
  return (
    <div className="flex rounded-full border p-0.5" style={{ borderColor: 'var(--border)' }}>
      {options.map((opt) => {
        const active = opt.value === value
        return (
          <button
            key={opt.value}
            onClick={() => onChange(opt.value)}
            className="label-medium flex-1 rounded-full border-none px-3 py-2"
            style={{ background: active ? 'var(--accent)' : 'transparent', color: active ? '#fff' : 'var(--text-primary)' }}
          >
            {opt.label}
          </button>
        )
      })}
    </div>
  )
}
