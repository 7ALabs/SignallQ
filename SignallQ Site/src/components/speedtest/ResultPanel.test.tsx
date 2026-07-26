import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ResultPanel } from './ResultPanel'
import { AdSlotsProvider } from '../AdSlotsProvider'
import { classifyDownload } from '../../lib/classification'
import type { SpeedTestResult } from '../../lib/speedEngine'

function makeResult(overrides: Partial<{ download: number; upload: number; latency: number; jitter: number | null; partial: boolean }> = {}): SpeedTestResult {
  const { download = 80, upload = 20, latency = 15, jitter = 5, partial = false } = overrides
  return {
    id: 'r1',
    timestamp: Date.now(),
    download: { mbps: download, peakMbps: download },
    upload: { mbps: upload, peakMbps: upload },
    mode: 'rapido',
    status: partial ? 'partial' : 'complete',
    latency: { ms: latency, samples: 14, validSamples: 14, timeouts: 0, maxMs: latency, p95Ms: latency, peaks: 0 },
    jitter: jitter == null ? null : { ms: jitter },
    packetLoss: { percent: 0 },
    loadedLatency: { downloadMs: latency, uploadMs: latency },
    bufferbloat: { ms: 0, severity: 'none' },
    stabilityScore: 100,
    dns: { latencyMs: 10, resolverIp: null, provider: 'cloudflare' },
    connectionType: null,
    server: 'teste',
    partial,
  }
}

describe('ResultPanel — versão enxuta do PWA (sem recomendações/casos de uso)', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('conexão boa -> mostra veredito positivo, métricas principais e chip de conexão', () => {
    // ResultPanel embute o AdBannerWide (achado 1, reconstrução v2) — sem
    // mock, o fallback do anúncio local dispararia um fetch real.
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
    const result = makeResult()
    render(
      <AdSlotsProvider>
        <ResultPanel
          result={result}
          downloadVerdict={classifyDownload(result.download.mbps)}
          connectionKind="wifi"
          onRetry={vi.fn()}
          onVerHistorico={vi.fn()}
        />
      </AdSlotsProvider>
    )

    expect(screen.getByText('Sua conexão está boa')).toBeInTheDocument()
    expect(screen.getByText(/Teste realizado via Wi-Fi/)).toBeInTheDocument()
    expect(screen.getByText('Download')).toBeInTheDocument()
    expect(screen.getByText('Upload')).toBeInTheDocument()
    expect(screen.getByText('Testar novamente')).toBeInTheDocument()
    expect(screen.getByText('Ver histórico')).toBeInTheDocument()
    expect(screen.queryByText('Recomendações')).not.toBeInTheDocument()
  })

  it('resultado parcial -> mostra aviso de resultado parcial e esconde o chip de conexão quando desconhecida', () => {
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
    const result = makeResult({ download: 10, partial: true })
    render(
      <AdSlotsProvider>
        <ResultPanel
          result={result}
          downloadVerdict={classifyDownload(result.download.mbps)}
          connectionKind={null}
          onRetry={vi.fn()}
          onVerHistorico={vi.fn()}
        />
      </AdSlotsProvider>
    )

    expect(screen.getByText(/Resultado parcial\./)).toBeInTheDocument()
    expect(screen.queryByText(/Teste realizado via/)).not.toBeInTheDocument()
  })
})
