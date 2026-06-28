package io.signallq.app.feature.diagnostico.topology.model

data class UpnpDeviceInfo(
    val friendlyName: String?,
    val manufacturer: String?,
    val modelName: String?,
    val modelNumber: String?,
    val controlUrl: String?  // URL resolvida para WANIPConnection ou WANPPPConnection
)
