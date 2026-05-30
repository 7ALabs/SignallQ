package io.veloo.app.feature.fibra

data class SnapshotFibra(
    val estado: EstadoFibra,
    val gpon: GponStatus?,
    val wan: WanStatus?,
    val ppp: PppStatus?,
    val deviceInfo: DeviceInfoFibra?,
    val erroMensagem: String?,
    val gatewayIpDetectado: String? = null,
    val modemHost: String? = null,
)
