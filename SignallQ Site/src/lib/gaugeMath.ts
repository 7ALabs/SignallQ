// Geometria do velocímetro (arco SVG) — porte 1:1 das constantes do protótipo
// (Home.dc.html), extraídas para funções puras e testáveis.
export const GAUGE_CX = 160
export const GAUGE_CY = 168
export const GAUGE_R = 132
export const GAUGE_ARC_LEN = Math.PI * GAUGE_R
export const GAUGE_ARC_PATH = `M ${GAUGE_CX - GAUGE_R} ${GAUGE_CY} A ${GAUGE_R} ${GAUGE_R} 0 0 1 ${GAUGE_CX + GAUGE_R} ${GAUGE_CY}`

export function pointOnArc(radius: number, fraction: number): { x: number; y: number } {
  const rad = ((180 - fraction * 180) * Math.PI) / 180
  return { x: GAUGE_CX + radius * Math.cos(rad), y: GAUGE_CY - radius * Math.sin(rad) }
}

export interface Tick {
  x1: number
  y1: number
  x2: number
  y2: number
}

export const GAUGE_TICKS: Tick[] = [0, 0.25, 0.5, 0.75, 1].map((f) => {
  const a = pointOnArc(GAUGE_R + 14, f)
  const b = pointOnArc(GAUGE_R + 26, f)
  return { x1: a.x, y1: a.y, x2: b.x, y2: b.y }
})

export function clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v))
}

// Fração do arco preenchida por fase — mesma curva do protótipo:
// latência usa escala linear invertida (menor = mais cheio), throughput usa
// raiz quadrada para não saturar o arco cedo demais em conexões rápidas.
export function fractionForLatency(ms: number): number {
  return clamp(1 - ms / 150, 0, 1)
}

export function fractionForThroughput(mbps: number): number {
  return clamp(Math.sqrt(mbps / 300), 0, 1)
}
