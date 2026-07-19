import { describe, expect, it } from 'vitest'
import { classifyDownload, classifyJitter, classifyLatency, classifyUpload, interpretUseCases } from './classification'

// Portados de shared/tests.js do protótipo, ajustados para os cortes reais em
// produção (3 níveis Boa/Aceitável/Ruim, ver comentário em classification.ts).
describe('classifyDownload', () => {
  it('24.9 Mbps -> Ruim', () => {
    expect(classifyDownload(24.9).label).toBe('Ruim')
  })
  it('25 Mbps -> Aceitável (limite inclusivo)', () => {
    expect(classifyDownload(25).label).toBe('Aceitável')
  })
  it('49.9 Mbps -> Aceitável', () => {
    expect(classifyDownload(49.9).label).toBe('Aceitável')
  })
  it('50 Mbps -> Boa (limite inclusivo)', () => {
    expect(classifyDownload(50).label).toBe('Boa')
  })
  it('500 Mbps -> Boa', () => {
    expect(classifyDownload(500).label).toBe('Boa')
  })
  it('null -> Indisponível (nunca fabrica número)', () => {
    expect(classifyDownload(null).label).toBe('Indisponível')
    expect(classifyLatency(undefined).label).toBe('Indisponível')
  })
})

describe('classifyUpload', () => {
  it('2 Mbps -> Ruim', () => expect(classifyUpload(2).label).toBe('Ruim'))
  it('3 Mbps -> Aceitável (limite inclusivo)', () => expect(classifyUpload(3).label).toBe('Aceitável'))
  it('10 Mbps -> Boa (limite inclusivo)', () => expect(classifyUpload(10).label).toBe('Boa'))
})

describe('classifyLatency', () => {
  it('15 ms -> Boa', () => expect(classifyLatency(15).label).toBe('Boa'))
  it('20 ms -> Aceitável (limite exclusivo p/ Boa)', () => expect(classifyLatency(20).label).toBe('Aceitável'))
  it('59 ms -> Aceitável', () => expect(classifyLatency(59).label).toBe('Aceitável'))
  it('60 ms -> Ruim', () => expect(classifyLatency(60).label).toBe('Ruim'))
})

describe('classifyJitter', () => {
  it('3 ms -> Boa', () => expect(classifyJitter(3).label).toBe('Boa'))
  it('40 ms -> Ruim', () => expect(classifyJitter(40).label).toBe('Ruim'))
})

describe('interpretUseCases', () => {
  it('streaming com boa banda e latência baixa -> Boa', () => {
    expect(interpretUseCases({ download: 100, upload: 20, latency: 10, jitter: 2 }).streaming.label).toBe('Boa')
  })
  it('jogos online com latência alta -> Ruim', () => {
    expect(interpretUseCases({ download: 100, upload: 20, latency: 180, jitter: 40 }).jogosOnline.label).toBe('Ruim')
  })
  it('videochamada com upload baixo -> Ruim', () => {
    expect(interpretUseCases({ download: 100, upload: 0.5, latency: 20, jitter: 5 }).videochamada.label).toBe('Ruim')
  })
})
