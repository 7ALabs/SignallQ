package io.veloo.app.ui.screen

import io.veloo.app.feature.diagnostico.pulse.AiAnalysisEntry
import io.veloo.app.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.veloo.app.feature.diagnostico.pulse.OpcaoResposta
import io.veloo.app.feature.diagnostico.pulse.PulseState
import io.veloo.app.feature.diagnostico.pulse.QuestionNode

sealed interface VelooPulseUiState {
    data object Idle : VelooPulseUiState

    data class Collecting(
        val mensagem: String,
    ) : VelooPulseUiState

    data class Thinking(
        val mensagem: String,
    ) : VelooPulseUiState

    data class Analyzing(
        val mensagem: String,
    ) : VelooPulseUiState

    data class AwaitingChipSelection(
        val lastAnalysis: AiAnalysisEntry,
        val chips: List<OpcaoResposta>,
    ) : VelooPulseUiState

    data class AwaitingAnswer(
        val question: QuestionNode,
    ) : VelooPulseUiState

    data class Result(
        val session: IntelligentDiagnosticSession,
        val latestAnalysis: AiAnalysisEntry,
        val pulseState: PulseState,
        val availableChips: List<OpcaoResposta>,
    ) : VelooPulseUiState

    data class Erro(
        val mensagem: String,
    ) : VelooPulseUiState
}
