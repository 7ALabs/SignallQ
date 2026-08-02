// Dados estruturados (JSON-LD) injetados no HTML inicial por
// `functions/_middleware.ts` (issue #1369 Fase 2). Funções puras — sem
// dependência de DOM/window — pra serem chamáveis tanto do client (se algum
// dia fizer sentido) quanto da Pages Function (runtime de Workers).
//
// Sem sameAs (redes sociais) e sem oferta/preço no /pro: não inventar dado que
// não existe — schema.org errado é pior que ausente (Google ignora ou pune
// markup que não bate com o conteúdo real da página).
const ORGANIZATION_NAME = 'SignallQ'

export function buildOrganizationJsonLd(origin: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: ORGANIZATION_NAME,
    url: origin + '/',
    logo: origin + '/signallq-symbol.png',
  }
}

export function buildWebSiteJsonLd(origin: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: ORGANIZATION_NAME,
    url: origin + '/',
  }
}

export function buildSpeedTestWebApplicationJsonLd(origin: string, description: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebApplication',
    name: 'SignallQ — Teste de velocidade',
    url: origin + '/',
    description,
    applicationCategory: 'UtilitiesApplication',
    operatingSystem: 'Web',
    offers: { '@type': 'Offer', price: '0', priceCurrency: 'BRL' },
  }
}

// Conteúdo editorial (issue #1399) -- diferente das rotas de app puro, essas páginas
// são texto de verdade e se qualificam como Article (headline/data de publicação
// reais, sem sameAs/oferta inventados, mesmo princípio das funções acima).
export function buildArticleJsonLd(origin: string, path: string, headline: string, description: string, datePublished: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Article',
    headline,
    description,
    url: origin + path,
    datePublished,
    dateModified: datePublished,
    author: { '@type': 'Organization', name: ORGANIZATION_NAME },
    publisher: {
      '@type': 'Organization',
      name: ORGANIZATION_NAME,
      logo: { '@type': 'ImageObject', url: origin + '/signallq-symbol.png' },
    },
    mainEntityOfPage: { '@type': 'WebPage', '@id': origin + path },
  }
}

// Ainda consumida por functions/_middleware.ts (Cloudflare Pages Function, fora do escopo desta
// PR de frontend) — não remover sem também atualizar aquele arquivo numa PR de backend própria.
export function buildProSoftwareApplicationJsonLd(origin: string, description: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'SignallQ PRO',
    url: origin + '/pro',
    description,
    applicationCategory: 'BusinessApplication',
    operatingSystem: 'Android',
  }
}
