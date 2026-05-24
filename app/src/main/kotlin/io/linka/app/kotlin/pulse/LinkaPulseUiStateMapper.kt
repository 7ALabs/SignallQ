package io.linka.app.kotlin.pulse

import io.linka.app.kotlin.feature.diagnostico.pulse.PulseState
import io.linka.app.kotlin.feature.diagnostico.pulse.SnapshotLinkaPulse
import io.linka.app.kotlin.ui.screen.LinkaPulseUiState

object LinkaPulseUiStateMapper {
    fun from(snapshot: SnapshotLinkaPulse): LinkaPulseUiState {
        val session = snapshot.session
        val mensagem = snapshot.mensagemAtual ?: ""

        return when (snapshot.estado) {
            PulseState.Idle -> LinkaPulseUiState.Idle
            PulseState.Collecting -> LinkaPulseUiState.Collecting(mensagem)
            PulseState.Thinking -> LinkaPulseUiState.Thinking(mensagem)
            PulseState.Analyzing -> LinkaPulseUiState.Analyzing(mensagem)
            PulseState.AwaitingInput, PulseState.Success, PulseState.Warning, PulseState.Critical -> {
                if (session == null) return LinkaPulseUiState.Idle
                val pendingQ = session.pendingQuestion
                if (pendingQ != null) return LinkaPulseUiState.AwaitingAnswer(pendingQ)
                val lastAnalysis = session.analyses.lastOrNull() ?: return LinkaPulseUiState.Idle
                if (snapshot.estado == PulseState.AwaitingInput) {
                    return LinkaPulseUiState.AwaitingChipSelection(lastAnalysis, session.activeChips)
                }
                LinkaPulseUiState.Result(
                    session = session,
                    latestAnalysis = lastAnalysis,
                    pulseState = snapshot.estado,
                    availableChips = session.activeChips,
                )
            }
        }
    }
}
