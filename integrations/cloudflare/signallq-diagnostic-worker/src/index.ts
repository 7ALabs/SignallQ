import {
  evaluateSnapshot,
  validateRuleset,
  validateSnapshot,
} from "./diagnostic-engine.ts";
import { buildDiagnosticAiPrompt } from "./diagnostic-ai.ts";
import { buildDiagnosticReport, buildInconclusiveReport } from "./diagnostic-report.ts";
import { createSession, hashPassword, revokeSession, validateSession, verifyPassword } from "./auth.ts";
import { getBundledRuleset } from "./bundled-ruleset.ts";
import type { DiagnosticResult, ProviderDetectionInput, ProviderSupport } from "./contracts.ts";
import {
  getGameCatalogItem,
  getGameCatalogVersion,
  listGameAudit,
  listGameCatalog,
  listGameProfiles,
  setGameCatalogItemActive,
  syncGameSeedToDb,
  upsertGameCatalogItem,
  upsertGameProfile,
} from "./game-catalog.ts";
import {
  enqueuePendingProviderReviews,
  getProviderByAsn,
  getProviderById,
  getProviderLogoBinary,
  listProviderReviewQueue,
  listStaleProviders,
  registerProviderDetection,
  reviewProvider,
  searchProviders,
  syncSeedProvidersToDb,
  updateProviderSupport,
  uploadProviderLogo,
  upsertProvider,
} from "./provider-directory.ts";
import { createRulesetDraft, getPublishedRulesetJson, getRuleset, listRulesets, publishRuleset, rollbackRuleset } from "./ruleset-store.ts";

type Env = {
  DB?: D1Database;
  DIAGNOSTIC_RULESET_JSON?: string;
  PROVIDER_DIRECTORY_SEED_JSON?: string;
  GAME_CATALOG_SEED_JSON?: string;
  GAME_PROFILE_SEED_JSON?: string;
  ADMIN_AUTH_PEPPER?: string;
  ADMIN_BOOTSTRAP_TOKEN?: string;
  // GH#960 — origem permitida pra CORS em /admin/* (mesmo padrao do
  // signallq-admin-worker). Sem esta var, Access-Control-Allow-Origin fica
  // vazio (nenhuma origem liberada) em vez de quebrar o worker.
  ALLOWED_ORIGIN?: string;
  // GH#965 (revisado) — base publica pra montar `ProviderLogo.url` (o proprio
  // worker serve o binario via `GET /providers/:id/logo`, gravado como BLOB
  // base64 no D1 — ver provider-directory.ts). R2 descartado por decisao de
  // produto (Cloudflare exige cartao mesmo no tier gratis).
  PROVIDER_LOGO_PUBLIC_BASE_URL?: string;
};

// GH#960 — decisao de arquitetura: o worker e consumido tanto pelo app Android
// (server-to-server, nao sujeito a CORS) quanto potencialmente por um futuro
// painel administrativo em browser sob /admin/* (mesmo padrao ja usado pelo
// signallq-admin-worker, que serve o SignallQ Console). Em vez de inventar um
// padrao novo, aplicamos os mesmos headers do worker irmao — mas so no ponto
// de saida (`route`/`fetch`), pra nao precisar threadar `env` por cada handler.
function corsHeaders(env: Env): Record<string, string> {
  return {
    "Access-Control-Allow-Origin": env.ALLOWED_ORIGIN ?? "",
    "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization, Cookie",
    "Access-Control-Allow-Credentials": "true",
    "Access-Control-Max-Age": "86400",
  };
}

function withCors(response: Response, env: Env): Response {
  const headers = new Headers(response.headers);
  for (const [key, value] of Object.entries(corsHeaders(env))) {
    headers.set(key, value);
  }
  return new Response(response.body, { status: response.status, headers });
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body, null, 2), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
    },
  });
}

