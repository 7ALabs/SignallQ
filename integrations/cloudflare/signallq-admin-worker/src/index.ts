// SignallQ Admin API Worker
// /admin/* exige Bearer ADMIN_SECRET (painel web, nao vai no APK).
// /ingest/* exige Bearer INGEST_KEY (chave separada, scope limitado, vai no APK).
// Separar os secrets reduz o blast radius: vazar INGEST_KEY nao da acesso
// aos dados do admin. INGEST_KEY so pode escrever em /ingest/*.

export interface Env {
  ALLOWED_ORIGIN: string;
  FIREBASE_PROJECT_ID: string;
  FIREBASE_GA4_PROPERTY_ID: string;
  ADMIN_SECRET: string;
  /** Chave separada para ingest do app Android. Scope: POST /ingest/* apenas. */
  INGEST_KEY: string;
  FIREBASE_CLIENT_EMAIL: string;
  FIREBASE_PRIVATE_KEY: string;
  DB: D1Database;
}

function corsHeaders(env: Env): Record<string, string> {
  return {
    "Access-Control-Allow-Origin": env.ALLOWED_ORIGIN,
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
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

function periodToSeconds(period: string): number {
  const map: Record<string, number> = {
    "1d": 86400, "7d": 604800, "30d": 2592000, "90d": 7776000,
  };
  return map[period] ?? 604800;
}

function nowSec(): number {
  return Math.floor(Date.now() / 1000);
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
  const url    = new URL(request.url);
  const period = url.searchParams.get("period") ?? "7d";
  const since  = nowSec() - periodToSeconds(period);
  const todaySince = nowSec() - 86400;

  const [sessions, aiRows] = await Promise.all([
    env.DB.prepare(
      `SELECT COUNT(*) AS total,
              SUM(CASE WHEN resolved=0 THEN 1 ELSE 0 END) AS active,
              AVG(CAST(score AS REAL)) AS avg_score
       FROM diagnostic_sessions WHERE created_at >= ?`
    ).bind(since).first<{ total: number; active: number; avg_score: number | null }>(),
    env.DB.prepare(
      `SELECT COUNT(*) AS calls, SUM(cost_usd) AS cost, SUM(total_tokens) AS tokens
       FROM ai_usage WHERE created_at >= ?`
    ).bind(todaySince).first<{ calls: number; cost: number; tokens: number }>(),
  ]);

  return json({
    source: "d1", period,
    totalDiagnostics: sessions?.total ?? 0,
    activeSessions:   sessions?.active ?? 0,
    avgNetworkScore:  sessions?.avg_score ? Math.round(sessions.avg_score) : 0,
    aiCallsToday:     aiRows?.calls  ?? 0,
    aiCostToday:      aiRows?.cost   ?? 0,
    aiTokensToday:    aiRows?.tokens ?? 0,
  }, 200, env);
}

async function handleDiagnostics(request: Request, env: Env): Promise<Response> {
  const url    = new URL(request.url);
  const period = url.searchParams.get("period") ?? "7d";
  const limit  = Math.min(parseInt(url.searchParams.get("limit") ?? "50"), 200);
  const since  = nowSec() - periodToSeconds(period);

  const rows = await env.DB.prepare(
    `SELECT id, created_at, network_type, status, score,
            download_mbps, upload_mbps, latency_ms, jitter_ms, packet_loss,
            issues, resolved
     FROM diagnostic_sessions WHERE created_at >= ?
     ORDER BY created_at DESC LIMIT ?`
  ).bind(since, limit).all();

  const sessions = (rows.results ?? []).map((r: any) => ({
    ...r,
    issues: JSON.parse(r.issues ?? "[]"),
  }));

  return json({ source: "d1", period, sessions }, 200, env);
}

async function handleAiCost(request: Request, env: Env): Promise<Response> {
  const url    = new URL(request.url);
  const period = url.searchParams.get("period") ?? "7d";
  const since  = nowSec() - periodToSeconds(period);

  const rows = await env.DB.prepare(
    `SELECT model, COUNT(*) AS calls, SUM(total_tokens) AS tokens, SUM(cost_usd) AS cost_usd
     FROM ai_usage WHERE created_at >= ? GROUP BY model ORDER BY calls DESC`
  ).bind(since).all();

  const totals = (rows.results ?? []).reduce(
    (acc: any, r: any) => ({
      calls:  acc.calls  + (r.calls  ?? 0),
      tokens: acc.tokens + (r.tokens ?? 0),
      cost:   acc.cost   + (r.cost_usd ?? 0),
    }),
    { calls: 0, tokens: 0, cost: 0 }
  );

  return json({ source: "d1", period, byModel: rows.results ?? [], totals }, 200, env);
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
  return json({ source: "worker", settings: {} }, 200, env);
}

// --- handlers /ingest ---

// Migration SQL necessaria no D1 para suportar o campo operator:
// ALTER TABLE diagnostic_sessions ADD COLUMN operator TEXT;
async function handleIngestDiagnostic(request: Request, env: Env): Promise<Response> {
  let p: any;
  try { p = await request.json(); } catch { return err("invalid JSON", 400, env); }
  if (!p.id) return err("id obrigatorio", 400, env);

  // Tenta INSERT com campo operator. Se falhar por coluna inexistente (D1 sem migration),
  // re-tenta sem o campo para nao perder o registro.
  try {
    await env.DB.prepare(
      `INSERT OR REPLACE INTO diagnostic_sessions
         (id, created_at, network_type, status, score,
          download_mbps, upload_mbps, latency_ms, jitter_ms, packet_loss, issues, resolved, operator)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)`
    ).bind(
      p.id, p.created_at ?? nowSec(),
      p.network_type ?? "unknown", p.status ?? "unknown", p.score ?? null,
      p.download_mbps ?? null, p.upload_mbps ?? null,
      p.latency_ms ?? null, p.jitter_ms ?? null, p.packet_loss ?? null,
      JSON.stringify(p.issues ?? []),
      p.operator ?? null,
    ).run();
  } catch (e: any) {
    // Fallback: coluna operator ainda nao existe no D1 — inserir sem ela
    if (e?.message?.includes("no column named operator") || e?.message?.includes("table diagnostic_sessions has no column")) {
      await env.DB.prepare(
        `INSERT OR REPLACE INTO diagnostic_sessions
           (id, created_at, network_type, status, score,
            download_mbps, upload_mbps, latency_ms, jitter_ms, packet_loss, issues, resolved)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`
      ).bind(
        p.id, p.created_at ?? nowSec(),
        p.network_type ?? "unknown", p.status ?? "unknown", p.score ?? null,
        p.download_mbps ?? null, p.upload_mbps ?? null,
        p.latency_ms ?? null, p.jitter_ms ?? null, p.packet_loss ?? null,
        JSON.stringify(p.issues ?? []),
      ).run();
    } else {
      throw e;
    }
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

  await env.DB.prepare(
    `INSERT OR REPLACE INTO ai_usage
       (id, session_id, created_at, model,
        prompt_tokens, completion_tokens, total_tokens, cost_usd)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    p.id, p.session_id ?? null, p.created_at ?? nowSec(), p.model,
    prompt, completion, total, cost,
  ).run();

  return json({ ok: true, id: p.id }, 201, env);
}

// --- router ---

type Handler = (req: Request, env: Env) => Promise<Response>;

const ROUTES: Array<{ method: string; pattern: RegExp; handler: Handler }> = [
  { method: "GET",  pattern: /^\/admin\/metrics\/overview$/,                    handler: handleOverview },
  { method: "GET",  pattern: /^\/admin\/metrics\/diagnostics$/,                 handler: handleDiagnostics },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-usage$/,                    handler: handleAiCost },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/status$/,       handler: handleFirebaseStatus },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/analytics$/,    handler: handleFirebaseAnalytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crashlytics$/,  handler: handleFirebaseCrashlytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/versions$/,     handler: handleFirebaseVersions },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crash-issues$/, handler: handleFirebaseCrashIssues },
  { method: "POST", pattern: /^\/admin\/integrations\/firebase\/sync$/,         handler: handleFirebaseSync },
  { method: "GET",  pattern: /^\/admin\/settings$/,                             handler: handleSettings },
  { method: "POST", pattern: /^\/admin\/settings$/,                             handler: handleSettings },
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

    // Rotas /ingest/* — autenticam com INGEST_KEY (scope limitado, vai no APK).
    for (const route of INGEST_ROUTES) {
      if (route.method === request.method && route.pattern.test(url.pathname)) {
        if (!authenticateIngest(request, env)) return err("Unauthorized", 401, env);
        return route.handler(request, env);
      }
    }

    // Rotas /admin/* — autenticam com ADMIN_SECRET (painel web, nao vai no APK).
    if (!authenticate(request, env)) {
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
