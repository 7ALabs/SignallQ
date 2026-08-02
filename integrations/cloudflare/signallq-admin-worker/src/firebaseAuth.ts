// Troca de credencial de service account (JWT assinado com RS256) por access token OAuth,
// usada por todas as integrações Firebase deste worker (GA4/BigQuery, Firebase Management,
// App Check, App Distribution, FCM, Remote Config — leitura legada em GET
// /admin/integrations/firebase/remote-config/* e o backend admin novo em
// /admin/firebase/remote-config/* — issue #1478).
//
// Extraído de src/index.ts (groundwork #1478, 2026-07-26) sem alterar comportamento — mesma
// função, mesmo escopo, só movida pra módulo próprio pra não inchar ainda mais um arquivo já
// com 5000+ linhas (ver .claude/rules/higiene-e-padronizacao-repositorio.md, seção 7).
//
// Scope "https://www.googleapis.com/auth/cloud-platform" já cobre a Firebase Remote Config
// REST API (confirmado via chamada real em 2026-07-24, ver docs_ai/decisions/
// DECISAO_STATUS_CREDENCIAIS_GOOGLE_PLAY_FIREBASE_2026-07-24.md) — não estreitado aqui pra não
// regredir nenhuma integração já em produção que depende desta mesma função.

export interface FirebaseServiceAccountEnv {
  FIREBASE_CLIENT_EMAIL: string;
  FIREBASE_PRIVATE_KEY: string;
}

function nowSec(): number {
  return Math.floor(Date.now() / 1000);
}

export async function getFirebaseAccessToken(env: FirebaseServiceAccountEnv): Promise<string> {
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
