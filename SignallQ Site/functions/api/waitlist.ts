// Pages Function: proxy server-side da lista de espera (POST /ingest/waitlist
// no signallq-admin-worker). Mesmo padrão de functions/api/track.ts — nunca
// expõe a INGEST_KEY ao navegador do visitante; ela vive só como secret do
// projeto Cloudflare Pages (`wrangler pages secret put SITE_INGEST_KEY
// --project-name signallq`).
const ADMIN_WORKER_URL = 'https://signallq-admin.giammattey-luiz.workers.dev'

interface Env {
  SITE_INGEST_KEY?: string
}

export async function onRequestPost(context: { request: Request; env: Env }) {
  const { request, env } = context

  if (!env.SITE_INGEST_KEY) {
    // Sem a secret configurada, sinaliza 501 pra quem inspecionar a network —
    // não finge sucesso, mas também não quebra a experiência (o dialog trata
    // o erro e mostra a mensagem de "tente de novo").
    return new Response(JSON.stringify({ ok: false, reason: 'SITE_INGEST_KEY não configurada' }), {
      status: 501,
      headers: { 'content-type': 'application/json' },
    })
  }

  const body = await request.arrayBuffer()

  const workerResponse = await fetch(`${ADMIN_WORKER_URL}/ingest/waitlist`, {
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
