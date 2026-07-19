import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ResultPanel } from './ResultPanel'
import { classifyDownload } from '../../lib/classification'
import type { SpeedTestResult } from '../../lib/speedEngine'

function makeResult(overrides: Partial<{ download: number; upload: number; latency: number; jitter: number | null; partial: boolean }> = {}): SpeedTestResult {
  const { download = 80, upload = 20, latency = 15, jitter = 5, partial = false } = overrides
  return {
    id: 'r1',
    timestamp: Date.now(),
    download: { mbps: download },
    upload: { mbps: upload },
    latency: { ms: latency },
    jitter: jitter == null ? null : { ms: jitter },
    loadedLatency: null,
    connectionType: null,
    server: 'teste',
    partial,
  }
}

describe('ResultPanel — versão enxuta do PWA (sem recomendações/casos de uso)', () => {
  it('conexão boa -> mostra veredito positivo, métricas principais e chip de conexão', () => {
    const result = makeResult()
    render(
      <ResultPanel
        result={result}
        downloadVerdict={classifyDownload(result.download.mbps)}
        connectionKind="wifi"
        onRetry={vi.fn()}
        onVerHistorico={vi.fn()}
      />
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
    const result = makeResult({ download: 10, partial: true })
    render(
      <ResultPanel
        result={result}
        downloadVerdict={classifyDownload(result.download.mbps)}
        connectionKind={null}
        onRetry={vi.fn()}
        onVerHistorico={vi.fn()}
      />
    )

    expect(screen.getByText(/Resultado parcial\./)).toBeInTheDocument()
    expect(screen.queryByText(/Teste realizado via/)).not.toBeInTheDocument()
  })
})
