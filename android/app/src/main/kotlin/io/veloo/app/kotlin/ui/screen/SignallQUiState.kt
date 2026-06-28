package io.signallq.app.ui.screen

import io.signallq.app.feature.diagnostico.pulse.AiAnalysisEntry
import io.signallq.app.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.signallq.app.feature.diagnostico.pulse.OpcaoResposta
import io.signallq.app.feature.diagnostico.pulse.QuestionNode
import io.signallq.app.feature.diagnostico.pulse.SignallQState

sealed interface SignallQUiState {
    data object Idle : SignallQUiState

    data class Collecting(
        val mensagem: String,
        val focoDiagnostico: String? = null,
    ) : SignallQUiState

    data class Thinking(
        val mensagem: String,
        val focoDiagnostico: String? = null,
    ) : SignallQUiState

    data class Analyzing(
        val mensagem: String,
        val session: IntelligentDiagnosticSession? = null,
        val focoDiagnostico: String? = null,
    ) : SignallQUiState

    data class AwaitingChipSelection(
        val session: IntelligentDiagnosticSession,
        val chips: List<OpcaoResposta>,
        val focoDiagnostico: String? = null,
    ) : SignallQUiState

    data class AwaitingAnswer(
        val session: IntelligentDiagnosticSession,
        val question: QuestionNode,
        val focoDiagnostico: String? = null,
    ) : SignallQUiState

    data class Result(
        val session: IntelligentDiagnosticSession,
        val latestAnalysis: AiAnalysisEntry,
        val signallQState: SignallQState,
        val availableChips: List<OpcaoResposta>,
        val focoDiagnostico: String? = null,
    ) : SignallQUiState

    data class Erro(
        val mensagem: String,
        val focoDiagnostico: String? = null,
    ) : SignallQUiState
}

/** Extrai o foco diagnóstico de qualquer estado. */
val SignallQUiState.focoDiagnostico: String?
    get() =
        when (this) {
            is SignallQUiState.Idle -> null
            is SignallQUiState.Collecting -> focoDiagnostico
            is SignallQUiState.Thinking -> focoDiagnostico
            is SignallQUiState.Analyzing -> focoDiagnostico
            is SignallQUiState.AwaitingChipSelection -> focoDiagnostico
            is SignallQUiState.AwaitingAnswer -> focoDiagnostico
            is SignallQUiState.Result -> focoDiagnostico
            is SignallQUiState.Erro -> focoDiagnostico
        }
