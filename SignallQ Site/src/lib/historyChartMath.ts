// Matemática do gráfico "Evolução das medições" do Histórico — reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHistorico.dc.html`).
// Porte da curva suave (Catmull-Rom → Bézier) e do layout SVG do `<script
// data-dc-script>` do protótipo, adaptado para consumir `MedicaoRegistro[]`
// real (`historyStore.ts`) em vez da constante `SERIES` de exemplo.
//
// Divergências deliberadas do protótipo (documentadas aqui, não escondidas):
// - `MAX_CHART_POINTS = 12`: o protótipo mostra 4 pontos a título de exemplo;
//   12 é um limite razoável para o gráfico continuar legível sem virar ruído
//   visual com histórico extenso.
// - Escala vertical dinâmica (`maxValue` calculado a partir dos dados reais)
//   em vez do `MAX = 200` fixo do protótipo — histórico real pode superar ou
//   ficar bem abaixo de 200 Mbps; um teto fixo cortaria ou desperdiçaria a
//   régua vertical.
// - Rótulos do eixo X são amostrados (`sampleLabelIndexes`) em vez de 1
//   rótulo por ponto — com até 12 pontos, rotular todos empilharia texto
//   ilegível; sempre inclui o primeiro e o último ponto.
import type { MedicaoRegistro } from './historyStore'
import { formatarTempoRelativo } from './relativeTime'

export const MAX_CHART_POINTS = 12
export const MAX_CHART_LABELS = 5

export interface ChartPonto {
  label: string
  download: number
  upload: number
  timestamp: number
}

export interface Ponto2D {
  x: number
  y: number
}

export interface ChartGeometry {
  width: number
  height: number
  downloadPath: string
  uploadPath: string
  downloadAreaPath: string
  downloadDots: Ponto2D[]
  uploadDots: Ponto2D[]
  gridLines: number[]
  labels: Array<{ index: number; x: number; text: string }>
}

/**
 * `records` deve vir na ordem de `listRecords()` (mais recente primeiro).
 * Retorna a série em ordem cronológica (mais antigo → mais recente), limitada
 * às `MAX_CHART_POINTS` medições mais recentes.
 */
export function buildEvolutionSeries(records: MedicaoRegistro[], now: number = Date.now()): ChartPonto[] {
  return records
    .slice(0, MAX_CHART_POINTS)
    .map((r) => ({
      label: formatarTempoRelativo(r.timestamp, now),
      download: r.download,
      upload: r.upload,
      timestamp: r.timestamp,
    }))
    .reverse()
}

// Curva suave (Catmull-Rom → Bézier) — mesma matemática do protótipo.
export function smoothPath(points: Ponto2D[]): string {
  if (points.length === 0) return ''
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`

  let d = `M ${points[0].x} ${points[0].y}`
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i - 1] || points[i]
    const p1 = points[i]
    const p2 = points[i + 1]
    const p3 = points[i + 2] || p2
    const c1x = p1.x + (p2.x - p0.x) / 6
    const c1y = p1.y + (p2.y - p0.y) / 6
    const c2x = p2.x - (p3.x - p1.x) / 6
    const c2y = p2.y - (p3.y - p1.y) / 6
    d += ` C ${c1x.toFixed(1)} ${c1y.toFixed(1)}, ${c2x.toFixed(1)} ${c2y.toFixed(1)}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`
  }
  return d
}

// Sempre inclui o primeiro e o último índice; distribui o restante o mais
// uniformemente possível entre eles.
export function sampleLabelIndexes(count: number, maxLabels: number = MAX_CHART_LABELS): number[] {
  if (count <= 0) return []
  if (count <= maxLabels) return Array.from({ length: count }, (_, i) => i)

  const step = (count - 1) / (maxLabels - 1)
  const out = new Set<number>()
  for (let i = 0; i < maxLabels; i++) {
    out.add(Math.round(i * step))
  }
  return Array.from(out).sort((a, b) => a - b)
}

export function buildChartGeometry(series: ChartPonto[]): ChartGeometry | null {
  if (series.length === 0) return null

  const W = 600
  const H = 140
  const PAD = 12

  const allValues = series.flatMap((s) => [s.download, s.upload])
  const maxRaw = Math.max(...allValues, 1)
  // Teto arredondado com folga de 15% para a linha nunca encostar no topo.
  const maxValue = Math.max(maxRaw * 1.15, 1)

  const denom = Math.max(series.length - 1, 1)
  const px = (i: number) => (series.length === 1 ? W / 2 : PAD + (i * (W - PAD * 2)) / denom)
  const py = (v: number) => H - PAD - (Math.min(v, maxValue) / maxValue) * (H - PAD * 2)

  const downloadDots = series.map((s, i) => ({ x: px(i), y: py(s.download) }))
  const uploadDots = series.map((s, i) => ({ x: px(i), y: py(s.upload) }))

  const downloadPath = smoothPath(downloadDots)
  const uploadPath = smoothPath(uploadDots)
  const downloadAreaPath = series.length > 0
    ? `${downloadPath} L ${px(series.length - 1)} ${H - PAD} L ${px(0)} ${H - PAD} Z`
    : ''

  const gridLines = [0.25, 0.5, 0.75].map((f) => PAD + f * (H - PAD * 2))

  const labelIndexes = sampleLabelIndexes(series.length)
  const labels = labelIndexes.map((i) => ({ index: i, x: px(i), text: series[i].label }))

  return { width: W, height: H, downloadPath, uploadPath, downloadAreaPath, downloadDots, uploadDots, gridLines, labels }
}
