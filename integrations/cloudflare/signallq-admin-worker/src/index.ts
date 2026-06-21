// =============================================================================
// SignallQ Admin API Worker
// Substitui: esquilo-invest-web.giammattey-luiz.workers.dev
// =============================================================================
// Serve os endpoints consumidos pelo painel React em SignallQ Admin/.
// Todas as rotas exigem Bearer token (ADMIN_SECRET) no header Authorization.
// Firebase Analytics e Crashlytics são consultados via REST API com service
// account. D1 e KV ficam comentados até os recursos serem criados no dashboard.
// =============================================================================

export interface Env {
  ALLOWED_ORIGIN: string;
  FIREBASE_PROJECT_ID: string;
  FIREBASE_GA4_PROPERTY_ID: string;
  // Secrets (npx wrangler secret put):
  ADMIN_SECRET: string;
  FIREBASE_CLIENT_EMAIL: string;
  FIREBASE_PRIVATE_KEY: string; // PEM, com \n escapados
  // Futuros:
  // DB: D1Database;
  // CACHE: KVNamespace;
}

// ---------------------------------------------------------------------------
// CORS
// ---------------------------------------------------------------------------

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

function error(message: string, status: number, env: Env): Response {
  return json({ error: message }, status, env);
}

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------

function authenticate(request: Request, env: Env): boolean {
  const auth = request.headers.get("Authorization") ?? "";
  const [scheme, token] = auth.split(" ");
  return scheme === "Bearer" && token === env.ADMIN_SECRET;
}

// ---------------------------------------------------------------------------
// Firebase REST helpers
// ---------------------------------------------------------------------------

async function getFirebaseAccessToken(env: Env): Promise<string> {
  // Gera JWT assinado com service account para autenticar na API do Firebase.
  // Scope: Analytics Data API + Firebase Management API.
  const now = Math.floor(Date.now() / 1000);
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

  // Importa a chave RSA privada
  const keyData = privateKey
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");

  const binaryKey = Uint8Array.from(atob(keyData), (c) => c.charCodeAt(0));

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryKey,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  // Monta o JWT
  const header = btoa(JSON.stringify({ alg: "RS256", typ: "JWT" }))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
  const body = btoa(JSON.stringify(payload))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");

  const sigInput = new TextEncoder().encode(`${header}.${body}`);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, sigInput);
  const sig = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");

  const jwtAssertion = `${header}.${body}.${sig}`;

  // Troca o JWT por access token OAuth2
  const tokenResp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwtAssertion,
    }),
  });

  const tokenData = (await tokenResp.json()) as { access_token: string };
  return tokenData.access_token;
}

// ---------------------------------------------------------------------------
// Handlers de cada rota
// ---------------------------------------------------------------------------

async function handleOverview(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const period = url.searchParams.get("period") ?? "7d";
  const environment = url.searchParams.get("environment") ?? "production";

  // TODO: substituir por consulta real ao D1 quando o banco for criado
  // e por métricas reais do Firebase Analytics quando o service account for configurado.
  // Por enquanto retorna estrutura compatível com o mock do painel.
  const stub = {
    source: "worker_stub",
    period,
    environment,
    totalDiagnostics: 0,
    activeSessions: 0,
    aiCallsToday: 0,
    aiCostToday: 0,
    avgNetworkScore: 0,
    crashFreeUsers: 0,
    message: "Configure FIREBASE_CLIENT_EMAIL e FIREBASE_PRIVATE_KEY via wrangler secret put para ativar dados reais.",
  };

  return json(stub, 200, env);
}

async function handleFirebaseAnalytics(request: Request, env: Env): Promise<Response> {
  const hasCredentials =
    env.FIREBASE_CLIENT_EMAIL &&
    env.FIREBASE_CLIENT_EMAIL !== "" &&
    env.FIREBASE_PRIVATE_KEY &&
    env.FIREBASE_PRIVATE_KEY !== "";

  if (!hasCredentials) {
    return json(
      {
        source: "no_credentials",
        activeUsersToday: 0,
        sessionsToday: 0,
        message: "Configurar FIREBASE_CLIENT_EMAIL e FIREBASE_PRIVATE_KEY.",
      },
      200,
      env
    );
  }

  try {
    const token = await getFirebaseAccessToken(env);

    // GA4 Data API — DAU e sessões dos últimos 7 dias
    // Usa FIREBASE_GA4_PROPERTY_ID (número puro), não o project_id do Firebase
    const propertyId = env.FIREBASE_GA4_PROPERTY_ID || env.FIREBASE_PROJECT_ID;
    const analyticsResp = await fetch(
      `https://analyticsdata.googleapis.com/v1beta/properties/${propertyId}:runReport`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          dateRanges: [{ startDate: "7daysAgo", endDate: "today" }],
          metrics: [
            { name: "activeUsers" },
            { name: "sessions" },
            { name: "crashAffectedUsers" },
          ],
          dimensions: [{ name: "date" }],
        }),
      }
    );

    const data = await analyticsResp.json();
    return json({ source: "firebase_analytics", data }, 200, env);
  } catch (err) {
    return json({ source: "error", message: String(err) }, 500, env);
  }
}

