package io.linka.app.kotlin.feature.devices

data class SnapshotScanDispositivos(
    val estado: EstadoScanDispositivos,
    val progressoPercentual: Int,
    val dispositivos: List<DispositivoRede>,
    val erroMensagem: String?,
)

