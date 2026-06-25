const WORKER_URL = 'https://signallq-admin.giammattey-luiz.workers.dev'

export async function onRequest(context: EventContext<Record<string, unknown>, string, Record<string, unknown>>) {
  const { request } = context
  const url = new URL(request.url)

  // Strip /api prefix to get the worker path
  const workerPath = url.pathname.replace(/^\/api/, '') || '/'
  const target = `${WORKER_URL}${workerPath}${url.search}`

  const body = ['GET', 'HEAD'].includes(request.method) ? null : await request.arrayBuffer()

  const workerResponse = await fetch(target, {
    method: request.method,
    headers: request.headers,
    body,
  })

  const newHeaders = new Headers(workerResponse.headers)

  // Rewrite Set-Cookie para o domínio correto (pages.dev, não workers.dev)
  const setCookie = workerResponse.headers.get('set-cookie')
  if (setCookie) {
    const tokenMatch = setCookie.match(/session=([^;]+)/)
    const maxAgeMatch = setCookie.match(/Max-Age=(\d+)/i)
    if (tokenMatch) {
      const maxAge = maxAgeMatch ? maxAgeMatch[1] : '604800'
      newHeaders.set(
        'set-cookie',
        `session=${tokenMatch[1]}; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=${maxAge}`
      )
    }
  }

  // Remove CORS headers (desnecessários para same-origin)
  newHeaders.delete('access-control-allow-origin')
  newHeaders.delete('access-control-allow-methods')
  newHeaders.delete('access-control-allow-headers')
  newHeaders.delete('access-control-allow-credentials')
  newHeaders.delete('access-control-max-age')

  return new Response(workerResponse.body, {
    status: workerResponse.status,
    statusText: workerResponse.statusText,
    headers: newHeaders,
  })
}
