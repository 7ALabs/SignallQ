// Backend admin de Feature Flags / Firebase Remote Config — issue #1478 (Épico #1347, Feature F2).
//
// Groundwork (Bruno, 2026-07-26, PR #1493): autenticação de service account, roteamento,
// autorização de sessão e trilha de auditoria; leitura de template/ETag e histórico de versões
// (GET) REAIS desde o começo. validate/publish/rollback ficavam bloqueados (501) até o catálogo
// canônico (#1477) fechar.
//
// Lógica real completa (Camilo, 2026-07-26, fecha #1478): validate/publish/rollback contra o
// catálogo canônico (`src/featureFlagCatalog.ts`), merge preservando parâmetros não gerenciados,
// criação automática de parâmetros ausentes + detecção de órfãos (`GET .../feature-flags/catalog`
// e `POST .../feature-flags/sync`), confirmação reforçada (motivo obrigatório) para flags
// HIGH/CRITICAL, rate limit dedicado nas rotas de escrita.

import type { Env } from './index.ts'
import { json, err, logError } from './index.ts'
import { getFirebaseAccessToken } from './firebaseAuth.ts'
import {
  loadFeatureFlagCatalog,
  findCatalogFlag,
  adminManagedFlags,
  requiresReinforcedConfirmation,
  toRemoteConfigValueType,
  catalogEntryToRemoteConfigParameter,
  type FeatureFlagCatalogEntry,
} from './featureFlagCatalog.ts'

type Session = { userId: string; role: string }

function remoteConfigBaseUrl(env: Env): string {
  return `https://firebaseremoteconfig.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/remoteConfig`
}

function hasFirebaseCredentials(env: Env): boolean {
  return !!(env.FIREBASE_CLIENT_EMAIL && env.FIREBASE_PRIVATE_KEY)
}

function nowSec(): number {
  return Math.floor(Date.now() / 1000)
}

// Trunca snapshots antes de gravar no D1 — evita linha de auditoria desproporcional caso o
// template venha com muitos parâmetros (mesmo espírito do truncamento em errText.slice(0, 300)
// já usado nos outros handlers Firebase de src/index.ts).
function truncateForAudit(value: unknown): string {
  const raw = JSON.stringify(value ?? null)
  return raw.length > 4000 ? `${raw.slice(0, 4000)}…(truncado)` : raw
}

interface RemoteConfigAuditEntry {
  action: 'validate' | 'publish' | 'rollback' | 'sync'
  status: 'ok' | 'error'
  templateVersionBefore?: string | null
  templateVersionAfter?: string | null
  etagBefore?: string | null
  etagAfter?: string | null
  before?: unknown
  after?: unknown
  message: string
  session: Session
}

