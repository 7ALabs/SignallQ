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
    // GH#983 Fase 4 — versao normalizada de [tipoConexao], atribuida dentro da
    // camada do driver (ex.: NokiaModemParser). Campo novo e opcional (default
    // null) para nao quebrar consumidores existentes de tipoConexao (string
    // crua), preservados por retrocompatibilidade — ver LocalDeviceSection.kt
    // e EquipamentoInternetScreen.kt, que ainda leem a string diretamente.
    val tipoConexaoFisica: TipoConexaoFisica? = null,
)
