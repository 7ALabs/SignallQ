// Suíte automatizada do backend admin de Feature Flags / Firebase Remote Config (issue #1478,
// Épico #1347 F2). Cobre os cenários exigidos pelo corpo da issue: conflito de ETag, autenticação/
// autorização, flag inexistente, dependência inválida, proteção de flags críticas — mais merge
// preservando parâmetros não gerenciados, criação automática de parâmetro ausente, detecção de
// órfãos, dry-run vs execução real, e rate limit dedicado das rotas de escrita.
import assert from 'node:assert/strict'
import test from 'node:test'
import worker from '../src/index.ts'
import {
  handleRemoteConfigAdminGet,
  handleRemoteConfigAdminValidate,
  handleRemoteConfigAdminPublish,
  handleRemoteConfigAdminRollback,
  handleFeatureFlagsCatalogGet,
  handleFeatureFlagsSync,
  validateParametersAgainstCatalog,
  reasonRequiredMessage,
} from '../src/remoteConfigAdmin.ts'
import { loadFeatureFlagCatalog } from '../src/featureFlagCatalog.ts'
import { FakeD1Database, buildEnv, createFakeFirebase, withMockedFetch } from './support.ts'

const ADMIN_SESSION = { userId: 'admin-1', role: 'admin' }
const VIEWER_SESSION = { userId: 'viewer-1', role: 'viewer' }

// Flags reais do catálogo (#1477 + #1480) — usadas em vez de fixtures sintéticas pra provar que
// o backend valida contra o arquivo de verdade, não um mock do catálogo. Catálogo tem 10 entradas
// desde #1480 (F4): as 2 originais de #1477 + as 8 chaves principais de módulo instrumentadas.
const MEDIUM_KEY = 'consumer.speedtest.cloudflare_engine_enabled' // criticality MEDIUM
const HIGH_KEY = 'consumer.speedtest.enabled' // criticality HIGH
const CATALOG_FLAG_COUNT = 10

function jsonRequest(url: string, body: unknown, init: { method?: string; headers?: Record<string, string> } = {}): Request {
  return new Request(url, {
    method: init.method ?? 'POST',
    headers: { 'content-type': 'application/json', ...(init.headers ?? {}) },
    body: JSON.stringify(body),
  })
}

test('catálogo canônico expõe as flags reais do #1477/#1480', () => {
  const catalog = loadFeatureFlagCatalog()
  assert.equal(catalog.schemaVersion, '1.0')
  assert.equal(catalog.flags.length, CATALOG_FLAG_COUNT)
  assert.ok(catalog.flags.some((f) => f.key === HIGH_KEY))
})

test('GET /feature-flags/catalog serve o catálogo real', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await handleFeatureFlagsCatalogGet(new Request('https://x/admin/firebase/feature-flags/catalog'), env, ADMIN_SESSION)
  const payload = await resp.json() as { flagCount: number; adminManagedCount: number; flags: Array<{ key: string }> }
  assert.equal(resp.status, 200)
  assert.equal(payload.flagCount, CATALOG_FLAG_COUNT)
  assert.equal(payload.adminManagedCount, CATALOG_FLAG_COUNT)
  assert.ok(payload.flags.some((f) => f.key === MEDIUM_KEY))
})

// --- flag inexistente ---

test('validate rejeita flag inexistente no catálogo (não cria chave arbitrária)', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await handleRemoteConfigAdminValidate(
    jsonRequest('https://x/validate', { parameters: { 'consumer.nao_existe.enabled': { value: true } } }),
    env, ADMIN_SESSION
  )
  const payload = await resp.json() as { ok: boolean; errors: string[] }
  assert.equal(resp.status, 200)
  assert.equal(payload.ok, false)
  assert.ok(payload.errors.some((e) => e.includes('não existe no catálogo')))
})

// --- dependência inválida (catálogo sintético via lookup injetável) ---

