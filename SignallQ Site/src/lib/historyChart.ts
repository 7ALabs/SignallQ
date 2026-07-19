// Cálculo puro do gráfico de evolução do histórico (SVG polyline) — porte do
// Historico.dc.html, extraído para função testável.
export function niceMax(v: number): number {
  if (v <= 0) return 100
  const mag = Math.pow(10, Math.floor(Math.log10(v)))
  const norm = v / mag
  const nice = norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10
  return nice * mag
}

export interface PontoSerie {
  timestamp: number
  value: number
}

export interface ChartData {
  points: { x: string; y: string }[]
  polyline: string
  gridLines: { y: number; label: string }[]
  fromLabel: string
  toLabel: string
}

export function buildHistoryChart(chronological: PontoSerie[]): ChartData {
  const values = chronological.map((p) => p.value)
  const maxV = niceMax(Math.max(...values, 1) * 1.15)
  const minT = chronological.length ? chronological[0].timestamp : 0
  const maxT = chronological.length ? chronological[chronological.length - 1].timestamp : 1
  const spanT = maxT - minT || 1

  const points = chronological.map((p) => {
    const x = 40 + (chronological.length === 1 ? 290 : ((p.timestamp - minT) / spanT) * 580)
    const y = 10 + (1 - p.value / maxV) * 180
    return { x: x.toFixed(1), y: y.toFixed(1) }
  })
  const polyline = points.map((p) => `${p.x},${p.y}`).join(' ')
  const gridLines = [0, 1, 2, 3].map((i) => {
    const v = Math.round(maxV - (maxV / 3) * i)
    return { y: 10 + (maxV ? (1 - v / maxV) * 180 : 0), label: `${v}` }
  })
  const fmtMonth = (ts: number) => new Date(ts).toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' })

  return {
    points,
    polyline,
    gridLines,
    fromLabel: chronological.length ? fmtMonth(minT) : '',
    toLabel: chronological.length ? fmtMonth(maxT) : '',
  }
}
