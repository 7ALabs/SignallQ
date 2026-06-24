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

    // Score 0–100 derivado do status da decisão final.
    // ok=90, info=75, attention=65/55, critical=25/15, inconclusive=50.
    val scoreConexao: Int
        get() =
            when (decisao.status) {
                DiagnosticStatus.ok -> 90
                DiagnosticStatus.info -> 75
                DiagnosticStatus.attention -> if (decisao.podeConcluir) 55 else 65
                DiagnosticStatus.critical -> if (decisao.podeConcluir) 15 else 25
                DiagnosticStatus.inconclusive -> 50
            }

    // Veredito legível para o usuário baseado no score.
    val veredito: String
        get() =
            when {
                scoreConexao >= 85 -> "Excelente"
                scoreConexao >= 65 -> "Bom"
                scoreConexao >= 40 -> "Regular"
                else -> "Fraco"
            }

    // Confiança do diagnóstico (0.0–1.0). Alta quando a decisão é conclusiva.
    val confianca: Double
        get() =
            when {
                decisao.status == DiagnosticStatus.inconclusive -> 0.30
                decisao.status == DiagnosticStatus.ok -> 0.90
                decisao.podeConcluir -> 0.88
                else -> 0.65
            }
}
