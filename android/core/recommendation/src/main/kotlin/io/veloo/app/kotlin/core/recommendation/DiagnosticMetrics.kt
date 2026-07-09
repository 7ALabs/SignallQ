package io.signallq.app.core.recommendation

/** Metricas cruas do teste que originou o diagnostico. Todas opcionais -- nem todo teste mede tudo. */
data class DiagnosticMetrics(
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val packetLossPercent: Double? = null,
    val bufferbloatMs: Double? = null,
)

/** Dados do dispositivo/ambiente no momento do diagnostico, quando disponiveis. */
data class DeviceContext(
    val manufacturer: String? = null,
    val model: String? = null,
    val wifiFrequencyGhz: Double? = null,
    val signalStrengthDbm: Int? = null,
)
