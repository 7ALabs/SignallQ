package io.linka.app.kotlin.ui.screen

import io.linka.app.kotlin.feature.diagnostico.pulse.AiAnalysisEntry
import io.linka.app.kotlin.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.feature.diagnostico.pulse.OrbitState
import io.linka.app.kotlin.feature.diagnostico.pulse.QuestionNode

sealed interface OrbitUiState {
    data object Idle : OrbitUiState

    data class Collecting(
        val mensagem: String,
        val focoDiagnostico: String? = null,
    ) : OrbitUiState

    data class Thinking(
        val mensagem: String,
        val focoDiagnostico: String? = null,
    ) : OrbitUiState

    data class Analyzing(
        val mensagem: String,
        val session: IntelligentDiagnosticSession? = null,
        val focoDiagnostico: String? = null,
    ) : OrbitUiState

    data class AwaitingChipSelection(
        val session: IntelligentDiagnosticSession,
        val chips: List<OpcaoResposta>,
        val focoDiagnostico: String? = null,
    ) : OrbitUiState

    data class AwaitingAnswer(
        val session: IntelligentDiagnosticSession,
        val question: QuestionNode,
        val focoDiagnostico: String? = null,
    ) : OrbitUiState

    data class Result(
        val session: IntelligentDiagnosticSession,
        val latestAnalysis: AiAnalysisEntry,
        val orbitState: OrbitState,
        val availableChips: List<OpcaoResposta>,
        val focoDiagnostico: String? = null,
    ) : OrbitUiState

    data class Erro(
        val mensagem: String,
        val focoDiagnostico: String? = null,
    ) : OrbitUiState
}

/** Extrai o foco diagnóstico de qualquer estado. */
val OrbitUiState.focoDiagnostico: String?
    get() =
        when (this) {
            is OrbitUiState.Idle -> null
            is OrbitUiState.Collecting -> focoDiagnostico
            is OrbitUiState.Thinking -> focoDiagnostico
            is OrbitUiState.Analyzing -> focoDiagnostico
            is OrbitUiState.AwaitingChipSelection -> focoDiagnostico
            is OrbitUiState.AwaitingAnswer -> focoDiagnostico
            is OrbitUiState.Result -> focoDiagnostico
            is OrbitUiState.Erro -> focoDiagnostico
        }
