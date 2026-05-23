package io.linka.app.kotlin.feature.diagnostico.pulse

data class SnapshotOrbit(
    val estado: OrbitState = OrbitState.Idle,
    val session: IntelligentDiagnosticSession? = null,
    val mensagemAtual: String? = null,
    val erro: String? = null,
    /** Foco escolhido no intent picker. Mantido no snapshot para que a UI
     *  exiba a UserMessageBubble no topo do chat durante todos os estados. */
    val focoDiagnostico: String? = null,
)

@Deprecated("Use SnapshotOrbit", ReplaceWith("SnapshotOrbit"))
typealias SnapshotLinkaPulse = SnapshotOrbit
