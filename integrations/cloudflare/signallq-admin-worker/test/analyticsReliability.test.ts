import assert from 'node:assert/strict'
import test from 'node:test'
import { getAnalyticsPlatformFilter, handleIngestAnalytics, handleIntegrationReadiness, handleProductAnalytics, normalizeAnalyticsTimestamp } from '../src/index.ts'
import { buildEnv } from './support.ts'

class IngestDb {
  readonly ids = new Set<string>()
  readonly statements: Array<{ sql: string; values: unknown[] }> = []

  prepare(sql: string) {
    const statement = {
      sql,
      values: [] as unknown[],
      bind(...values: unknown[]) { this.values = values; return this },
    }
    this.statements.push(statement)
    return statement
  }

  async batch(statements: Array<{ values: unknown[] }>) {
    return statements.map((statement) => {
      const id = String(statement.values[0])
      if (this.ids.has(id)) return { meta: { changes: 0 } }
      this.ids.add(id)
      return { meta: { changes: 1 } }
    })
  }
}

class ProductDb {
  readonly statements: Array<{ sql: string; values: unknown[] }> = []
  private firstCalls = 0
  private readonly sessionStartCount: number
  private readonly sessionEndCount: number

  constructor(sessionStartCount = 10, sessionEndCount = 8) {
    this.sessionStartCount = sessionStartCount
    this.sessionEndCount = sessionEndCount
  }

  prepare(sql: string) {
    const db = this
    return {
      values: [] as unknown[],
      bind(...values: unknown[]) { this.values = values; db.statements.push({ sql, values }); return this },
      async all() { return { results: [] } },
      async first() {
        db.firstCalls += 1
        switch (db.firstCalls) {
          case 1: return { avg_duration_ms: 42_000 }
          case 2: return { session_count: 8 }
          case 3: return { session_start_count: db.sessionStartCount, session_end_count: db.sessionEndCount }
          case 4: return { count: 455 }
          default: return { cohort_d1: 0, returned_d1: 0, cohort_d7: 0, returned_d7: 0, cohort_d30: 0, returned_d30: 0, total_devices: 0, avg_active_span_days: null, inactive_14d: 0, total_with_activity: 0 }
        }
      },
    }
  }
}

class ReadinessDb {
  prepare(_sql: string) {
    return {
      bind(key: string) {
        return {
          async first() {
            if (key === 'firebase_sync') return { value: JSON.stringify({ syncedAt: new Date().toISOString(), eventsImported: 3, crashesImported: 2 }) }
            if (key === 'google_play_sync') return { value: JSON.stringify({ syncedAt: new Date().toISOString(), reviewsSampled: 0 }) }
            return null
          },
        }
      },
    }
  }
}

test('normaliza milissegundos, aceita segundos e rejeita relógio inválido', () => {
  const now = 1_785_801_900
  assert.deepEqual(normalizeAnalyticsTimestamp(1_785_801_000_123, now), { timestamp: 1_785_801_000, normalized: true })
  assert.deepEqual(normalizeAnalyticsTimestamp(1_785_801_000, now), { timestamp: 1_785_801_000, normalized: false })
  assert.deepEqual(normalizeAnalyticsTimestamp(undefined, now), { timestamp: now, normalized: false })
  assert.deepEqual(normalizeAnalyticsTimestamp(9_999, now), { timestamp: null, normalized: false })
})

test('ingest informa normalização, rejeição parcial e idempotência por id', async () => {
  const db = new IngestDb()
  const env = buildEnv(db as any)
  const body = { events: [
    { id: 'milliseconds', name: 'session_start', timestamp: 1_785_801_000_123 },
    { id: 'missing', name: 'session_end' },
    { id: 'invalid', name: 'session_end', timestamp: 9_999 },
    { id: 'unknown', name: 'unlisted_event' },
  ] }
  const request = () => new Request('https://x/ingest/analytics', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) })

  const first = await handleIngestAnalytics(request(), env)
  assert.deepEqual(await first.json(), { ok: true, inserted: 2, normalized: 1, rejected: 2 })
  assert.equal(db.statements[0].values[3], 1_785_801_000)

  const second = await handleIngestAnalytics(request(), env)
  assert.deepEqual(await second.json(), { ok: true, inserted: 0, normalized: 1, rejected: 2 })
})

