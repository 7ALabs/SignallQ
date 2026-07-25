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
