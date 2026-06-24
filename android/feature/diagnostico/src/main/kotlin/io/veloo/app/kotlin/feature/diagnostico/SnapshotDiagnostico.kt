package io.veloo.app.feature.diagnostico

data class SnapshotDiagnostico(
    val estado: EstadoDiagnostico,
    val relatorio: DiagnosticReport?,
    val erroMensagem: String?,
    /**
     * Snapshot do input que originou o relatorio. Opcional para retrocompat.
     * Quando presente, e usado para enviar metricas estruturadas (downloadMbps,
     * latenciaMs, jitterMs etc.) para a IA via DiagnosisAiContextFactory.
     */
    val input: DiagnosticInput? = null,
)