test('analytics de produto assume Android, rejeita all e devolve qualidade da cobertura', async () => {
  assert.equal(getAnalyticsPlatformFilter(new URL('https://x/admin/analytics/product')), 'android')
  assert.equal(getAnalyticsPlatformFilter(new URL('https://x/admin/analytics/product?platform=web')), 'web')
  assert.equal(getAnalyticsPlatformFilter(new URL('https://x/admin/analytics/product?platform=all')), null)

  const db = new ProductDb()
  const env = buildEnv(db as any)
  const response = await handleProductAnalytics(new Request('https://x/admin/analytics/product?platform=android'), env)
  const payload = await response.json() as { platform: string; dataQuality: { sessionEndRate: number; isSessionMetricReliable: boolean; excludedHistoricalTimestampEvents: number } }
  assert.equal(response.status, 200)
  assert.equal(payload.platform, 'android')
  assert.equal(payload.dataQuality.sessionEndRate, 0.8)
  assert.equal(payload.dataQuality.isSessionMetricReliable, true)
  assert.equal(payload.dataQuality.excludedHistoricalTimestampEvents, 455)
  assert.ok(db.statements.some((statement) => statement.sql.includes('platform = ?') && statement.sql.includes("app_version = 'site'")))
  assert.ok(db.statements.some((statement) => statement.sql.includes("COUNT(DISTINCT CASE WHEN event_name = 'session_start'")))

  const invalid = await handleProductAnalytics(new Request('https://x/admin/analytics/product?platform=all'), env)
  assert.equal(invalid.status, 400)
})

test('cobertura de session_end só libera retenção e duração a partir de 80%', async () => {
  const unreliable = await handleProductAnalytics(
    new Request('https://x/admin/metrics/analytics/product?platform=web'),
    buildEnv(new ProductDb(1_000, 799) as any),
  )
  const unreliablePayload = await unreliable.json() as {
    platform: string
    avg_session_duration_ms: number | null
    retention: unknown[]
    dataQuality: { sessionEndRate: number; isSessionMetricReliable: boolean }
  }
  assert.equal(unreliablePayload.platform, 'web')
  assert.equal(unreliablePayload.dataQuality.sessionEndRate, 0.799)
  assert.equal(unreliablePayload.dataQuality.isSessionMetricReliable, false)
  assert.equal(unreliablePayload.avg_session_duration_ms, null)
  assert.deepEqual(unreliablePayload.retention, [])

  const reliable = await handleProductAnalytics(
    new Request('https://x/admin/metrics/analytics/product?platform=web'),
    buildEnv(new ProductDb(1_000, 800) as any),
  )
  const reliablePayload = await reliable.json() as {
    avg_session_duration_ms: number | null
    dataQuality: { sessionEndRate: number; isSessionMetricReliable: boolean }
  }
  assert.equal(reliablePayload.dataQuality.sessionEndRate, 0.8)
  assert.equal(reliablePayload.dataQuality.isSessionMetricReliable, true)
  assert.equal(reliablePayload.avg_session_duration_ms, 42_000)
})

test('prontidão de integração não confunde credencial, sync, frescor e dados recebidos', async () => {
  const response = await handleIntegrationReadiness(
    new Request('https://x/admin/integrations/readiness'),
    buildEnv(new ReadinessDb() as any),
  )
  const payload = await response.json() as { integrations: Array<{ id: string; state: string; recordsReceived: number | null; apiReachability: string }> }
  const firebase = payload.integrations.find((item) => item.id === 'firebase_analytics')!
  const reviews = payload.integrations.find((item) => item.id === 'google_play_reviews')!
  assert.equal(firebase.state, 'ready')
  assert.equal(firebase.recordsReceived, 5)
  assert.equal(firebase.apiReachability, 'check_system_health')
  assert.equal(reviews.state, 'not_configured')
  assert.equal(reviews.recordsReceived, 0)
})
