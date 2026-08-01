import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import HomePage from './HomePage'
import type { SpeedTestResult } from '../lib/speedEngine'

// `useSpeedTest` controla toda a máquina de estados do teste (idle/running/
// resultado/problema) — mockado aqui para exercitar cada variante da Home
// (reconstrução v2, `ScreenHome.dc.html`) sem depender do motor de medição
// real nem de temporizadores.
const useSpeedTestMock = vi.fn()
vi.mock('../hooks/useSpeedTest', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../hooks/useSpeedTest')>()
  return { ...actual, useSpeedTest: () => useSpeedTestMock() }
})

function makeResult(overrides: Partial<SpeedTestResult> = {}): SpeedTestResult {
  return {
    id: 'r1',
    timestamp: Date.now(),
    download: { mbps: 92.4, peakMbps: 92.4 },
    upload: { mbps: 41.2, peakMbps: 41.2 },
    mode: 'rapido',
    status: 'complete',
    latency: { ms: 14, samples: 14, validSamples: 14, timeouts: 0, maxMs: 14, p95Ms: 14, peaks: 0 },
    jitter: { ms: 3.2 },
    packetLoss: { percent: 0 },
    loadedLatency: { downloadMs: 14, uploadMs: 14 },
    bufferbloat: { ms: 18.4, severity: 'mild' },
    stabilityScore: 94,
    dns: { latencyMs: 10, resolverIp: null, provider: 'cloudflare' },
    connectionType: null,
    server: 'teste',
    partial: false,
    ...overrides,
  }
}

const BASE_STATE = {
  liveValue: 0,
  phaseResults: {},
  result: null,
  connectionKind: null,
  round: null,
  cancelTest: vi.fn(),
  retry: vi.fn(),
  forceStart: vi.fn(),
  goToIdle: vi.fn(),
}

function renderHome() {
  return render(<HomePage />, { wrapper: MemoryRouter })
}

describe('HomePage — reconstrução v2 (ScreenHome)', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    useSpeedTestMock.mockReset()
  })

  it('idle -> mostra título, CTA "Iniciar teste" dentro do velocímetro e nav desktop + mobile', () => {
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
    useSpeedTestMock.mockReturnValue({ ...BASE_STATE, phase: 'idle' })

    renderHome()

    expect(screen.getByRole('heading', { level: 1, name: 'Teste de velocidade' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Iniciar teste' })).toBeInTheDocument()
    // SiteNav (desktop) e FlowTopBar (mobile) coexistem no DOM, alternados por CSS —
    // ambos os links "Teste" identificam a rota ativa em cada chrome.
    expect(screen.getAllByText('Teste').length).toBeGreaterThan(0)
  })

  it('running (fase download) -> mostra readout de fase, grade de 3 passos e cancelar', () => {
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
    useSpeedTestMock.mockReturnValue({ ...BASE_STATE, phase: 'download', liveValue: 78.3 })

    renderHome()

    // "Download" aparece tanto no readout do velocímetro quanto na grade de 3 passos.
    expect(screen.getAllByText('Download').length).toBeGreaterThan(0)
    expect(screen.getByText('78.3')).toBeInTheDocument()
    expect(screen.getByText('Cancelar teste')).toBeInTheDocument()
  })

  it('problema (sem-conexao) -> mostra painel de erro com ação de retry', () => {
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
    useSpeedTestMock.mockReturnValue({ ...BASE_STATE, phase: 'sem-conexao' })

    renderHome()

    expect(screen.getByText('Sem conexão')).toBeInTheDocument()
    expect(screen.getByText('Tentar novamente')).toBeInTheDocument()
  })

  it('resultado -> mostra o card de resultado e ESCONDE o AdBannerWide de rodapé da página (o card já embute o próprio anúncio)', () => {
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
    const result = makeResult()
    useSpeedTestMock.mockReturnValue({ ...BASE_STATE, phase: 'concluido', result, connectionKind: 'wifi' })

    renderHome()

    expect(screen.getByText('Sua conexão está boa')).toBeInTheDocument()
    // Nenhum <AdBannerWide> montado na página neste estado: showWideAd é `!isResult` no
    // protótipo, então o AdBannerWide de rodapé não deve renderizar. O card de Resultado
    // embute o `ResultAdCard` dedicado (achado da Marina/auditoria 1:1 de 2026-07-25 — antes
    // reaproveitava o `AdBannerWide` genérico), com copy própria do protótipo
    // (`ScreenHome.dc.html`), não o texto de fallback do `AdBannerWide`.
    expect(screen.queryByText('Banner simulado 320×50')).not.toBeInTheDocument()
    expect(screen.getByText('O Wi-Fi some no quarto?')).toBeInTheDocument()
  })
})