test('validateParametersAgainstCatalog rejeita dependência inválida', () => {
  const fakeEntry = {
    key: 'consumer.foo.enabled', module: ':featureFoo', type: 'BOOLEAN' as const, defaultValue: true,
    criticality: 'LOW' as const, owner: 'x', description: 'x', disabledBehavior: 'SILENT_NO_OP',
    disabledMessage: null, dependencies: ['consumer.dependencia_fantasma.enabled'],
    androidImplemented: false, adminManaged: true, analyticsEvent: null,
  }
  const lookup = (key: string) => (key === fakeEntry.key ? fakeEntry : undefined)
  const result = validateParametersAgainstCatalog({ 'consumer.foo.enabled': { value: true } }, lookup)
  assert.ok(result.errors.some((e) => e.includes('dependência inválida')))
})

// --- proteção de flags críticas ---

test('reasonRequiredMessage exige motivo só para criticidade HIGH/CRITICAL', () => {
  const highEntry = { criticality: 'HIGH' as const, key: HIGH_KEY } as any
  const mediumEntry = { criticality: 'MEDIUM' as const, key: MEDIUM_KEY } as any
  assert.ok(reasonRequiredMessage([highEntry], null))
  assert.equal(reasonRequiredMessage([highEntry], 'rollout controlado'), null)
  assert.equal(reasonRequiredMessage([mediumEntry], null), null)
})

test('validate: flag HIGH sem reason retorna ok:false com mensagem de confirmação reforçada', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await handleRemoteConfigAdminValidate(
    jsonRequest('https://x/validate', { parameters: { [HIGH_KEY]: { value: false } } }),
    env, ADMIN_SESSION
  )
  const payload = await resp.json() as { ok: boolean; errors: string[] }
  assert.equal(payload.ok, false)
  assert.ok(payload.errors.some((e) => e.includes('Confirmação reforçada obrigatória')))
})

test('validate: flag HIGH com reason passa', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await handleRemoteConfigAdminValidate(
    jsonRequest('https://x/validate', { parameters: { [HIGH_KEY]: { value: false } }, reason: 'kill switch emergencial' }),
    env, ADMIN_SESSION
  )
  const payload = await resp.json() as { ok: boolean; errors: string[] }
  assert.equal(payload.ok, true)
  assert.deepEqual(payload.errors, [])
})

// --- autenticação/autorização ---

test('worker.fetch: publish sem cookie de sessão retorna 401 antes de chegar no handler', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await worker.fetch(
    jsonRequest('https://x/admin/firebase/remote-config/publish', { parameters: {} }, { headers: { 'If-Match': 'etag-1' } }),
    env
  )
  assert.equal(resp.status, 401)
})

test('publish: role não-admin retorna 403 (mesmo com body válido)', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await handleRemoteConfigAdminPublish(
    jsonRequest('https://x/publish', { parameters: { [MEDIUM_KEY]: { value: false } } }, { headers: { 'If-Match': 'etag-1' } }),
    env, VIEWER_SESSION
  )
  assert.equal(resp.status, 403)
})

test('rollback: role não-admin retorna 403', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await handleRemoteConfigAdminRollback(
    jsonRequest('https://x/rollback', { targetVersion: 3, reason: 'reverter incidente' }),
    env, VIEWER_SESSION
  )
  assert.equal(resp.status, 403)
})

// --- conflito de ETag ---

test('publish: If-Match desatualizado (ETag conflict) retorna 409 e grava auditoria', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {})
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleRemoteConfigAdminPublish(
      jsonRequest('https://x/publish', { parameters: { [MEDIUM_KEY]: { value: false } } }, { headers: { 'If-Match': 'etag-desatualizado' } }),
      env, ADMIN_SESSION
    )
  )

  assert.equal(resp.status, 409)
  assert.equal(fake.putCallCount(), 0, 'não deve nem tentar publicar com ETag divergente')
  assert.ok(db.auditLog.some((row) => row.action === 'publish' && row.status === 'error' && String(row.message).includes('etag_conflict')))
})

// --- publish real: merge preservando parâmetros não gerenciados ---

