package io.signallq.app.ui.screen

import io.signallq.app.feature.diagnostico.pulse.AiAnalysisEntry
import io.signallq.app.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.signallq.app.feature.diagnostico.pulse.OpcaoResposta
import io.signallq.app.feature.diagnostico.pulse.PulseState
import io.signallq.app.feature.diagnostico.pulse.QuestionNode

sealed interface SignallQPulseUiState {
    data object Idle : SignallQPulseUiState

    data class Collecting(
        val mensagem: String,
    ) : SignallQPulseUiState

    data class Thinking(
        val mensagem: String,
    ) : SignallQPulseUiState

    data class Analyzing(
        val mensagem: String,
    ) : SignallQPulseUiState

    data class AwaitingChipSelection(
        val lastAnalysis: AiAnalysisEntry,
        val chips: List<OpcaoResposta>,
    ) : SignallQPulseUiState

    data class AwaitingAnswer(
        val question: QuestionNode,
    ) : SignallQPulseUiState

    data class Result(
        val session: IntelligentDiagnosticSession,
        val latestAnalysis: AiAnalysisEntry,
        val pulseState: PulseState,
        val availableChips: List<OpcaoResposta>,
    ) : SignallQPulseUiState

    data class Erro(
        val mensagem: String,
    ) : SignallQPulseUiState
}
