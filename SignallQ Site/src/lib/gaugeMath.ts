// Geometria do velocímetro (arco SVG) — porte 1:1 das constantes da
// reconstrução v2 (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHome.dc.html`,
// script `renderVals()`), extraídas para funções puras e testáveis.
export const GAUGE_CX = 180
export const GAUGE_CY = 186
export const GAUGE_R = 136
export const GAUGE_ARC_LEN = Math.PI * GAUGE_R
export const GAUGE_ARC_PATH = `M ${GAUGE_CX - GAUGE_R} ${GAUGE_CY} A ${GAUGE_R} ${GAUGE_R} 0 0 1 ${GAUGE_CX + GAUGE_R} ${GAUGE_CY}`

// Raios do ponteiro (linha needleFrom -> needleTo), 1:1 com o protótipo.
export const GAUGE_NEEDLE_FROM_R = GAUGE_R - 36
export const GAUGE_NEEDLE_TO_R = GAUGE_R - 2

// Escala de rótulos exibida sob o arco — não-linear de propósito (o
// protótipo rotula posições igualmente espaçadas do arco com esses valores;
// a fração real percorrida continua vindo de fractionForLatency/
// fractionForThroughput, não desta lista).
export const GAUGE_SCALE_VALUES = ['0', '1', '5', '10', '20', '30', '50', '75', '100'] as const

export function pointOnArc(radius: number, fraction: number): { x: number; y: number } {
  const rad = ((180 - fraction * 180) * Math.PI) / 180
  return { x: GAUGE_CX + radius * Math.cos(rad), y: GAUGE_CY - radius * Math.sin(rad) }
}

export interface Tick {
  x1: number
  y1: number
  x2: number
  y2: number
  major: boolean
}

// 49 traços (i/48), traço maior a cada 6 (9 traços maiores — um por rótulo
// de escala), 1:1 com o protótipo.
export const GAUGE_TICKS: Tick[] = Array.from({ length: 49 }, (_, i) => {
  const fraction = i / 48
  const major = i % 6 === 0
  const a = pointOnArc(GAUGE_R + 11, fraction)
  const b = pointOnArc(GAUGE_R + (major ? 21 : 17), fraction)
  return { x1: a.x, y1: a.y, x2: b.x, y2: b.y, major }
})

export interface ScaleLabel {
  text: string
  leftPercent: number
  topPercent: number
}

// Posições dos 9 rótulos de escala (0/1/5/10/20/30/50/75/100), em % da
// viewBox 360x210 — usados como `left`/`top` absolutos sobre o SVG.
export const GAUGE_SCALE_LABELS: ScaleLabel[] = GAUGE_SCALE_VALUES.map((text, i) => {
  const p = pointOnArc(GAUGE_R + 36, i / 8)
  return { text, leftPercent: (p.x / 360) * 100, topPercent: (p.y / 210) * 100 }
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
