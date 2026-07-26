export interface StepInfo {
  label: string
  value: string
  color: string
}

export function StepRow({ steps }: { steps: StepInfo[] }) {
  return (
    <div className="flex w-full max-w-[460px] overflow-hidden rounded-2xl border" style={{ borderColor: 'color-mix(in srgb, var(--border) 22%, transparent)' }}>
      {steps.map((step, i) => (
        <div
          key={step.label}
          className="flex flex-1 flex-col items-center gap-1"
          style={{ padding: '14px 8px', borderLeft: i === 0 ? 'none' : '1px solid color-mix(in srgb, var(--border) 22%, transparent)' }}
        >
          <div className="overline">{step.label}</div>
          <div className="label-large" style={{ color: step.color }}>
            {step.value}
          </div>
        </div>
      ))}
    </div>
  )
}
