// SignallQ Admin API Worker
// /admin/* exige sessão httpOnly via cookie (SIG-136 — auth própria via D1).
// /ingest/* exige Bearer INGEST_KEY (chave separada, scope limitado, vai no APK).
// /health exige Bearer ADMIN_SECRET (retrocompat dev/monitoramento externo).
// Separar os secrets reduz o blast radius: vazar INGEST_KEY nao da acesso
// aos dados do admin. INGEST_KEY so pode escrever em /ingest/*.

import { hashPassword, verifyPassword, createSession, validateSession, revokeSession } from './auth'

export interface Env {
  ALLOWED_ORIGIN: string;
  FIREBASE_PROJECT_ID: string;
  FIREBASE_GA4_PROPERTY_ID: string;
  /** Mantido apenas para /health (retrocompat). NÃO protege mais /admin/*. */
  ADMIN_SECRET: string;
  /** Chave separada para ingest do app Android. Scope: POST /ingest/* apenas. */
  INGEST_KEY: string;
  FIREBASE_CLIENT_EMAIL: string;
  FIREBASE_PRIVATE_KEY: string;
  DB: D1Database;
  /** Pepper para PBKDF2 das senhas admin — SIG-136. */
  ADMIN_AUTH_PEPPER: string;
}

function corsHeaders(env: Env): Record<string, string> {
  return {
    "Access-Control-Allow-Origin": env.ALLOWED_ORIGIN,
    "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Environment",
    "Access-Control-Allow-Credentials": "true",
    "Access-Control-Max-Age": "86400",
  };
}

function json(body: unknown, status = 200, env: Env): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...corsHeaders(env) },
  });
}

function err(message: string, status: number, env: Env): Response {
  return json({ error: message }, status, env);
}

/** Mantido apenas para /health (retrocompat). */
function authenticate(request: Request, env: Env): boolean {
  const auth = request.headers.get("Authorization") ?? "";
  const [scheme, token] = auth.split(" ");
  return scheme === "Bearer" && token === env.ADMIN_SECRET;
}

/** Autentica rotas /ingest/* com a INGEST_KEY (scope limitado — so POST /ingest/*). */
function authenticateIngest(request: Request, env: Env): boolean {
  const auth = request.headers.get("Authorization") ?? "";
  const [scheme, token] = auth.split(" ");
  if (scheme !== "Bearer") return false;
  // Aceita INGEST_KEY (chave do app) OU ADMIN_SECRET (retrocompat e dev local).
  return token === env.INGEST_KEY || token === env.ADMIN_SECRET;
}

// --- SIG-136: Auth por sessão ---

function getSessionToken(request: Request): string | null {
  const cookie = request.headers.get('Cookie') ?? ''
  const match = cookie.match(/(?:^|;\s*)session=([^;]+)/)
  return match ? match[1] : null
}

async function authenticateSession(request: Request, env: Env): Promise<{ userId: string; role: string } | null> {
  const token = getSessionToken(request)
  if (!token) return null
  return validateSession(token, env.DB)
}

/** Verifica rate limit: > 5 tentativas em 15 min por IP → bloqueado. */
async function checkRateLimit(ip: string, db: D1Database): Promise<boolean> {
  const now = Math.floor(Date.now() / 1000)
  const windowSec = 15 * 60
  const row = await db.prepare(
    'SELECT count, window_start FROM auth_rate_limit WHERE ip = ?'
  ).bind(ip).first<{ count: number; window_start: number }>()
  if (!row) return false
  if (now - row.window_start > windowSec) return false
  return row.count > 5
}

async function incrementRateLimit(ip: string, db: D1Database): Promise<void> {
  const now = Math.floor(Date.now() / 1000)
  const windowSec = 15 * 60
  const row = await db.prepare(
    'SELECT count, window_start FROM auth_rate_limit WHERE ip = ?'
  ).bind(ip).first<{ count: number; window_start: number }>()
  if (!row || now - row.window_start > windowSec) {
    await db.prepare(
      'INSERT OR REPLACE INTO auth_rate_limit (ip, count, window_start) VALUES (?, 1, ?)'
    ).bind(ip, now).run()
  } else {
    await db.prepare(
      'UPDATE auth_rate_limit SET count = count + 1 WHERE ip = ?'
    ).bind(ip).run()
  }
}

async function handleAuthLogin(request: Request, env: Env): Promise<Response> {
  const ip = request.headers.get('CF-Connecting-IP') ?? 'unknown'
  if (await checkRateLimit(ip, env.DB)) {
    return err('Muitas tentativas. Tente novamente em 15 minutos.', 429, env)
  }

  let body: { email?: string; password?: string }
  try { body = await request.json() } catch { return err('body JSON inválido', 400, env) }
  const { email, password } = body
  if (!email || !password) return err('email e password obrigatórios', 400, env)

  const user = await env.DB.prepare(
    'SELECT id, password_hash, role FROM admin_users WHERE email = ? AND active = 1'
  ).bind(email).first<{ id: string; password_hash: string; role: string }>()

  if (!user || !(await verifyPassword(password, user.password_hash, env.ADMIN_AUTH_PEPPER))) {
    await incrementRateLimit(ip, env.DB)
    return err('E-mail ou senha inválidos', 401, env)
  }

  const token = await createSession(user.id, env.DB)
  await env.DB.prepare('UPDATE admin_users SET last_login = ? WHERE id = ?')
    .bind(Math.floor(Date.now() / 1000), user.id).run()

  return new Response(JSON.stringify({ ok: true, role: user.role }), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Set-Cookie': `session=${token}; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800`,
      ...corsHeaders(env),
    },
  })
}

async function handleAuthLogout(request: Request, env: Env): Promise<Response> {
  const token = getSessionToken(request)
  if (token) await revokeSession(token, env.DB)
  return new Response(JSON.stringify({ ok: true }), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Set-Cookie': 'session=; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=0',
      ...corsHeaders(env),
    },
  })
}

async function handleAuthMe(request: Request, env: Env): Promise<Response> {
  const session = await authenticateSession(request, env)
  if (!session) return err('Unauthorized', 401, env)
  const user = await env.DB.prepare(
    'SELECT email, role FROM admin_users WHERE id = ?'
  ).bind(session.userId).first<{ email: string; role: string }>()
  if (!user) return err('Unauthorized', 401, env)
  return json({ email: user.email, role: user.role }, 200, env)
}

async function handleAuthCreateUser(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  if (session.role !== 'admin') return err('Forbidden', 403, env)
  let body: { email?: string; password?: string }
  try { body = await request.json() } catch { return err('body JSON inválido', 400, env) }
  const { email, password } = body
  if (!email || !password) return err('email e password obrigatórios', 400, env)

  const hash = await hashPassword(password, env.ADMIN_AUTH_PEPPER)
  const id = crypto.randomUUID()
  const now = Math.floor(Date.now() / 1000)

  try {
    await env.DB.prepare(
      'INSERT INTO admin_users (id, email, password_hash, role, active, created_at) VALUES (?, ?, ?, ?, 1, ?)'
    ).bind(id, email, hash, 'admin', now).run()
  } catch (e) {
    const msg = String(e)
    if (msg.includes('UNIQUE')) return err('E-mail já cadastrado', 409, env)
    throw e
  }

  return json({ ok: true, id }, 201, env)
}

async function handleAuthChangePassword(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  let body: { currentPassword?: string; newPassword?: string }
  try { body = await request.json() } catch { return err('body JSON inválido', 400, env) }
  const { currentPassword, newPassword } = body
  if (!currentPassword || !newPassword) return err('currentPassword e newPassword obrigatórios', 400, env)

  const user = await env.DB.prepare(
    'SELECT password_hash FROM admin_users WHERE id = ?'
  ).bind(session.userId).first<{ password_hash: string }>()

  if (!user || !(await verifyPassword(currentPassword, user.password_hash, env.ADMIN_AUTH_PEPPER))) {
    return err('Senha atual incorreta', 401, env)
  }

  const hash = await hashPassword(newPassword, env.ADMIN_AUTH_PEPPER)
  await env.DB.prepare('UPDATE admin_users SET password_hash = ? WHERE id = ?')
    .bind(hash, session.userId).run()

  return json({ ok: true }, 200, env)
}

