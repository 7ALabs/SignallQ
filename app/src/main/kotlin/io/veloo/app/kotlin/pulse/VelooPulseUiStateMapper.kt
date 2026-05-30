package io.veloo.app.pulse

import io.veloo.app.feature.diagnostico.pulse.PulseState
import io.veloo.app.feature.diagnostico.pulse.SnapshotLinkaPulse
import io.veloo.app.ui.screen.VelooPulseUiState

object VelooPulseUiStateMapper {
    fun from(snapshot: SnapshotLinkaPulse): VelooPulseUiState {
        val session = snapshot.session
        val mensagem = snapshot.mensagemAtual ?: ""

        return when (snapshot.estado) {
            PulseState.Idle -> VelooPulseUiState.Idle
            PulseState.Collecting -> VelooPulseUiState.Collecting(mensagem)
            PulseState.Thinking -> VelooPulseUiState.Thinking(mensagem)
            PulseState.Analyzing -> VelooPulseUiState.Analyzing(mensagem)
            PulseState.AwaitingInput, PulseState.Success, PulseState.Warning, PulseState.Critical -> {
                if (session == null) return VelooPulseUiState.Idle
                val pendingQ = session.pendingQuestion
                if (pendingQ != null) return VelooPulseUiState.AwaitingAnswer(pendingQ)
                val lastAnalysis = session.analyses.lastOrNull() ?: return VelooPulseUiState.Idle
                if (snapshot.estado == PulseState.AwaitingInput) {
                    return VelooPulseUiState.AwaitingChipSelection(lastAnalysis, session.activeChips)
                }
                VelooPulseUiState.Result(
                    session = session,
                    latestAnalysis = lastAnalysis,
                    pulseState = snapshot.estado,
                    availableChips = session.activeChips,
                )
            }
        }
    }
}