function parseJsonSafely<T>(value: string | null | undefined): T | null {
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

// GH#954 — evaluationSource precisa refletir a origem real do ruleset usado,
// nao um "REMOTE" fixo. REMOTE cobre tanto o ruleset publicado no D1 quanto o
// fallback via DIAGNOSTIC_RULESET_JSON (var de ambiente que representa um
// ruleset remoto configurado, so que sem D1) — os dois sao "nao e o bundled
// default". CACHED_LOCAL fica reservado pro contrato: nao ha camada de cache
// implementada hoje (toda leitura remota bate direto no D1), entao nunca e
// emitido por este worker ainda.
async function loadRuleset(env: Env): Promise<{ ruleset: ReturnType<typeof getBundledRuleset>; source: DiagnosticResult["evaluationSource"] }> {
  const publishedJson = env.DB ? await getPublishedRulesetJson(env.DB) : null;
  const parsed = parseJsonSafely(publishedJson ?? env.DIAGNOSTIC_RULESET_JSON);
  const validation = parsed ? validateRuleset(parsed) : null;
  if (validation?.ok) {
    return { ruleset: validation.ruleset, source: "REMOTE" };
  }
  return { ruleset: getBundledRuleset(), source: "BUNDLED_LOCAL" };
}

function getSessionToken(request: Request): string | null {
  const cookie = request.headers.get("Cookie") ?? "";
  const match = cookie.match(/(?:^|;\s*)session=([^;]+)/);
  return match ? match[1] : null;
}

async function authenticateSession(request: Request, env: Env): Promise<{ userId: string; role: string } | null> {
  if (!env.DB) return null;
  const token = getSessionToken(request);
  if (!token) return null;
  return validateSession(token, env.DB);
}

async function checkRateLimit(ip: string, db: D1Database): Promise<boolean> {
  const now = Math.floor(Date.now() / 1000);
  const row = await db.prepare(
    "SELECT count, window_start FROM auth_rate_limit WHERE ip = ?",
  ).bind(ip).first<{ count: number; window_start: number }>();
  if (!row) return false;
  if (now - row.window_start > 15 * 60) return false;
  return row.count > 5;
}

async function incrementRateLimit(ip: string, db: D1Database): Promise<void> {
  const now = Math.floor(Date.now() / 1000);
  const row = await db.prepare(
    "SELECT count, window_start FROM auth_rate_limit WHERE ip = ?",
  ).bind(ip).first<{ count: number; window_start: number }>();
  if (!row || now - row.window_start > 15 * 60) {
    await db.prepare(
      "INSERT OR REPLACE INTO auth_rate_limit (ip, count, window_start) VALUES (?, 1, ?)",
    ).bind(ip, now).run();
    return;
  }
  await db.prepare("UPDATE auth_rate_limit SET count = count + 1 WHERE ip = ?").bind(ip).run();
}

async function handleDiagnosticEvaluate(request: Request, env: Env): Promise<Response> {
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  const snapshotValidation = validateSnapshot(payload);
  if (!snapshotValidation.ok) {
    return json({ error: "Invalid diagnostic snapshot.", details: snapshotValidation.errors }, 400);
  }

  // GH#959 — fallback minimo garantido: qualquer falha do motor daqui pra
  // frente (D1 indisponivel, excecao interna no engine/report/IA) ainda
  // retorna um payload valido no contrato, com veredito "inconclusivo" e
  // motivo legivel — nunca 500 cru.
  try {
    const { ruleset, source } = await loadRuleset(env);
    const result = evaluateSnapshot(snapshotValidation.snapshot, ruleset, source);
    const gameProfiles = await listGameProfiles(env).catch(() => []);
    const report = buildDiagnosticReport(snapshotValidation.snapshot, result, gameProfiles);
    report.aiAssist = buildDiagnosticAiPrompt(snapshotValidation.snapshot, report);
    return json(report);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error("[handleDiagnosticEvaluate] falha no motor:", message);
    return json(buildInconclusiveReport(`Motivo tecnico: ${message}.`), 200);
  }
}

async function handleRulesetValidate(request: Request): Promise<Response> {
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  const validation = validateRuleset(payload);
  if (!validation.ok) {
    return json({ ok: false, errors: validation.errors }, 400);
  }
  return json({ ok: true, version: validation.ruleset.version, rules: validation.ruleset.rules.length });
}

async function handleRulesetList(env: Env): Promise<Response> {
  if (!env.DB) {
    const bundled = getBundledRuleset();
    return json({
      items: [
        {
          version: bundled.version,
          schemaVersion: bundled.schemaVersion,
          engineVersion: bundled.engineVersion,
          status: "BUNDLED_LOCAL",
          publishedAt: bundled.publishedAt,
        },
      ],
    });
  }

  return json({ items: await listRulesets(env.DB) });
}

async function handleRulesetGet(version: number, env: Env): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  const ruleset = await getRuleset(env.DB, version);
  if (!ruleset) return json({ error: "Ruleset not found." }, 404);
  return json(ruleset);
}

