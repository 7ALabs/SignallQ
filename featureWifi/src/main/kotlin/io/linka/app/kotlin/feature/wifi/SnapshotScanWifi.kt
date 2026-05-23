package io.linka.app.kotlin.feature.wifi

enum class EstadoScanWifi { idle, scanning, concluido, erro }

data class SnapshotScanWifi(
    val estado: EstadoScanWifi,
    val redes: List<RedeVizinha>,
    val erroMensagem: String?,
)
