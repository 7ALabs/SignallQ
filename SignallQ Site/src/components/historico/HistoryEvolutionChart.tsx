import { buildChartGeometry, buildEvolutionSeries } from '../../lib/historyChartMath'
import type { MedicaoRegistro } from '../../lib/historyStore'

interface HistoryEvolutionChartProps {
  /** Registros na ordem de `listRecords()` (mais recente primeiro). */
  records: MedicaoRegistro[]
}

// Card "Evolução das medições" — gráfico novo do Histórico na reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHistorico.dc.html`).
// Mostra download/upload ao longo do tempo, sempre a partir do histórico
// completo (não é filtrado pelo chip Wi-Fi/Rede móvel/Ethernet/Todos — o
// protótipo não tinha o filtro conectado à série, e a leitura de "evolução
// geral" fica mais útil sem recortar por tipo de conexão).
export function HistoryEvolutionChart({ records }: HistoryEvolutionChartProps) {
  const series = buildEvolutionSeries(records)
  const geometry = buildChartGeometry(series)

  if (!geometry || series.length < 2) return null

  return (
    <div className="flex flex-col gap-2 rounded-2xl box-border" style={{ background: 'var(--bg-secondary)', padding: '14px 16px', boxShadow: '0 10px 26px rgba(0,0,0,.16)' }}>
      <div className="flex items-center justify-between gap-3">
        <span className="label-overline">Evolução das medições</span>
        <div className="flex items-center gap-3.5">
          <div className="flex items-center gap-1.5">
            <span className="block h-[3px] w-3 rounded-sm" style={{ background: 'var(--phase-download)' }} />
            <span className="body-small">Download</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="block h-[3px] w-3 rounded-sm" style={{ background: 'var(--accent)' }} />
            <span className="body-small">Upload</span>
          </div>
        </div>
      </div>

      <svg
        viewBox={`0 0 ${geometry.width} ${geometry.height}`}
        preserveAspectRatio="none"
        className="block h-[120px] w-full"
        role="img"
        aria-label="Gráfico de evolução de download e upload ao longo do tempo"
      >
        {geometry.gridLines.map((y, i) => (
          <line
            key={i}
            x1={0}
            y1={y}
            x2={geometry.width}
            y2={y}
            stroke="color-mix(in srgb, var(--border) 18%, transparent)"
            strokeWidth={1}
          />
        ))}
        <path d={geometry.downloadAreaPath} fill="color-mix(in srgb, var(--phase-download) 12%, transparent)" stroke="none" />
        <path d={geometry.downloadPath} fill="none" stroke="var(--phase-download)" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
        <path d={geometry.uploadPath} fill="none" stroke="var(--accent)" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
        {geometry.downloadDots.map((d, i) => (
          <circle key={`d-${i}`} cx={d.x} cy={d.y} r={4} fill="var(--phase-download)" stroke="var(--bg-secondary)" strokeWidth={2} />
        ))}
        {geometry.uploadDots.map((d, i) => (
          <circle key={`u-${i}`} cx={d.x} cy={d.y} r={4} fill="var(--accent)" stroke="var(--bg-secondary)" strokeWidth={2} />
        ))}
      </svg>

      <div className="relative h-4 w-full">
        {geometry.labels.map((l) => (
          <span
            key={l.index}
            className="absolute -translate-x-1/2 whitespace-nowrap"
            style={{ left: `${(l.x / geometry.width) * 100}%`, font: '400 11px/1.45 var(--font-sans)', color: 'var(--text-tertiary)' }}
          >
            {l.text}
          </span>
        ))}
      </div>
    </div>
  )
}
