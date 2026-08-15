package io.signallq.app.core.network.contracts.topologia

/** De onde veio o conjunto de dados usado para produzir uma [ClassificacaoTopologia]. */
enum class OrigemDados {
    SCAN_WIFI_PASSIVO,
    SCAN_LAN_ATIVO,
    GATEWAY_DIRETO,
    CORRELACAO,
}
