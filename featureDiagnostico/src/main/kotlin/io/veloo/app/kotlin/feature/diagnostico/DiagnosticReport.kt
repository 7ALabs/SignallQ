package io.veloo.app.feature.diagnostico

data class DiagnosticReport(
    val wifiResultados: List<DiagnosticResult>,
    val internetResultados: List<DiagnosticResult>,
    val mobileResultados: List<DiagnosticResult> = emptyList(),
    val fibraResultados: List<DiagnosticResult>,
    val dnsResultados: List<DiagnosticResult> = emptyList(),
    val historicoResultados: List<DiagnosticResult> = emptyList(),
    val wifiCanalResultados: List<DiagnosticResult> = emptyList(),
    val decisao: DiagnosticResult,
    val perfisUsoSpeedtest: SpeedtestQualityInput? = null,
    val geradoEmMs: Long,
) {
    private val todos: List<DiagnosticResult>
        get() =
            wifiResultados +
                internetResultados +
                mobileResultados +
                fibraResultados +
                dnsResultados +
                historicoResultados +
                wifiCanalResultados +
                listOf(decisao)

    val temCritico: Boolean get() = todos.any { it.status == DiagnosticStatus.critical }
    val temAtencao: Boolean get() = todos.any { it.status == DiagnosticStatus.attention }
}
