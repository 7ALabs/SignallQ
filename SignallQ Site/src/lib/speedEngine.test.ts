import { describe, expect, it } from 'vitest'
import { bytesToMbps, meanAbsJitter, median } from './speedEngine'

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
