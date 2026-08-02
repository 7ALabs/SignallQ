// Metadados por página (title, description, Open Graph, canonical). Porte do
// shared/seo.js do protótipo — mesma lógica de upsert via DOM, chamada a
// partir de um hook React (useDocumentMeta) em vez de componentDidMount.
//
// Limitação conhecida (issue #1369): esse upsert roda depois do JS montar —
// o HTML inicial servido pelo Cloudflare Pages não carrega esses metadados
// (SPA client-side puro, sem SSR/pre-render). Rastreadores que não executam
// JS (e a etapa inicial de rastreamento do próprio Google) veem só o
// <title> genérico do index.html. Fase 2 da issue avalia pre-render/SSR
// pra resolver isso na raiz — este arquivo não muda até essa decisão.
export interface PageMeta {
  title: string
  description: string
  path: string
  /** 'index,follow' por padrão — usar 'noindex,follow' pra página cujo conteúdo
   * depende de dado local do visitante (ex. histórico) ou não deve ser indexada. */
  robots?: string
  /** Caminho absoluto (a partir da raiz) de uma imagem Open Graph específica
   * desta rota — ex. '/og/teste.png'. Sem isso, usa `signallq-symbol.png`. */
  ogImage?: string
}

export function applyPageMeta({ title, description, path, robots = 'index,follow', ogImage }: PageMeta) {
  if (typeof document === 'undefined') return
  document.title = title
  const origin = typeof location !== 'undefined' ? location.origin : ''
  const url = origin ? origin + path : path
  const image = origin + (ogImage ?? '/signallq-symbol.png')

  const upsert = (selector: string, attrs: Record<string, string>) => {
    let el = document.head.querySelector(selector)
    if (!el) {
      el = document.createElement(selector.startsWith('link') ? 'link' : 'meta')
      document.head.appendChild(el)
    }
    Object.entries(attrs).forEach(([k, v]) => el!.setAttribute(k, v))
  }

  upsert('meta[name="description"]', { name: 'description', content: description })
  upsert('meta[name="robots"]', { name: 'robots', content: robots })
  upsert('meta[property="og:title"]', { property: 'og:title', content: title })
  upsert('meta[property="og:description"]', { property: 'og:description', content: description })
  upsert('meta[property="og:type"]', { property: 'og:type', content: 'website' })
  upsert('meta[property="og:url"]', { property: 'og:url', content: url })
  upsert('meta[property="og:image"]', { property: 'og:image', content: image })
  upsert('meta[name="twitter:card"]', { name: 'twitter:card', content: 'summary_large_image' })
  upsert('link[rel="canonical"]', { rel: 'canonical', href: url })
}
