// Next.js Route Handler: proxy server-side do pipeline de analytics do
// signallq-admin-worker (POST /ingest/analytics). Substitui a antiga Cloudflare
// Pages Function (`functions/api/track.ts`, mantida só até o desligamento do
// Cloudflare Pages) — Vercel não executa `functions/api/*`. Nunca expõe a
// INGEST_KEY ao navegador do visitante; ela vive só como env var server-side
// do projeto Vercel (`SITE_INGEST_KEY`).
//
// Pendência de infra (não é código): o INGEST_KEY do site pode ser o mesmo do
// app Android (retrocompat, authenticateIngest aceita INGEST_KEY OU
// ADMIN_SECRET) ou um novo com escopo próprio — decisão/config do Luiz, fora
// do escopo desta implementação.
const ADMIN_WORKER_URL = 'https://signallq-admin.giammattey-luiz.workers.dev'

export async function POST(request: Request) {
  const ingestKey = process.env.SITE_INGEST_KEY

  if (!ingestKey) {
    // Sem a env var configurada, falha de forma silenciosa para o cliente
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
      authorization: `Bearer ${ingestKey}`,
    },
    body,
  })

  return new Response(workerResponse.body, {
    status: workerResponse.status,
    headers: { 'content-type': 'application/json' },
  })
}
