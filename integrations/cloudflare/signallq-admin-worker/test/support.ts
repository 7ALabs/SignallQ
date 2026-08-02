// Suporte de teste do backend admin de Feature Flags / Remote Config (issue #1478). Este worker
// nunca teve suíte automatizada antes (groundwork da PR #1493 só validou manualmente via curl
// contra produção) — este arquivo estabelece o padrão mínimo pros próximos testes do worker:
// FakeD1 (mesmo espírito de `signallq-diagnostic-worker/test/fake-d1.ts`, só o subconjunto de
// tabelas que este backend usa) + mock de `fetch` cobrindo OAuth token exchange e a Firebase
// Remote Config REST API (GET/PUT/:rollback/:listVersions), sem nenhuma chamada de rede real.
import { generateKeyPairSync } from 'node:crypto'
import type { Env } from '../src/index.ts'

type Row = Record<string, unknown>

function normalize(sql: string): string {
  return sql.replace(/\s+/g, ' ').trim().toLowerCase()
}

class FakeStatement {
  private bindings: unknown[] = []
  private readonly db: FakeD1Database
  private readonly sql: string

  constructor(db: FakeD1Database, sql: string) {
    this.db = db
    this.sql = sql
  }

  bind(...values: unknown[]): FakeStatement {
    this.bindings = values
    return this
  }

  async run(): Promise<{ success: boolean }> {
    this.db.execute(this.sql, this.bindings)
    return { success: true }
  }

  async first<T>(): Promise<T | null> {
    return this.db.first(this.sql, this.bindings) as T | null
  }

  async all<T>(): Promise<{ results: T[] }> {
    return { results: [] as T[] }
  }
}

export class FakeD1Database {
  auditLog: Row[] = []
  rateLimits = new Map<string, { count: number; window_start: number }>()
  systemErrors: Row[] = []

  prepare(sql: string): FakeStatement {
    return new FakeStatement(this, sql)
  }

  execute(sql: string, bindings: unknown[]): void {
    const q = normalize(sql)

    if (q.startsWith('insert into remote_config_audit_log')) {
      const [id, action, status, templateVersionBefore, templateVersionAfter, etagBefore, etagAfter, beforeJson, afterJson, message, actorUserId, actorRole, createdAt] = bindings
      this.auditLog.push({
        id, action, status,
        template_version_before: templateVersionBefore, template_version_after: templateVersionAfter,
        etag_before: etagBefore, etag_after: etagAfter,
        before_json: beforeJson, after_json: afterJson,
        message, actor_user_id: actorUserId, actor_role: actorRole, created_at: createdAt,
      })
      return
    }

    if (q.startsWith('insert or replace into remote_config_write_rate_limit')) {
      const [actorUserId, route, windowStart] = bindings
      this.rateLimits.set(`${String(actorUserId)}::${String(route)}`, { count: 1, window_start: Number(windowStart) })
      return
    }

    if (q.startsWith('update remote_config_write_rate_limit set count = count + 1')) {
      const [actorUserId, route] = bindings
      const key = `${String(actorUserId)}::${String(route)}`
      const row = this.rateLimits.get(key)
      if (row) row.count += 1
      return
    }

    if (q.startsWith('insert into system_errors')) {
      this.systemErrors.push({ bindings })
      return
    }

    // Qualquer outra escrita (fora do escopo deste backend) é ignorada silenciosamente — mesmo
    // espírito "fire-and-forget" do código real (ex.: logError nunca deve derrubar o teste).
  }

  first(sql: string, bindings: unknown[]): Row | null {
    const q = normalize(sql)

    if (q.startsWith('select count, window_start from remote_config_write_rate_limit')) {
      const [actorUserId, route] = bindings
      const row = this.rateLimits.get(`${String(actorUserId)}::${String(route)}`)
      return row ? { count: row.count, window_start: row.window_start } : null
    }

    if (q.startsWith('select s.user_id, u.role from admin_sessions')) {
      // Sem sessão real seedada — usado só pelo teste de nível HTTP sem cookie (sempre null).
      return null
    }

    return null
  }

  // Helper de teste — simula rate limit já estourado (teto real: WRITE_RATE_LIMIT_MAX = 20).
  seedRateLimitExceeded(actorUserId: string, route: string): void {
    this.rateLimits.set(`${actorUserId}::${route}`, { count: 25, window_start: Math.floor(Date.now() / 1000) })
  }
}

// --- Chave privada RSA de teste (nunca usada fora deste processo) — gerada uma vez por processo
// de teste pra evitar custo de RSA 2048 repetido em cada teste individual.
let cachedTestPrivateKeyPem: string | null = null
export function testFirebasePrivateKeyPem(): string {
  if (cachedTestPrivateKeyPem) return cachedTestPrivateKeyPem
  const { privateKey } = generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: { type: 'spki', format: 'pem' },
    privateKeyEncoding: { type: 'pkcs8', format: 'pem' },
  })
  cachedTestPrivateKeyPem = privateKey as unknown as string
  return cachedTestPrivateKeyPem
}

