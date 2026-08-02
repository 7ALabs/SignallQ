// Next.js Route Handler: proxy server-side da lista de espera (POST
// /ingest/waitlist no signallq-admin-worker). Substitui a antiga Cloudflare
// Pages Function (`functions/api/waitlist.ts`, mantida só até o desligamento
// do Cloudflare Pages) — Vercel não executa `functions/api/*`. Mesmo padrão
// de src/app/api/track/route.ts: nunca expõe a INGEST_KEY ao navegador do
// visitante; ela vive só como env var server-side do projeto Vercel
// (`SITE_INGEST_KEY`).
const ADMIN_WORKER_URL = 'https://signallq-admin.giammattey-luiz.workers.dev'

export async function POST(request: Request) {
  const ingestKey = process.env.SITE_INGEST_KEY

  if (!ingestKey) {
    // Sem a env var configurada, sinaliza 501 pra quem inspecionar a network —
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
      authorization: `Bearer ${ingestKey}`,
    },
    body,
  })

  return new Response(workerResponse.body, {
    status: workerResponse.status,
    headers: { 'content-type': 'application/json' },
  })
}
