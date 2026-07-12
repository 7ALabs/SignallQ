package io.signallq.app.ui.component.ads

/**
 * Origem do card de anuncio nativo -- issue #555, feedback do Luiz em 2026-07-12.
 *
 * - [ADMOB]: `native_ad_fallback` puro, sem relacao comercial alem do ad network.
 *   Unica variante usada de fato nesta entrega -- AdMob e a fonte de receita
 *   principal, sempre ativa nas 4 telas.
 * - [PARTNER]: `affiliate_product`/`partner_offer` do `coreRecommendation`, quando o
 *   achado do diagnostico casa com uma oferta curada. O componente ja aceita esta
 *   variante (badge "Parceiro", tom accentBlue), mas nao ha catalogo de parceiros
 *   real ainda -- fora do escopo desta entrega (ver plano da #555).
 */
enum class NativeAdSource {
    ADMOB,
    PARTNER,
}
