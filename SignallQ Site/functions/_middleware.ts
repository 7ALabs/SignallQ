// Middleware raiz de todas as Pages Functions do projeto `signallq` — inclusive
// as do embed /leste (Agente Virtual, ver public/_redirects). Dois propósitos,
// ambos da issue #1369:
//
// Fase 1 — corrigir o soft-404 do SPA fallback. `public/_redirects` reescreve
// qualquer rota pra /index.html com status 200 (necessário pro React Router
// funcionar em recarregamento direto), o que faz rota inexistente responder 200
// em vez de 404 real -- ruim pra rastreamento/indexação.
//
// Fase 2 — injetar title/description/canonical/OG/JSON-LD corretos no HTML
// inicial de cada rota conhecida, via HTMLRewriter (streaming, sem renderizar
// React em lugar nenhum). Hoje o `index.html` estático só tem um <title>
// genérico -- rastreador que não roda JS (e a etapa inicial do próprio
// Googlebot) via só isso. Catálogo de meta por rota vive em
// `src/lib/pageMetaCatalog.ts`, consumido também pelo client (useDocumentMeta)
// -- uma única fonte de verdade, sem string duplicada entre os dois.
//
// Como funciona: Pages Functions roda antes de `_redirects` ser aplicado. Rota
// conhecida do React Router (KNOWN_ROUTES, espelha as rotas de src/App.tsx e as
// chaves de PAGE_META) chama next() pra obter o shell da SPA e reescreve o
// <head> com os metadados daquela rota. Path desconhecido também chama next(),
// reescreve com os metadados de "não encontrada" e devolve com status 404 real
// -- o React Router ainda monta e mostra a NotFoundPage (UX igual), só o
// código HTTP e os metadados mudam.
import { NOT_FOUND_META, PAGE_META } from '../src/lib/pageMetaCatalog'
import type { PageMeta } from '../src/lib/seo'
import {
  buildArticleJsonLd,
  buildOrganizationJsonLd,
  buildProSoftwareApplicationJsonLd,
  buildSpeedTestWebApplicationJsonLd,
  buildWebSiteJsonLd,
} from '../src/lib/structuredData'

const HAS_FILE_EXTENSION = /\.[a-zA-Z0-9]+$/

// Data de publicação das páginas editoriais de long-tail SEO (issue #1399) --
// usada só pro Article JSON-LD (datePublished/dateModified), não é dado inventado.
const LONGTAIL_ARTICLE_PUBLISHED_AT = '2026-07-25'

// Token público emitido pelo Google Search Console para verificar a propriedade
// https://signallq.pages.dev/. Fica no HTML inicial, entregue pelo Worker, para
// que a validação não dependa da hidratação do React.
const GOOGLE_SITE_VERIFICATION_CONTENT = 'Kb_Flz9uN3gJfrushxDl3GbMWkfkifjlz-x5JC9rQP0'

// Identifica a conta que pode monetizar este domínio no Google AdSense. A tag
// verifica a propriedade sem carregar anúncios nem scripts de publicidade.
const GOOGLE_ADSENSE_ACCOUNT_ID = 'ca-pub-5542349230926522'

function escapeHtmlAttr(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

// JSON-LD vai dentro de <script>; escapar "<" evita que um valor de campo
// (nenhum hoje, mas nenhum dos textos é hardcoded a prova de futuro) feche a
// tag prematuramente com "</script>".
function escapeJsonForScriptTag(json: string): string {
  return json.replace(/</g, '\\u003c')
}

// Organization/WebSite em toda rota conhecida (identidade da marca); WebApplication
// só em / (o teste em si) e SoftwareApplication só em /pro (vitrine do produto) --
// são as únicas rotas que de fato descrevem uma aplicação/produto, ver avaliação
// técnica do Camilo em #1369.
function structuredDataFor(path: string, meta: PageMeta, origin: string): unknown[] {
  const data: unknown[] = [buildOrganizationJsonLd(origin), buildWebSiteJsonLd(origin)]
  if (path === '/') data.push(buildSpeedTestWebApplicationJsonLd(origin, meta.description))
  if (path === '/pro') data.push(buildProSoftwareApplicationJsonLd(origin, meta.description))
  if (path === '/internet-boa-mas-travando' || path === '/lag-em-jogos-online') {
    data.push(buildArticleJsonLd(origin, path, meta.title, meta.description, LONGTAIL_ARTICLE_PUBLISHED_AT))
  }
  return data
}

function buildHeadInjectionHtml(meta: PageMeta, origin: string, structuredData: unknown[]): string {
  const url = origin + meta.path
  const robots = meta.robots ?? 'index,follow'
  const image = origin + '/signallq-symbol.png'

  const tags = [
    `<meta name="google-site-verification" content="${GOOGLE_SITE_VERIFICATION_CONTENT}">`,
    `<meta name="google-adsense-account" content="${GOOGLE_ADSENSE_ACCOUNT_ID}">`,
    `<meta name="description" content="${escapeHtmlAttr(meta.description)}">`,
    `<meta name="robots" content="${escapeHtmlAttr(robots)}">`,
    `<link rel="canonical" href="${escapeHtmlAttr(url)}">`,
    `<meta property="og:title" content="${escapeHtmlAttr(meta.title)}">`,
    `<meta property="og:description" content="${escapeHtmlAttr(meta.description)}">`,
    '<meta property="og:type" content="website">',
    `<meta property="og:url" content="${escapeHtmlAttr(url)}">`,
    `<meta property="og:image" content="${escapeHtmlAttr(image)}">`,
    '<meta name="twitter:card" content="summary_large_image">',
    ...structuredData.map((data) => `<script type="application/ld+json">${escapeJsonForScriptTag(JSON.stringify(data))}</script>`),
  ]

  return tags.join('\n')
}

// Reescreve o <title> (texto, HTMLRewriter escapa por padrão) e injeta o resto
// dos metadados como último filho de <head> -- não existe seletor CSS pra
// "elemento que ainda não existe", então tags novas entram via append no head.
function injectMeta(response: Response, meta: PageMeta, origin: string, structuredData: unknown[]): Response {
  const headHtml = buildHeadInjectionHtml(meta, origin, structuredData)
  return new HTMLRewriter()
    .on('title', {
      element(element) {
        element.setInnerContent(meta.title)
      },
    })
    .on('head', {
      element(element) {
        element.append(headHtml, { html: true })
      },
    })
    .transform(response)
}

export const onRequest: PagesFunction = async (context) => {
  const { request, next } = context
  const url = new URL(request.url)
  const path = url.pathname

  // API do site (functions/api/*) e o embed /leste (tenant piloto, própria árvore
  // de rotas/arquivos) não passam pela lógica de meta/404 do SPA -- seguem intactos.
  if (path.startsWith('/api/') || path.startsWith('/leste/')) {
    return next()
  }

  // Asset estático real (ícone, manifest, sitemap, robots.txt, fontes etc.) --
  // deixa o roteamento normal de arquivo estático resolver, sem reescrita de HTML.
  if (HAS_FILE_EXTENSION.test(path)) {
    return next()
  }

  const meta = PAGE_META[path]
  if (meta) {
    const shell = await next()
    return injectMeta(shell, meta, url.origin, structuredDataFor(path, meta, url.origin))
  }

  const shell = await next()
  const rewritten = injectMeta(shell, NOT_FOUND_META, url.origin, [])
  return new Response(rewritten.body, {
    status: 404,
    statusText: 'Not Found',
    headers: rewritten.headers,
  })
}