export function buildEnv(db: FakeD1Database, overrides: Partial<Env> = {}): Env {
  return {
    ALLOWED_ORIGIN: 'https://signallq-admin-panel.pages.dev',
    FIREBASE_PROJECT_ID: 'signallq-test',
    FIREBASE_GA4_PROPERTY_ID: '',
    FIREBASE_SYNC_ENABLED: 'false',
    ADMIN_SECRET: 'test-admin-secret',
    INGEST_KEY: 'test-ingest-key',
    FIREBASE_CLIENT_EMAIL: 'test@signallq-test.iam.gserviceaccount.com',
    FIREBASE_PRIVATE_KEY: testFirebasePrivateKeyPem(),
    GOOGLE_PLAY_CLIENT_EMAIL: '',
    GOOGLE_PLAY_PRIVATE_KEY: '',
    DB: db as unknown as Env['DB'],
    ADMIN_AUTH_PEPPER: 'test-pepper',
    ...overrides,
  } as Env
}

export interface FakeFirebaseHandle {
  fetch: typeof fetch
  state: {
    parameters: Record<string, unknown>
    parameterGroups: Record<string, unknown>
    conditions: unknown[]
    etag: string
    versionNumber: number
  }
  putCallCount: () => number
}

// Simula a Firebase Remote Config REST API (GET/PUT/:rollback/:listVersions) + a troca de token
// OAuth (`getFirebaseAccessToken`, que também usa `fetch` puro) — nenhuma chamada sai pra rede
// real durante os testes.
export function createFakeFirebase(projectId: string, initialParameters: Record<string, unknown> = {}): FakeFirebaseHandle {
  const base = `https://firebaseremoteconfig.googleapis.com/v1/projects/${projectId}/remoteConfig`
  const state = {
    parameters: { ...initialParameters },
    parameterGroups: {} as Record<string, unknown>,
    conditions: [] as unknown[],
    etag: 'etag-1',
    versionNumber: 1,
  }
  let putCalls = 0

  const templateBody = () => JSON.stringify({
    parameters: state.parameters,
    parameterGroups: state.parameterGroups,
    conditions: state.conditions,
    version: { versionNumber: String(state.versionNumber) },
  })

  const fetchImpl = (async (input: unknown, init?: { method?: string; headers?: Record<string, string>; body?: string }) => {
    const url = String(input)
    const method = (init?.method ?? 'GET').toUpperCase()

    if (url === 'https://oauth2.googleapis.com/token') {
      return new Response(JSON.stringify({ access_token: 'fake-access-token' }), { status: 200 })
    }

    if (url.startsWith(`${base}:listVersions`)) {
      return new Response(JSON.stringify({ versions: [{ versionNumber: String(state.versionNumber) }] }), { status: 200 })
    }

    if (url === `${base}:rollback` && method === 'POST') {
      state.versionNumber += 1
      state.etag = `etag-${state.versionNumber}`
      return new Response(templateBody(), { status: 200, headers: { ETag: state.etag } })
    }

    if (url === base && method === 'GET') {
      return new Response(templateBody(), { status: 200, headers: { ETag: state.etag } })
    }

    if (url === base && method === 'PUT') {
      putCalls += 1
      const ifMatch = init?.headers?.['If-Match']
      if (ifMatch !== state.etag) {
        return new Response(JSON.stringify({ error: { message: 'ETag mismatch' } }), { status: 412 })
      }
      const parsed = JSON.parse(init?.body ?? '{}') as {
        parameters?: Record<string, unknown>
        parameterGroups?: Record<string, unknown>
        conditions?: unknown[]
      }
      state.parameters = parsed.parameters ?? {}
      state.parameterGroups = parsed.parameterGroups ?? {}
      state.conditions = parsed.conditions ?? []
      state.versionNumber += 1
      state.etag = `etag-${state.versionNumber}`
      return new Response(templateBody(), { status: 200, headers: { ETag: state.etag } })
    }

    throw new Error(`fetch mock: rota não esperada em teste: ${method} ${url}`)
  }) as unknown as typeof fetch

  return { fetch: fetchImpl, state, putCallCount: () => putCalls }
}

// Troca `globalThis.fetch` durante a duração de `run()`, restaurando o original mesmo se `run`
// lançar — usado por todo teste que precisa da Firebase Remote Config API simulada.
export async function withMockedFetch<T>(mockFetch: typeof fetch, run: () => Promise<T>): Promise<T> {
  const original = globalThis.fetch
  globalThis.fetch = mockFetch
  try {
    return await run()
  } finally {
    globalThis.fetch = original
  }
}