async function handleRulesetCreate(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }
  const body = payload as { ruleset?: unknown; justification?: string };
  const validation = validateRuleset(body.ruleset);
  if (!validation.ok) return json({ error: "Invalid ruleset.", details: validation.errors }, 400);
  await createRulesetDraft(env.DB, validation.ruleset, session.userId, body.justification ?? "");
  return json({ ok: true, version: validation.ruleset.version }, 201);
}

async function handleRulesetPublish(version: number, env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  await publishRuleset(env.DB, version, session.userId);
  return json({ ok: true, version, status: "PUBLISHED" });
}

async function handleRulesetRollback(version: number, env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  await rollbackRuleset(env.DB, version, session.userId);
  return json({ ok: true, version, status: "ROLLED_BACK" });
}

async function handleDiagnosticSimulate(request: Request): Promise<Response> {
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  const body = payload as { snapshot?: unknown; ruleset?: unknown };
  const snapshotValidation = validateSnapshot(body.snapshot);
  if (!snapshotValidation.ok) {
    return json({ error: "Invalid diagnostic snapshot.", details: snapshotValidation.errors }, 400);
  }

  const rulesetValidation = validateRuleset(body.ruleset);
  if (!rulesetValidation.ok) {
    return json({ error: "Invalid ruleset.", details: rulesetValidation.errors }, 400);
  }

  return json({
    ok: true,
    result: evaluateSnapshot(snapshotValidation.snapshot, rulesetValidation.ruleset),
  });
}

async function handleProviderByAsn(asn: number, env: Env): Promise<Response> {
  const provider = await getProviderByAsn(env, asn);
  if (!provider) return json({ error: "Provider not found." }, 404);
  return json(provider);
}

async function handleProviderById(providerId: string, env: Env): Promise<Response> {
  const provider = await getProviderById(env, providerId);
  if (!provider) return json({ error: "Provider not found." }, 404);
  return json(provider);
}

async function handleProviderSupport(providerId: string, env: Env): Promise<Response> {
  const provider = await getProviderById(env, providerId);
  if (!provider) return json({ error: "Provider not found." }, 404);
  return json(provider.support);
}

async function handleProviderSearch(url: URL, env: Env): Promise<Response> {
  const query = url.searchParams.get("q") ?? "";
  return json({ items: await searchProviders(env, query) });
}

// GH#961 — endpoint publico/anonimo: payload malformado ou null nao pode
// derrubar o worker com 500 cru. Valida shape minimo antes de tocar D1.
function validateProviderDetectionInput(input: unknown): { ok: true; input: ProviderDetectionInput } | { ok: false; errors: string[] } {
  if (!input || typeof input !== "object") {
    return { ok: false, errors: ["Body must be an object."] };
  }
  const body = input as Record<string, unknown>;
  const errors: string[] = [];
  const optionalString = (key: string) => {
    if (key in body && body[key] !== null && typeof body[key] !== "string") {
      errors.push(`${key} must be a string when present.`);
    }
  };
  optionalString("providerId");
  optionalString("rawNameSample");
  optionalString("normalizedName");
  optionalString("installationHash");
  optionalString("appVersion");
  optionalString("platform");
  optionalString("detectedAt");
  if ("asn" in body && body.asn !== null && typeof body.asn !== "number") {
    errors.push("asn must be a number when present.");
  }
  if (errors.length > 0) {
    return { ok: false, errors };
  }
  return { ok: true, input: body as ProviderDetectionInput };
}

