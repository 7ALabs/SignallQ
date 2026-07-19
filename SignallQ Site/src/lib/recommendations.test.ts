import { describe, expect, it } from 'vitest'
import { buildRecommendations } from './recommendations'
import type { MedicaoRegistro } from './historyStore'
import type { SpeedTestResult } from './speedEngine'

function makeResult(overrides: Partial<{ download: number; upload: number; latency: number; jitter: number | null }> = {}): SpeedTestResult {
  const { download = 80, upload = 20, latency = 15, jitter = 5 } = overrides
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
    partial: false,
  }
}

function makeHistoryRecord(overrides: Partial<MedicaoRegistro> = {}): MedicaoRegistro {
  return {
    id: 'h1',
    timestamp: Date.now(),
    download: 10,
    upload: 5,
    latency: 40,
    jitter: 10,
    connectionType: null,
    connectionKind: null,
    server: 'teste',
    ...overrides,
  }
}

describe('buildRecommendations', () => {
  it('resultado excelente -> nenhum card (nunca força recomendação sem problema)', () => {
    expect(buildRecommendations(makeResult())).toEqual([])
  })

  it('download indisponível -> recomenda verificar conexão com ação de repetir teste', () => {
    const recs = buildRecommendations(makeResult({ download: NaN }))
    expect(recs).toHaveLength(1)
    expect(recs[0].id).toBe('check_conn')
    expect(recs[0].actionType).toBe('repeat_test')
  })

  it('download baixo recorrente no histórico -> recomenda contato com a operadora', () => {
    const history = Array.from({ length: 4 }, () => makeHistoryRecord({ download: 8 }))
    const recs = buildRecommendations(makeResult({ download: 10 }), history)
    expect(recs.some((r) => r.id === 'contact_op')).toBe(true)
  })

  it('download baixo sem recorrência -> recomenda fechar apps, não operadora', () => {
    const recs = buildRecommendations(makeResult({ download: 10 }), [])
    expect(recs.some((r) => r.id === 'close_apps')).toBe(true)
    expect(recs.some((r) => r.id === 'contact_op')).toBe(false)
  })

  it('latência alta -> recomenda aproximar do roteador', () => {
    const recs = buildRecommendations(makeResult({ latency: 300 }))
    expect(recs.some((r) => r.id === 'move_router')).toBe(true)
  })

  it('upload fraco -> recomenda cabo para chamadas', () => {
    const recs = buildRecommendations(makeResult({ upload: 1 }))
    expect(recs.some((r) => r.id === 'upload_warn')).toBe(true)
  })

  it('nunca retorna mais que 3 recomendações', () => {
    const recs = buildRecommendations(makeResult({ download: 5, upload: 1, latency: 300, jitter: 60 }))
    expect(recs.length).toBeLessThanOrEqual(3)
  })
})
