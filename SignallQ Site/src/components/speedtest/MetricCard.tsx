interface MetricCardProps {
  icon: string
  iconColor: string
  label: string
  value: string
  detail: string
}

// Extraído de `ResultPanel.tsx` (auditoria 1:1 2026-07-26) — os 3 cards de métrica
// (download/upload/latência) da grade de Resultado eram inline e idênticos entre si;
// vira componente reutilizável, mesma marcação/estilo, sem mudança visual.
export function MetricCard({ icon, iconColor, label, value, detail }: MetricCardProps) {
  return (
    <div className="flex flex-col items-center gap-1 rounded-2xl px-2 py-5" style={{ background: 'var(--bg-secondary)' }}>
      <div className="flex items-center gap-1.5">
        <span className="material-symbols-outlined" style={{ fontSize: 16, color: iconColor }}>
          {icon}
        </span>
        <span className="label-overline">{label}</span>
      </div>
      <div className="font-bold" style={{ font: '700 34px/1.1 var(--font-sans)', color: 'var(--text-primary)' }}>
        {value}
      </div>
      <div className="body-small" style={{ color: 'var(--text-secondary)' }}>
        {detail}
      </div>
    </div>
  )
}