test('publish: sucesso preserva parâmetro não gerenciado pelo catálogo (merge, nunca sobrescreve)', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {
    legacy_unrelated_param: { defaultValue: { value: 'x' }, valueType: 'STRING', description: 'não pertence ao catálogo' },
  })
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleRemoteConfigAdminPublish(
      jsonRequest('https://x/publish', { parameters: { [MEDIUM_KEY]: { value: false } } }, { headers: { 'If-Match': fake.state.etag } }),
      env, ADMIN_SESSION
    )
  )

  assert.equal(resp.status, 200)
  assert.equal(fake.putCallCount(), 1)
  assert.ok('legacy_unrelated_param' in fake.state.parameters, 'parâmetro não gerenciado não pode desaparecer')
  assert.deepEqual(fake.state.parameters.legacy_unrelated_param, { defaultValue: { value: 'x' }, valueType: 'STRING', description: 'não pertence ao catálogo' })
  const updated = fake.state.parameters[MEDIUM_KEY] as { defaultValue: { value: string } }
  assert.equal(updated.defaultValue.value, 'false')
})

test('publish: flag HIGH sem reason bloqueia antes de chamar o Firebase (nenhum PUT)', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {})
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleRemoteConfigAdminPublish(
      jsonRequest('https://x/publish', { parameters: { [HIGH_KEY]: { value: false } } }, { headers: { 'If-Match': fake.state.etag } }),
      env, ADMIN_SESSION
    )
  )

  assert.equal(resp.status, 400)
  assert.equal(fake.putCallCount(), 0)
})

test('publish: flag HIGH com reason publica e grava motivo na auditoria', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {})
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleRemoteConfigAdminPublish(
      jsonRequest('https://x/publish', { parameters: { [HIGH_KEY]: { value: false } }, reason: 'kill switch emergencial' },
        { headers: { 'If-Match': fake.state.etag } }),
      env, ADMIN_SESSION
    )
  )

  assert.equal(resp.status, 200)
  const okRow = db.auditLog.find((row) => row.action === 'publish' && row.status === 'ok')
  assert.ok(okRow)
  assert.ok(String(okRow?.message).includes('kill switch emergencial'))
})

// --- rate limit dedicado ---

test('publish: rate limit dedicado bloqueia com 429 quando estourado', async () => {
  const db = new FakeD1Database()
  db.seedRateLimitExceeded(ADMIN_SESSION.userId, 'publish')
  const fake = createFakeFirebase('signallq-test', {})
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleRemoteConfigAdminPublish(
      jsonRequest('https://x/publish', { parameters: { [MEDIUM_KEY]: { value: false } } }, { headers: { 'If-Match': fake.state.etag } }),
      env, ADMIN_SESSION
    )
  )

  assert.equal(resp.status, 429)
  assert.equal(fake.putCallCount(), 0)
})

test('sync: rate limit dedicado bloqueia execução real (dryRun continua liberado)', async () => {
  const db = new FakeD1Database()
  db.seedRateLimitExceeded(ADMIN_SESSION.userId, 'sync')
  const fake = createFakeFirebase('signallq-test', {})
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const blockedResp = await withMockedFetch(fake.fetch, () =>
    handleFeatureFlagsSync(new Request('https://x/sync'), env, ADMIN_SESSION)
  )
  assert.equal(blockedResp.status, 429)

  const dryRunResp = await withMockedFetch(fake.fetch, () =>
    handleFeatureFlagsSync(new Request('https://x/sync?dryRun=true'), env, ADMIN_SESSION)
  )
  assert.equal(dryRunResp.status, 200)
})

// --- rollback ---

test('rollback: sem reason retorna 400 (confirmação reforçada sempre exigida)', async () => {
  const db = new FakeD1Database()
  const env = buildEnv(db)
  const resp = await handleRemoteConfigAdminRollback(
    jsonRequest('https://x/rollback', { targetVersion: 2 }),
    env, ADMIN_SESSION
  )
  assert.equal(resp.status, 400)
})

test('rollback: com reason chama a API e avança a versão', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {})
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })
  const versionBefore = fake.state.versionNumber

  const resp = await withMockedFetch(fake.fetch, () =>
    handleRemoteConfigAdminRollback(
      jsonRequest('https://x/rollback', { targetVersion: 2, reason: 'reverter incidente de produção' }),
      env, ADMIN_SESSION
    )
  )

  assert.equal(resp.status, 200)
  assert.ok(fake.state.versionNumber > versionBefore)
  assert.ok(db.auditLog.some((row) => row.action === 'rollback' && row.status === 'ok'))
})

// --- criação automática de parâmetro ausente + detecção de órfãos + dry-run vs real ---

