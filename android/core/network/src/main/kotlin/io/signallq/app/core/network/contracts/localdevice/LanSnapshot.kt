package io.signallq.app.core.network.contracts.localdevice

/** Configuração da rede local (LAN) reportada pelo equipamento. */
data class LanSnapshot(
    val ipRoteador: String?,
    val mascara: String?,
    val dhcpHabilitado: Boolean?,
    val faixaDhcpInicio: String?,
    val faixaDhcpFim: String?,
)
