// Middleware raiz de todas as Pages Functions do projeto `signallq` — inclusive
// as do embed /leste (Agente Virtual, ver public/_redirects). Único propósito:
// corrigir o soft-404 do SPA fallback (issue #1369). `public/_redirects` reescreve
// qualquer rota pra /index.html com status 200 (necessário pro React Router
// funcionar em recarregamento direto), o que faz rota inexistente responder 200
// em vez de 404 real -- ruim pra rastreamento/indexação.
//
// Como funciona: Pages Functions roda antes de `_redirects` ser aplicado. Rota
// conhecida do React Router (KNOWN_ROUTES, espelha as rotas de src/App.tsx) e
// qualquer path com extensão de arquivo (asset estático) seguem o fluxo normal
// via next(). Path desconhecido também chama next() pra obter o shell da SPA
// (mesmo HTML que /index.html serviria), mas devolve com status 404 real --
// o React Router ainda monta e mostra a NotFoundPage (UX igual), só o código
// HTTP muda.
const KNOWN_ROUTES = new Set(['/', '/pro', '/historico', '/quem-somos', '/privacidade', '/termos'])

const HAS_FILE_EXTENSION = /\.[a-zA-Z0-9]+$/

export const onRequest: PagesFunction = async (context) => {
  const { request, next } = context
  const url = new URL(request.url)
  const path = url.pathname

  // API do site (functions/api/*) e o embed /leste (tenant piloto, própria árvore
  // de rotas/arquivos) não passam pela lógica de 404 do SPA -- seguem intactos.
  if (path.startsWith('/api/') || path.startsWith('/leste/')) {
    return next()
  }

  // Asset estático real (ícone, manifest, sitemap, robots.txt, fontes etc.) --
  // deixa o roteamento normal de arquivo estático resolver.
  if (HAS_FILE_EXTENSION.test(path)) {
    return next()
  }

  if (KNOWN_ROUTES.has(path)) {
    return next()
  }

  const shell = await next()
  return new Response(shell.body, {
    status: 404,
    statusText: 'Not Found',
    headers: shell.headers,
  })
}
