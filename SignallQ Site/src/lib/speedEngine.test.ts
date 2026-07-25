import { describe, expect, it } from 'vitest'
import { bytesToMbps, meanAbsJitter, median, SPEED_TEST_MODE_CONFIG } from './speedEngine'

// Porte dos testes matemáticos de shared/tests.js — lógica de cálculo não mudou.
describe('median', () => {
  it('median([10,20,30]) = 20', () => expect(median([10, 20, 30])).toBe(20))
  it('median([10,20]) = 15', () => expect(median([10, 20])).toBe(15))
})

describe('meanAbsJitter', () => {
  it('meanAbsJitter([10,15,10]) ~= 5', () => {
    expect(meanAbsJitter([10, 15, 10])).toBeCloseTo(5, 2)
  })
  it('com menos de 2 amostras retorna null', () => {
    expect(meanAbsJitter([10])).toBeNull()
  })
})

describe('bytesToMbps', () => {
  it('1_000_000 bytes em 1000ms = 8 Mbps', () => {
    expect(bytesToMbps(1e6, 1000)).toBeCloseTo(8, 2)
  })
  it('nunca fabrica valor com ms=0', () => {
    expect(bytesToMbps(1e6, 0)).toBe(0)
  })
})

// GH#1367 — "Completo" precisa ser mensuravelmente mais rigoroso/longo que
// "Rápido", não só um rótulo diferente disparando o mesmo motor.
describe('SPEED_TEST_MODE_CONFIG (diferenciação Rápido x Completo)', () => {
  const sum = (nums: number[]) => nums.reduce((a, b) => a + b, 0)
  const { rapido, completo } = SPEED_TEST_MODE_CONFIG

  it('"completo" coleta mais amostras de latência que "rapido"', () => {
    expect(completo.latencySampleCount).toBeGreaterThan(rapido.latencySampleCount)
  })

  it('"completo" transfere mais bytes de download que "rapido"', () => {
    expect(sum(completo.downloadSizes)).toBeGreaterThan(sum(rapido.downloadSizes))
  })

  it('"completo" transfere mais bytes de upload que "rapido"', () => {
    expect(sum(completo.uploadSizes)).toBeGreaterThan(sum(rapido.uploadSizes))
  })

  it('"completo" tem teto de tempo por fase maior que "rapido"', () => {
    expect(completo.phaseTimeoutMs).toBeGreaterThan(rapido.phaseTimeoutMs)
  })

  it('"rapido" preserva os parâmetros originais do motor (comportamento pré-#1367)', () => {
    expect(rapido).toEqual({
      latencySampleCount: 7,
      downloadSizes: [4e6, 8e6, 16e6, 32e6],
      uploadSizes: [2e6, 4e6, 8e6, 16e6],
      phaseTimeoutMs: 12000,
    })
  })
})