async function handleProviderDetections(request: Request, env: Env): Promise<Response> {
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  const validation = validateProviderDetectionInput(payload);
  if (!validation.ok) {
    return json({ error: "Invalid provider detection payload.", details: validation.errors }, 400);
  }

  const result = await registerProviderDetection(env, validation.input);
  return json({ ok: true, ...result }, 202);
}

async function handleProviderSeedSync(env: Env): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  const result = await syncSeedProvidersToDb(env);
  return json({ ok: true, ...result });
}

async function handleProviderReviewQueue(env: Env): Promise<Response> {
  if (!env.DB) return json({ items: [] });
  return json({ items: await listProviderReviewQueue(env) });
}

async function handleProviderStale(env: Env): Promise<Response> {
  if (!env.DB) return json({ items: [] });
  return json({ items: await listStaleProviders(env) });
}

async function handleProviderUpsert(request: Request, env: Env): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  const body = payload as { provider?: { id?: string; displayName?: string } };
  if (!body.provider?.id || !body.provider.displayName) {
    return json({ error: "provider.id and provider.displayName are required." }, 400);
  }

  const result = await upsertProvider(env, body.provider as Parameters<typeof upsertProvider>[1]);
  return json({ ok: true, ...result }, 201);
}

async function handleProviderReview(request: Request, env: Env, providerId: string): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }
  const result = await reviewProvider(env, providerId, payload as Parameters<typeof reviewProvider>[2]);
  return json(result);
}

// GH#965 — payload aceita chaves parciais: presenca da chave = "editar este
// canal" (mesmo com valor null/vazio, que remove); ausencia = "nao mexer".
// Por isso valida tipos (nao objeto -> erro) mas NAO exige nenhuma chave.
function validateProviderSupportInput(input: unknown): { ok: true; support: Partial<ProviderSupport> } | { ok: false; errors: string[] } {
  if (!input || typeof input !== "object") {
    return { ok: false, errors: ["Body must be an object."] };
  }
  const body = input as Record<string, unknown>;
  const fields: Array<keyof ProviderSupport> = [
    "sacPhone",
    "technicalSupportPhone",
    "whatsappUrl",
    "websiteUrl",
    "customerAreaUrl",
    "ombudsmanPhone",
  ];
  const errors: string[] = [];
  const support: Partial<ProviderSupport> = {};
  for (const field of fields) {
    if (!(field in body)) continue;
    const value = body[field];
    if (value !== null && typeof value !== "string") {
      errors.push(`${field} must be a string or null when present.`);
      continue;
    }
    support[field] = value as string | null;
  }
  if (errors.length > 0) return { ok: false, errors };
  return { ok: true, support };
}

async function handleProviderSupportUpdate(request: Request, env: Env, providerId: string): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  const validation = validateProviderSupportInput(payload);
  if (!validation.ok) {
    return json({ error: "Invalid provider support payload.", details: validation.errors }, 400);
  }

  const result = await updateProviderSupport(env, providerId, validation.support);
  if (!result) return json({ error: "Provider not found." }, 404);
  return json(result);
}

async function handleProviderLogoUpload(request: Request, env: Env, providerId: string): Promise<Response> {
  const contentType = request.headers.get("Content-Type") ?? "";
  if (!contentType.startsWith("image/")) {
    return json({ error: "Content-Type must be an image/* mime type." }, 400);
  }

  let bytes: ArrayBuffer;
  try {
    bytes = await request.arrayBuffer();
  } catch {
    return json({ error: "Invalid request body." }, 400);
  }
  if (bytes.byteLength === 0) {
    return json({ error: "Empty request body." }, 400);
  }

  const result = await uploadProviderLogo(env, providerId, bytes, contentType);
  if (!result.ok) return json({ error: result.error }, result.status);
  return json({ ok: true, url: result.url, version: result.version }, 201);
}

// GH#965 — rota publica que serve o binario da logo gravado no D1 (BLOB
// base64) diretamente, com o content-type real de upload. 404 tratado quando
// o provedor nao tem logo com blob (seeded local via sourceUrl, ou nunca
// enviado) — nunca 500 cru.
async function handleProviderLogoGet(providerId: string, env: Env): Promise<Response> {
  const asset = await getProviderLogoBinary(env, providerId);
  if (!asset) return json({ error: "Logo not found." }, 404);
  return new Response(asset.bytes, {
    status: 200,
    headers: {
      "content-type": asset.contentType,
      "cache-control": "public, max-age=86400",
    },
  });
}

