package io.signallq.app.core.network

data class WifiLinkSnapshot(
    val ssid: String?,
    val bssid: String?,
    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,
    val frequenciaMhz: Int?,
    val padraoWifi: String?,
    /** Suporte do aparelho a 5GHz (WifiManager.is5GHzBandSupported). Null quando
     *  a leitura falhou (ex.: sem permissao/servico indisponivel) — nesse caso o
     *  RecommendationEngine trata como "desconhecido", nao "sem suporte". */
    val is5GhzCapable: Boolean? = null,
)

