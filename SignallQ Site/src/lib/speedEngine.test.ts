import { describe, expect, it } from 'vitest'
import { bytesToMbps, meanAbsJitter, median, SPEED_TEST_MODE_CONFIG, summarizeLatency } from './speedEngine'

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

describe('summarizeLatency', () => {
  it('descarta aquecimento, calcula perda e filtra pico acima de três medianas', () => {
    const result = summarizeLatency([5, 10, 12, 11, 100, null])
    expect(result.totalSamples).toBe(5)
    expect(result.timeouts).toBe(1)
    expect(result.packetLossPercent).toBeCloseTo(20)
    expect(result.ms).toBe(11)
    expect(result.peaks).toBe(1)
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
  const { rapido, completo } = SPEED_TEST_MODE_CONFIG

  it('"completo" coleta mais amostras de latência que "rapido"', () => {
    expect(completo.latencySampleCount).toBeGreaterThan(rapido.latencySampleCount)
  })

  it('"completo" transfere blocos maiores de download que "rapido"', () => {
    expect(completo.downloadPayloadBytes).toBeGreaterThan(rapido.downloadPayloadBytes)
  })

  it('"completo" transfere blocos maiores de upload que "rapido"', () => {
    expect(completo.uploadPayloadBytes).toBeGreaterThan(rapido.uploadPayloadBytes)
  })

  it('"completo" tem duração por fase maior que "rapido"', () => {
    expect(completo.downloadDurationMs).toBeGreaterThan(rapido.downloadDurationMs)
  })

  it('"rapido" replica a configuração Android', () => {
    expect(rapido).toEqual({
      latencySampleCount: 15,
      downloadDurationMs: 7000,
      uploadDurationMs: 7000,
      downloadPayloadBytes: 10_000_000,
      uploadPayloadBytes: 5_000_000,
      downloadInitialStreams: 2,
      downloadMaxStreams: 4,
      uploadInitialStreams: 4,
      uploadMaxStreams: 4,
      warmupMs: 1000,
    })
  })
})
