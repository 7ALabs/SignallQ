package io.signallq.app.feature.devices

data class SnapshotScanDispositivos(
    val estado: EstadoScanDispositivos,
    val progressoPercentual: Int,
    val dispositivos: List<DispositivoRede>,
    val erroMensagem: String?,
)

