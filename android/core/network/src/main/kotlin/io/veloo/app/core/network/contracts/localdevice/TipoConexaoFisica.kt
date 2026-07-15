package io.signallq.app.core.network.contracts.localdevice

/**
 * Tipo de conexão física normalizado a partir do `tipoConexao` (string crua
 * reportada pelo firmware do equipamento). A normalização acontece dentro da
 * camada do driver (ex.: `NokiaModemParser`) — a string crua do firmware
 * nunca deve vazar para fora dessa camada como identidade de conexão.
 */
enum class TipoConexaoFisica {
    ETHERNET,
    WIFI,
    DESCONHECIDO,
}