function periodToSeconds(period: string): number {
  const map: Record<string, number> = {
    "1d": 86400, "7d": 604800, "30d": 2592000, "90d": 7776000,
  };
  return map[period] ?? 604800;
}

function nowSec(): number {
  return Math.floor(Date.now() / 1000);
}

/**
 * Extrai filtro de environment da query string.
 * ?environment=production → filtra por 'production'
 * ?environment=all ou ausente → sem filtro (retorna null)
 */
function getEnvironmentFilter(url: URL): string | null {
  const env = url.searchParams.get("environment");
  return env === "all" || !env ? null : env;
}

async function getFirebaseAccessToken(env: Env): Promise<string> {
  const now = nowSec();
  const payload = {
    iss: env.FIREBASE_CLIENT_EMAIL,
    sub: env.FIREBASE_CLIENT_EMAIL,
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
    scope: [
      "https://www.googleapis.com/auth/firebase",
      "https://www.googleapis.com/auth/analytics.readonly",
      "https://www.googleapis.com/auth/cloud-platform",
    ].join(" "),
  };
  const privateKey = env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, "\n");
  const keyData = privateKey
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const binaryKey = Uint8Array.from(atob(keyData), (c) => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8", binaryKey,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false, ["sign"]
  );
  const toB64Url = (s: string) =>
    btoa(s).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
  const header = toB64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const body   = toB64Url(JSON.stringify(payload));
  const sigInput = new TextEncoder().encode(`${header}.${body}`);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, sigInput);
  const sig = toB64Url(String.fromCharCode(...new Uint8Array(signature)));
  const tokenResp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${body}.${sig}`,
    }),
  });
  const tokenData = (await tokenResp.json()) as { access_token: string };
  return tokenData.access_token;
}

async function queryBigQuery<T = unknown>(
  env: Env,
  query: string
): Promise<{ rows: T[]; error?: string }> {
  let token: string;
  try {
    token = await getFirebaseAccessToken(env);
  } catch (e) {
    return { rows: [], error: `auth_failed: ${String(e)}` };
  }

  const resp = await fetch(
    `https://bigquery.googleapis.com/bigquery/v2/projects/${env.FIREBASE_PROJECT_ID}/queries`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ query, useLegacySql: false, timeoutMs: 10000 }),
    }
  );

  if (!resp.ok) {
    const errText = await resp.text();
    if (errText.includes("Not found") || errText.includes("notFound")) {
      return { rows: [], error: "table_not_found" };
    }
    return { rows: [], error: `bq_error_${resp.status}: ${errText.slice(0, 200)}` };
  }

  const data = (await resp.json()) as {
    rows?: Array<{ f: Array<{ v: string | null }> }>;
    schema?: { fields: Array<{ name: string }> };
    jobComplete?: boolean;
  };

  if (!data.jobComplete || !data.rows || !data.schema) return { rows: [] };

  const fields = data.schema.fields.map((f) => f.name);
  const rows = data.rows.map((row) => {
    const obj: Record<string, string | null> = {};
    row.f.forEach((cell, i) => { obj[fields[i]] = cell.v; });
    return obj as unknown as T;
  });

  return { rows };
}

// --- handlers /admin ---

async function handleOverview(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const todaySince = nowSec() - 86400;
  const envFilter = getEnvironmentFilter(url);

  const envClause    = envFilter ? " AND environment = ?" : "";
  const envBinds     = envFilter ? [envFilter]            : [];

  const [sessions, aiRows] = await Promise.all([
    env.DB.prepare(
      `SELECT COUNT(*) AS total,
              SUM(CASE WHEN resolved=0 THEN 1 ELSE 0 END) AS active,
              AVG(CAST(score AS REAL)) AS avg_score
       FROM diagnostic_sessions WHERE created_at >= ?${envClause}`
    ).bind(since, ...envBinds).first<{ total: number; active: number; avg_score: number | null }>(),
    env.DB.prepare(
      `SELECT COUNT(*) AS calls, SUM(cost_usd) AS cost, SUM(total_tokens) AS tokens
       FROM ai_usage WHERE created_at >= ?${envClause}`
    ).bind(todaySince, ...envBinds).first<{ calls: number; cost: number; tokens: number }>(),
  ]);

  return json({
    source: "d1", period,
    environment:      envFilter ?? "all",
    totalDiagnostics: sessions?.total ?? 0,
    activeSessions:   sessions?.active ?? 0,
    avgNetworkScore:  sessions?.avg_score ? Math.round(sessions.avg_score) : 0,
    aiCallsToday:     aiRows?.calls  ?? 0,
    aiCostToday:      aiRows?.cost   ?? 0,
    aiTokensToday:    aiRows?.tokens ?? 0,
  }, 200, env);
}

