package io.signallq.app.core.network.contracts.localdevice

/**
 * Um dispositivo cliente reportado pelo próprio equipamento local (não pelo
 * scanner ativo do Android) — tende a ser mais estável que descoberta via
 * ARP/SSDP, conforme observado no field-map do TP-Link (`onemesh_network`,
 * `access_devices_wired`).
 */
data class ClientSnapshot(
    val mac: String?,
    val ip: String?,
    val hostname: String?,
    val tipoConexao: String?,
)