async function handleFirebaseCrashlytics(request: Request, env: Env): Promise<Response> {
  // Crashlytics não tem REST API pública — dados vêm do Firebase Management API
  // ou via exportação para BigQuery. Retorna stub até integração ser definida.
  return json(
    {
      source: "crashlytics_stub",
      unresolvedCrashes: 0,
      crashFreeUsersPercentage: 100,
      message: "Crashlytics requer exportação BigQuery ou Firebase Management API.",
    },
    200,
    env
  );
}

async function handleDiagnostics(request: Request, env: Env): Promise<Response> {
  // Futuramente: SELECT * FROM diagnostics ORDER BY created_at DESC LIMIT 50
  // usando o binding DB (D1).
  return json(
    {
      source: "d1_stub",
      sessions: [],
      message: "Criar D1 'signallq-admin-db' e descomentar binding no wrangler.toml.",
    },
    200,
    env
  );
}

async function handleAiCost(request: Request, env: Env): Promise<Response> {
  // Futuramente: consulta à tabela ai_usage no D1,
  // populada pelo AI Diagnosis Worker a cada inferência.
  return json(
    {
      source: "d1_stub",
      totalTokensToday: 0,
      totalCostUsd: 0,
      message: "AI Diagnosis Worker ainda não grava no D1. Integrar após criar o banco.",
    },
    200,
    env
  );
}

async function handleFirebaseStatus(_request: Request, env: Env): Promise<Response> {
  return json(
    {
      source: "worker",
      projectId: env.FIREBASE_PROJECT_ID,
      status: "connected",
      hasCredentials: !!(env.FIREBASE_CLIENT_EMAIL && env.FIREBASE_PRIVATE_KEY),
    },
    200,
    env
  );
}

async function handleFirebaseVersions(request: Request, env: Env): Promise<Response> {
  // Futuramente: busca do Firebase Crashlytics via Management API ou exportação BigQuery
  return json(
    { source: "stub", versions: [], message: "Requer exportação BigQuery do Crashlytics." },
    200,
    env
  );
}

async function handleFirebaseCrashIssues(request: Request, env: Env): Promise<Response> {
  return json(
    { source: "stub", issues: [], message: "Requer exportação BigQuery do Crashlytics." },
    200,
    env
  );
}

async function handleFirebaseSync(_request: Request, env: Env): Promise<Response> {
  return json(
    {
      jobId: `job_fb_${Date.now().toString(36)}`,
      status: "started",
      startedAt: new Date().toISOString(),
      message: "Sincronização iniciada. Dados serão atualizados em até 5 minutos.",
    },
    200,
    env
  );
}

async function handleSettings(request: Request, env: Env): Promise<Response> {
  if (request.method === "GET") {
    return json({ source: "worker", settings: {} }, 200, env);
  }
  // POST — salvar configurações no KV futuramente
  return json({ success: true, message: "KV não configurado ainda." }, 200, env);
}

// ---------------------------------------------------------------------------
// Router principal
// ---------------------------------------------------------------------------

const ROUTES: Array<{
  method: string;
  pattern: RegExp;
  handler: (req: Request, env: Env) => Promise<Response>;
}> = [
  // Métricas agregadas
  { method: "GET",  pattern: /^\/admin\/metrics\/overview$/,              handler: handleOverview },
  { method: "GET",  pattern: /^\/admin\/metrics\/diagnostics$/,           handler: handleDiagnostics },
  { method: "GET",  pattern: /^\/admin\/metrics\/ai-usage$/,              handler: handleAiCost },
  // Firebase (dados buscados pelo worker, credenciais ficam aqui como secrets)
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/status$/, handler: handleFirebaseStatus },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/analytics$/,    handler: handleFirebaseAnalytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crashlytics$/,  handler: handleFirebaseCrashlytics },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/versions$/,     handler: handleFirebaseVersions },
  { method: "GET",  pattern: /^\/admin\/integrations\/firebase\/crash-issues$/, handler: handleFirebaseCrashIssues },
  { method: "POST", pattern: /^\/admin\/integrations\/firebase\/sync$/,         handler: handleFirebaseSync },
  // Configurações
  { method: "GET",  pattern: /^\/admin\/settings$/,                       handler: handleSettings },
  { method: "POST", pattern: /^\/admin\/settings$/,                       handler: handleSettings },
];

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // Preflight CORS
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(env) });
    }

    // Health check público — usado para checar se o worker está ativo
    const url = new URL(request.url);
    if (url.pathname === "/health") {
      return json({ status: "ok", worker: "signallq-admin-worker" }, 200, env);
    }

    // Todas as outras rotas exigem autenticação
    if (!authenticate(request, env)) {
      return error("Unauthorized", 401, env);
    }

    for (const route of ROUTES) {
      if (route.method === request.method && route.pattern.test(url.pathname)) {
        return route.handler(request, env);
      }
    }

    return error("Not found", 404, env);
  },
};
