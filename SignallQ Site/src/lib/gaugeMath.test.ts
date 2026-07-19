import { describe, expect, it } from 'vitest'
import { clamp, fractionForLatency, fractionForThroughput } from './gaugeMath'

describe('clamp', () => {
  it('mantém valor dentro do intervalo', () => expect(clamp(5, 0, 10)).toBe(5))
  it('limita no mínimo', () => expect(clamp(-5, 0, 10)).toBe(0))
  it('limita no máximo', () => expect(clamp(15, 0, 10)).toBe(10))
})

describe('fractionForLatency', () => {
  it('0 ms -> fração 1 (arco cheio)', () => expect(fractionForLatency(0)).toBe(1))
  it('150 ms ou mais -> fração 0', () => expect(fractionForLatency(150)).toBe(0))
  it('75 ms -> fração 0.5', () => expect(fractionForLatency(75)).toBeCloseTo(0.5, 5))
})

describe('fractionForThroughput', () => {
  it('0 Mbps -> fração 0', () => expect(fractionForThroughput(0)).toBe(0))
  it('300 Mbps -> fração 1', () => expect(fractionForThroughput(300)).toBe(1))
  it('nunca ultrapassa 1', () => expect(fractionForThroughput(1000)).toBe(1))
})
