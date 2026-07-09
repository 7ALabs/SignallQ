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
  /** GH#761 — service account com acesso à Android Publisher API (reviews). */
  GOOGLE_PLAY_CLIENT_EMAIL: string;
  GOOGLE_PLAY_PRIVATE_KEY: string;
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

/**
 * Extrai filtro de platform da query string (GH#442).
 * ?platform=android|web → filtra por origem do dado.
 * ?platform=all ou ausente → sem filtro (retorna null, mostra as duas origens).
 */
function getPlatformFilter(url: URL): string | null {
  const platform = url.searchParams.get("platform");
  return platform === "all" || !platform ? null : platform;
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
  const platformFilter = getPlatformFilter(url);

  const envClause    = envFilter ? " AND environment = ?" : "";
  const envBinds     = envFilter ? [envFilter]            : [];
  const platformClause = platformFilter ? " AND platform = ?" : "";
  const platformBinds  = platformFilter ? [platformFilter]    : [];
  const filterClause = `${envClause}${platformClause}`;
  const filterBinds  = [...envBinds, ...platformBinds];

  const [sessions, aiRows, successRows, networkRows, issueRows] = await Promise.all([
    env.DB.prepare(
      `SELECT COUNT(*) AS total,
              SUM(CASE WHEN resolved=0 THEN 1 ELSE 0 END) AS active,
              AVG(CAST(score AS REAL)) AS avg_score
       FROM diagnostic_sessions WHERE created_at >= ?${filterClause}`
    ).bind(since, ...filterBinds).first<{ total: number; active: number; avg_score: number | null }>(),
    env.DB.prepare(
      `SELECT COUNT(*) AS calls, SUM(cost_usd) AS cost, SUM(total_tokens) AS tokens
       FROM ai_usage WHERE created_at >= ?${filterClause}`
    ).bind(todaySince, ...filterBinds).first<{ calls: number; cost: number; tokens: number }>(),
    // successRate: % de sessões com status bom/regular (não ruim/critico/inconclusivo)
    env.DB.prepare(
      `SELECT COUNT(*) AS total,
              SUM(CASE WHEN status IN ('bom','excelente','regular') THEN 1 ELSE 0 END) AS successful
       FROM diagnostic_sessions WHERE created_at >= ?${filterClause}`
    ).bind(since, ...filterBinds).first<{ total: number; successful: number }>(),
    // mostTestType: tipo de rede predominante
    env.DB.prepare(
      `SELECT network_type, COUNT(*) AS cnt
       FROM diagnostic_sessions WHERE created_at >= ?${filterClause}
         AND network_type IS NOT NULL AND network_type != '' AND network_type != 'unknown'
       GROUP BY network_type ORDER BY cnt DESC LIMIT 1`
    ).bind(since, ...filterBinds).first<{ network_type: string; cnt: number }>(),
    // topProblem: issue mais frequente no período
    env.DB.prepare(
      `SELECT issues FROM diagnostic_sessions WHERE created_at >= ?${filterClause} AND issues != '[]'`
    ).bind(since, ...filterBinds).all(),
  ]);

  // Calcula successRate
  const total = successRows?.total ?? 0;
  const successRate = total > 0
    ? Math.round(((successRows?.successful ?? 0) / total) * 100)
    : null;

  // Determina topProblem a partir dos arrays JSON de issues
  const countMap: Record<string, number> = {};
  for (const row of (issueRows.results ?? [])) {
    let issues: string[] = [];
    try { issues = JSON.parse((row as any).issues ?? "[]"); } catch { /* ignora */ }
    for (const label of issues) {
      if (isRealIssueLabel(label)) {
        countMap[label] = (countMap[label] ?? 0) + 1;
      }
    }
  }
  const topEntry = Object.entries(countMap).sort(([, a], [, b]) => b - a)[0];
  const topProblem = topEntry ? topEntry[0] : null;

  // mostTestType
  const mostTestType = networkRows?.network_type ?? null;
  const mostTestTypeCount = networkRows?.cnt ?? 0;
  const mostTestTypePercentage = total > 0 && mostTestTypeCount > 0
    ? Math.round((mostTestTypeCount / total) * 100)
    : null;

  return json({
    source: "d1", period,
    environment:          envFilter ?? "all",
    platform:             platformFilter ?? "all",
    totalDiagnostics:     sessions?.total ?? 0,
    activeSessions:       sessions?.active ?? 0,
    avgNetworkScore:      sessions?.avg_score ? Math.round(sessions.avg_score) : 0,
    aiCallsToday:         aiRows?.calls  ?? 0,
    aiCostToday:          aiRows?.cost   ?? 0,
    aiTokensToday:        aiRows?.tokens ?? 0,
    successRate,
    topProblem,
    mostTestType,
    mostTestTypePercentage,
  }, 200, env);
}

