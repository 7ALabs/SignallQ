// Cliente do catálogo de anúncios locais (house ads) — issue #1402/#1403.
// Consumido pelo AdBanner quando o AdSense não preenche o slot (no-fill) ou não
// está configurado. Falha de rede/contrato nunca pode quebrar a UI — sempre
// resolve para `null`, deixando o AdBanner cair para o placeholder honesto atual.
//
// Contrato de `GET /local-ads` (público, só ativos) implementado pelo Camilo em
// signallq-admin-worker/src/index.ts (`handlePublicLocalAds`) — campos em
// camelCase, sem `active`/timestamps (já filtrado pela query).

import { LOCAL_ADS_ENDPOINT } from './config'

export interface AnuncioLocal {
  id: string
  title: string
  description: string
  ctaLabel: string
  targetUrl: string
}

interface LocalAdsResponse {
  ads?: unknown
}

function ehAnuncioLocalValido(valor: unknown): valor is AnuncioLocal {
  if (!valor || typeof valor !== 'object') return false
  const anuncio = valor as Record<string, unknown>
  return (
    typeof anuncio.id === 'string' &&
    typeof anuncio.title === 'string' &&
    typeof anuncio.description === 'string' &&
    typeof anuncio.ctaLabel === 'string' &&
    typeof anuncio.targetUrl === 'string'
  )
}

/** Busca o catálogo de anúncios locais ativos. Nunca lança — retorna [] em qualquer falha. */
export async function buscarAnunciosLocais(signal?: AbortSignal): Promise<AnuncioLocal[]> {
  if (!LOCAL_ADS_ENDPOINT) return []

  try {
    const resposta = await fetch(LOCAL_ADS_ENDPOINT, { signal })
    if (!resposta.ok) return []

    const corpo = (await resposta.json()) as LocalAdsResponse
    if (!Array.isArray(corpo.ads)) return []

    return corpo.ads.filter(ehAnuncioLocalValido)
  } catch {
    return []
  }
}

/** Sorteia um item do catálogo. Retorna `null` se a lista estiver vazia. */
export function sortearAnuncioLocal(anuncios: AnuncioLocal[]): AnuncioLocal | null {
  if (anuncios.length === 0) return null
  const indice = Math.floor(Math.random() * anuncios.length)
  return anuncios[indice]
}
