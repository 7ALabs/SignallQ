// Backend admin de Feature Flags / Firebase Remote Config — issue #1478 (Épico #1347, Feature F2).
//
// Groundwork parcial (2026-07-26): autenticação de service account, roteamento, autorização de
// sessão e trilha de auditoria estão REAIS. Leitura de template/ETag e histórico de versões
// (GET) também são REAIS — passam direto pela Firebase Remote Config REST API, sem depender do
// catálogo canônico.
//
// validate/publish/rollback ficam BLOQUEADOS de propósito (HTTP 501, `blockedPendingCatalog:
// true`) até a issue #1477 (F1, catálogo JSON canônico — Camilo) fechar. Sem o catálogo não dá
// pra cumprir a regra de governança do épico #1347: "o Admin não pode permitir criação de chaves
// arbitrárias; apenas chaves presentes no catálogo" — publicar/reverter o template sem esse gate
// seria escrever no Firebase sem controle algum, o oposto do que a feature pede. Cada handler já
// registra a tentativa na trilha de auditoria (status='blocked_pending_catalog') para provar a
// cadeia completa (sessão → rota → D1) mesmo antes da lógica de negócio existir.
//
// Não implementado ainda (fica para quando #1477 fechar):
//   - validar o template recebido contra o catálogo (chave existe? tipo bate? default bate?);
//   - mesclar o rascunho publicado preservando parâmetros existentes fora do catálogo;
//   - criar automaticamente parâmetros do catálogo ainda ausentes no Firebase;
//   - detectar parâmetros órfãos (no Firebase mas fora do catálogo);
//   - confirmação reforçada para flags com `criticality: "HIGH"` (campo vem do catálogo);
//   - `GET /admin/firebase/feature-flags/catalog` e `POST /admin/firebase/feature-flags/sync`
//     (endpoints do próprio catálogo, fora do escopo deste groundwork).

import type { Env } from './index'
import { json, err, logError } from './index'
import { getFirebaseAccessToken } from './firebaseAuth'

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
  action: 'validate' | 'publish' | 'rollback'
  status: 'ok' | 'error' | 'blocked_pending_catalog'
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

