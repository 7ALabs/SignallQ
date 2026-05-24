package io.linka.app.kotlin.ui.screen

import io.linka.app.kotlin.feature.diagnostico.pulse.AiAnalysisEntry
import io.linka.app.kotlin.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.feature.diagnostico.pulse.PulseState
import io.linka.app.kotlin.feature.diagnostico.pulse.QuestionNode

sealed interface LinkaPulseUiState {
    data object Idle : LinkaPulseUiState

    data class Collecting(
        val mensagem: String,
    ) : LinkaPulseUiState

    data class Thinking(
        val mensagem: String,
    ) : LinkaPulseUiState

    data class Analyzing(
        val mensagem: String,
    ) : LinkaPulseUiState

    data class AwaitingChipSelection(
        val lastAnalysis: AiAnalysisEntry,
        val chips: List<OpcaoResposta>,
    ) : LinkaPulseUiState

    data class AwaitingAnswer(
        val question: QuestionNode,
    ) : LinkaPulseUiState

    data class Result(
        val session: IntelligentDiagnosticSession,
        val latestAnalysis: AiAnalysisEntry,
        val pulseState: PulseState,
        val availableChips: List<OpcaoResposta>,
    ) : LinkaPulseUiState

    data class Erro(
        val mensagem: String,
    ) : LinkaPulseUiState
}
