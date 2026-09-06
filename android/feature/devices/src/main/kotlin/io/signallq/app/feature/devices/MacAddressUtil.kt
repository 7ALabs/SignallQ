package io.signallq.app.feature.devices

/**
 * GH#1217 item 5/9 — endereço MAC "localmente administrado" (bit U/L, segundo bit menos
 * significativo do primeiro octeto = 1) nunca corresponde a um OUI real atribuído pela
 * IEEE — é o esquema usado pelo Android 10+ pra MAC randomizado por rede. Consultar
 * [OuiCatalog] com um MAC desses pode, em tese, colidir com uma entrada do catálogo por
 * acaso (o catálogo é indexado só pelos 3 primeiros octetos) e apresentar um fabricante
 * como se fosse confirmado, quando na verdade não há nenhuma evidência real de fabricante
 * nesse MAC.
 */
internal object MacAddressUtil {
    fun ehLocalmenteAdministrado(mac: String): Boolean {
        val primeiroOcteto =
            mac
                .split(":", "-")
                .firstOrNull()
                ?.let { runCatching { it.toInt(16) }.getOrNull() }
                ?: return false
        return (primeiroOcteto and 0x02) != 0
    }
}