async function handleGameCatalogList(url: URL, env: Env): Promise<Response> {
  const platform = url.searchParams.get("platform");
  return json({ items: await listGameCatalog(env, platform) });
}

async function handleGameCatalogVersion(env: Env): Promise<Response> {
  return json(await getGameCatalogVersion(env));
}

async function handleGameCatalogGet(env: Env, gameId: string): Promise<Response> {
  const item = await getGameCatalogItem(env, gameId);
  if (!item) return json({ error: "Game not found." }, 404);
  return json(item);
}

async function handleGameProfilesList(env: Env): Promise<Response> {
  return json({ items: await listGameProfiles(env) });
}

async function handleGameSeedSync(env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  const result = await syncGameSeedToDb(env, session.userId);
  return json({ ok: true, ...result });
}

async function handleGameCatalogUpsert(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }
  const body = payload as { game?: { gameId?: string; displayName?: string; slug?: string; profileCode?: string; testStrategy?: string; regionCode?: string; resultLabel?: string; platforms?: string[] } };
  if (!body.game?.gameId || !body.game.displayName || !body.game.slug || !body.game.profileCode || !body.game.testStrategy || !body.game.regionCode || !body.game.resultLabel || !Array.isArray(body.game.platforms) || body.game.platforms.length === 0) {
    return json({ error: "game payload is incomplete." }, 400);
  }
  const result = await upsertGameCatalogItem(env, body.game as Parameters<typeof upsertGameCatalogItem>[1], session.userId);
  return json({ ok: true, ...result }, 201);
}

async function handleGameProfileUpsert(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }
  const body = payload as { profile?: { profileCode?: string; displayName?: string } };
  if (!body.profile?.profileCode || !body.profile.displayName) {
    return json({ error: "profile.profileCode and profile.displayName are required." }, 400);
  }
  const result = await upsertGameProfile(env, body.profile as Parameters<typeof upsertGameProfile>[1], session.userId);
  return json({ ok: true, ...result }, 201);
}

async function handleGameSetActive(env: Env, gameId: string, active: boolean, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB) return json({ error: "DB binding not configured." }, 500);
  try {
    const result = await setGameCatalogItemActive(env, gameId, active, session.userId);
    return json({ ok: true, ...result });
  } catch (error) {
    if (error instanceof Error && error.message === "Game not found.") {
      return json({ error: error.message }, 404);
    }
    throw error;
  }
}

async function handleGameAudit(env: Env): Promise<Response> {
  if (!env.DB) return json({ items: [] });
  return json({ items: await listGameAudit(env) });
}

async function handleAuthLogin(request: Request, env: Env): Promise<Response> {
  if (!env.DB || !env.ADMIN_AUTH_PEPPER) return json({ error: "Admin auth not configured." }, 500);
  const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
  if (await checkRateLimit(ip, env.DB)) {
    return json({ error: "Too many attempts. Try again later." }, 429);
  }
  let body: { email?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }
  if (!body.email || !body.password) return json({ error: "email and password are required." }, 400);
  const user = await env.DB.prepare(
    "SELECT id, password_hash, role FROM admin_users WHERE email = ? AND active = 1",
  ).bind(body.email).first<{ id: string; password_hash: string; role: string }>();
  if (!user || !(await verifyPassword(body.password, user.password_hash, env.ADMIN_AUTH_PEPPER))) {
    await incrementRateLimit(ip, env.DB);
    return json({ error: "Invalid credentials." }, 401);
  }
  const token = await createSession(user.id, env.DB);
  await env.DB.prepare("UPDATE admin_users SET last_login = ? WHERE id = ?")
    .bind(Math.floor(Date.now() / 1000), user.id).run();
  return new Response(JSON.stringify({ ok: true, role: user.role }), {
    status: 200,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "set-cookie": `session=${token}; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800`,
    },
  });
}

