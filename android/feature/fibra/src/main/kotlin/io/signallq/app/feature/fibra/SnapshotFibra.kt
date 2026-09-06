package io.signallq.app.feature.fibra

data class SnapshotFibra(
    val estado: EstadoFibra,
    val gpon: GponStatus?,
    val wan: WanStatus?,
    val ppp: PppStatus?,
    val deviceInfo: DeviceInfoFibra?,
    val erroMensagem: String?,
    val gatewayIpDetectado: String? = null,
    val modemHost: String? = null,
    // GH#865 Fase 1 — Wi-Fi/LAN reais do roteador Nokia (antes so existiam no
    // contrato LocalNetworkDeviceSnapshot sem nenhum parser de producao os
    // preenchendo).
    val wifi: WifiStatus? = null,
    val lan: LanStatus? = null,
    // GH#839/#865 Fase 2 — lista real de clientes conectados (device_cfg +
    // alias_cfg de lan_status.cgi?wlan).
    val clientes: List<ClienteFibra> = emptyList(),
)
