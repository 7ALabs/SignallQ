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
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
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

  const rows = await env.DB.prepare(
    `SELECT model, COUNT(*) AS calls, SUM(total_tokens) AS tokens, SUM(cost_usd) AS cost_usd
     FROM ai_usage WHERE created_at >= ?${envClause} GROUP BY model ORDER BY calls DESC`
  ).bind(since, ...envBinds).all();

  const totals = (rows.results ?? []).reduce(
    (acc: any, r: any) => ({
      calls:  acc.calls  + (r.calls  ?? 0),
      tokens: acc.tokens + (r.tokens ?? 0),
      cost:   acc.cost   + (r.cost_usd ?? 0),
    }),
    { calls: 0, tokens: 0, cost: 0 }
  );

  return json({ source: "d1", period, environment: envFilter ?? "all", byModel: rows.results ?? [], totals }, 200, env);
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

// SIG-133: alertas reais baseados em threshold checking contra dados do D1.
async function handleRecentAlerts(_request: Request, env: Env): Promise<Response> {
  const now        = nowSec();
  const oneDayAgo  = now - 86400;
  const oneHourAgo = now - 3600;

  // Thresholds lidos de admin_settings, com defaults conservadores para não gerar falsos positivos.
  const settingsRow = await env.DB.prepare(
    "SELECT value FROM admin_settings WHERE key = 'admin'"
  ).first<{ value: string }>();
  const settings = settingsRow?.value ? JSON.parse(settingsRow.value) : {};
  const AI_DAILY_BUDGET  = settings.aiDailyBudgetUsd       ?? 1.0;   // USD
  const ERROR_THRESHOLD  = settings.errorSpikeThreshold    ?? 10;    // erros/hora
  const MIN_SCORE        = settings.criticalScoreThreshold ?? 50;    // score médio mínimo

  const alerts: Array<{
    id: string; type: string; severity: string;
    title: string; message: string; created_at: number; resolved: boolean;
  }> = [];

  const [aiCost, recentErrors, scoreRow] = await Promise.all([
    env.DB.prepare(
      "SELECT SUM(cost_usd) AS total FROM ai_usage WHERE created_at >= ?"
    ).bind(oneDayAgo).first<{ total: number | null }>(),

    env.DB.prepare(
      "SELECT COUNT(*) AS count FROM system_errors WHERE last_seen >= ?"
    ).bind(oneHourAgo * 1000).first<{ count: number }>(), // last_seen em ms

    env.DB.prepare(
      `SELECT AVG(CAST(score AS REAL)) AS avg FROM diagnostic_sessions
       WHERE created_at >= ? AND score IS NOT NULL`
    ).bind(oneDayAgo).first<{ avg: number | null }>(),
  ]);

  if ((aiCost?.total ?? 0) > AI_DAILY_BUDGET) {
    alerts.push({
      id:         'ai_budget_exceeded',
      type:       'AI_BUDGET',
      severity:   'critical',
      title:      'Orçamento diário de IA excedido',
      message:    `Custo nas últimas 24h: $${(aiCost?.total ?? 0).toFixed(4)} USD (limite: $${AI_DAILY_BUDGET})`,
      created_at: now,
      resolved:   false,
    });
  }

  if ((recentErrors?.count ?? 0) > ERROR_THRESHOLD) {
    alerts.push({
      id:         'error_spike',
      type:       'ERROR_SPIKE',
      severity:   'warning',
      title:      'Pico de erros detectado',
      message:    `${recentErrors?.count} erros na última hora (limite: ${ERROR_THRESHOLD})`,
      created_at: now,
      resolved:   false,
    });
  }

  if (scoreRow?.avg != null && scoreRow.avg < MIN_SCORE) {
    alerts.push({
      id:         'low_avg_score',
      type:       'LOW_SCORE',
      severity:   'warning',
      title:      'Qualidade de rede baixa',
      message:    `Score médio nas últimas 24h: ${Math.round(scoreRow.avg)} (mínimo: ${MIN_SCORE})`,
      created_at: now,
      resolved:   false,
    });
  }

  return json({ source: "d1", items: alerts }, 200, env);
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
     FROM ai_usage WHERE created_at >= ?`
  ).bind(since).first<{ total: number; successful: number }>();
  const reliabilityPercentage = (reliabilityRow?.total ?? 0) > 0
    ? Math.round(((reliabilityRow?.successful ?? 0) / reliabilityRow!.total) * 100)
    : 100;

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
  return json({
    source: "stub",
    message: "Crashlytics requer exportacao BigQuery.",
    unresolvedCrashes: 0,
    crashFreeUsersPercentage: 100,
  }, 200, env);
}

async function handleFirebaseVersions(_req: Request, env: Env): Promise<Response> {
  return json({ source: "stub", versions: [], message: "Requer BigQuery Crashlytics export." }, 200, env);
}

async function handleFirebaseCrashIssues(_req: Request, env: Env): Promise<Response> {
  return json({ source: "stub", issues: [], message: "Requer BigQuery Crashlytics export." }, 200, env);
}

async function handleFirebaseSync(_req: Request, env: Env): Promise<Response> {
  return json({ jobId: `sync_${Date.now().toString(36)}`, status: "started" }, 200, env);
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

// --- SIG-135 Fase A: pipeline de erros via D1 ---

// Fire-and-forget: nunca propaga exceção. Deduplica por (source + message) via id determinístico.
async function logError(env: Env, source: string, message: string, stack = ''): Promise<void> {
  try {
    const id = btoa(`${source}:${message}`).slice(0, 64);
    const now = Date.now();
    await env.DB.prepare(`
      INSERT INTO system_errors (id, source, message, stack_trace, count, first_seen, last_seen)
      VALUES (?, ?, ?, ?, 1, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        count      = count + 1,
        last_seen  = excluded.last_seen,
        stack_trace = excluded.stack_trace
    `).bind(id, source, message, stack, now, now).run();
  } catch { /* fire-and-forget — nunca propaga */ }
}

async function handleErrors(request: Request, env: Env): Promise<Response> {
  const url    = new URL(request.url);
  const period = url.searchParams.get("period") ?? "30d";
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

// --- router ---

type Handler = (req: Request, env: Env) => Promise<Response>;

const ROUTES: Array<{ method: string; pattern: RegExp; handler: Handler }> = [
  { method: "GET",  pattern: /^\/admin\/metrics\/overview$/,                    handler: handleOverview },
  { method: "GET",  pattern: /^\/admin\/metrics\/diagnostics$/,                 handler: handleDiagnostics },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-usage$/,                    handler: handleAiCost },
  { method: "GET",  pattern: /^\/admin\/metrics\/timeline$/,                    handler: handleTimeline },
  { method: "GET",  pattern: /^\/admin\/metrics\/network$/,                     handler: handleNetworkInsights },
  { method: "GET",  pattern: /^\/admin\/metrics\/top-issues$/,                  handler: handleTopIssues },
  { method: "GET",  pattern: /^\/admin\/metrics\/alerts$/,                      handler: handleRecentAlerts },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-costs$/,                    handler: handleAiCostMetrics },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-providers$/,                handler: handleAiProviders },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-usage\/timeline$/,          handler: handleAiUsageTimeline },
  { method: "GET",  pattern: /^\/admin\/metrics\/operators$/,                   handler: handleOperators },
  { method: "GET",  pattern: /^\/admin\/metrics\/errors$/,                      handler: handleErrors },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/status$/,       handler: handleFirebaseStatus },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/analytics$/,    handler: handleFirebaseAnalytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crashlytics$/,  handler: handleFirebaseCrashlytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/versions$/,     handler: handleFirebaseVersions },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crash-issues$/, handler: handleFirebaseCrashIssues },
  { method: "POST", pattern: /^\/admin\/integrations\/firebase\/sync$/,         handler: handleFirebaseSync },
  { method: "GET",  pattern: /^\/admin\/settings$/,                             handler: handleSettings },
  { method: "POST", pattern: /^\/admin\/settings$/,                             handler: handleSettings },
  { method: "GET",  pattern: /^\/admin\/feature-flags$/,                        handler: handleGetFeatureFlags },
  { method: "POST", pattern: /^\/admin\/feature-flags$/,                        handler: handleSetFeatureFlags },
];

const INGEST_ROUTES: Array<{ method: string; pattern: RegExp; handler: Handler }> = [
  { method: "POST", pattern: /^\/ingest\/diagnostic$/, handler: handleIngestDiagnostic },
  { method: "POST", pattern: /^\/ingest\/ai-usage$/,   handler: handleIngestAiUsage },
];

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(env) });
    }
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      if (!authenticate(request, env)) return err("Unauthorized", 401, env);
      return json({ status: "ok", worker: "signallq-admin-worker" }, 200, env);
    }

    // /feature-flags — público, sem auth. O app Android consome este endpoint sem credenciais.
    if (url.pathname === '/feature-flags' && request.method === 'GET') {
      return handlePublicFeatureFlags(request, env);
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