async function handleAuthLogout(request: Request, env: Env): Promise<Response> {
  if (env.DB) {
    const token = getSessionToken(request);
    if (token) await revokeSession(token, env.DB);
  }
  return new Response(JSON.stringify({ ok: true }), {
    status: 200,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "set-cookie": "session=; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=0",
    },
  });
}

async function handleAuthMe(request: Request, env: Env): Promise<Response> {
  if (!env.DB) return json({ error: "Admin auth not configured." }, 500);
  const session = await authenticateSession(request, env);
  if (!session) return json({ error: "Unauthorized." }, 401);
  const user = await env.DB.prepare(
    "SELECT email, role FROM admin_users WHERE id = ?",
  ).bind(session.userId).first<{ email: string; role: string }>();
  if (!user) return json({ error: "Unauthorized." }, 401);
  return json({ email: user.email, role: user.role });
}

async function handleAuthCreateUser(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (!env.DB || !env.ADMIN_AUTH_PEPPER) return json({ error: "Admin auth not configured." }, 500);
  if (session.role !== "admin") return json({ error: "Forbidden." }, 403);
  let body: { email?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }
  if (!body.email || !body.password) return json({ error: "email and password are required." }, 400);
  const passwordHash = await hashPassword(body.password, env.ADMIN_AUTH_PEPPER);
  const id = crypto.randomUUID();
  await env.DB.prepare(
    "INSERT INTO admin_users (id, email, password_hash, role, active, created_at) VALUES (?, ?, ?, 'admin', 1, ?)",
  ).bind(id, body.email, passwordHash, Math.floor(Date.now() / 1000)).run();
  return json({ ok: true, id }, 201);
}