async function handleDiagnostics(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const limit     = Math.min(parseInt(url.searchParams.get("limit") ?? "50"), 200);
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  const rows = await env.DB.prepare(
    `SELECT id, created_at, network_type, status, score,
            download_mbps, upload_mbps, latency_ms, jitter_ms, packet_loss,
            issues, resolved, operator,
            device_model, os_version, app_version, ai_summary_report,
            environment, dist_channel, build_type, version_code, device_id
     FROM diagnostic_sessions WHERE created_at >= ?${envClause}
     ORDER BY created_at DESC LIMIT ?`
  ).bind(since, ...envBinds, limit).all();

  const sessions = (rows.results ?? []).map((r: any) => ({
    id:                r.id,
    created_at:        r.created_at,
    network_type:      r.network_type,
    status:            r.status,
    score:             r.score,
    download_mbps:     r.download_mbps,
    upload_mbps:       r.upload_mbps,
    latency_ms:        r.latency_ms,
    jitter_ms:         r.jitter_ms,
    packet_loss:       r.packet_loss,
    issues:            JSON.parse(r.issues ?? "[]"),
    resolved:          r.resolved,
    operator:          r.operator,
    device_model:      r.device_model      ?? '',
    os_version:        r.os_version        ?? '',
    app_version:       r.app_version       ?? '',
    ai_summary_report: r.ai_summary_report ?? '',
    environment:       r.environment       ?? 'production',
    dist_channel:      r.dist_channel      ?? '',
    build_type:        r.build_type        ?? 'release',
    version_code:      r.version_code      ?? 0,
    device_id:         r.device_id         ?? '',
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", sessions }, 200, env);
}

async function handleAiCost(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  // SIG-125: inclui contagem de chamadas com resposta real (completion_tokens > 0)
  // para calcular reliabilityPercentage por modelo.
  // A tabela ai_usage não possui coluna status — o proxy de sucesso adotado é
  // completion_tokens > 0: a chamada gerou tokens de resposta, portanto foi bem-sucedida.
  // Mesmo critério usado em handleAiCostMetrics. Com tabela vazia, o campo retorna null.
  const rows = await env.DB.prepare(
    `SELECT model,
            COUNT(*) AS calls,
            SUM(total_tokens) AS tokens,
            SUM(cost_usd) AS cost_usd,
            SUM(CASE WHEN completion_tokens > 0 THEN 1 ELSE 0 END) AS successful_calls
     FROM ai_usage WHERE created_at >= ?${envClause} GROUP BY model ORDER BY calls DESC`
  ).bind(since, ...envBinds).all();

  const byModel = (rows.results ?? []).map((r: any) => {
    const calls     = r.calls ?? 0;
    const successful = r.successful_calls ?? 0;
    return {
      model:                 r.model,
      calls,
      tokens:                r.tokens    ?? 0,
      cost_usd:              r.cost_usd  ?? 0,
      // null quando não há registros; valor calculado a 2 casas decimais caso contrário.
      reliabilityPercentage: calls > 0 ? Math.round((successful / calls) * 10000) / 100 : null,
    };
  });

  const totals = byModel.reduce(
    (acc: any, r: any) => ({
      calls:  acc.calls  + r.calls,
      tokens: acc.tokens + r.tokens,
      cost:   acc.cost   + r.cost_usd,
    }),
    { calls: 0, tokens: 0, cost: 0 }
  );

  return json({ source: "d1", period, environment: envFilter ?? "all", byModel, totals }, 200, env);
}

// --- handlers /admin/metrics (SIG-110) ---

async function handleTimeline(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  // Agrega diagnostic_sessions por dia (DATE unix→ISO via strftime).
  // activeUsers: não há user_id na tabela — aproximado por contagem de sessões do dia.
  // Documentação da aproximação: 1 sessão ≈ 1 usuário único (subestima heavy users,
  // mas é a melhor proxy disponível sem PII no D1). Ver SIG-110.
  // criticalAlerts: sessões com status='failed' ou score < 40 (limiar "Fraco").
  const rows = await env.DB.prepare(
    `SELECT
       strftime('%Y-%m-%d', datetime(created_at, 'unixepoch')) AS date,
       COUNT(*) AS completedDiagnostics,
       COUNT(*) AS activeUsers,
       SUM(CASE WHEN status = 'failed' OR (score IS NOT NULL AND score < 40) THEN 1 ELSE 0 END) AS criticalAlerts
     FROM diagnostic_sessions
     WHERE created_at >= ?${envClause}
     GROUP BY date
     ORDER BY date ASC`
  ).bind(since, ...envBinds).all();

  const timeline = (rows.results ?? []).map((r: any) => ({
    date:                 r.date,
    completedDiagnostics: r.completedDiagnostics ?? 0,
    activeUsers:          r.activeUsers          ?? 0,
    criticalAlerts:       r.criticalAlerts        ?? 0,
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", timeline }, 200, env);
}

async function handleNetworkInsights(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  // SIG-132: stats completas por network_type.
  // Window function SUM(COUNT(*)) OVER() calcula o total no mesmo passo do GROUP BY
  // (SQLite 3.25+ / D1 suporta). Evita subquery correlacionada com bind duplo.
  const rows = await env.DB.prepare(
    `SELECT
       network_type AS name,
       COUNT(*) AS count,
       AVG(CAST(score AS REAL)) AS avg_score,
       AVG(download_mbps) AS avg_download_mbps,
       AVG(latency_ms) AS avg_latency_ms,
       COUNT(*) * 100.0 / SUM(COUNT(*)) OVER() AS percentage
     FROM diagnostic_sessions
     WHERE created_at >= ?${envClause}
     GROUP BY network_type
     ORDER BY count DESC`
  ).bind(since, ...envBinds).all();

  const items = (rows.results ?? []).map((r: any) => ({
    name:             r.name             ?? "Desconhecido",
    count:            r.count            ?? 0,
    avg_score:        r.avg_score        != null ? Math.round(r.avg_score) : null,
    avg_download_mbps: r.avg_download_mbps ?? null,
    avg_latency_ms:   r.avg_latency_ms   != null ? Math.round(r.avg_latency_ms) : null,
    percentage:       r.percentage       != null ? Math.round(r.percentage * 10) / 10 : 0,
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", items }, 200, env);
}

async function handleTopIssues(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  // Busca todas as sessões do período para explodir o array JSON de issues.
  // D1 não suporta json_each nativamente de forma cross-platform — fazemos no runtime.
  const rows = await env.DB.prepare(
    `SELECT issues FROM diagnostic_sessions WHERE created_at >= ?${envClause}`
  ).bind(since, ...envBinds).all();

  const countMap: Record<string, number> = {};
  let totalIssues = 0;

  for (const row of (rows.results ?? [])) {
    let issues: string[] = [];
    try { issues = JSON.parse((row as any).issues ?? "[]"); } catch { /* ignora linha malformada */ }
    for (const label of issues) {
      if (typeof label === "string" && label.trim()) {
        countMap[label] = (countMap[label] ?? 0) + 1;
        totalIssues++;
      }
    }
  }

  const sorted = Object.entries(countMap)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5);

  const items = sorted.map(([problem, count], idx) => ({
    id:         `issue_${idx + 1}`,
    problem,
    count,
    percentage: totalIssues > 0 ? Math.round((count / totalIssues) * 100) : 0,
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", items }, 200, env);
}

// SIG-133: tipos e helpers para alertas persistidos.

interface AlertRow {
  id: string;
  type: string;
  severity: string;
  title: string;
  message: string;
  created_at: number;
  resolved: number;
}

interface AlertResponse {
  id: string;
  type: string;
  severity: string;
  title: string;
  message: string;
  created_at: number;
  timestamp: string;
  resolved: boolean;
}

function alertRowToResponse(r: AlertRow): AlertResponse {
  return {
    id:         r.id,
    type:       r.type,
    severity:   r.severity,
    title:      r.title,
    message:    r.message,
    created_at: r.created_at,
    timestamp:  new Date(r.created_at * 1000).toISOString(),
    resolved:   r.resolved === 1,
  };
}

/**
 * Verifica thresholds e persiste alertas candidatos de forma idempotente.
 * Regras:
 * - Custo IA > budget diário → critical (requer budget configurado em admin_settings)
 * - Taxa de erros > threshold/hora → warning (usa system_errors.last_seen)
 * - Score médio < mínimo → warning
 *
 * Idempotência: INSERT OR IGNORE por id determinístico. Se o alerta já existe
 * e não foi resolvido, não duplica. Se foi resolvido, não reabre
 * (evita flood de alertas repetidos — operador resolve manualmente).
 */
async function generateAndPersistAlerts(env: Env): Promise<void> {
  const now        = nowSec();
  const oneDayAgo  = now - 86400;
  const oneHourAgo = now - 3600;

  const settingsRow = await env.DB.prepare(
    "SELECT value FROM admin_settings WHERE key = 'admin'"
  ).first<{ value: string }>();
  const settings = settingsRow?.value ? JSON.parse(settingsRow.value) : {};

  // Budget: se não configurado, não gera alerta de custo (sem fabricar).
  const AI_DAILY_BUDGET: number | null = settings.aiDailyBudgetUsd ?? null;
  const ERROR_THRESHOLD  = settings.errorSpikeThreshold    ?? 10;
  const MIN_SCORE        = settings.criticalScoreThreshold ?? 50;

  const [aiCost, recentErrors, scoreRow] = await Promise.all([
    env.DB.prepare(
      "SELECT SUM(cost_usd) AS total FROM ai_usage WHERE created_at >= ?"
    ).bind(oneDayAgo).first<{ total: number | null }>(),

    env.DB.prepare(
      "SELECT COUNT(*) AS count FROM system_errors WHERE last_seen >= ?"
    ).bind(oneHourAgo * 1000).first<{ count: number }>(),

    env.DB.prepare(
      `SELECT AVG(CAST(score AS REAL)) AS avg FROM diagnostic_sessions
       WHERE created_at >= ? AND score IS NOT NULL`
    ).bind(oneDayAgo).first<{ avg: number | null }>(),
  ]);

  const candidates: Array<{ id: string; type: string; severity: string; title: string; message: string }> = [];

  // Alerta de custo: só dispara se budget estiver configurado e custo > 0.
  if (AI_DAILY_BUDGET !== null && (aiCost?.total ?? 0) > AI_DAILY_BUDGET) {
    candidates.push({
      id:       'ai_budget_exceeded',
      type:     'AI_BUDGET',
      severity: 'critical',
      title:    'Orçamento diário de IA excedido',
      message:  `Custo nas últimas 24h: $${(aiCost?.total ?? 0).toFixed(4)} USD (limite: $${AI_DAILY_BUDGET})`,
    });
  }

  if ((recentErrors?.count ?? 0) > ERROR_THRESHOLD) {
    candidates.push({
      id:       'error_spike',
      type:     'ERROR_SPIKE',
      severity: 'warning',
      title:    'Pico de erros detectado',
      message:  `${recentErrors?.count} erros na última hora (limite: ${ERROR_THRESHOLD})`,
    });
  }

  if (scoreRow?.avg != null && scoreRow.avg < MIN_SCORE) {
    candidates.push({
      id:       'low_avg_score',
      type:     'LOW_SCORE',
      severity: 'warning',
      title:    'Qualidade de rede baixa',
      message:  `Score médio nas últimas 24h: ${Math.round(scoreRow.avg)} (mínimo: ${MIN_SCORE})`,
    });
  }

  // Persiste idempotente: INSERT OR IGNORE mantém o alerta original se já existir
  // (ativo ou resolvido). Não reabre alerta resolvido automaticamente.
  for (const c of candidates) {
    await env.DB.prepare(
      `INSERT OR IGNORE INTO alerts (id, type, severity, title, message, created_at, resolved)
       VALUES (?, ?, ?, ?, ?, ?, 0)`
    ).bind(c.id, c.type, c.severity, c.title, c.message, now).run();
  }
}

// GET /admin/alerts — gera alertas + retorna ativos + histórico recente (24h).
// Compat: também serve GET /admin/metrics/alerts para o adminMetricsService legado.
async function handleAlerts(_request: Request, env: Env): Promise<Response> {
  await generateAndPersistAlerts(env);

  const since = nowSec() - 86400;
  const rows = await env.DB.prepare(
    `SELECT id, type, severity, title, message, created_at, resolved
     FROM alerts
     WHERE resolved = 0 OR created_at >= ?
     ORDER BY created_at DESC
     LIMIT 50`
  ).bind(since).all<AlertRow>();

  const items = (rows.results ?? []).map(alertRowToResponse);
  return json({ source: "d1", items }, 200, env);
}

// POST /admin/alerts/:id/resolve — marca alerta como resolvido.
async function handleResolveAlert(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  // Extrai o id do path: /admin/alerts/:id/resolve
  const match = url.pathname.match(/^\/admin\/alerts\/([^/]+)\/resolve$/);
  if (!match) return err('id inválido', 400, env);
  const id = match[1];

  const result = await env.DB.prepare(
    'UPDATE alerts SET resolved = 1 WHERE id = ?'
  ).bind(id).run();

  if ((result.meta?.changes ?? 0) === 0) {
    return err('alerta não encontrado', 404, env);
  }

  return json({ ok: true, id }, 200, env);
}

// /admin/metrics/alerts — mantido para compatibilidade; delega para handleAlerts.
async function handleRecentAlerts(request: Request, env: Env): Promise<Response> {
  return handleAlerts(request, env);
}

async function handleAiCostMetrics(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  const row = await env.DB.prepare(
    `SELECT
       COUNT(*)             AS totalRequests,
       SUM(cost_usd)        AS totalCostUsd,
       SUM(prompt_tokens)   AS promptTokens,
       SUM(completion_tokens) AS completionTokens,
       SUM(total_tokens)    AS totalTokens
     FROM ai_usage
     WHERE created_at >= ?${envClause}`
  ).bind(since, ...envBinds).first<{
    totalRequests: number;
    totalCostUsd: number | null;
    promptTokens: number | null;
    completionTokens: number | null;
    totalTokens: number | null;
  }>();

  const totalRequests    = row?.totalRequests     ?? 0;
  const totalCostUsd     = row?.totalCostUsd       ?? 0;
  const promptTokens     = row?.promptTokens       ?? 0;
  const completionTokens = row?.completionTokens   ?? 0;
  const totalTokens      = row?.totalTokens        ?? 0;

  // avgCostPerRequest: guard contra divisão por zero.
  const avgCostPerRequest = totalRequests > 0 ? totalCostUsd / totalRequests : 0;

  // SIG-125: reliabilityPercentage — % de chamadas com completion_tokens > 0 (resposta real gerada).
  const reliabilityRow = await env.DB.prepare(
    `SELECT COUNT(*) AS total,
            SUM(CASE WHEN completion_tokens > 0 THEN 1 ELSE 0 END) AS successful
     FROM ai_usage WHERE created_at >= ?${envClause}`
  ).bind(since, ...envBinds).first<{ total: number; successful: number }>();
  const reliabilityPercentage = (reliabilityRow?.total ?? 0) > 0
    ? Math.round(((reliabilityRow?.successful ?? 0) / reliabilityRow!.total) * 100)
    : null;

  return json({
    source: "d1",
    period,
    environment: envFilter ?? "all",
    totalCostUsd,
    totalRequests,
    avgCostPerRequest,
    totalTokens,
    promptTokens,
    completionTokens,
    reliabilityPercentage,
  }, 200, env);
}

// Mapeia model → nome legível do provedor. Cor não vem do worker.
function providerName(model: string): string {
  const m = (model ?? "").toLowerCase();
  if (m.includes("gemini"))                      return "Gemini";
  if (m.includes("qwen") || m.startsWith("@cf/")) return "Qwen / Workers AI";
  if (m.includes("gpt"))                         return "OpenAI GPT";
  if (m.includes("claude"))                      return "Anthropic Claude";
  return model; // fallback: nome técnico do modelo
}

async function handleAiProviders(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  const rows = await env.DB.prepare(
    `SELECT model, SUM(total_tokens) AS tokensProcessed
     FROM ai_usage
     WHERE created_at >= ?${envClause}
     GROUP BY model
     ORDER BY tokensProcessed DESC`
  ).bind(since, ...envBinds).all();

  const results = rows.results ?? [];
  const grandTotal = results.reduce((acc: number, r: any) => acc + (r.tokensProcessed ?? 0), 0);

  const items = results.map((r: any) => ({
    name:            providerName(r.model ?? ""),
    tokensProcessed: r.tokensProcessed ?? 0,
    percentage:      grandTotal > 0 ? Math.round(((r.tokensProcessed ?? 0) / grandTotal) * 100) : 0,
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", items }, 200, env);
}

async function handleAiUsageTimeline(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const days      = Math.min(Math.max(parseInt(url.searchParams.get("days") ?? "30"), 1), 90);
  const since     = nowSec() - days * 86400;
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  // Agrega total_tokens por dia × provedor (via providerName).
  // strftime converte unix epoch → YYYY-MM-DD (UTC). Ordenado por data asc para plotagem.
  const rows = await env.DB.prepare(
    `SELECT
       strftime('%Y-%m-%d', datetime(created_at, 'unixepoch')) AS date,
       model,
       SUM(total_tokens) AS tokens
     FROM ai_usage
     WHERE created_at >= ?${envClause}
     GROUP BY date, model
     ORDER BY date ASC`
  ).bind(since, ...envBinds).all();

  // Reagrega por data e provedor (múltiplos modelos podem pertencer ao mesmo provedor).
  const byDate = new Map<string, Record<string, number>>();
  for (const r of (rows.results ?? []) as any[]) {
    const date     = r.date as string;
    const provider = providerName(r.model ?? "");
    const tokens   = (r.tokens as number) ?? 0;
    if (!byDate.has(date)) byDate.set(date, {});
    const entry = byDate.get(date)!;
    entry[provider] = (entry[provider] ?? 0) + tokens;
  }

  const series = Array.from(byDate.entries()).map(([date, byProvider]) => ({ date, byProvider }));

  return json({ source: "d1", days, environment: envFilter ?? "all", series }, 200, env);
}

// SIG-139: métricas de diagnóstico agrupadas por operadora.
async function handleOperators(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "30d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  const rows = await env.DB.prepare(
    `SELECT
       operator,
       COUNT(*)                                                  AS total_diagnostics,
       AVG(score)                                               AS avg_score,
       AVG(download_mbps)                                       AS avg_download,
       AVG(upload_mbps)                                         AS avg_upload,
       AVG(latency_ms)                                          AS avg_latency,
       SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END)   AS completed,
       SUM(CASE WHEN resolved = 1         THEN 1 ELSE 0 END)   AS resolved
     FROM diagnostic_sessions
     WHERE created_at >= ?
       AND operator IS NOT NULL AND operator != ''${envClause}
     GROUP BY operator
     ORDER BY total_diagnostics DESC`
  ).bind(since, ...envBinds).all();

  const operators = (rows.results ?? []).map((r: any) => ({
    operator:          r.operator,
    total_diagnostics: r.total_diagnostics ?? 0,
    avg_score:         r.avg_score         != null ? Math.round(r.avg_score) : null,
    avg_download:      r.avg_download      ?? null,
    avg_upload:        r.avg_upload        ?? null,
    avg_latency:       r.avg_latency       != null ? Math.round(r.avg_latency) : null,
    completed:         r.completed         ?? 0,
    resolved:          r.resolved          ?? 0,
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", operators }, 200, env);
}

async function handleFirebaseAnalytics(request: Request, env: Env): Promise<Response> {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return json({ source: "no_credentials", activeUsersToday: 0, sessionsToday: 0 }, 200, env);
  }
  if (!env.FIREBASE_GA4_PROPERTY_ID) {
    return json({
      source: "no_ga4_property_id",
      message: "Configure FIREBASE_GA4_PROPERTY_ID no wrangler.toml.",
    }, 200, env);
  }
  try {
    const token = await getFirebaseAccessToken(env);
    const resp  = await fetch(
      `https://analyticsdata.googleapis.com/v1beta/properties/${env.FIREBASE_GA4_PROPERTY_ID}:runReport`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          dateRanges: [{ startDate: "7daysAgo", endDate: "today" }],
          metrics: [{ name: "activeUsers" }, { name: "sessions" }, { name: "crashAffectedUsers" }],
          dimensions: [{ name: "date" }],
        }),
      }
    );
    const data = await resp.json();
    return json({ source: "firebase_analytics", data }, 200, env);
  } catch (e) {
    await logError(env, 'firebase', String(e), e instanceof Error ? (e.stack ?? '') : '');
    return json({ source: "error", message: String(e) }, 500, env);
  }
}

async function handleFirebaseStatus(_req: Request, env: Env): Promise<Response> {
  return json({
    source: "worker",
    projectId: env.FIREBASE_PROJECT_ID,
    status: "connected",
    hasCredentials: !!(env.FIREBASE_CLIENT_EMAIL && env.FIREBASE_PRIVATE_KEY),
    ga4PropertyConfigured: !!env.FIREBASE_GA4_PROPERTY_ID,
  }, 200, env);
}

async function handleFirebaseCrashlytics(_req: Request, env: Env): Promise<Response> {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return json({ source: "no_credentials", unresolvedCrashes: 0, crashFreeUsersPercentage: 100 }, 200, env);
  }

  const { rows, error } = await queryBigQuery<{
    total_crashes: string;
    affected_users: string;
    total_users: string;
  }>(env, `
    SELECT
      COUNT(*)                                   AS total_crashes,
      COUNT(DISTINCT installation_uuid)           AS affected_users,
      (SELECT COUNT(DISTINCT installation_uuid)
       FROM \`${env.FIREBASE_PROJECT_ID}.analytics_${env.FIREBASE_GA4_PROPERTY_ID}.events_*\`
       WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
         AND event_name = 'session_start'
      )                                          AS total_users
    FROM \`${env.FIREBASE_PROJECT_ID}.firebase_crashlytics.android_crashes_*\`
    WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
  `);

  if (error === "table_not_found" || rows.length === 0) {
    return json({
      source: "no_data_yet",
      message: "BigQuery export ativo; dados disponíveis em até 24h do primeiro crash.",
      unresolvedCrashes: 0,
      crashFreeUsersPercentage: 100,
    }, 200, env);
  }
  if (error) {
    await logError(env, 'bigquery-crashlytics', error, '');
    return json({ source: "error", message: error, unresolvedCrashes: 0, crashFreeUsersPercentage: 100 }, 200, env);
  }

  const row = rows[0];
  const totalCrashes  = parseInt(row.total_crashes  ?? "0", 10);
  const affectedUsers = parseInt(row.affected_users ?? "0", 10);
  const totalUsers    = parseInt(row.total_users    ?? "1", 10);
  const crashFreeUsersPercentage = totalUsers > 0
    ? Math.round(((totalUsers - affectedUsers) / totalUsers) * 10000) / 100
    : 100;

  return json({
    source: "bigquery",
    unresolvedCrashes: totalCrashes,
    affectedUsers,
    crashFreeUsersPercentage,
  }, 200, env);
}

async function handleFirebaseVersions(_req: Request, env: Env): Promise<Response> {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return json({ source: "no_credentials", versions: [] }, 200, env);
  }

  const { rows, error } = await queryBigQuery<{
    app_version: string;
    total_crashes: string;
    affected_users: string;
  }>(env, `
    SELECT
      app_version,
      COUNT(*)                          AS total_crashes,
      COUNT(DISTINCT installation_uuid) AS affected_users
    FROM \`${env.FIREBASE_PROJECT_ID}.firebase_crashlytics.android_crashes_*\`
    WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))
    GROUP BY app_version
    ORDER BY total_crashes DESC
    LIMIT 10
  `);

  if (error === "table_not_found" || !rows.length) {
    return json({ source: "no_data_yet", versions: [] }, 200, env);
  }
  if (error) {
    await logError(env, 'bigquery-versions', error, '');
    return json({ source: "error", versions: [] }, 200, env);
  }

  const versions = rows.map((r) => ({
    version:       r.app_version ?? "unknown",
    totalCrashes:  parseInt(r.total_crashes ?? "0", 10),
    affectedUsers: parseInt(r.affected_users ?? "0", 10),
  }));

  return json({ source: "bigquery", versions }, 200, env);
}

async function handleFirebaseCrashIssues(_req: Request, env: Env): Promise<Response> {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return json({ source: "no_credentials", issues: [] }, 200, env);
  }

  const { rows, error } = await queryBigQuery<{
    issue_id: string;
    issue_title: string;
    total_crashes: string;
    affected_users: string;
    last_seen: string;
  }>(env, `
    SELECT
      issue_id,
      issue_title,
      COUNT(*)                              AS total_crashes,
      COUNT(DISTINCT installation_uuid)     AS affected_users,
      MAX(event_timestamp)                  AS last_seen
    FROM \`${env.FIREBASE_PROJECT_ID}.firebase_crashlytics.android_crashes_*\`
    WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))
    GROUP BY issue_id, issue_title
    ORDER BY total_crashes DESC
    LIMIT 20
  `);

  if (error === "table_not_found" || !rows.length) {
    return json({ source: "no_data_yet", issues: [] }, 200, env);
  }
  if (error) {
    await logError(env, 'bigquery-crash-issues', error, '');
    return json({ source: "error", issues: [] }, 200, env);
  }

  const issues = rows.map((r) => ({
    id:            r.issue_id ?? "",
    title:         r.issue_title ?? "Unknown crash",
    totalCrashes:  parseInt(r.total_crashes ?? "0", 10),
    affectedUsers: parseInt(r.affected_users ?? "0", 10),
    lastSeen:      r.last_seen ? parseInt(r.last_seen, 10) / 1000 : 0,
  }));

  return json({ source: "bigquery", issues }, 200, env);
}

async function handleFirebaseSync(_req: Request, env: Env): Promise<Response> {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return json({ source: "no_credentials", ok: false }, 200, env);
  }

  const { rows, error } = await queryBigQuery<{ sessions: string }>(env, `
    SELECT COUNT(*) AS sessions
    FROM \`${env.FIREBASE_PROJECT_ID}.analytics_${env.FIREBASE_GA4_PROPERTY_ID}.events_*\`
    WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY))
      AND event_name = 'session_start'
  `);

  if (error === "table_not_found" || !rows.length) {
    return json({ ok: false, source: "no_data_yet", sessionsYesterday: 0, syncedAt: nowSec() }, 200, env);
  }
  if (error) {
    return json({ ok: false, source: "error", message: error }, 200, env);
  }

  const sessions = parseInt(rows[0]?.sessions ?? "0", 10);
  return json({ ok: true, source: "bigquery", sessionsYesterday: sessions, syncedAt: nowSec() }, 200, env);
}

async function handleSettings(request: Request, env: Env): Promise<Response> {
  if (request.method === "GET") {
    const row = await env.DB.prepare(
      "SELECT value FROM admin_settings WHERE key = 'admin'"
    ).first<{ value: string }>();

    const settings = row?.value ? (JSON.parse(row.value) as unknown) : {};
    return json({ source: "d1", settings }, 200, env);
  }

  if (request.method === "POST") {
    let body: unknown;
    try {
      body = await request.json();
    } catch {
      return err("body JSON inválido", 400, env);
    }

    if (!body || typeof body !== "object" || Array.isArray(body)) {
      return err("body deve ser um objeto JSON", 400, env);
    }

    await env.DB.prepare(
      "INSERT OR REPLACE INTO admin_settings (key, value, updated_at) VALUES ('admin', ?, ?)"
    ).bind(JSON.stringify(body), nowSec()).run();

    return json({ ok: true, settings: body }, 200, env);
  }

  return err("método não suportado", 405, env);
}

// --- SIG-129 / SIG-135 Fase A: pipeline de erros via D1 ---

// djb2: hash determinístico simples, sem dependência externa, adequado para ids de deduplicação.
function djb2(s: string): string {
  let h = 5381;
  for (let i = 0; i < s.length; i++) {
    h = ((h << 5) + h) ^ s.charCodeAt(i);
    h = h >>> 0; // mantém uint32
  }
  return h.toString(16).padStart(8, '0');
}

// Fire-and-forget: nunca propaga exceção. Deduplica por (source + message) via hash djb2.
async function logError(env: Env, source: string, message: string, stack = ''): Promise<void> {
  try {
    const id = djb2(`${source}:${message}`);
    const now = Date.now();
    await env.DB.prepare(`
      INSERT INTO system_errors (id, source, message, stack_trace, count, first_seen, last_seen)
      VALUES (?, ?, ?, ?, 1, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        count       = count + 1,
        last_seen   = excluded.last_seen,
        stack_trace = excluded.stack_trace
    `).bind(id, source, message, stack, now, now).run();
  } catch { /* fire-and-forget — nunca propaga */ }
}

// Envolve um handler de métricas para capturar exceções sem alterar o comportamento.
// O handler original mantém seu throw/fallback; o log é adicional.
function withErrorLogging(source: string, handler: Handler): Handler {
  return async (req: Request, env: Env): Promise<Response> => {
    try {
      return await handler(req, env);
    } catch (e) {
      await logError(env, source, String(e), e instanceof Error ? (e.stack ?? '') : '');
      throw e;
    }
  };
}

async function handleErrors(request: Request, env: Env): Promise<Response> {
  const url    = new URL(request.url);
  const period = url.searchParams.get("period") ?? "30d";
  // Fase A: o parâmetro ?environment= enviado pelo frontend é IGNORADO aqui.
  // A tabela system_errors não possui coluna environment — os erros são do worker,
  // não do app. Filtro por environment entra na Fase B junto com SIG-143.
  // last_seen é em milissegundos (Date.now()), mas periodToSeconds retorna segundos.
  // Multiplicamos por 1000 para ficar na mesma escala.
  const sinceMs = (Date.now()) - periodToSeconds(period) * 1000;

  const rows = await env.DB.prepare(
    `SELECT id, source, message, stack_trace, count, first_seen, last_seen
     FROM system_errors
     WHERE last_seen >= ?
     ORDER BY count DESC, last_seen DESC
     LIMIT 100`
  ).bind(sinceMs).all();

  const errors = (rows.results ?? []).map((r: any) => ({
    id:               r.id,
    source:           r.source,
    message:          r.message,
    stackTrace:       r.stack_trace ?? '',
    count:            r.count       ?? 1,
    first_seen:       r.first_seen,
    last_seen:        r.last_seen,
    timestamp:        new Date(r.last_seen).toISOString(),
    // O backend não rastreia usuários únicos por erro — sem PII no D1.
    // Derivar por device_id (já presente em diagnostic_sessions) é Fase B.
    affectedUserCount: 0,
  }));

  return json({ source: "d1", period, errors }, 200, env);
}

// --- handlers /ingest ---

async function handleIngestDiagnostic(request: Request, env: Env): Promise<Response> {
  let p: any;
  try { p = await request.json(); } catch { return err("invalid JSON", 400, env); }
  if (!p.id) return err("id obrigatorio", 400, env);

  // SIG-138: campos de dispositivo e laudo IA opcionais (default '' no schema).
  const deviceModel      = p.device_model      ?? '';
  const osVersion        = p.os_version        ?? '';
  const appVersion       = p.app_version       ?? '';
  const aiSummaryReport  = p.ai_summary_report ?? '';

  // SIG-143: campos de contexto de ambiente e dispositivo.
  const environment = p.environment  ?? 'production';
  const distChannel = p.dist_channel ?? '';
  const buildType   = p.build_type   ?? 'release';
  const versionCode = p.version_code ?? 0;
  const deviceId    = p.device_id    ?? '';

  try {
    await env.DB.prepare(
      `INSERT OR REPLACE INTO diagnostic_sessions
         (id, created_at, network_type, status, score,
          download_mbps, upload_mbps, latency_ms, jitter_ms, packet_loss,
          issues, resolved, operator,
          device_model, os_version, app_version, ai_summary_report,
          environment, dist_channel, build_type, version_code, device_id)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      p.id, p.created_at ?? nowSec(),
      p.network_type ?? "unknown", p.status ?? "unknown", p.score ?? null,
      p.download_mbps ?? null, p.upload_mbps ?? null,
      p.latency_ms ?? null, p.jitter_ms ?? null, p.packet_loss ?? null,
      JSON.stringify(p.issues ?? []),
      p.operator ?? null,
      deviceModel, osVersion, appVersion, aiSummaryReport,
      environment, distChannel, buildType, versionCode, deviceId,
    ).run();
  } catch (e) {
    await logError(env, 'ingest', String(e), e instanceof Error ? e.stack ?? '' : '');
    throw e;
  }

  return json({ ok: true, id: p.id }, 201, env);
}

// Preço por token (USD), por modelo. Ver docs_ai/decisions/ADR-005.
// Modelos do free tier (Gemini, Qwen/Workers AI) custam 0. Ao adotar um modelo
// pago, adicione sua tarifa aqui (ex.: "gemini-2.5-pro": 0.0000003).
const AI_MODEL_RATE_USD: Record<string, number> = {};

function costForModel(model: string, totalTokens: number): number {
  const key = (model ?? "").toLowerCase();
  // Free tier: Gemini (Google AI Studio) e Qwen (Cloudflare Workers AI) não têm custo por token.
  if (key.includes("gemini") || key.includes("qwen") || key.startsWith("@cf/")) return 0;
  for (const [m, rate] of Object.entries(AI_MODEL_RATE_USD)) {
    if (key.includes(m)) return totalTokens * rate;
  }
  // Modelo sem tarifa cadastrada → 0 (arquitetura atual é 100% free tier).
  return 0;
}

async function handleIngestAiUsage(request: Request, env: Env): Promise<Response> {
  let p: any;
  try { p = await request.json(); } catch { return err("invalid JSON", 400, env); }
  if (!p.id || !p.model) return err("id e model obrigatorios", 400, env);

  const prompt     = p.prompt_tokens     ?? 0;
  const completion = p.completion_tokens ?? 0;
  const total      = p.total_tokens      ?? (prompt + completion);
  const cost       = p.cost_usd          ?? costForModel(p.model, total);

  // SIG-143: campos de contexto de ambiente.
  const environment = p.environment  ?? 'production';
  const versionCode = p.version_code ?? 0;

  try {
    await env.DB.prepare(
      `INSERT OR REPLACE INTO ai_usage
         (id, session_id, created_at, model,
          prompt_tokens, completion_tokens, total_tokens, cost_usd,
          environment, version_code)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      p.id, p.session_id ?? null, p.created_at ?? nowSec(), p.model,
      prompt, completion, total, cost,
      environment, versionCode,
    ).run();
  } catch (e) {
    await logError(env, 'ai-usage', String(e), e instanceof Error ? e.stack ?? '' : '');
    throw e;
  }

  return json({ ok: true, id: p.id }, 201, env);
}

// --- SIG-134: analytics de produto ---

async function handleIngestAnalytics(request: Request, env: Env): Promise<Response> {
  let body: any;
  try { body = await request.json(); } catch { return err('body JSON inválido', 400, env); }

  const events: any[] = Array.isArray(body.events) ? body.events : [];
  if (events.length === 0) return json({ ok: true, inserted: 0 }, 200, env);
  if (events.length > 500) return err('máximo 500 eventos por batch', 400, env);

  const VALID_EVENTS = new Set(['feature_used', 'screen_view', 'session_start', 'feature_crash', 'battery_snapshot']);
  const now = nowSec();

  const stmts = events
    .filter((e) => e && VALID_EVENTS.has(e.name))
    .map((e) =>
      env.DB.prepare(
        `INSERT OR IGNORE INTO analytics_events
           (id, event_name, session_id, created_at, app_version, feature_id, screen_name, error_type, battery_level, battery_charging, environment)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      ).bind(
        crypto.randomUUID(),
        e.name,
        e.session_id ?? '',
        typeof e.timestamp === 'number' ? e.timestamp : now,
        e.app_version ?? '',
        e.feature_id  ?? '',
        e.screen_name ?? '',
        e.error_type  ?? '',
        typeof e.battery_level    === 'number' ? e.battery_level    : null,
        typeof e.battery_charging === 'boolean' ? (e.battery_charging ? 1 : 0) : null,
        e.environment ?? 'production',
      )
    );

  if (stmts.length > 0) await env.DB.batch(stmts);

  return json({ ok: true, inserted: stmts.length }, 201, env);
}

async function handleProductAnalytics(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get('period') ?? '7d';
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? ' AND environment = ?' : '';
  const envBinds  = envFilter ? [envFilter]            : [];

  const [featureRows, screenRows, crashRows, totalRows] = await Promise.all([
    // Uso por feature: contagem total e sessões únicas
    env.DB.prepare(
      `SELECT feature_id, COUNT(*) AS usage_count, COUNT(DISTINCT session_id) AS unique_sessions
       FROM analytics_events
       WHERE event_name = 'feature_used' AND created_at >= ?${envClause}
         AND feature_id != ''
       GROUP BY feature_id ORDER BY usage_count DESC`
    ).bind(since, ...envBinds).all(),

    // Navegação por tela
    env.DB.prepare(
      `SELECT screen_name, COUNT(*) AS views, COUNT(DISTINCT session_id) AS unique_sessions
       FROM analytics_events
       WHERE event_name = 'screen_view' AND created_at >= ?${envClause}
         AND screen_name != ''
       GROUP BY screen_name ORDER BY views DESC`
    ).bind(since, ...envBinds).all(),

    // Crashes por feature
    env.DB.prepare(
      `SELECT feature_id,
              COUNT(*) AS crashes,
              GROUP_CONCAT(DISTINCT app_version) AS affected_versions
       FROM analytics_events
       WHERE event_name = 'feature_crash' AND created_at >= ?${envClause}
         AND feature_id != ''
       GROUP BY feature_id ORDER BY crashes DESC`
    ).bind(since, ...envBinds).all(),

    // Total de feature_used para calcular crash rate
    env.DB.prepare(
      `SELECT feature_id, COUNT(*) AS total
       FROM analytics_events
       WHERE event_name = 'feature_used' AND created_at >= ?${envClause}
         AND feature_id != ''
       GROUP BY feature_id`
    ).bind(since, ...envBinds).all(),
  ]);

  const usageByFeature = new Map<string, number>(
    (totalRows.results ?? []).map((r: any) => [r.feature_id, r.total])
  );

  const feature_usage = (featureRows.results ?? []).map((r: any) => ({
    feature:        r.feature_id,
    label:          r.feature_id,
    usageCount:     r.usage_count   ?? 0,
    uniqueUsers:    r.unique_sessions ?? 0,
    completionRate: 0,
    failureRate:    0,
    avgDurationMs:  0,
    trendPercent:   0,
  }));

  const screen_navigation = (screenRows.results ?? []).map((r: any) => ({
    screen:              r.screen_name,
    label:               r.screen_name,
    views:               r.views           ?? 0,
    uniqueUsers:         r.unique_sessions  ?? 0,
    avgTimeOnScreenSec:  0,
    exitRate:            0,
    nextMostCommonScreen: null,
  }));

  const feature_crashes = (crashRows.results ?? []).map((r: any) => {
    const total   = usageByFeature.get(r.feature_id) ?? 0;
    const crashes = r.crashes ?? 0;
    const rate    = total > 0 ? crashes / total : 0;
    const versions = r.affected_versions
      ? String(r.affected_versions).split(',').filter(Boolean)
      : [];
    return {
      feature:         r.feature_id,
      label:           r.feature_id,
      crashes,
      nonFatalErrors:  0,
      anrs:            0,
      crashRate:       Math.round(rate * 1000) / 10,
      affectedVersions: versions,
      severity:        crashes === 0 ? 'ok' : rate > 0.05 ? 'critical' : 'attention',
    };
  });

  return json({
    source: 'd1',
    period,
    environment: envFilter ?? 'all',
    no_data_yet: feature_usage.length === 0 && screen_navigation.length === 0,
    feature_usage,
    screen_navigation,
    feature_crashes,
  }, 200, env);
}

async function handleBatteryAnalytics(request: Request, env: Env): Promise<Response> {
  const url    = new URL(request.url);
  const period = url.searchParams.get('period') ?? '7d';
  const since  = nowSec() - periodToSeconds(period);

  const rows = await env.DB.prepare(
    `SELECT
       AVG(CAST(battery_level AS REAL)) AS avg_level,
       SUM(battery_charging) AS charging_count,
       COUNT(*) AS total
     FROM analytics_events
     WHERE event_name = 'battery_snapshot' AND created_at >= ?
       AND battery_level IS NOT NULL`
  ).bind(since).first<{ avg_level: number | null; charging_count: number; total: number }>();

  const total = rows?.total ?? 0;

  return json({
    source:     'd1',
    period,
    no_data_yet: total === 0,
    summary: total === 0 ? null : {
      avg_battery_level:       rows?.avg_level != null ? Math.round(rows.avg_level) : null,
      charging_sessions_pct:   rows && total > 0 ? Math.round((rows.charging_count / total) * 100) : 0,
      total_snapshots:         total,
    },
    items: [],
  }, 200, env);
}

// --- SIG-13: feature flags ---

interface FeatureFlag {
  key: string;
  enabled: boolean;
  scope: 'public' | 'internal';
  description: string;
}

function getDefaultFlags(): FeatureFlag[] {
  return [
    { key: 'ai_diagnosis_enabled',  enabled: true,  scope: 'public',   description: 'Habilita diagnóstico por IA' },
    { key: 'speedtest_enabled',     enabled: true,  scope: 'public',   description: 'Habilita speedtest' },
    { key: 'fibra_module_enabled',  enabled: true,  scope: 'public',   description: 'Habilita módulo fibra' },
    { key: 'new_ui_diagnostics',    enabled: false, scope: 'internal', description: 'Nova UI de diagnósticos (internal)' },
  ];
}

async function handleGetFeatureFlags(_request: Request, env: Env): Promise<Response> {
  const row = await env.DB.prepare(
    "SELECT value FROM admin_settings WHERE key = 'feature_flags'"
  ).first<{ value: string }>();
  const flags: FeatureFlag[] = row?.value ? JSON.parse(row.value) : getDefaultFlags();
  return json({ source: "d1", flags }, 200, env);
}

async function handleSetFeatureFlags(request: Request, env: Env): Promise<Response> {
  let body: any;
  try { body = await request.json(); } catch { return err("body JSON inválido", 400, env); }
  const flags = body.flags ?? body;
  await env.DB.prepare(
    "INSERT OR REPLACE INTO admin_settings (key, value, updated_at) VALUES ('feature_flags', ?, ?)"
  ).bind(JSON.stringify(flags), nowSec()).run();
  return json({ ok: true }, 200, env);
}

// GET /feature-flags — público, sem auth. Retorna apenas flags com scope='public'.
// Crítico para o app Android verificar flags sem credenciais de admin.
async function handlePublicFeatureFlags(_request: Request, env: Env): Promise<Response> {
  const row = await env.DB.prepare(
    "SELECT value FROM admin_settings WHERE key = 'feature_flags'"
  ).first<{ value: string }>();
  const allFlags: FeatureFlag[] = row?.value ? JSON.parse(row.value) : getDefaultFlags();
  const publicFlags = allFlags.filter((f) => f.scope === 'public');
  return json({ flags: publicFlags }, 200, env);
}

// --- SIG-13: feature flags via tabela dedicada (substitui JSON blob em admin_settings) ---

// GET /admin/feature-flags — lista todas as flags da tabela feature_flags.
async function handleFeatureFlags(_request: Request, env: Env): Promise<Response> {
  const rows = await env.DB.prepare(
    'SELECT key, enabled, description, updated_at, updated_by FROM feature_flags ORDER BY key'
  ).all();

  const flags = (rows.results ?? []).map((r: any) => ({
    key:         r.key,
    enabled:     r.enabled === 1,
    description: r.description ?? '',
    updatedAt:   r.updated_at  ?? 0,
    updatedBy:   r.updated_by  ?? '',
  }));

  return json({ flags }, 200, env);
}

// PUT /admin/feature-flags/:key — atualiza enabled de uma flag e grava audit log.
async function handleUpdateFeatureFlag(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  const url = new URL(request.url);
  const match = url.pathname.match(/^\/admin\/feature-flags\/([^/]+)$/);
  if (!match) return err('key inválida', 400, env);
  const key = match[1];

  let body: { enabled?: boolean };
  try { body = await request.json(); } catch { return err('body JSON inválido', 400, env); }
  if (typeof body.enabled !== 'boolean') return err('enabled (boolean) obrigatório', 400, env);

  // Busca estado atual para registrar old_enabled no audit.
  const current = await env.DB.prepare(
    'SELECT enabled FROM feature_flags WHERE key = ?'
  ).bind(key).first<{ enabled: number }>();
  if (!current) return err('flag não encontrada', 404, env);

  const now       = Math.floor(Date.now() / 1000);
  const newEnabled = body.enabled ? 1 : 0;

  // Obtém email do usuário da sessão para o audit log.
  const userRow = await env.DB.prepare(
    'SELECT email FROM admin_users WHERE id = ?'
  ).bind(session.userId).first<{ email: string }>();
  const changedBy = userRow?.email ?? 'admin';

  await env.DB.batch([
    env.DB.prepare(
      'UPDATE feature_flags SET enabled = ?, updated_at = ?, updated_by = ? WHERE key = ?'
    ).bind(newEnabled, now, changedBy, key),
    env.DB.prepare(
      'INSERT INTO feature_flag_audit (id, flag_key, old_enabled, new_enabled, changed_at, changed_by) VALUES (?, ?, ?, ?, ?, ?)'
    ).bind(crypto.randomUUID(), key, current.enabled, newEnabled, now, changedBy),
  ]);

  return json({ ok: true, key, enabled: body.enabled }, 200, env);
}

// GET /flags — público, sem auth. Retorna key + enabled para consumo do Android.
async function handlePublicFlags(_request: Request, env: Env): Promise<Response> {
  const rows = await env.DB.prepare(
    'SELECT key, enabled FROM feature_flags ORDER BY key'
  ).all();

  const flags = (rows.results ?? []).map((r: any) => ({
    key:     r.key,
    enabled: r.enabled === 1,
  }));

  return json({ flags }, 200, env);
}

// --- router ---

type Handler = (req: Request, env: Env) => Promise<Response>;

const ROUTES: Array<{ method: string; pattern: RegExp; handler: Handler }> = [
  { method: "GET",  pattern: /^\/admin\/metrics\/overview$/,                    handler: withErrorLogging('metrics', handleOverview) },
  { method: "GET",  pattern: /^\/admin\/metrics\/diagnostics$/,                 handler: withErrorLogging('metrics', handleDiagnostics) },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-usage$/,                    handler: withErrorLogging('metrics', handleAiCost) },
  { method: "GET",  pattern: /^\/admin\/metrics\/timeline$/,                    handler: withErrorLogging('metrics', handleTimeline) },
  { method: "GET",  pattern: /^\/admin\/metrics\/network$/,                     handler: withErrorLogging('metrics', handleNetworkInsights) },
  { method: "GET",  pattern: /^\/admin\/metrics\/top-issues$/,                  handler: withErrorLogging('metrics', handleTopIssues) },
  { method: "GET",  pattern: /^\/admin\/metrics\/alerts$/,                      handler: withErrorLogging('metrics', handleRecentAlerts) },
  { method: "GET",  pattern: /^\/admin\/alerts$/,                               handler: withErrorLogging('alerts', handleAlerts) },
  { method: "POST", pattern: /^\/admin\/alerts\/[^/]+\/resolve$/,               handler: withErrorLogging('alerts', handleResolveAlert) },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-costs$/,                    handler: withErrorLogging('metrics', handleAiCostMetrics) },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-providers$/,                handler: withErrorLogging('metrics', handleAiProviders) },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-usage\/timeline$/,          handler: withErrorLogging('metrics', handleAiUsageTimeline) },
  { method: "GET",  pattern: /^\/admin\/metrics\/operators$/,                   handler: withErrorLogging('metrics', handleOperators) },
  { method: "GET",  pattern: /^\/admin\/metrics\/errors$/,                      handler: handleErrors },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/status$/,       handler: handleFirebaseStatus },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/analytics$/,    handler: handleFirebaseAnalytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crashlytics$/,  handler: handleFirebaseCrashlytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/versions$/,     handler: handleFirebaseVersions },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crash-issues$/, handler: handleFirebaseCrashIssues },
  { method: "POST", pattern: /^\/admin\/integrations\/firebase\/sync$/,         handler: handleFirebaseSync },
  { method: "GET",  pattern: /^\/admin\/analytics\/product$/,                   handler: withErrorLogging('analytics', handleProductAnalytics) },
  { method: "GET",  pattern: /^\/admin\/analytics\/battery$/,                   handler: withErrorLogging('analytics', handleBatteryAnalytics) },
  { method: "GET",  pattern: /^\/admin\/settings$/,                             handler: handleSettings },
  { method: "POST", pattern: /^\/admin\/settings$/,                             handler: handleSettings },
  // SIG-13: GET usa nova tabela; POST mantido para compat com adminSettingsService legado.
  { method: "GET",  pattern: /^\/admin\/feature-flags$/,                        handler: withErrorLogging('feature-flags', handleFeatureFlags) },
  { method: "POST", pattern: /^\/admin\/feature-flags$/,                        handler: handleSetFeatureFlags },
  { method: "PUT",  pattern: /^\/admin\/feature-flags\/[^/]+$/,                 handler: withErrorLogging('feature-flags', async (req, env) => {
      const session = await authenticateSession(req, env);
      if (!session) return err('Unauthorized', 401, env);
      return handleUpdateFeatureFlag(req, env, session);
    }) },
];

const INGEST_ROUTES: Array<{ method: string; pattern: RegExp; handler: Handler }> = [
  { method: "POST", pattern: /^\/ingest\/diagnostic$/, handler: handleIngestDiagnostic },
  { method: "POST", pattern: /^\/ingest\/ai-usage$/,   handler: handleIngestAiUsage },
  { method: "POST", pattern: /^\/ingest\/analytics$/,  handler: withErrorLogging('analytics', handleIngestAnalytics) },
];

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(env) });
    }
    const url = new URL(request.url);

    // Redireciona browser direto para o painel (evita exibir JSON cru em Edge/Chrome).
    if (url.pathname === "/" || url.pathname === "") {
      return Response.redirect("https://signallq-admin-panel.pages.dev", 302);
    }

    if (url.pathname === "/health") {
      if (!authenticate(request, env)) return err("Unauthorized", 401, env);
      return json({ status: "ok", worker: "signallq-admin-worker" }, 200, env);
    }

    // Endpoints públicos sem auth — o app Android consome sem credenciais.
    if (url.pathname === '/feature-flags' && request.method === 'GET') {
      return handlePublicFeatureFlags(request, env);
    }
    // SIG-13: /flags — endpoint público da nova tabela feature_flags.
    if (url.pathname === '/flags' && request.method === 'GET') {
      return withErrorLogging('flags', handlePublicFlags)(request, env);
    }

    // Rotas /ingest/* — autenticam com INGEST_KEY (scope limitado, vai no APK).
    for (const route of INGEST_ROUTES) {
      if (route.method === request.method && route.pattern.test(url.pathname)) {
        if (!authenticateIngest(request, env)) return err("Unauthorized", 401, env);
        return route.handler(request, env);
      }
    }

    // SIG-136: rotas de auth — login é a única /admin/* sem sessão prévia.
    if (url.pathname === '/admin/auth/login' && request.method === 'POST') {
      return handleAuthLogin(request, env)
    }
    if (url.pathname === '/admin/auth/logout' && request.method === 'POST') {
      return handleAuthLogout(request, env)
    }
    if (url.pathname === '/admin/auth/me' && request.method === 'GET') {
      return handleAuthMe(request, env)
    }
    if (url.pathname === '/admin/auth/users' && request.method === 'POST') {
      const session = await authenticateSession(request, env)
      if (!session) return err('Unauthorized', 401, env)
      return handleAuthCreateUser(request, env, session)
    }
    if (url.pathname === '/admin/auth/password' && request.method === 'POST') {
      const session = await authenticateSession(request, env)
      if (!session) return err('Unauthorized', 401, env)
      return handleAuthChangePassword(request, env, session)
    }

    // Rotas /admin/* — autenticam por sessão httpOnly (SIG-136).
    const session = await authenticateSession(request, env)
    if (!session) {
      return err("Unauthorized", 401, env);
    }
    for (const route of ROUTES) {
      if (route.method === request.method && route.pattern.test(url.pathname)) {
        return route.handler(request, env);
      }
    }
    return err("Not found", 404, env);
  },
};