// GET /admin/firebase/remote-config — lê o template atual + ETag direto da Firebase Remote
// Config REST API. Real, sem dependência de catálogo (só passthrough de leitura).
export async function handleRemoteConfigAdminGet(_request: Request, env: Env, _session: Session): Promise<Response> {
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

  const resp = await fetch(remoteConfigBaseUrl(env), { headers: { Authorization: `Bearer ${token}` } })
  if (!resp.ok) {
    const errText = await resp.text()
    await logError(env, 'remote-config-admin', `template_get_${resp.status}: ${errText.slice(0, 300)}`)
    return err(`Falha ao consultar template do Remote Config (HTTP ${resp.status}).`, 502, env)
  }

  const etag = resp.headers.get('ETag')
  const template = await resp.json() as {
    parameters?: Record<string, unknown>
    parameterGroups?: Record<string, unknown>
    conditions?: unknown[]
    version?: { versionNumber?: string }
  }

  return json({
    projectId: env.FIREBASE_PROJECT_ID,
    etag,
    parameters: template.parameters ?? {},
    parameterGroups: template.parameterGroups ?? {},
    conditions: template.conditions ?? [],
    version: template.version ?? null,
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

// Checagem estrutural mínima do corpo recebido — não é a validação contra o catálogo (isso
// depende de #1477), só garante que o payload tem a forma esperada de um template parcial.
function structuralCheckTemplateBody(body: unknown): { ok: true } | { ok: false; message: string } {
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return { ok: false, message: 'body deve ser um objeto JSON.' }
  }
  const record = body as Record<string, unknown>
  if ('parameters' in record && (typeof record.parameters !== 'object' || record.parameters === null || Array.isArray(record.parameters))) {
    return { ok: false, message: '"parameters" deve ser um objeto quando presente.' }
  }
  return { ok: true }
}

function blockedPendingCatalogResponse(env: Env, message: string): Response {
  return json({
    status: 'blocked_pending_catalog',
    implemented: false,
    blockedBy: '#1477',
    message,
  }, 501, env)
}

// POST /admin/firebase/remote-config/validate — skeleton. Roteamento/sessão/parsing são reais;
// validação contra catálogo aguarda #1477 (ver cabeçalho do arquivo).
export async function handleRemoteConfigAdminValidate(request: Request, env: Env, session: Session): Promise<Response> {
  let body: unknown
  try {
    body = await request.json()
  } catch {
    return err('body JSON inválido.', 400, env)
  }
  const structural = structuralCheckTemplateBody(body)
  if (!structural.ok) return err(structural.message, 400, env)

  await writeRemoteConfigAuditLog(env, {
    action: 'validate',
    status: 'blocked_pending_catalog',
    message: 'Validação estrutural ok; validação contra o catálogo canônico aguarda #1477.',
    session,
    after: body,
  })

  return blockedPendingCatalogResponse(
    env,
    'Estrutura do body ok. Validação contra o catálogo canônico de flags (#1477) ainda não implementada.'
  )
}

// POST /admin/firebase/remote-config/publish — skeleton. Exige If-Match (ETag) já agora, igual
// ao contrato final, mas ainda não escreve no Firebase. Só role 'admin' pode chegar até a
// checagem estrutural — publicação é ação de escrita/produção, mesmo padrão de autorização
// usado em handleAuthCreateUser (index.ts).
export async function handleRemoteConfigAdminPublish(request: Request, env: Env, session: Session): Promise<Response> {
  if (session.role !== 'admin') return err('Forbidden — apenas role admin pode publicar.', 403, env)

  const ifMatch = request.headers.get('If-Match')
  if (!ifMatch) return err('Header If-Match (ETag do template atual) obrigatório.', 400, env)

  let body: unknown
  try {
    body = await request.json()
  } catch {
    return err('body JSON inválido.', 400, env)
  }
  const structural = structuralCheckTemplateBody(body)
  if (!structural.ok) return err(structural.message, 400, env)

  await writeRemoteConfigAuditLog(env, {
    action: 'publish',
    status: 'blocked_pending_catalog',
    etagBefore: ifMatch,
    message: 'Estrutura do body e ETag ok; publicação real (merge com catálogo, criação de parâmetros ausentes, confirmação reforçada para flags críticas) aguarda #1477.',
    session,
    after: body,
  })

  return blockedPendingCatalogResponse(
    env,
    'Estrutura do body e If-Match ok. Publicação real aguarda o catálogo canônico de flags (#1477).'
  )
}

// POST /admin/firebase/remote-config/rollback — skeleton. Exige targetVersion no body (contrato
// final da Firebase Remote Config REST API — remoteConfig:rollback). Só role 'admin'.
export async function handleRemoteConfigAdminRollback(request: Request, env: Env, session: Session): Promise<Response> {
  if (session.role !== 'admin') return err('Forbidden — apenas role admin pode reverter.', 403, env)

  let body: unknown
  try {
    body = await request.json()
  } catch {
    return err('body JSON inválido.', 400, env)
  }
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return err('body deve ser um objeto JSON.', 400, env)
  }
  const targetVersion = (body as Record<string, unknown>).targetVersion
  if (typeof targetVersion !== 'string' && typeof targetVersion !== 'number') {
    return err('"targetVersion" (string ou number) obrigatório.', 400, env)
  }

  await writeRemoteConfigAuditLog(env, {
    action: 'rollback',
    status: 'blocked_pending_catalog',
    templateVersionAfter: String(targetVersion),
    message: 'targetVersion ok; rollback real aguarda o catálogo canônico de flags (#1477).',
    session,
  })

  return blockedPendingCatalogResponse(
    env,
    `Recebido targetVersion=${String(targetVersion)}. Rollback real aguarda o catálogo canônico de flags (#1477).`
  )
}