async function writeRemoteConfigAuditLog(env: Env, entry: RemoteConfigAuditEntry): Promise<void> {
  try {
    await env.DB.prepare(
      `INSERT INTO remote_config_audit_log
        (id, action, status, template_version_before, template_version_after, etag_before, etag_after,
         before_json, after_json, message, actor_user_id, actor_role, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      crypto.randomUUID(),
      entry.action,
      entry.status,
      entry.templateVersionBefore ?? null,
      entry.templateVersionAfter ?? null,
      entry.etagBefore ?? null,
      entry.etagAfter ?? null,
      entry.before !== undefined ? truncateForAudit(entry.before) : null,
      entry.after !== undefined ? truncateForAudit(entry.after) : null,
      entry.message,
      entry.session.userId,
      entry.session.role,
      nowSec()
    ).run()
  } catch (e) {
    // Fire-and-forget, mesmo espírito de logError() em index.ts — a trilha de auditoria não pode
    // derrubar a resposta ao usuário, mas a falha em si precisa ficar registrada em system_errors.
    await logError(env, 'remote-config-admin', `audit_write_failed: ${String(e)}`, e instanceof Error ? (e.stack ?? '') : '')
  }
}

// --- Rate limit dedicado das rotas de escrita (publish/rollback/feature-flags/sync) ---
// Distinto do rate limit de login (`auth_rate_limit`, por IP) — este é por usuário autenticado
// (já passou pela sessão), janela mais generosa (10min/20 req) pensada pra não travar um
// operador legítimo em sequência de publicações, só conter automação/erro em loop.
const WRITE_RATE_LIMIT_WINDOW_SEC = 10 * 60
const WRITE_RATE_LIMIT_MAX = 20

async function checkWriteRateLimit(env: Env, actorUserId: string, route: string): Promise<boolean> {
  const now = nowSec()
  const row = await env.DB.prepare(
    'SELECT count, window_start FROM remote_config_write_rate_limit WHERE actor_user_id = ? AND route = ?'
  ).bind(actorUserId, route).first<{ count: number; window_start: number }>()
  if (!row) return false
  if (now - row.window_start > WRITE_RATE_LIMIT_WINDOW_SEC) return false
  return row.count > WRITE_RATE_LIMIT_MAX
}

async function incrementWriteRateLimit(env: Env, actorUserId: string, route: string): Promise<void> {
  const now = nowSec()
  const row = await env.DB.prepare(
    'SELECT count, window_start FROM remote_config_write_rate_limit WHERE actor_user_id = ? AND route = ?'
  ).bind(actorUserId, route).first<{ count: number; window_start: number }>()
  if (!row || now - row.window_start > WRITE_RATE_LIMIT_WINDOW_SEC) {
    await env.DB.prepare(
      'INSERT OR REPLACE INTO remote_config_write_rate_limit (actor_user_id, route, count, window_start) VALUES (?, ?, 1, ?)'
    ).bind(actorUserId, route, now).run()
  } else {
    await env.DB.prepare(
      'UPDATE remote_config_write_rate_limit SET count = count + 1 WHERE actor_user_id = ? AND route = ?'
    ).bind(actorUserId, route).run()
  }
}

// --- Template ao vivo (GET + ETag) — reaproveitado por publish/rollback/sync ---

interface RemoteConfigTemplate {
  parameters: Record<string, unknown>
  parameterGroups: Record<string, unknown>
  conditions: unknown[]
  version?: { versionNumber?: string } | null
}

type LiveTemplateResult =
  | { ok: true; etag: string | null; template: RemoteConfigTemplate }
  | { ok: false; response: Response }

async function fetchLiveTemplate(env: Env): Promise<LiveTemplateResult> {
  if (!hasFirebaseCredentials(env)) {
    return { ok: false, response: err('FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY não configurados.', 503, env) }
  }
  let token: string
  try {
    token = await getFirebaseAccessToken(env)
  } catch (e) {
    await logError(env, 'remote-config-admin', `auth_failed: ${String(e)}`)
    return { ok: false, response: err('Falha ao autenticar service account Firebase.', 502, env) }
  }

  const resp = await fetch(remoteConfigBaseUrl(env), { headers: { Authorization: `Bearer ${token}` } })
  if (!resp.ok) {
    const errText = await resp.text()
    await logError(env, 'remote-config-admin', `template_get_${resp.status}: ${errText.slice(0, 300)}`)
    return { ok: false, response: err(`Falha ao consultar template do Remote Config (HTTP ${resp.status}).`, 502, env) }
  }

  const etag = resp.headers.get('ETag')
  const template = await resp.json() as RemoteConfigTemplate
  return {
    ok: true,
    etag,
    template: {
      parameters: template.parameters ?? {},
      parameterGroups: template.parameterGroups ?? {},
      conditions: template.conditions ?? [],
      version: template.version ?? null,
    },
  }
}

// GET /admin/firebase/remote-config — lê o template atual + ETag direto da Firebase Remote
// Config REST API. Real, sem dependência de catálogo (só passthrough de leitura).
export async function handleRemoteConfigAdminGet(_request: Request, env: Env, _session: Session): Promise<Response> {
  const live = await fetchLiveTemplate(env)
  if (!live.ok) return live.response
  return json({
    projectId: env.FIREBASE_PROJECT_ID,
    etag: live.etag,
    parameters: live.template.parameters,
    parameterGroups: live.template.parameterGroups,
    conditions: live.template.conditions,
    version: live.template.version,
  }, 200, env)
}

// GET /admin/firebase/remote-config/versions — histórico de versões do template. Real, sem
// dependência de catálogo. ?pageSize (default 10, teto 300 conforme limite da própria API do
// Firebase) e ?pageToken (paginação) repassados como estão.
export async function handleRemoteConfigAdminVersions(request: Request, env: Env, _session: Session): Promise<Response> {
  if (!hasFirebaseCredentials(env)) {
    return err('FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY não configurados.', 503, env)
  }
  const url = new URL(request.url)
  const pageSizeParam = Number(url.searchParams.get('pageSize') ?? '10')
  const pageSize = Number.isFinite(pageSizeParam) ? Math.min(Math.max(pageSizeParam, 1), 300) : 10
  const pageToken = url.searchParams.get('pageToken')

  let token: string
  try {
    token = await getFirebaseAccessToken(env)
  } catch (e) {
    await logError(env, 'remote-config-admin', `auth_failed: ${String(e)}`)
    return err('Falha ao autenticar service account Firebase.', 502, env)
  }

  const versionsUrl = new URL(`${remoteConfigBaseUrl(env)}:listVersions`)
  versionsUrl.searchParams.set('pageSize', String(pageSize))
  if (pageToken) versionsUrl.searchParams.set('pageToken', pageToken)

  const resp = await fetch(versionsUrl.toString(), { headers: { Authorization: `Bearer ${token}` } })
  if (!resp.ok) {
    const errText = await resp.text()
    await logError(env, 'remote-config-admin', `versions_${resp.status}: ${errText.slice(0, 300)}`)
    return err(`Falha ao consultar histórico de versões (HTTP ${resp.status}).`, 502, env)
  }

  const data = await resp.json() as { versions?: unknown[]; nextPageToken?: string }
  return json({ versions: data.versions ?? [], nextPageToken: data.nextPageToken ?? null }, 200, env)
}

// --- Validação contra o catálogo (compartilhada por validate/publish) ---

interface ParameterPatchEntry {
  value: boolean | string | number
}

interface ParsedPublishBody {
  parameters: Record<string, ParameterPatchEntry>
  reason: string | null
}

function parsePublishBody(body: unknown): { ok: true; value: ParsedPublishBody } | { ok: false; message: string } {
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return { ok: false, message: 'body deve ser um objeto JSON.' }
  }
  const record = body as Record<string, unknown>
  const rawParameters = record.parameters
  if (typeof rawParameters !== 'object' || rawParameters === null || Array.isArray(rawParameters)) {
    return { ok: false, message: '"parameters" deve ser um objeto { chave: { value } }.' }
  }
  const parameters: Record<string, ParameterPatchEntry> = {}
  for (const [key, raw] of Object.entries(rawParameters as Record<string, unknown>)) {
    if (typeof raw !== 'object' || raw === null || !('value' in raw)) {
      return { ok: false, message: `parameters["${key}"] deve ser um objeto { value }.` }
    }
    const value = (raw as { value: unknown }).value
    if (typeof value !== 'boolean' && typeof value !== 'string' && typeof value !== 'number') {
      return { ok: false, message: `parameters["${key}"].value deve ser boolean, string ou number.` }
    }
    parameters[key] = { value }
  }
  const reason = typeof record.reason === 'string' ? record.reason : null
  return { ok: true, value: { parameters, reason } }
}

interface CatalogValidationResult {
  errors: string[]
  warnings: string[]
  touchedEntries: FeatureFlagCatalogEntry[]
}

// Valida as chaves de um patch de parâmetros contra o catálogo canônico — regra do Épico #1347:
// "o Admin não pode permitir criação de chaves arbitrárias; apenas chaves presentes no catálogo".
// `lookup` é injetável (default: catálogo real via `findCatalogFlag`) só pra permitir testar a
// regra de "dependência inválida" com um catálogo sintético, sem precisar mutar o JSON real.
export function validateParametersAgainstCatalog(
  parameters: Record<string, ParameterPatchEntry>,
  lookup: (key: string) => FeatureFlagCatalogEntry | undefined = findCatalogFlag
): CatalogValidationResult {
  const errors: string[] = []
  const warnings: string[] = []
  const touchedEntries: FeatureFlagCatalogEntry[] = []

  for (const [key, patch] of Object.entries(parameters)) {
    const entry = lookup(key)
    if (!entry) {
      errors.push(`Flag "${key}" não existe no catálogo canônico — não é possível criar chave arbitrária pelo Admin.`)
      continue
    }
    if (!entry.adminManaged) {
      errors.push(`Flag "${key}" existe no catálogo mas não é adminManaged — não pode ser alterada pelo Admin.`)
      continue
    }
    // dependência inválida — toda flag listada em `dependencies` precisa existir no catálogo.
    for (const dependencyKey of entry.dependencies) {
      if (!lookup(dependencyKey)) {
        errors.push(`Flag "${key}" depende de "${dependencyKey}", que não existe no catálogo — dependência inválida.`)
      }
    }
    const expectedType = toRemoteConfigValueType(entry.type)
    const actualType = typeof patch.value === 'boolean' ? 'BOOLEAN' : typeof patch.value === 'number' ? 'NUMBER' : 'STRING'
    if (expectedType !== actualType) {
      errors.push(`Flag "${key}" espera tipo ${entry.type} (${expectedType}), valor enviado é ${actualType}.`)
      continue
    }
    if (!entry.androidImplemented) {
      warnings.push(`Flag "${key}" ainda não tem consumo real no Android (androidImplemented=false) — alterar o valor não afeta nenhum comportamento do app ainda.`)
    }
    touchedEntries.push(entry)
  }

  return { errors, warnings, touchedEntries }
}

// Confirmação reforçada — regra do Épico #1347: "kill switches críticos devem exigir confirmação
// explícita" / "alterações devem possuir comentário/motivo obrigatório".
export function reasonRequiredMessage(touchedEntries: FeatureFlagCatalogEntry[], reason: string | null): string | null {
  const criticalKeys = touchedEntries.filter((entry) => requiresReinforcedConfirmation(entry.criticality)).map((entry) => entry.key)
  if (criticalKeys.length === 0) return null
  if (reason && reason.trim().length >= 3) return null
  return `Confirmação reforçada obrigatória: informe "reason" (motivo, mínimo 3 caracteres) para alterar flag(s) crítica(s): ${criticalKeys.join(', ')}.`
}

// POST /admin/firebase/remote-config/validate — valida um patch de parâmetros contra o catálogo
// canônico SEM publicar. Sempre HTTP 200 quando o JSON é bem formado (erros de conteúdo voltam
// em `errors[]`, não como falha HTTP) — é uma checagem "a seco" pensada pra UI do Admin (F3)
// pré-validar antes de habilitar o botão de publicar.
export async function handleRemoteConfigAdminValidate(request: Request, env: Env, session: Session): Promise<Response> {
  let body: unknown
  try {
    body = await request.json()
  } catch {
    return err('body JSON inválido.', 400, env)
  }
  const parsed = parsePublishBody(body)
  if (!parsed.ok) return err(parsed.message, 400, env)

  const { errors, warnings, touchedEntries } = validateParametersAgainstCatalog(parsed.value.parameters)
  const reasonError = reasonRequiredMessage(touchedEntries, parsed.value.reason)
  if (reasonError) errors.push(reasonError)

  const ok = errors.length === 0
  await writeRemoteConfigAuditLog(env, {
    action: 'validate',
    status: ok ? 'ok' : 'error',
    message: ok ? 'Validação ok.' : errors.join(' | '),
    session,
    after: parsed.value.parameters,
  })

  return json({ ok, errors, warnings }, 200, env)
}

// POST /admin/firebase/remote-config/publish — valida contra o catálogo, faz merge preservando
// parâmetros não gerenciados (só sobrescreve as chaves do próprio patch), protege contra
// sobrescrita concorrente (ETag/If-Match) e exige confirmação reforçada pra flags HIGH/CRITICAL.
export async function handleRemoteConfigAdminPublish(request: Request, env: Env, session: Session): Promise<Response> {
  if (session.role !== 'admin') return err('Forbidden — apenas role admin pode publicar.', 403, env)

  if (await checkWriteRateLimit(env, session.userId, 'publish')) {
    return err('Muitas publicações em sequência. Tente novamente em alguns minutos.', 429, env)
  }

  const ifMatch = request.headers.get('If-Match')
  if (!ifMatch) return err('Header If-Match (ETag do template atual) obrigatório.', 400, env)

  let body: unknown
  try {
    body = await request.json()
  } catch {
    return err('body JSON inválido.', 400, env)
  }
  const parsed = parsePublishBody(body)
  if (!parsed.ok) return err(parsed.message, 400, env)
  if (Object.keys(parsed.value.parameters).length === 0) {
    return err('"parameters" não pode ser vazio.', 400, env)
  }

  const { errors, touchedEntries } = validateParametersAgainstCatalog(parsed.value.parameters)
  const reasonError = reasonRequiredMessage(touchedEntries, parsed.value.reason)
  if (reasonError) errors.push(reasonError)
  if (errors.length > 0) {
    await writeRemoteConfigAuditLog(env, {
      action: 'publish', status: 'error', message: errors.join(' | '), session, after: parsed.value.parameters,
    })
    return err(errors.join(' | '), 400, env)
  }

  const live = await fetchLiveTemplate(env)
  if (!live.ok) return live.response

  if (live.etag !== ifMatch) {
    await writeRemoteConfigAuditLog(env, {
      action: 'publish', status: 'error', etagBefore: ifMatch, etagAfter: live.etag,
      message: 'etag_conflict: template foi alterado desde a última leitura — recarregue antes de publicar.',
      session, after: parsed.value.parameters,
    })
    return err('Conflito de versão: o template do Remote Config mudou desde a última leitura (ETag desatualizado). Recarregue e tente novamente.', 409, env)
  }

  const before: Record<string, unknown> = {}
  const mergedParameters: Record<string, unknown> = { ...live.template.parameters }
  for (const [key, patch] of Object.entries(parsed.value.parameters)) {
    const entry = findCatalogFlag(key) as FeatureFlagCatalogEntry
    before[key] = live.template.parameters[key] ?? null
    mergedParameters[key] = {
      ...(typeof live.template.parameters[key] === 'object' && live.template.parameters[key] !== null ? live.template.parameters[key] as Record<string, unknown> : {}),
      defaultValue: { value: String(patch.value) },
      valueType: toRemoteConfigValueType(entry.type),
      description: entry.description,
    }
  }

  let token: string
  try {
    token = await getFirebaseAccessToken(env)
  } catch (e) {
    await logError(env, 'remote-config-admin', `auth_failed: ${String(e)}`)
    return err('Falha ao autenticar service account Firebase.', 502, env)
  }

  const putResp = await fetch(remoteConfigBaseUrl(env), {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json; UTF8',
      'If-Match': ifMatch,
    },
    body: JSON.stringify({
      parameters: mergedParameters,
      parameterGroups: live.template.parameterGroups,
      conditions: live.template.conditions,
    }),
  })

  if (!putResp.ok) {
    const errText = await putResp.text()
    await logError(env, 'remote-config-admin', `publish_${putResp.status}: ${errText.slice(0, 300)}`)
    await writeRemoteConfigAuditLog(env, {
      action: 'publish', status: 'error', etagBefore: ifMatch,
      message: `Firebase recusou a publicação (HTTP ${putResp.status}): ${errText.slice(0, 300)}`,
      session, before, after: parsed.value.parameters,
    })
    return err(
      putResp.status === 412
        ? 'Conflito de versão: o Firebase recusou a publicação (ETag desatualizado). Recarregue e tente novamente.'
        : `Falha ao publicar no Firebase (HTTP ${putResp.status}).`,
      putResp.status === 412 ? 409 : 502,
      env
    )
  }

  const newEtag = putResp.headers.get('ETag')
  const newTemplate = await putResp.json() as RemoteConfigTemplate
  await incrementWriteRateLimit(env, session.userId, 'publish')
  await writeRemoteConfigAuditLog(env, {
    action: 'publish', status: 'ok',
    templateVersionBefore: live.template.version?.versionNumber ?? null,
    templateVersionAfter: newTemplate.version?.versionNumber ?? null,
    etagBefore: ifMatch, etagAfter: newEtag,
    message: `Publicado: ${Object.keys(parsed.value.parameters).join(', ')}${parsed.value.reason ? ` — motivo: ${parsed.value.reason}` : ''}`,
    session, before, after: parsed.value.parameters,
  })

  return json({
    ok: true,
    templateVersion: newTemplate.version?.versionNumber ?? null,
    etag: newEtag,
    updatedKeys: Object.keys(parsed.value.parameters),
  }, 200, env)
}

// POST /admin/firebase/remote-config/rollback — chama remoteConfig:rollback da REST API.
// Reversão pra uma versão anterior é sempre tratada como ação de alto risco (independente da
// criticidade das flags envolvidas, já que reverte o template inteiro) — motivo obrigatório.
export async function handleRemoteConfigAdminRollback(request: Request, env: Env, session: Session): Promise<Response> {
  if (session.role !== 'admin') return err('Forbidden — apenas role admin pode reverter.', 403, env)

  if (await checkWriteRateLimit(env, session.userId, 'rollback')) {
    return err('Muitos rollbacks em sequência. Tente novamente em alguns minutos.', 429, env)
  }

  let body: unknown
  try {
    body = await request.json()
  } catch {
    return err('body JSON inválido.', 400, env)
  }
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return err('body deve ser um objeto JSON.', 400, env)
  }
  const record = body as Record<string, unknown>
  const targetVersion = record.targetVersion
  if (typeof targetVersion !== 'string' && typeof targetVersion !== 'number') {
    return err('"targetVersion" (string ou number) obrigatório.', 400, env)
  }
  const reason = typeof record.reason === 'string' ? record.reason : null
  if (!reason || reason.trim().length < 3) {
    return err('Confirmação reforçada obrigatória: informe "reason" (motivo, mínimo 3 caracteres) para reverter o template.', 400, env)
  }

  if (!hasFirebaseCredentials(env)) {
    return err('FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY não configurados.', 503, env)
  }
  let token: string
  try {
    token = await getFirebaseAccessToken(env)
  } catch (e) {
    await logError(env, 'remote-config-admin', `auth_failed: ${String(e)}`)
    return err('Falha ao autenticar service account Firebase.', 502, env)
  }

  const rollbackResp = await fetch(`${remoteConfigBaseUrl(env)}:rollback`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ versionNumber: String(targetVersion) }),
  })

  if (!rollbackResp.ok) {
    const errText = await rollbackResp.text()
    await logError(env, 'remote-config-admin', `rollback_${rollbackResp.status}: ${errText.slice(0, 300)}`)
    await writeRemoteConfigAuditLog(env, {
      action: 'rollback', status: 'error',
      message: `Firebase recusou o rollback (HTTP ${rollbackResp.status}): ${errText.slice(0, 300)} — motivo: ${reason}`,
      session, templateVersionAfter: String(targetVersion),
    })
    return err(`Falha ao reverter o template no Firebase (HTTP ${rollbackResp.status}).`, 502, env)
  }

  const newEtag = rollbackResp.headers.get('ETag')
  const newTemplate = await rollbackResp.json() as RemoteConfigTemplate
  await incrementWriteRateLimit(env, session.userId, 'rollback')
  await writeRemoteConfigAuditLog(env, {
    action: 'rollback', status: 'ok',
    templateVersionAfter: newTemplate.version?.versionNumber ?? String(targetVersion),
    etagAfter: newEtag,
    message: `Revertido para a versão ${String(targetVersion)} — motivo: ${reason}`,
    session,
  })

  return json({
    ok: true,
    templateVersion: newTemplate.version?.versionNumber ?? String(targetVersion),
    etag: newEtag,
  }, 200, env)
}

// GET /admin/firebase/feature-flags/catalog — serve o catálogo canônico do Consumer pro Admin
// (F3, UI ainda não implementada) consumir. Não depende de credencial Firebase — funciona mesmo
// sem FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY configurados (só lê o JSON embarcado no bundle).
export async function handleFeatureFlagsCatalogGet(_request: Request, env: Env, _session: Session): Promise<Response> {
  const full = loadFeatureFlagCatalog()
  const managedCount = adminManagedFlags().length
  return json({
    schemaVersion: full.schemaVersion,
    flagCount: full.flags.length,
    adminManagedCount: managedCount,
    flags: full.flags,
  }, 200, env)
}

interface SyncCreateEntry {
  key: string
  criticality: string
  valueType: string
  defaultValue: string
}

// POST /admin/firebase/feature-flags/sync[?dryRun=true] — cria no Firebase os parâmetros do
// catálogo (adminManaged=true) ainda ausentes, detecta órfãos (parâmetro no Firebase sem entrada
// no catálogo) e divergências de tipo — NUNCA sobrescreve ou apaga um parâmetro já existente
// (criação pura, aditiva). dryRun=true só calcula e retorna o diff, sem chamar PUT no Firebase;
// qualquer outro valor (ou ausência do param) executa de verdade.
export async function handleFeatureFlagsSync(request: Request, env: Env, session: Session): Promise<Response> {
  const url = new URL(request.url)
  const dryRun = url.searchParams.get('dryRun') === 'true'

  if (!dryRun && session.role !== 'admin') {
    return err('Forbidden — apenas role admin pode executar a sincronização real (use ?dryRun=true para simular).', 403, env)
  }
  if (!dryRun && await checkWriteRateLimit(env, session.userId, 'sync')) {
    return err('Muitas sincronizações em sequência. Tente novamente em alguns minutos.', 429, env)
  }

  const live = await fetchLiveTemplate(env)
  if (!live.ok) return live.response

  const catalogEntries = adminManagedFlags()
  const liveKeys = new Set(Object.keys(live.template.parameters))

  const toCreate: SyncCreateEntry[] = catalogEntries
    .filter((entry) => !liveKeys.has(entry.key))
    .map((entry) => ({
      key: entry.key,
      criticality: entry.criticality,
      valueType: toRemoteConfigValueType(entry.type),
      defaultValue: String(entry.defaultValue),
    }))

  const orphans = Object.keys(live.template.parameters).filter((key) => !findCatalogFlag(key))

  const divergent = catalogEntries
    .filter((entry) => liveKeys.has(entry.key))
    .filter((entry) => {
      const liveParam = live.template.parameters[entry.key] as { valueType?: string } | undefined
      return !!liveParam?.valueType && liveParam.valueType !== toRemoteConfigValueType(entry.type)
    })
    .map((entry) => entry.key)

  if (dryRun || toCreate.length === 0) {
    if (!dryRun) {
      await writeRemoteConfigAuditLog(env, {
        action: 'sync', status: 'ok',
        message: `Sincronização real: nada a criar (${catalogEntries.length} flags adminManaged já presentes). Órfãos: ${orphans.length}.`,
        session, after: { toCreate, orphans, divergent },
      })
    }
    return json({ dryRun, toCreate, created: [], orphans, divergent }, 200, env)
  }

  const mergedParameters: Record<string, unknown> = { ...live.template.parameters }
  for (const entry of catalogEntries) {
    if (!liveKeys.has(entry.key)) {
      mergedParameters[entry.key] = catalogEntryToRemoteConfigParameter(entry)
    }
  }

  let token: string
  try {
    token = await getFirebaseAccessToken(env)
  } catch (e) {
    await logError(env, 'remote-config-admin', `auth_failed: ${String(e)}`)
    return err('Falha ao autenticar service account Firebase.', 502, env)
  }

  const putResp = await fetch(remoteConfigBaseUrl(env), {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json; UTF8',
      'If-Match': live.etag ?? '*',
    },
    body: JSON.stringify({
      parameters: mergedParameters,
      parameterGroups: live.template.parameterGroups,
      conditions: live.template.conditions,
    }),
  })

  if (!putResp.ok) {
    const errText = await putResp.text()
    await logError(env, 'remote-config-admin', `sync_${putResp.status}: ${errText.slice(0, 300)}`)
    await writeRemoteConfigAuditLog(env, {
      action: 'sync', status: 'error', etagBefore: live.etag,
      message: `Firebase recusou a sincronização (HTTP ${putResp.status}): ${errText.slice(0, 300)}`,
      session, after: { toCreate },
    })
    return err(
      putResp.status === 412
        ? 'Conflito de versão durante a sincronização (ETag desatualizado). Tente novamente.'
        : `Falha ao sincronizar catálogo no Firebase (HTTP ${putResp.status}).`,
      putResp.status === 412 ? 409 : 502,
      env
    )
  }

  const newEtag = putResp.headers.get('ETag')
  const newTemplate = await putResp.json() as RemoteConfigTemplate
  await incrementWriteRateLimit(env, session.userId, 'sync')
  await writeRemoteConfigAuditLog(env, {
    action: 'sync', status: 'ok',
    templateVersionBefore: live.template.version?.versionNumber ?? null,
    templateVersionAfter: newTemplate.version?.versionNumber ?? null,
    etagBefore: live.etag, etagAfter: newEtag,
    message: `Criados ${toCreate.length} parâmetro(s) ausente(s): ${toCreate.map((c) => c.key).join(', ')}. Órfãos detectados: ${orphans.length}.`,
    session, after: { toCreate, orphans, divergent },
  })

  return json({
    dryRun: false,
    toCreate,
    created: toCreate.map((c) => c.key),
    orphans,
    divergent,
    templateVersion: newTemplate.version?.versionNumber ?? null,
    etag: newEtag,
  }, 200, env)
}
