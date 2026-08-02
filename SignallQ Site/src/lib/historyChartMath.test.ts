import { describe, expect, it } from 'vitest'
import {
  buildChartGeometry,
  buildEvolutionSeries,
  MAX_CHART_POINTS,
  sampleLabelIndexes,
  smoothPath,
} from './historyChartMath'
import type { MedicaoRegistro } from './historyStore'

const AGORA = new Date('2026-07-25T12:00:00Z').getTime()
const HORA = 60 * 60 * 1000

function registro(overrides: Partial<MedicaoRegistro>): MedicaoRegistro {
  return {
    id: 'r',
    timestamp: AGORA,
    download: 50,
    upload: 20,
    latency: 20,
    jitter: null,
    connectionType: '4g',
    connectionKind: 'wifi',
    server: 'sp',
    ...overrides,
  }
}

describe('buildEvolutionSeries', () => {
  it('inverte a ordem (listRecords retorna mais recente primeiro) para cronológica ascendente', () => {
    const records = [
      registro({ id: 'novo', timestamp: AGORA, download: 90 }),
      registro({ id: 'velho', timestamp: AGORA - HORA, download: 10 }),
    ]
    const serie = buildEvolutionSeries(records, AGORA)
    expect(serie.map((s) => s.download)).toEqual([10, 90])
  })

  it('limita a MAX_CHART_POINTS medições mais recentes', () => {
    const records = Array.from({ length: MAX_CHART_POINTS + 8 }, (_, i) =>
      registro({ id: `r${i}`, timestamp: AGORA - i * HORA, download: i })
    )
    const serie = buildEvolutionSeries(records, AGORA)
    expect(serie.length).toBe(MAX_CHART_POINTS)
    // O mais recente (i=0) deve ser o último ponto da série cronológica.
    expect(serie[serie.length - 1].download).toBe(0)
  })

  it('retorna vazio para histórico vazio', () => {
    expect(buildEvolutionSeries([], AGORA)).toEqual([])
  })
})

describe('smoothPath', () => {
  it('retorna string vazia para lista vazia', () => {
    expect(smoothPath([])).toBe('')
  })

  it('retorna um M simples para um único ponto', () => {
    expect(smoothPath([{ x: 10, y: 20 }])).toBe('M 10 20')
  })

  it('gera um comando M seguido de comandos C para múltiplos pontos', () => {
    const d = smoothPath([
      { x: 0, y: 0 },
      { x: 10, y: 10 },
      { x: 20, y: 0 },
    ])
    expect(d.startsWith('M 0 0')).toBe(true)
    expect(d.match(/C /g)?.length).toBe(2)
  })
})

describe('sampleLabelIndexes', () => {
  it('retorna todos os índices quando a contagem cabe no limite', () => {
    expect(sampleLabelIndexes(4, 5)).toEqual([0, 1, 2, 3])
  })

  it('sempre inclui o primeiro e o último índice ao amostrar', () => {
    const indices = sampleLabelIndexes(12, 5)
    expect(indices[0]).toBe(0)
    expect(indices[indices.length - 1]).toBe(11)
    expect(indices.length).toBeLessThanOrEqual(5)
  })

  it('retorna vazio para contagem zero', () => {
    expect(sampleLabelIndexes(0)).toEqual([])
  })
})

describe('buildChartGeometry', () => {
  it('retorna null para série vazia', () => {
    expect(buildChartGeometry([])).toBeNull()
  })

  it('gera pontos, paths e grid para uma série válida', () => {
    const serie = buildEvolutionSeries(
      [
        registro({ id: 'a', timestamp: AGORA - 2 * HORA, download: 40, upload: 10 }),
        registro({ id: 'b', timestamp: AGORA - HORA, download: 80, upload: 30 }),
        registro({ id: 'c', timestamp: AGORA, download: 60, upload: 20 }),
      ],
      AGORA
    )
    const geo = buildChartGeometry(serie)
    expect(geo).not.toBeNull()
    expect(geo!.downloadDots.length).toBe(3)
    expect(geo!.uploadDots.length).toBe(3)
    expect(geo!.downloadPath).toContain('M')
    expect(geo!.gridLines.length).toBe(3)
    expect(geo!.labels.length).toBeGreaterThan(0)
    // O ponto de maior valor (80 Mbps) deve ficar mais alto no SVG (y menor).
    const maiorValor = geo!.downloadDots[1]
    const menorValor = geo!.downloadDots[0]
    expect(maiorValor.y).toBeLessThan(menorValor.y)
  })

  it('escala dinamicamente conforme o maior valor da série (sem teto fixo de 200)', () => {
    const serieAlta = buildEvolutionSeries(
      [
        registro({ id: 'a', timestamp: AGORA - HORA, download: 500, upload: 100 }),
        registro({ id: 'b', timestamp: AGORA, download: 900, upload: 200 }),
      ],
      AGORA
    )
    const geo = buildChartGeometry(serieAlta)
    // Mesmo com valores > 200 Mbps, o ponto de maior download não deve encostar no topo (y >= PAD).
    expect(geo!.downloadDots[1].y).toBeGreaterThanOrEqual(0)
  })
})