async function handleAuthBootstrap(request: Request, env: Env): Promise<Response> {
  if (!env.DB || !env.ADMIN_AUTH_PEPPER || !env.ADMIN_BOOTSTRAP_TOKEN) {
    return json({ error: "Admin bootstrap not configured." }, 500);
  }

  const existing = await env.DB.prepare("SELECT COUNT(*) AS count FROM admin_users").first<{ count: number }>();
  if ((existing?.count ?? 0) > 0) {
    return json({ error: "Admin bootstrap already completed." }, 409);
  }

  let body: { bootstrapToken?: string; email?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  if (body.bootstrapToken !== env.ADMIN_BOOTSTRAP_TOKEN) {
    return json({ error: "Invalid bootstrap token." }, 401);
  }
  if (!body.email || !body.password) {
    return json({ error: "email and password are required." }, 400);
  }

  const passwordHash = await hashPassword(body.password, env.ADMIN_AUTH_PEPPER);
  const id = crypto.randomUUID();
  await env.DB.prepare(
    "INSERT INTO admin_users (id, email, password_hash, role, active, created_at) VALUES (?, ?, ?, 'admin', 1, ?)",
  ).bind(id, body.email, passwordHash, Math.floor(Date.now() / 1000)).run();
  return json({ ok: true, id, bootstrapped: true }, 201);
}

async function route(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);

  // GH#960 — preflight de CORS. Ver decisao no comentario de `corsHeaders`.
  if (request.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders(env) });
  }

  if (request.method === "GET" && url.pathname === "/health") {
    return json({
      ok: true,
      worker: "signallq-diagnostic",
      routes: [
        "/diagnostic/evaluate",
        "/admin/diagnostic/rulesets",
        "/admin/diagnostic/rulesets/:version",
        "/admin/diagnostic/rulesets/:version/publish",
        "/admin/diagnostic/rulesets/:version/rollback",
        "/admin/diagnostic/rulesets/validate",
        "/admin/diagnostic/simulate",
        "/admin/auth/bootstrap",
        "/admin/auth/login",
        "/admin/auth/logout",
        "/admin/auth/me",
        "/admin/providers/review-queue",
        "/admin/providers/stale",
        "/admin/providers/sync-seed",
        "/admin/providers",
        "/admin/providers/:providerId/review",
        "/admin/providers/:providerId/support",
        "/admin/providers/:providerId/logo",
        "/admin/games/sync-seed",
        "/admin/games/catalog",
        "/admin/games/catalog/:gameId/activate",
        "/admin/games/catalog/:gameId/deactivate",
        "/admin/games/profiles",
        "/admin/games/audit",
        "/games/catalog",
        "/games/catalog/version",
        "/games/catalog/:gameId",
        "/games/profiles",
        "/providers/by-asn/:asn",
        "/providers/:providerId",
        "/providers/:providerId/support",
        "/providers/:providerId/logo",
        "/providers/search",
        "/ingest/provider-detection",
      ],
    });
  }

  if (request.method === "POST" && (url.pathname === "/diagnostic/evaluate" || url.pathname === "/api/diagnostic/evaluate")) {
    return handleDiagnosticEvaluate(request, env);
  }

  if (url.pathname === "/admin/auth/login" && request.method === "POST") {
    return handleAuthLogin(request, env);
  }
  if (url.pathname === "/admin/auth/bootstrap" && request.method === "POST") {
    return handleAuthBootstrap(request, env);
  }
  if (url.pathname === "/admin/auth/logout" && request.method === "POST") {
    return handleAuthLogout(request, env);
  }
  if (url.pathname === "/admin/auth/me" && request.method === "GET") {
    return handleAuthMe(request, env);
  }

  const needsAdminSession = url.pathname.startsWith("/admin/") && Boolean(env.DB);
  const session = needsAdminSession ? await authenticateSession(request, env) : null;
  if (needsAdminSession && !url.pathname.startsWith("/admin/auth/") && !session) {
    return json({ error: "Unauthorized." }, 401);
  }

  if (request.method === "GET" && (url.pathname === "/admin/diagnostic/rulesets" || url.pathname === "/api/admin/diagnostic/rulesets")) {
    return handleRulesetList(env);
  }
  if (request.method === "POST" && url.pathname === "/admin/diagnostic/rulesets") {
    return handleRulesetCreate(request, env, session!);
  }
  const rulesetMatch = url.pathname.match(/^\/admin\/diagnostic\/rulesets\/(\d+)$/);
  if (request.method === "GET" && rulesetMatch) {
    return handleRulesetGet(Number(rulesetMatch[1]), env);
  }
  const rulesetPublishMatch = url.pathname.match(/^\/admin\/diagnostic\/rulesets\/(\d+)\/publish$/);
  if (request.method === "POST" && rulesetPublishMatch) {
    return handleRulesetPublish(Number(rulesetPublishMatch[1]), env, session!);
  }
  const rulesetRollbackMatch = url.pathname.match(/^\/admin\/diagnostic\/rulesets\/(\d+)\/rollback$/);
  if (request.method === "POST" && rulesetRollbackMatch) {
    return handleRulesetRollback(Number(rulesetRollbackMatch[1]), env, session!);
  }

  if (request.method === "POST" && (url.pathname === "/admin/diagnostic/rulesets/validate" || url.pathname === "/api/admin/diagnostic/rulesets/validate")) {
    return handleRulesetValidate(request);
  }

  if (request.method === "POST" && (url.pathname === "/admin/diagnostic/simulate" || url.pathname === "/api/admin/diagnostic/simulate")) {
    return handleDiagnosticSimulate(request);
  }
  if (request.method === "POST" && url.pathname === "/admin/auth/users") {
    return handleAuthCreateUser(request, env, session!);
  }
  if (request.method === "GET" && url.pathname === "/admin/providers/review-queue") {
    return handleProviderReviewQueue(env);
  }
  if (request.method === "GET" && url.pathname === "/admin/providers/stale") {
    return handleProviderStale(env);
  }
  if (request.method === "POST" && url.pathname === "/admin/providers/sync-seed") {
    return handleProviderSeedSync(env);
  }
  if (request.method === "POST" && url.pathname === "/admin/providers") {
    return handleProviderUpsert(request, env);
  }
  const providerReviewMatch = url.pathname.match(/^\/admin\/providers\/([^/]+)\/review$/);
  if (request.method === "POST" && providerReviewMatch) {
    return handleProviderReview(request, env, providerReviewMatch[1]!);
  }
  const providerSupportEditMatch = url.pathname.match(/^\/admin\/providers\/([^/]+)\/support$/);
  if ((request.method === "POST" || request.method === "PUT") && providerSupportEditMatch) {
    return handleProviderSupportUpdate(request, env, providerSupportEditMatch[1]!);
  }
  const providerLogoUploadMatch = url.pathname.match(/^\/admin\/providers\/([^/]+)\/logo$/);
  if (request.method === "POST" && providerLogoUploadMatch) {
    return handleProviderLogoUpload(request, env, providerLogoUploadMatch[1]!);
  }
  if (request.method === "POST" && url.pathname === "/admin/games/sync-seed") {
    return handleGameSeedSync(env, session!);
  }
  if ((request.method === "POST" || request.method === "PUT") && url.pathname === "/admin/games/catalog") {
    return handleGameCatalogUpsert(request, env, session!);
  }
  const gameActivateMatch = url.pathname.match(/^\/admin\/games\/catalog\/([^/]+)\/activate$/);
  if (request.method === "POST" && gameActivateMatch) {
    return handleGameSetActive(env, gameActivateMatch[1]!, true, session!);
  }
  const gameDeactivateMatch = url.pathname.match(/^\/admin\/games\/catalog\/([^/]+)\/deactivate$/);
  if (request.method === "POST" && gameDeactivateMatch) {
    return handleGameSetActive(env, gameDeactivateMatch[1]!, false, session!);
  }
  if ((request.method === "POST" || request.method === "PUT") && url.pathname === "/admin/games/profiles") {
    return handleGameProfileUpsert(request, env, session!);
  }
  if (request.method === "GET" && url.pathname === "/admin/games/audit") {
    return handleGameAudit(env);
  }

  if (request.method === "GET" && url.pathname === "/games/catalog") {
    return handleGameCatalogList(url, env);
  }
  if (request.method === "GET" && url.pathname === "/games/catalog/version") {
    return handleGameCatalogVersion(env);
  }
  const gameCatalogMatch = url.pathname.match(/^\/games\/catalog\/([^/]+)$/);
  if (request.method === "GET" && gameCatalogMatch) {
    return handleGameCatalogGet(env, gameCatalogMatch[1]!);
  }
  if (request.method === "GET" && url.pathname === "/games/profiles") {
    return handleGameProfilesList(env);
  }

  if (request.method === "GET" && (url.pathname.startsWith("/providers/by-asn/") || url.pathname.startsWith("/api/providers/by-asn/"))) {
    const asn = Number(url.pathname.split("/").pop());
    if (!Number.isInteger(asn)) {
      return json({ error: "ASN must be an integer." }, 400);
    }
    return handleProviderByAsn(asn, env);
  }

  if (request.method === "GET" && (url.pathname === "/providers/search" || url.pathname === "/api/providers/search")) {
    return handleProviderSearch(url, env);
  }

  if (request.method === "POST" && (url.pathname === "/ingest/provider-detection" || url.pathname === "/api/provider-detections")) {
    return handleProviderDetections(request, env);
  }

  if (request.method === "GET" && (url.pathname.startsWith("/providers/") || url.pathname.startsWith("/api/providers/"))) {
    const segments = url.pathname.split("/").filter(Boolean);
    const providerId = segments[1] === "api" ? segments[2] : segments[1];
    if (!providerId) return json({ error: "Provider id is required." }, 400);
    const subSegment = segments[1] === "api" ? segments[3] : segments[2];
    if (subSegment === "support") {
      return handleProviderSupport(providerId, env);
    }
    if (subSegment === "logo") {
      return handleProviderLogoGet(providerId, env);
    }
    return handleProviderById(providerId, env);
  }

  return json({ error: "Not found." }, 404);
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const response = await route(request, env);
    return withCors(response, env);
  },

  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    if (!env.DB) return;
    await env.DB.prepare(
      `UPDATE providers
       SET status = 'STALE'
       WHERE next_review_at IS NOT NULL
         AND next_review_at <= ?`,
    ).bind(new Date().toISOString()).run();
    await enqueuePendingProviderReviews(env);
  },
};
