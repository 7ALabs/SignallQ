import { describe, expect, it } from 'vitest'
import { buildHistoryChart, niceMax } from './historyChart'

describe('niceMax', () => {
  it('0 ou negativo -> 100', () => {
    expect(niceMax(0)).toBe(100)
    expect(niceMax(-5)).toBe(100)
  })
  it('arredonda para o próximo nice number', () => {
    expect(niceMax(42)).toBe(50)
    expect(niceMax(120)).toBe(200)
  })
})

describe('buildHistoryChart', () => {
  it('lista vazia não gera polyline nem quebra', () => {
    const chart = buildHistoryChart([])
    expect(chart.polyline).toBe('')
    expect(chart.fromLabel).toBe('')
  })
  it('um único ponto fica centralizado horizontalmente', () => {
    const chart = buildHistoryChart([{ timestamp: 1000, value: 50 }])
    expect(chart.points).toHaveLength(1)
    expect(Number(chart.points[0].x)).toBeCloseTo(330, 0)
  })
})
