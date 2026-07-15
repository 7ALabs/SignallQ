package io.signallq.app.core.network.contracts.topologia

/** Origem/natureza do sinal que compõe uma [Evidencia]. */
enum class TipoEvidencia {
    OUI,
    SSID,
    RSSI,
    BANDA,
    CLIENT_SNAPSHOT,
    CORRELACAO,
}
