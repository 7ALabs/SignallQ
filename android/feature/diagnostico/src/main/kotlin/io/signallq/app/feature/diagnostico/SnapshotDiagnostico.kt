package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport

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
