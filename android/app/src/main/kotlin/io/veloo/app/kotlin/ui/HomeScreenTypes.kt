package io.signallq.app.ui

enum class FiltroConexaoHistorico {
    TODOS,
    WIFI,
    MOVEL,
}

data class HistoryPoint(
    val timestampEpochMs: Long,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
)

enum class ConnectionNodeType {
    WifiRouter,
    WifiMesh,
    WifiExtender,
    Mobile,
    Unknown,
}

data class GatewayInfo(
    val ip: String?,
    val name: String,
    val type: ConnectionNodeType = ConnectionNodeType.Unknown,
)

data class IspInfo(
    val ip: String?,
    val isp: String?,
    val asn: String?,
    val country: String?,
    val region: String?,
)
