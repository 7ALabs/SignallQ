package io.signallq.app.ui

import io.signallq.app.core.network.contracts.topologia.NivelConfianca

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
    // #980 (Fase 2B) — confianca do motor de topologia unificado (TopologiaRedeEngine/#979) no
    // papel deste no. Nulo pra nos que nao vem desse motor (ex.: Mobile). Nao-ALTA sinaliza que
    // a Home deve mostrar o papel como "provavel", nunca afirmativo (ver GatewayInfoSheet).
    val confianca: NivelConfianca? = null,
)

data class IspInfo(
    val ip: String?,
    val isp: String?,
    val asn: String?,
    val country: String?,
    val region: String?,
)
