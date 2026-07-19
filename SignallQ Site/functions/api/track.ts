// Pages Function: proxy server-side do pipeline de analytics do
// signallq-admin-worker (POST /ingest/analytics). Segue o mesmo padrão de
// "SignallQ Admin/functions/api/[[path]].ts" — nunca expõe a INGEST_KEY ao
// navegador do visitante; ela vive só como secret do projeto Cloudflare Pages
// (`wrangler pages secret put SITE_INGEST_KEY --project-name signallq`).
//
// Pendência de infra (não é código): o INGEST_KEY do site pode ser o mesmo do
// app Android (retrocompat, authenticateIngest aceita INGEST_KEY OU
// ADMIN_SECRET) ou um novo com escopo próprio — decisão/config do Luiz, fora
// do escopo desta implementação.
const ADMIN_WORKER_URL = 'https://signallq-admin.giammattey-luiz.workers.dev'

interface Env {
  SITE_INGEST_KEY?: string
}

export async function onRequestPost(context: { request: Request; env: Env }) {
  const { request, env } = context

  if (!env.SITE_INGEST_KEY) {
    // Sem a secret configurada, falha de forma silenciosa para o cliente
    // (telemetria nunca pode quebrar a experiência do visitante) mas sinaliza
    // 501 para quem inspecionar a network — não finge sucesso.
    return new Response(JSON.stringify({ ok: false, reason: 'SITE_INGEST_KEY não configurada' }), {
      status: 501,
      headers: { 'content-type': 'application/json' },
    })
  }

  const body = await request.arrayBuffer()

  const workerResponse = await fetch(`${ADMIN_WORKER_URL}/ingest/analytics`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      authorization: `Bearer ${env.SITE_INGEST_KEY}`,
    },
    body,
  })

  return new Response(workerResponse.body, {
    status: workerResponse.status,
    headers: { 'content-type': 'application/json' },
  })
}