test('sync dryRun=true: calcula toCreate/orphans sem chamar PUT', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {
    legacy_orphan_param: { defaultValue: { value: '1' }, valueType: 'STRING', description: 'sem entrada no catálogo' },
  })
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleFeatureFlagsSync(new Request('https://x/sync?dryRun=true'), env, ADMIN_SESSION)
  )
  const payload = await resp.json() as { dryRun: boolean; toCreate: Array<{ key: string }>; orphans: string[]; created: string[] }

  assert.equal(resp.status, 200)
  assert.equal(payload.dryRun, true)
  assert.equal(payload.toCreate.length, CATALOG_FLAG_COUNT)
  assert.ok(payload.toCreate.some((c) => c.key === HIGH_KEY))
  assert.deepEqual(payload.orphans, ['legacy_orphan_param'])
  assert.deepEqual(payload.created, [])
  assert.equal(fake.putCallCount(), 0)
  assert.equal(Object.keys(fake.state.parameters).length, 1, 'estado do Firebase não pode mudar em dry-run')
})

test('sync dryRun=false (real, role admin): cria os parâmetros ausentes do catálogo no Firebase', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {
    legacy_orphan_param: { defaultValue: { value: '1' }, valueType: 'STRING', description: 'sem entrada no catálogo' },
  })
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleFeatureFlagsSync(new Request('https://x/sync'), env, ADMIN_SESSION)
  )
  const payload = await resp.json() as { dryRun: boolean; created: string[]; orphans: string[] }

  assert.equal(resp.status, 200)
  assert.equal(payload.dryRun, false)
  assert.equal(payload.created.length, CATALOG_FLAG_COUNT)
  assert.equal(fake.putCallCount(), 1)
  assert.ok(HIGH_KEY in fake.state.parameters)
  assert.ok(MEDIUM_KEY in fake.state.parameters)
  assert.ok('legacy_orphan_param' in fake.state.parameters, 'sync nunca remove órfão, só reporta')
  assert.ok(db.auditLog.some((row) => row.action === 'sync' && row.status === 'ok'))
})

test('sync real: role não-admin é bloqueada com 403 (dryRun exige só sessão autenticada)', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', {})
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const realResp = await withMockedFetch(fake.fetch, () =>
    handleFeatureFlagsSync(new Request('https://x/sync'), env, VIEWER_SESSION)
  )
  assert.equal(realResp.status, 403)

  const dryRunResp = await withMockedFetch(fake.fetch, () =>
    handleFeatureFlagsSync(new Request('https://x/sync?dryRun=true'), env, VIEWER_SESSION)
  )
  assert.equal(dryRunResp.status, 200)
})

test('sync: nada a criar quando o catálogo já está presente no Firebase', async () => {
  const db = new FakeD1Database()
  const catalog = loadFeatureFlagCatalog()
  const seeded: Record<string, unknown> = {}
  for (const entry of catalog.flags) {
    seeded[entry.key] = { defaultValue: { value: String(entry.defaultValue) }, valueType: 'BOOLEAN', description: entry.description }
  }
  const fake = createFakeFirebase('signallq-test', seeded)
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleFeatureFlagsSync(new Request('https://x/sync'), env, ADMIN_SESSION)
  )
  const payload = await resp.json() as { toCreate: unknown[]; created: unknown[] }
  assert.equal(payload.toCreate.length, 0)
  assert.equal(payload.created.length, 0)
  assert.equal(fake.putCallCount(), 0, 'não publica nada quando não há o que criar')
})

// --- GET passthrough (regressão do groundwork já existente) ---

test('GET remote-config continua real (passthrough) após a mudança', async () => {
  const db = new FakeD1Database()
  const fake = createFakeFirebase('signallq-test', { some_param: { defaultValue: { value: '1' }, valueType: 'STRING' } })
  const env = buildEnv(db, { FIREBASE_PROJECT_ID: 'signallq-test' })

  const resp = await withMockedFetch(fake.fetch, () =>
    handleRemoteConfigAdminGet(new Request('https://x/remote-config'), env, ADMIN_SESSION)
  )
  const payload = await resp.json() as { parameters: Record<string, unknown>; etag: string }
  assert.equal(resp.status, 200)
  assert.ok('some_param' in payload.parameters)
  assert.equal(payload.etag, fake.state.etag)
})