async function handleDiagnostics(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const limit     = Math.min(parseInt(url.searchParams.get("limit") ?? "50"), 200);
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);
  const platformFilter = getPlatformFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];
  const platformClause = platformFilter ? " AND platform = ?" : "";
  const platformBinds  = platformFilter ? [platformFilter]    : [];
  const filterClause = `${envClause}${platformClause}`;
  const filterBinds  = [...envBinds, ...platformBinds];

  const rows = await env.DB.prepare(
    `SELECT id, created_at, network_type, status, score,
            download_mbps, upload_mbps, latency_ms, jitter_ms, packet_loss,
            issues, resolved, operator,
            device_model, os_version, app_version, ai_summary_report,
            environment, dist_channel, build_type, version_code, device_id,
            rssi, banda_wifi, padrao_wifi, platform
     FROM diagnostic_sessions WHERE created_at >= ?${filterClause}
     ORDER BY created_at DESC LIMIT ?`
  ).bind(since, ...filterBinds, limit).all();

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
    rssi:              r.rssi              ?? null,
    banda_wifi:        r.banda_wifi        ?? null,
    padrao_wifi:       r.padrao_wifi       ?? null,
    // GH#442: origem do dado — 'android' | 'web'. Default 'android' preserva
    // semântica de dados anteriores à migration 011_gh442.sql.
    platform:          r.platform          ?? 'android',
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", platform: platformFilter ?? "all", sessions }, 200, env);
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
  // GH#427: adicionado jitter/packet_loss/upload por network_type — únicas métricas
  // físicas que o Android realmente coleta em diagnostic_sessions (ver
  // "SignallQ Admin/docs/architecture/data-architecture.md"). Nada de contagem de
  // torres/SSID ou índice de interferência — o app não mede isso.
  const rows = await env.DB.prepare(
    `SELECT
       network_type AS name,
       COUNT(*) AS count,
       AVG(CAST(score AS REAL)) AS avg_score,
       AVG(download_mbps) AS avg_download_mbps,
       AVG(upload_mbps) AS avg_upload_mbps,
       AVG(latency_ms) AS avg_latency_ms,
       AVG(jitter_ms) AS avg_jitter_ms,
       AVG(packet_loss) AS avg_packet_loss,
       COUNT(*) * 100.0 / SUM(COUNT(*)) OVER() AS percentage
     FROM diagnostic_sessions
     WHERE created_at >= ?${envClause}
     GROUP BY network_type
     ORDER BY count DESC`
  ).bind(since, ...envBinds).all();

  const round1 = (v: number | null) => v != null ? Math.round(v * 10) / 10 : null;

  const items = (rows.results ?? []).map((r: any) => ({
    name:              r.name              ?? "Desconhecido",
    count:             r.count             ?? 0,
    avg_score:         r.avg_score         != null ? Math.round(r.avg_score) : null,
    avg_download_mbps: round1(r.avg_download_mbps),
    avg_upload_mbps:   round1(r.avg_upload_mbps),
    avg_latency_ms:    r.avg_latency_ms    != null ? Math.round(r.avg_latency_ms) : null,
    avg_jitter_ms:     round1(r.avg_jitter_ms),
    avg_packet_loss:   round1(r.avg_packet_loss),
    percentage:        r.percentage        != null ? Math.round(r.percentage * 10) / 10 : 0,
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", items }, 200, env);
}

// GH#765 — o Android manda "none" no array de issues quando não há problema
// detectado (em vez de array vazio). Sem esse filtro, "none" era contado como
// o problema mais comum do painel (ex: 42% das sessões).
const NON_ISSUE_LABELS = new Set(["none", "unknown", "null", "n/a"]);
function isRealIssueLabel(label: unknown): label is string {
  if (typeof label !== "string") return false;
  const trimmed = label.trim();
  return trimmed.length > 0 && !NON_ISSUE_LABELS.has(trimmed.toLowerCase());
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
      if (isRealIssueLabel(label)) {
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

async function handleDiagnosticsSummary(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);
  const platformFilter = getPlatformFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];
  const platformClause = platformFilter ? " AND platform = ?" : "";
  const platformBinds  = platformFilter ? [platformFilter]    : [];
  const filterClause = `${envClause}${platformClause}`;
  const filterBinds  = [...envBinds, ...platformBinds];

  const row = await env.DB.prepare(
    `SELECT
       COUNT(*) AS total,
       AVG(latency_ms) AS avg_latency_ms,
       AVG(jitter_ms) AS avg_jitter_ms,
       AVG(packet_loss) AS avg_packet_loss,
       AVG(download_mbps) AS avg_download_mbps,
       AVG(upload_mbps) AS avg_upload_mbps,
       AVG(CAST(score AS REAL)) AS avg_score,
       SUM(CASE WHEN status IN ('ruim','critico') THEN 1 ELSE 0 END) AS critical_count,
       SUM(CASE WHEN resolved = 0 THEN 1 ELSE 0 END) AS active_count
     FROM diagnostic_sessions
     WHERE created_at >= ?${filterClause}`
  ).bind(since, ...filterBinds).first<{
    total: number;
    avg_latency_ms: number | null;
    avg_jitter_ms: number | null;
    avg_packet_loss: number | null;
    avg_download_mbps: number | null;
    avg_upload_mbps: number | null;
    avg_score: number | null;
    critical_count: number;
    active_count: number;
  }>();

  const round1 = (v: number | null) => v != null ? Math.round(v * 10) / 10 : null;

  return json({
    source:                    "d1",
    period,
    environment:               envFilter ?? "all",
    platform:                  platformFilter ?? "all",
    totalDiagnostics:          row?.total ?? 0,
    criticalCount:             row?.critical_count ?? 0,
    activeSessions:            row?.active_count ?? 0,
    averageScore:              row?.avg_score != null ? Math.round(row.avg_score) : null,
    averageLatencyMs:          round1(row?.avg_latency_ms ?? null),
    averageJitterMs:           round1(row?.avg_jitter_ms ?? null),
    averagePacketLossPercentage: round1(row?.avg_packet_loss ?? null),
    averageDownloadMbps:       round1(row?.avg_download_mbps ?? null),
    averageUploadMbps:         round1(row?.avg_upload_mbps ?? null),
  }, 200, env);
}

async function handleAiProviders(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  const rows = await env.DB.prepare(
    `SELECT
       model,
       SUM(total_tokens) AS tokensProcessed,
       COUNT(*) AS calls,
       SUM(CASE WHEN completion_tokens > 0 THEN 1 ELSE 0 END) AS successful_calls
     FROM ai_usage
     WHERE created_at >= ?${envClause}
     GROUP BY model
     ORDER BY tokensProcessed DESC`
  ).bind(since, ...envBinds).all();

  const results = rows.results ?? [];
  const grandTotal = results.reduce((acc: number, r: any) => acc + (r.tokensProcessed ?? 0), 0);

  const items = results.map((r: any) => {
    const calls      = r.calls ?? 0;
    const successful = r.successful_calls ?? 0;
    return {
      name:                  providerName(r.model ?? ""),
      tokensProcessed:       r.tokensProcessed ?? 0,
      percentage:            grandTotal > 0 ? Math.round(((r.tokensProcessed ?? 0) / grandTotal) * 100) : 0,
      reliabilityPercentage: calls > 0 ? Math.round((successful / calls) * 10000) / 100 : null,
    };
  });

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

// GH#421: histórico de execuções reais de IA — cada linha vem direto de `ai_usage`,
// correlacionada com `diagnostic_sessions` quando `session_id` existe. Sem invenção
// de latência: o schema não registra esse campo hoje (ver docs/architecture/data-architecture.md).
async function handleAiUsageRecords(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "7d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);
  const limit     = Math.min(Math.max(parseInt(url.searchParams.get("limit") ?? "100"), 1), 500);

  const envClause = envFilter ? " AND au.environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]               : [];

  const rows = await env.DB.prepare(
    `SELECT
       au.id, au.session_id, au.created_at, au.model,
       au.prompt_tokens, au.completion_tokens, au.cost_usd,
       au.status, au.error_message, au.environment,
       ds.id AS diag_id
     FROM ai_usage au
     LEFT JOIN diagnostic_sessions ds ON ds.id = au.session_id
     WHERE au.created_at >= ?${envClause}
     ORDER BY au.created_at DESC
     LIMIT ?`
  ).bind(since, ...envBinds, limit).all();

  const records = (rows.results ?? []).map((r: any) => ({
    id:               r.id,
    timestamp:        new Date((r.created_at ?? 0) * 1000).toISOString(),
    model:            r.model ?? "",
    provider:         providerName(r.model ?? ""),
    promptTokens:     r.prompt_tokens     ?? 0,
    completionTokens: r.completion_tokens ?? 0,
    costUsd:          r.cost_usd ?? 0,
    // status/error_message podem não existir ainda em registros anteriores à migration
    // 009_gh421 — default 'success' documentado na própria coluna (DEFAULT 'success').
    status:           r.status === "error" ? "error" : "success",
    errorMessage:     r.error_message || null,
    diagnosisId:      r.diag_id ?? null,
    environment:      r.environment ?? "production",
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", records }, 200, env);
}

// SIG-139: métricas de diagnóstico agrupadas por operadora.
async function handleOperators(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "30d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  // Gap 4/5 (SIG-164): packetLossAverage calculado do D1; type derivado do network_type
  // mais frequente por operadora via subquery correlacionada.
  const rows = await env.DB.prepare(
    `SELECT
       s1.operator,
       COUNT(*)                                                       AS total_diagnostics,
       AVG(s1.score)                                                  AS avg_score,
       AVG(s1.download_mbps)                                         AS avg_download,
       AVG(s1.upload_mbps)                                           AS avg_upload,
       AVG(s1.latency_ms)                                            AS avg_latency,
       AVG(s1.packet_loss)                                           AS avg_packet_loss,
       SUM(CASE WHEN s1.status = 'completed' THEN 1 ELSE 0 END)     AS completed,
       SUM(CASE WHEN s1.resolved = 1         THEN 1 ELSE 0 END)     AS resolved,
       (SELECT s2.network_type
        FROM diagnostic_sessions s2
        WHERE s2.operator = s1.operator
          AND s2.created_at >= ?
          AND s2.network_type IS NOT NULL AND s2.network_type != '' AND s2.network_type != 'unknown'
        GROUP BY s2.network_type
        ORDER BY COUNT(*) DESC
        LIMIT 1)                                                      AS dominant_network_type
     FROM diagnostic_sessions s1
     WHERE s1.created_at >= ?
       AND s1.operator IS NOT NULL AND s1.operator != ''${envClause}
     GROUP BY s1.operator
     ORDER BY total_diagnostics DESC`
  ).bind(since, since, ...envBinds).all();

  const operators = (rows.results ?? []).map((r: any) => ({
    operator:            r.operator,
    total_diagnostics:   r.total_diagnostics ?? 0,
    avg_score:           r.avg_score         != null ? Math.round(r.avg_score) : null,
    avg_download:        r.avg_download       ?? null,
    avg_upload:          r.avg_upload         ?? null,
    avg_latency:         r.avg_latency        != null ? Math.round(r.avg_latency) : null,
    packetLossAverage:   r.avg_packet_loss    != null ? Math.round(r.avg_packet_loss * 100) / 100 : null,
    // Não inventar "mobile" quando não há amostra suficiente para determinar o tipo dominante.
    type:                r.dominant_network_type ?? null,
    completed:           r.completed          ?? 0,
    resolved:            r.resolved           ?? 0,
  }));

  return json({ source: "d1", period, environment: envFilter ?? "all", operators }, 200, env);
}

// GH#423: agregação real de sessões por versão de app — versão em produção, canal de
// distribuição e dados por release, direto do D1 (não depende de BigQuery/Crashlytics).
async function handleAppVersions(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "30d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  const rows = await env.DB.prepare(
    `SELECT
       app_version,
       version_code,
       dist_channel,
       build_type,
       COUNT(*)          AS sessions,
       AVG(score)        AS avg_score,
       MIN(created_at)   AS first_seen,
       MAX(created_at)   AS last_seen
     FROM diagnostic_sessions
     WHERE created_at >= ?
       AND app_version IS NOT NULL AND app_version != ''${envClause}
     GROUP BY app_version, version_code, dist_channel, build_type
     ORDER BY version_code DESC, last_seen DESC`
  ).bind(since, ...envBinds).all();

  const versions = (rows.results ?? []).map((r: any) => ({
    appVersion:  r.app_version,
    versionCode: r.version_code ?? null,
    distChannel: r.dist_channel || "unknown",
    buildType:   r.build_type   || "unknown",
    sessions:    r.sessions     ?? 0,
    avgScore:    r.avg_score != null ? Math.round(r.avg_score) : null,
    firstSeen:   r.first_seen  ?? null,
    lastSeen:    r.last_seen   ?? null,
  }));

  // "Versão em produção": build mais recente visto no canal play_store; sem sessões
  // desse canal ainda (ex.: só beta interno), cai para a versão mais recente disponível.
  const productionVersion =
    versions.find((v: (typeof versions)[number]) => v.distChannel === "play_store") ?? versions[0] ?? null;

  return json(
    { source: "d1", period, environment: envFilter ?? "all", versions, productionVersion },
    200,
    env
  );
}

// Gap 6 (SIG-164): Diagnostic Intelligence Panel — agrega padrões por tipo de problema.
// Retorna os tipos de issue mais comuns, frequência relativa e score médio por tipo.
async function handleDiagnosticsIntelligence(request: Request, env: Env): Promise<Response> {
  const url       = new URL(request.url);
  const period    = url.searchParams.get("period") ?? "30d";
  const since     = nowSec() - periodToSeconds(period);
  const envFilter = getEnvironmentFilter(url);

  const envClause = envFilter ? " AND environment = ?" : "";
  const envBinds  = envFilter ? [envFilter]            : [];

  const rows = await env.DB.prepare(
    `SELECT issues, score FROM diagnostic_sessions
     WHERE created_at >= ?${envClause} AND issues IS NOT NULL AND issues != '[]'`
  ).bind(since, ...envBinds).all();

  const issueMap = new Map<string, { count: number; totalScore: number; scoreCount: number }>();

  for (const row of (rows.results ?? [])) {
    let issues: string[] = [];
    try { issues = JSON.parse((row as any).issues ?? "[]"); } catch { continue; }
    const score = (row as any).score;
    for (const label of issues) {
      if (isRealIssueLabel(label)) {
        const entry = issueMap.get(label) ?? { count: 0, totalScore: 0, scoreCount: 0 };
        entry.count++;
        if (score != null && !isNaN(Number(score))) {
          entry.totalScore += Number(score);
          entry.scoreCount++;
        }
        issueMap.set(label, entry);
      }
    }
  }

  const totalOccurrences = Array.from(issueMap.values()).reduce((s, e) => s + e.count, 0);

  const patterns = Array.from(issueMap.entries())
    .sort(([, a], [, b]) => b.count - a.count)
    .map(([issue, entry]) => ({
      issue,
      count:     entry.count,
      frequency: totalOccurrences > 0 ? Math.round((entry.count / totalOccurrences) * 1000) / 10 : 0,
      avgScore:  entry.scoreCount > 0 ? Math.round(entry.totalScore / entry.scoreCount) : null,
    }));

  return json({ source: "d1", period, environment: envFilter ?? "all", patterns }, 200, env);
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

interface FirebaseSyncState {
  syncedAt: string;
  eventsImported: number;
  crashesImported: number;
}

async function readFirebaseSyncState(env: Env): Promise<FirebaseSyncState | null> {
  const row = await env.DB.prepare(
    "SELECT value FROM admin_settings WHERE key = 'firebase_sync'"
  ).first<{ value: string }>();
  if (!row?.value) return null;
  try {
    return JSON.parse(row.value) as FirebaseSyncState;
  } catch {
    return null;
  }
}

async function writeFirebaseSyncState(env: Env, state: FirebaseSyncState): Promise<void> {
  await env.DB.prepare(
    "INSERT OR REPLACE INTO admin_settings (key, value, updated_at) VALUES ('firebase_sync', ?, ?)"
  ).bind(JSON.stringify(state), nowSec()).run();
}

async function handleFirebaseStatus(_req: Request, env: Env): Promise<Response> {
  const syncState = await readFirebaseSyncState(env);
  return json({
    source: "worker",
    projectId: env.FIREBASE_PROJECT_ID,
    status: "connected",
    hasCredentials: !!(env.FIREBASE_CLIENT_EMAIL && env.FIREBASE_PRIVATE_KEY),
    ga4PropertyConfigured: !!env.FIREBASE_GA4_PROPERTY_ID,
    lastSyncTimestamp: syncState?.syncedAt ?? null,
    eventsImported: syncState?.eventsImported ?? 0,
    crashesImported: syncState?.crashesImported ?? 0,
  }, 200, env);
}

// --- GH#761: integração real com Google Play (Android Publisher API) ---

const GOOGLE_PLAY_PACKAGE_NAME = "io.signallq.app";

async function getGooglePlayAccessToken(env: Env, scope: string): Promise<string> {
  const now = nowSec();
  const payload = {
    iss: env.GOOGLE_PLAY_CLIENT_EMAIL,
    sub: env.GOOGLE_PLAY_CLIENT_EMAIL,
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
    scope,
  };
  const privateKey = env.GOOGLE_PLAY_PRIVATE_KEY.replace(/\\n/g, "\n");
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
  if (!tokenResp.ok) {
    throw new Error(`google_play_oauth_${tokenResp.status}: ${(await tokenResp.text()).slice(0, 200)}`);
  }
  const tokenData = (await tokenResp.json()) as { access_token: string };
  return tokenData.access_token;
}

interface GooglePlaySyncState {
  syncedAt: string;
  ratingAverage: number | null;
  reviewsSampled: number;
}

async function readGooglePlaySyncState(env: Env): Promise<GooglePlaySyncState | null> {
  const row = await env.DB.prepare(
    "SELECT value FROM admin_settings WHERE key = 'google_play_sync'"
  ).first<{ value: string }>();
  if (!row?.value) return null;
  try {
    return JSON.parse(row.value) as GooglePlaySyncState;
  } catch {
    return null;
  }
}

async function writeGooglePlaySyncState(env: Env, state: GooglePlaySyncState): Promise<void> {
  await env.DB.prepare(
    "INSERT OR REPLACE INTO admin_settings (key, value, updated_at) VALUES ('google_play_sync', ?, ?)"
  ).bind(JSON.stringify(state), nowSec()).run();
}

async function handleGooglePlayStatus(_req: Request, env: Env): Promise<Response> {
  const hasCredentials = !!(env.GOOGLE_PLAY_CLIENT_EMAIL && env.GOOGLE_PLAY_PRIVATE_KEY);
  const syncState = await readGooglePlaySyncState(env);
  return json({
    source: "worker",
    packageName: GOOGLE_PLAY_PACKAGE_NAME,
    status: hasCredentials ? "connected" : "disabled",
    hasCredentials,
    lastSyncTimestamp: syncState?.syncedAt ?? null,
    ratingAverage: syncState?.ratingAverage ?? null,
    reviewsSampled: syncState?.reviewsSampled ?? 0,
  }, 200, env);
}

// GH#761 — a Android Publisher API não expõe contagem de downloads/instalações
// (isso só existe via export de relatórios CSV pro Cloud Storage, configurado
// à parte no Play Console — não implementado aqui). O dado real disponível
// via API é a lista de reviews (reviews.list), com nota (starRating) por
// review — usamos a média de uma amostra recente como sinal real de
// satisfação, sem inventar número de instalações.
async function handleGooglePlaySync(_req: Request, env: Env): Promise<Response> {
  if (!env.GOOGLE_PLAY_CLIENT_EMAIL || !env.GOOGLE_PLAY_PRIVATE_KEY) {
    return json({ status: "not_configured", message: "GOOGLE_PLAY_CLIENT_EMAIL/GOOGLE_PLAY_PRIVATE_KEY não configurados." }, 200, env);
  }
  try {
    const token = await getGooglePlayAccessToken(
      env,
      "https://www.googleapis.com/auth/androidpublisher"
    );
    const resp = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${GOOGLE_PLAY_PACKAGE_NAME}/reviews?maxResults=100`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    if (!resp.ok) {
      const errText = await resp.text();
      await logError(env, 'google-play', `reviews_${resp.status}: ${errText.slice(0, 300)}`, '');
      return json({ status: "error", message: `Falha ao consultar reviews (HTTP ${resp.status}) — app pode ainda não estar publicado.` }, 200, env);
    }
    const data = await resp.json() as {
      reviews?: Array<{ comments?: Array<{ userComment?: { starRating?: number } }> }>;
    };
    const ratings: number[] = [];
    for (const review of data.reviews ?? []) {
      for (const comment of review.comments ?? []) {
        const rating = comment.userComment?.starRating;
        if (typeof rating === "number") ratings.push(rating);
      }
    }
    const ratingAverage = ratings.length
      ? Math.round((ratings.reduce((a, b) => a + b, 0) / ratings.length) * 100) / 100
      : null;
    const syncedAt = new Date().toISOString();
    await writeGooglePlaySyncState(env, { syncedAt, ratingAverage, reviewsSampled: ratings.length });
    return json({ status: "ok", syncedAt, ratingAverage, reviewsSampled: ratings.length }, 200, env);
  } catch (e) {
    await logError(env, 'google-play', String(e), e instanceof Error ? (e.stack ?? '') : '');
    return json({ status: "error", message: String(e) }, 200, env);
  }
}

// --- GH#417 / GH#425: saúde do sistema com verificação real de cada dependência ---

interface HealthCheckResult {
  status: "ok" | "error" | "not_configured" | "idle";
  latencyMs?: number;
  message?: string;
}

async function checkD1Health(env: Env): Promise<HealthCheckResult> {
  const start = Date.now();
  try {
    await env.DB.prepare("SELECT 1 AS ok").first();
    return { status: "ok", latencyMs: Date.now() - start };
  } catch (e) {
    return { status: "error", latencyMs: Date.now() - start, message: String(e) };
  }
}

async function checkFirebaseCredentialsHealth(env: Env): Promise<HealthCheckResult> {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return { status: "not_configured", message: "FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY não configurados." };
  }
  const start = Date.now();
  try {
    await getFirebaseAccessToken(env);
    return { status: "ok", latencyMs: Date.now() - start };
  } catch (e) {
    return { status: "error", latencyMs: Date.now() - start, message: String(e) };
  }
}

async function checkBigQueryHealth(env: Env, firebaseOk: boolean): Promise<HealthCheckResult> {
  if (!firebaseOk) {
    return { status: "not_configured", message: "Requer credenciais Firebase válidas para autenticar no BigQuery." };
  }
  const start = Date.now();
  const { error } = await queryBigQuery(env, "SELECT 1 AS ok");
  if (error) return { status: "error", latencyMs: Date.now() - start, message: error };
  return { status: "ok", latencyMs: Date.now() - start };
}

interface IngestHealthResult extends HealthCheckResult {
  keyConfigured: boolean;
  lastSuccessAt: string | null;
}

async function checkIngestHealth(env: Env): Promise<IngestHealthResult> {
  const keyConfigured = !!env.INGEST_KEY;
  if (!keyConfigured) {
    return { status: "not_configured", keyConfigured: false, lastSuccessAt: null, message: "INGEST_KEY não configurada." };
  }

  const row = await env.DB.prepare(
    "SELECT MAX(created_at) AS last FROM diagnostic_sessions"
  ).first<{ last: number | null }>();
  const lastSuccessAt = row?.last ? new Date(row.last * 1000).toISOString() : null;

  // Sem ingest nas últimas 48h é sinal de app parado de enviar dados, não necessariamente erro,
  // mas o painel não deve mostrar "ok" silencioso quando não há evidência de atividade recente.
  const idleThresholdMs = 48 * 60 * 60 * 1000;
  const isIdle = !lastSuccessAt || Date.now() - new Date(lastSuccessAt).getTime() > idleThresholdMs;

  return {
    status: isIdle ? "idle" : "ok",
    keyConfigured: true,
    lastSuccessAt,
    message: isIdle ? "Nenhum ingest de diagnóstico recebido nas últimas 48h." : undefined,
  };
}

async function handleSystemHealth(_req: Request, env: Env): Promise<Response> {
  const d1 = await checkD1Health(env);
  const firebaseCredentials = await checkFirebaseCredentialsHealth(env);
  const bigQuery = await checkBigQueryHealth(env, firebaseCredentials.status === "ok");
  const ingest = await checkIngestHealth(env);

  const lastErrorRow = await env.DB.prepare(
    "SELECT source, message, last_seen FROM system_errors ORDER BY last_seen DESC LIMIT 1"
  ).first<{ source: string; message: string; last_seen: number }>();
  const lastFailure = lastErrorRow
    ? {
        source: lastErrorRow.source,
        message: lastErrorRow.message,
        timestamp: new Date(lastErrorRow.last_seen).toISOString(),
      }
    : null;

  const lastSuccess = ingest.lastSuccessAt
    ? { source: "ingest", timestamp: ingest.lastSuccessAt }
    : null;

  return json(
    {
      source: "worker",
      timestamp: new Date().toISOString(),
      checks: {
        worker: { status: "ok" as const },
        d1,
        firebaseCredentials,
        bigQuery,
        ingest,
      },
      lastFailure,
      lastSuccess,
    },
    200,
    env
  );
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
    const syncedAt = new Date().toISOString();
    await writeFirebaseSyncState(env, { syncedAt, eventsImported: 0, crashesImported: 0 });
    return json({ ok: false, source: "no_data_yet", sessionsYesterday: 0, syncedAt }, 200, env);
  }
  if (error) {
    // Query falhou: não persiste estado de sync, pois nada foi de fato sincronizado.
    return json({ ok: false, source: "error", message: error }, 200, env);
  }

  const sessions = parseInt(rows[0]?.sessions ?? "0", 10);
  const syncedAt = new Date().toISOString();
  // crashesImported não é coletado nesta query (só conta session_start via BigQuery);
  // não há fonte real para esse número aqui, então preserva o valor anterior em vez de inventar.
  const previous = await readFirebaseSyncState(env);
  await writeFirebaseSyncState(env, {
    syncedAt,
    eventsImported: sessions,
    crashesImported: previous?.crashesImported ?? 0,
  });
  return json({ ok: true, source: "bigquery", sessionsYesterday: sessions, syncedAt }, 200, env);
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

    // Validação dos limiares consumidos em GET /admin/metrics/alerts (GH#426).
    const b = body as Record<string, unknown>;
    if ("aiDailyBudgetUsd" in b) {
      const v = b.aiDailyBudgetUsd;
      if (typeof v !== "number" || !Number.isFinite(v) || v < 0) {
        return err("aiDailyBudgetUsd deve ser um número >= 0", 400, env);
      }
    }
    if ("errorSpikeThreshold" in b) {
      const v = b.errorSpikeThreshold;
      if (typeof v !== "number" || !Number.isInteger(v) || v < 1) {
        return err("errorSpikeThreshold deve ser um inteiro >= 1", 400, env);
      }
    }
    if ("criticalScoreThreshold" in b) {
      const v = b.criticalScoreThreshold;
      if (typeof v !== "number" || !Number.isInteger(v) || v < 0 || v > 100) {
        return err("criticalScoreThreshold deve ser um inteiro entre 0 e 100", 400, env);
      }
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

// GH#422: categoriza a origem do erro para diferenciar app / backend / IA /
// integração no painel (antes só existia o campo `source` livre, sem semântica
// de camada). Mapeamento fixo por `source` conhecido do próprio Worker —
// `source` de erros vindos do app real (feature_crash) usa categoria 'app'
// diretamente em handleErrors, não passa por aqui.
const ERROR_CATEGORY_BY_SOURCE: Record<string, 'app' | 'backend' | 'ia' | 'integration'> = {
  'firebase':               'integration',
  'bigquery-crashlytics':   'integration',
  'bigquery-versions':      'integration',
  'bigquery-crash-issues':  'integration',
  'ai-usage':               'ia',
};

function errorCategoryForSource(source: string): 'app' | 'backend' | 'ia' | 'integration' {
  return ERROR_CATEGORY_BY_SOURCE[source] ?? 'backend';
}

// Fire-and-forget: nunca propaga exceção. Deduplica por (source + message) via hash djb2.
async function logError(env: Env, source: string, message: string, stack = ''): Promise<void> {
  try {
    const id = djb2(`${source}:${message}`);
    const now = Date.now();
    const category = errorCategoryForSource(source);
    await env.DB.prepare(`
      INSERT INTO system_errors (id, source, category, message, stack_trace, count, first_seen, last_seen)
      VALUES (?, ?, ?, ?, ?, 1, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        count       = count + 1,
        last_seen   = excluded.last_seen,
        stack_trace = excluded.stack_trace
    `).bind(id, source, category, message, stack, now, now).run();
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

// GH#422: por padrão só devolve erros ativos (resolved=0) — é o que faz o
// erro "deixar de aparecer como ativo" quando tratado (critério de aceite).
// ?resolved=all devolve tudo (histórico); ?resolved=true devolve só resolvidos.
async function handleErrors(request: Request, env: Env): Promise<Response> {
  const url        = new URL(request.url);
  const period     = url.searchParams.get("period") ?? "30d";
  const resolvedQ  = url.searchParams.get("resolved"); // null | "all" | "true" | "false"
  // Fase A: o parâmetro ?environment= enviado pelo frontend é IGNORADO aqui.
  // A tabela system_errors não possui coluna environment — os erros são do worker,
  // não do app. Filtro por environment entra na Fase B junto com SIG-143.
  // last_seen é em milissegundos (Date.now()), mas periodToSeconds retorna segundos.
  // Multiplicamos por 1000 para ficar na mesma escala.
  const sinceMs = (Date.now()) - periodToSeconds(period) * 1000;

  const resolvedClause =
    resolvedQ === "all"   ? ""                 :
    resolvedQ === "true"  ? " AND resolved = 1" :
    " AND resolved = 0"; // default: só erros ativos

  const rows = await env.DB.prepare(
    `SELECT id, source, category, message, stack_trace, count, first_seen, last_seen,
            resolved, resolved_by, resolved_at, resolution_note
     FROM system_errors
     WHERE last_seen >= ?${resolvedClause}
     ORDER BY count DESC, last_seen DESC
     LIMIT 100`
  ).bind(sinceMs).all();

  const workerErrors = (rows.results ?? []).map((r: any) => ({
    id:               r.id,
    source:           r.source,
    category:         r.category ?? 'backend',
    message:          r.message,
    stackTrace:       r.stack_trace ?? '',
    count:            r.count       ?? 1,
    timestamp:        new Date(r.last_seen).toISOString(),
    // O backend não rastreia usuários únicos por erro — sem PII no D1.
    // Derivar por device_id (já presente em diagnostic_sessions) é Fase B.
    affectedUserCount: 0,
    resolved:         r.resolved === 1,
    resolvedBy:       r.resolved_by ?? '',
    resolvedAt:       r.resolved_at ? new Date(r.resolved_at).toISOString() : null,
    resolutionNote:   r.resolution_note ?? '',
  }));

  // GH#422 gap 5: integra fonte real de erro do app — feature_crash já é
  // aceito por /ingest/analytics (whitelist), mas nunca era lido aqui.
  // Resolução desses eventos usa a mesma tabela system_errors (chave =
  // "app:<id do evento>"), criada sob demanda em handleResolveSystemError.
  const sinceSec = Math.floor(sinceMs / 1000);
  const crashRows = await env.DB.prepare(
    `SELECT id, event_name, error_type, session_id, created_at
     FROM analytics_events
     WHERE event_name = 'feature_crash' AND created_at >= ?
     ORDER BY created_at DESC
     LIMIT 100`
  ).bind(sinceSec).all();

  const appErrorIds = (crashRows.results ?? []).map((r: any) => `app:${r.id}`);
  const resolutionByAppId = new Map<string, { resolved: boolean; resolvedBy: string; resolvedAt: number | null; resolutionNote: string }>();
  if (appErrorIds.length > 0) {
    const placeholders = appErrorIds.map(() => '?').join(',');
    const resRows = await env.DB.prepare(
      `SELECT id, resolved, resolved_by, resolved_at, resolution_note
       FROM system_errors WHERE id IN (${placeholders})`
    ).bind(...appErrorIds).all();
    for (const r of (resRows.results ?? []) as any[]) {
      resolutionByAppId.set(r.id, {
        resolved:       r.resolved === 1,
        resolvedBy:     r.resolved_by ?? '',
        resolvedAt:     r.resolved_at || null,
        resolutionNote: r.resolution_note ?? '',
      });
    }
  }

  const appErrors = (crashRows.results ?? [])
    .map((r: any) => {
      const id  = `app:${r.id}`;
      const res = resolutionByAppId.get(id);
      return {
        id,
        source:            'android_app',
        category:          'app' as const,
        message:           r.error_type || 'Crash reportado pelo app (sem error_type)',
        stackTrace:        '',
        count:             1,
        timestamp:         new Date(r.created_at * 1000).toISOString(),
        affectedUserCount: 0,
        resolved:          res?.resolved ?? false,
        resolvedBy:        res?.resolvedBy ?? '',
        resolvedAt:        res?.resolvedAt ? new Date(res.resolvedAt).toISOString() : null,
        resolutionNote:    res?.resolutionNote ?? '',
      };
    })
    .filter((e: { resolved: boolean }) => {
      if (resolvedQ === "all") return true;
      if (resolvedQ === "true") return e.resolved;
      return !e.resolved; // default e "false": só ativos
    });

  const errors = [...workerErrors, ...appErrors]
    .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
    .slice(0, 100);

  return json({ source: "d1", period, errors }, 200, env);
}

// POST /admin/errors/:id/resolve — GH#422. Marca erro (do worker ou do app)
// como resolvido, gravando responsável (da sessão autenticada), data (server)
// e observação (body). UPSERT: erros "app:<id>" ainda não têm linha em
// system_errors (só existem via analytics_events) — cria a linha sob demanda.
async function handleResolveSystemError(request: Request, env: Env, session: { userId: string; role: string }): Promise<Response> {
  const url   = new URL(request.url);
  const match = url.pathname.match(/^\/admin\/errors\/([^/]+)\/resolve$/);
  if (!match) return err('id inválido', 400, env);
  const id = decodeURIComponent(match[1]);

  let body: any = {};
  try { body = await request.json(); } catch { /* body opcional */ }
  const note = typeof body.note === 'string' ? body.note.slice(0, 2000) : '';

  const userRow = await env.DB.prepare(
    'SELECT email FROM admin_users WHERE id = ?'
  ).bind(session.userId).first<{ email: string }>();
  const resolvedBy = userRow?.email ?? 'admin';
  const now = Date.now();

  // A categoria abaixo só é gravada se a linha ainda não existir (ON CONFLICT
  // não a atualiza) — para erros do worker que já existem em system_errors,
  // a categoria original (gravada por logError) é preservada.
  const isAppError = id.startsWith('app:');

  await env.DB.prepare(`
    INSERT INTO system_errors (id, source, category, message, stack_trace, count, first_seen, last_seen, resolved, resolved_by, resolved_at, resolution_note)
    VALUES (?, ?, ?, '', '', 1, ?, ?, 1, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
      resolved        = 1,
      resolved_by     = excluded.resolved_by,
      resolved_at     = excluded.resolved_at,
      resolution_note = excluded.resolution_note
  `).bind(id, isAppError ? 'android_app' : 'unknown', isAppError ? 'app' : 'backend', now, now, resolvedBy, now, note).run();

  return json({ ok: true, id, resolvedBy, resolvedAt: new Date(now).toISOString() }, 200, env);
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

  // GH#442: origem do dado. Default 'android' porque o app Android ainda não
  // envia este campo (todo dado histórico é dele) — o extinto PWA mandava
  // 'web' explicitamente. Mantido para não quebrar leitura de dados históricos.
  const platform = p.platform === 'web' ? 'web' : 'android';

  // Gap 3 (SIG-164): rssi, banda_wifi, padrao_wifi enviados ao AI Worker mas nunca persistidos.
  const rssi       = p.rssi        ?? p.rssiDbm ?? null;
  const bandaWifi  = p.banda_wifi  ?? p.bandaWifi  ?? null;
  const padraoWifi = p.padrao_wifi ?? p.padraoWifi ?? null;

  try {
    await env.DB.prepare(
      `INSERT OR REPLACE INTO diagnostic_sessions
         (id, created_at, network_type, status, score,
          download_mbps, upload_mbps, latency_ms, jitter_ms, packet_loss,
          issues, resolved, operator,
          device_model, os_version, app_version, ai_summary_report,
          environment, dist_channel, build_type, version_code, device_id,
          rssi, banda_wifi, padrao_wifi, platform)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      p.id, p.created_at ?? nowSec(),
      p.network_type ?? "unknown", p.status ?? "unknown", p.score ?? null,
      p.download_mbps ?? null, p.upload_mbps ?? null,
      p.latency_ms ?? null, p.jitter_ms ?? null, p.packet_loss ?? null,
      JSON.stringify(p.issues ?? []),
      p.operator ?? null,
      deviceModel, osVersion, appVersion, aiSummaryReport,
      environment, distChannel, buildType, versionCode, deviceId,
      rssi, bandaWifi, padraoWifi, platform,
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

  // Gap 1 (SIG-164): cost_usd null em registros retroativos.
  // Se o Android envia cost_usd=null mas total_tokens > 0, aplica tarifa aproximada
  // do Qwen3 30B (0.30 USD / 1M tokens) para não perder rastreabilidade de custo.
  const cost = p.cost_usd != null
    ? p.cost_usd
    : total > 0
      ? (total / 1_000_000) * 0.30
      : costForModel(p.model, total);

  // SIG-143: campos de contexto de ambiente.
  const environment = p.environment  ?? 'production';
  const versionCode = p.version_code ?? 0;

  // Gap 2 (SIG-164): dist_channel, build_type, device_id antes descartados.
  const distChannel = p.dist_channel ?? '';
  const buildType   = p.build_type   ?? 'release';
  const deviceId    = p.device_id    ?? '';

  // GH#421: status/erro da execução. Default 'success' porque o app hoje só
  // envia ai_usage ao final de uma chamada concluída (sem retry/erro reportado
  // ainda) — quando o Android passar a enviar falhas, o valor real prevalece.
  const status       = p.status === 'error' ? 'error' : 'success';
  const errorMessage = p.error_message ?? p.error ?? '';

  // GH#442: mesmo critério de origem do /ingest/diagnostic.
  const platform = p.platform === 'web' ? 'web' : 'android';

  try {
    await env.DB.prepare(
      `INSERT OR REPLACE INTO ai_usage
         (id, session_id, created_at, model,
          prompt_tokens, completion_tokens, total_tokens, cost_usd,
          environment, version_code, dist_channel, build_type, device_id,
          status, error_message, platform)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      p.id, p.session_id ?? null, p.created_at ?? nowSec(), p.model,
      prompt, completion, total, cost,
      environment, versionCode, distChannel, buildType, deviceId,
      status, errorMessage, platform,
    ).run();
  } catch (e) {
    await logError(env, 'ai-usage', String(e), e instanceof Error ? e.stack ?? '' : '');
    throw e;
  }

  return json({ ok: true, id: p.id }, 201, env);
}

// --- SIG-134: analytics de produto ---

// GH#417: 'session_end' carrega duration_ms — é o único evento com essa métrica,
// necessária para calcular tempo médio de sessão em #418. Sem esse evento, a
// aba Produto & Uso não tem como reportar duração sem heurística inventada.
const VALID_ANALYTICS_EVENTS = new Set([
  'feature_used', 'screen_view', 'session_start', 'session_end', 'feature_crash', 'battery_snapshot',
]);

async function handleIngestAnalytics(request: Request, env: Env): Promise<Response> {
  let body: any;
  try { body = await request.json(); } catch { return err('body JSON inválido', 400, env); }

  const events: any[] = Array.isArray(body.events) ? body.events : [];
  if (events.length === 0) return json({ ok: true, inserted: 0 }, 200, env);
  if (events.length > 500) return err('máximo 500 eventos por batch', 400, env);

  const now = nowSec();

  // GH#417: id é responsabilidade do cliente (UUID gerado no momento do evento).
  // Sem isso, um retry de rede reenviaria o mesmo batch e o INSERT OR IGNORE geraria
  // linhas duplicadas (o id anterior era sempre aleatório no worker, então nunca
  // colidia). Clientes antigos que não enviam `id` ainda funcionam (fallback abaixo),
  // mas não são protegidos contra duplicação em retry — motivo para o app adotar
  // id determinístico assim que a fila local (retry/backoff) for implementada.
  const stmts = events
    .filter((e) => e && VALID_ANALYTICS_EVENTS.has(e.name))
    .map((e) =>
      env.DB.prepare(
        `INSERT OR IGNORE INTO analytics_events
           (id, event_name, session_id, created_at, app_version, feature_id, screen_name, error_type,
            battery_level, battery_charging, environment, device_id, version_code, dist_channel, build_type, duration_ms, platform)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      ).bind(
        typeof e.id === 'string' && e.id.length > 0 ? e.id : crypto.randomUUID(),
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
        e.device_id    ?? '',
        typeof e.version_code === 'number' ? e.version_code : 0,
        e.dist_channel ?? '',
        e.build_type   ?? 'release',
        typeof e.duration_ms === 'number' ? e.duration_ms : null,
        // GH#442: mesmo critério de origem dos demais endpoints de ingest.
        e.platform === 'web' ? 'web' : 'android',
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

  // GH#418: tempo médio de sessão real (duration_ms só existe em session_end, SIG-295/GH#417).
  const sessionDurationRow = await env.DB.prepare(
    `SELECT AVG(duration_ms) AS avg_duration_ms, COUNT(*) AS session_count
     FROM analytics_events
     WHERE event_name = 'session_end' AND duration_ms IS NOT NULL AND created_at >= ?${envClause}`
  ).bind(since, ...envBinds).first<{ avg_duration_ms: number | null; session_count: number }>();

  // GH#418: retenção D1/D7/D30 por cohort de device_id (device_id só existe desde GH#417/migration 008).
  // Cohort de retenção usa histórico completo (não limitado ao `period` do request) porque D1/D7/D30
  // exigem que o dispositivo já tenha tempo suficiente decorrido desde o primeiro evento visto.
  const retentionRow = await env.DB.prepare(
    `WITH first_seen AS (
       SELECT device_id, MIN(created_at) AS install_ts
       FROM analytics_events
       WHERE device_id != ''${envFilter ? ' AND environment = ?' : ''}
       GROUP BY device_id
     ),
     elapsed AS (
       SELECT device_id, install_ts,
         CAST((strftime('%s','now') - install_ts) / 86400 AS INTEGER) AS days_elapsed
       FROM first_seen
     ),
     last_activity AS (
       SELECT device_id, MAX(created_at) AS last_seen
       FROM analytics_events
       WHERE device_id != ''${envFilter ? ' AND environment = ?' : ''}
       GROUP BY device_id
     ),
     returned_d1 AS (
       SELECT DISTINCT a.device_id FROM analytics_events a
       JOIN first_seen f ON a.device_id = f.device_id
       WHERE a.created_at >= f.install_ts + 86400 AND a.created_at < f.install_ts + 172800
     ),
     returned_d7 AS (
       SELECT DISTINCT a.device_id FROM analytics_events a
       JOIN first_seen f ON a.device_id = f.device_id
       WHERE a.created_at >= f.install_ts + 86400 AND a.created_at < f.install_ts + 691200
     ),
     returned_d30 AS (
       SELECT DISTINCT a.device_id FROM analytics_events a
       JOIN first_seen f ON a.device_id = f.device_id
       WHERE a.created_at >= f.install_ts + 86400 AND a.created_at < f.install_ts + 2678400
     )
     SELECT
       (SELECT COUNT(*) FROM elapsed WHERE days_elapsed >= 1)  AS cohort_d1,
       (SELECT COUNT(*) FROM returned_d1)                      AS returned_d1,
       (SELECT COUNT(*) FROM elapsed WHERE days_elapsed >= 7)  AS cohort_d7,
       (SELECT COUNT(*) FROM returned_d7)                      AS returned_d7,
       (SELECT COUNT(*) FROM elapsed WHERE days_elapsed >= 30) AS cohort_d30,
       (SELECT COUNT(*) FROM returned_d30)                     AS returned_d30,
       (SELECT COUNT(*) FROM first_seen)                       AS total_devices,
       (SELECT AVG(days_elapsed) FROM elapsed)                 AS avg_active_span_days,
       (SELECT COUNT(*) FROM last_activity WHERE last_seen < strftime('%s','now') - 1209600) AS inactive_14d,
       (SELECT COUNT(*) FROM last_activity)                    AS total_with_activity
    `
  ).bind(...(envFilter ? [envFilter] : []), ...(envFilter ? [envFilter] : [])).first<{
    cohort_d1: number; returned_d1: number;
    cohort_d7: number; returned_d7: number;
    cohort_d30: number; returned_d30: number;
    total_devices: number; avg_active_span_days: number | null;
    inactive_14d: number; total_with_activity: number;
  }>();

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

  const avg_session_duration_ms = sessionDurationRow?.avg_duration_ms != null
    ? Math.round(sessionDurationRow.avg_duration_ms)
    : null;
  const session_count = sessionDurationRow?.session_count ?? 0;

  const totalDevices = retentionRow?.total_devices ?? 0;
  const retention = totalDevices === 0 ? [] : [{
    cohort:           `Cohort geral (${period})`,
    cohortSize:       totalDevices,
    day1:             retentionRow!.cohort_d1  > 0 ? Math.round((retentionRow!.returned_d1  / retentionRow!.cohort_d1)  * 1000) / 10 : null,
    day7:             retentionRow!.cohort_d7  > 0 ? Math.round((retentionRow!.returned_d7  / retentionRow!.cohort_d7)  * 1000) / 10 : null,
    day30:            retentionRow!.cohort_d30 > 0 ? Math.round((retentionRow!.returned_d30 / retentionRow!.cohort_d30) * 1000) / 10 : null,
    avgInstalledDays: retentionRow!.avg_active_span_days != null ? Math.round(retentionRow!.avg_active_span_days * 10) / 10 : null,
    // Proxy de churn: % de dispositivos sem nenhum evento nos últimos 14 dias. NÃO é confirmação
    // de desinstalação real (isso exigiria Play Console/FCM, não integrado — ver data-architecture.md).
    uninstallRate:    retentionRow!.total_with_activity > 0 ? Math.round((retentionRow!.inactive_14d / retentionRow!.total_with_activity) * 1000) / 10 : null,
  }];

  return json({
    source: 'd1',
    period,
    environment: envFilter ?? 'all',
    no_data_yet: feature_usage.length === 0 && screen_navigation.length === 0,
    feature_usage,
    screen_navigation,
    feature_crashes,
    avg_session_duration_ms,
    session_count,
    retention,
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
  { method: "GET",  pattern: /^\/admin\/metrics\/diagnostics\/summary$/,        handler: withErrorLogging('metrics', handleDiagnosticsSummary) },
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
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-usage\/records$/,          handler: withErrorLogging('metrics', handleAiUsageRecords) },
  { method: "GET",  pattern: /^\/admin\/metrics\/operators$/,                   handler: withErrorLogging('metrics', handleOperators) },
  { method: "GET",  pattern: /^\/admin\/metrics\/app-versions$/,                handler: withErrorLogging('metrics', handleAppVersions) },
  { method: "GET",  pattern: /^\/admin\/metrics\/intelligence$/,                handler: withErrorLogging('metrics', handleDiagnosticsIntelligence) },
  { method: "GET",  pattern: /^\/admin\/diagnostics\/intelligence$/,            handler: withErrorLogging('metrics', handleDiagnosticsIntelligence) },
  { method: "GET",  pattern: /^\/admin\/metrics\/analytics\/product$/,          handler: withErrorLogging('analytics', handleProductAnalytics) },
  { method: "GET",  pattern: /^\/admin\/analytics\/product$/,                   handler: withErrorLogging('analytics', handleProductAnalytics) },
  { method: "GET",  pattern: /^\/admin\/metrics\/analytics\/battery$/,          handler: withErrorLogging('analytics', handleBatteryAnalytics) },
  { method: "GET",  pattern: /^\/admin\/analytics\/battery$/,                   handler: withErrorLogging('analytics', handleBatteryAnalytics) },
  { method: "GET",  pattern: /^\/admin\/metrics\/errors$/,                      handler: handleErrors },
  { method: "POST", pattern: /^\/admin\/errors\/[^/]+\/resolve$/,               handler: withErrorLogging('errors', async (req, env) => {
      const session = await authenticateSession(req, env);
      if (!session) return err('Unauthorized', 401, env);
      return handleResolveSystemError(req, env, session);
    }) },
  { method: "GET",  pattern: /^\/admin\/system-health$/,                        handler: withErrorLogging('system-health', handleSystemHealth) },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/status$/,       handler: handleFirebaseStatus },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/analytics$/,    handler: handleFirebaseAnalytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crashlytics$/,  handler: handleFirebaseCrashlytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/versions$/,     handler: handleFirebaseVersions },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crash-issues$/, handler: handleFirebaseCrashIssues },
  { method: "POST", pattern: /^\/admin\/integrations\/firebase\/sync$/,         handler: handleFirebaseSync },
  { method: "GET",  pattern: /^\/admin\/integrations\/google-play\/status$/,   handler: handleGooglePlayStatus },
  { method: "POST", pattern: /^\/admin\/integrations\/google-play\/sync$/,     handler: handleGooglePlaySync },
  { method: "GET",  pattern: /^\/admin\/analytics\/product$/,                   handler: withErrorLogging('analytics', handleProductAnalytics) },
  { method: "GET",  pattern: /^\/admin\/analytics\/battery$/,                   handler: withErrorLogging('analytics', handleBatteryAnalytics) },
  { method: "GET",  pattern: /^\/admin\/settings$/,                             handler: handleSettings },
  { method: "POST", pattern: /^\/admin\/settings$/,                             handler: handleSettings },
  { method: "GET",  pattern: /^\/admin\/feature-flags$/,                        handler: withErrorLogging('feature-flags', handleFeatureFlags) },
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
      return json({ status: "ok", worker: "signallq-admin-worker", timestamp: new Date().toISOString() }, 200, env);
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
