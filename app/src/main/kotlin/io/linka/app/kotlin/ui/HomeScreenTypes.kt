package io.linka.app.kotlin.ui

data class HistoryPoint(
    val timestampEpochMs: Long,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
)

enum class ConnectionNodeType {
    wifiRouter,
    wifiMesh,
    wifiExtender,
    mobile,
    unknown,
}

data class GatewayInfo(
    val ip: String?,
    val name: String,
    val type: ConnectionNodeType = ConnectionNodeType.unknown,
)

data class IspInfo(
    val ip: String?,
    val isp: String?,
    val asn: String?,
    val country: String?,
    val region: String?,
)
