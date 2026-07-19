// Metadados por página (title, description, Open Graph, canonical). Porte do
// shared/seo.js do protótipo — mesma lógica de upsert via DOM, chamada a
// partir de um hook React (useDocumentMeta) em vez de componentDidMount.
export interface PageMeta {
  title: string
  description: string
  path: string
}

export function applyPageMeta({ title, description, path }: PageMeta) {
  if (typeof document === 'undefined') return
  document.title = title
  const origin = typeof location !== 'undefined' ? location.origin : ''
  const url = origin ? origin + path : path

  const upsert = (selector: string, attrs: Record<string, string>) => {
    let el = document.head.querySelector(selector)
    if (!el) {
      el = document.createElement(selector.startsWith('link') ? 'link' : 'meta')
      document.head.appendChild(el)
    }
    Object.entries(attrs).forEach(([k, v]) => el!.setAttribute(k, v))
  }

  upsert('meta[name="description"]', { name: 'description', content: description })
  upsert('meta[property="og:title"]', { property: 'og:title', content: title })
  upsert('meta[property="og:description"]', { property: 'og:description', content: description })
  upsert('meta[property="og:type"]', { property: 'og:type', content: 'website' })
  upsert('meta[property="og:url"]', { property: 'og:url', content: url })
  upsert('meta[property="og:image"]', { property: 'og:image', content: `${origin}/signallq-symbol.png` })
  upsert('meta[name="twitter:card"]', { name: 'twitter:card', content: 'summary' })
  upsert('link[rel="canonical"]', { rel: 'canonical', href: url })
}
