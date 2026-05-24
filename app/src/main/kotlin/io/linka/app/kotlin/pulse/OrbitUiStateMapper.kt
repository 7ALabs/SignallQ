package io.linka.app.kotlin.pulse

import io.linka.app.kotlin.feature.diagnostico.pulse.OrbitState
import io.linka.app.kotlin.feature.diagnostico.pulse.SnapshotOrbit
import io.linka.app.kotlin.ui.screen.OrbitUiState

object OrbitUiStateMapper {
    fun from(snapshot: SnapshotOrbit): OrbitUiState {
        val session = snapshot.session
        val mensagem = snapshot.mensagemAtual ?: ""
        val erro = snapshot.erro
        val foco = snapshot.focoDiagnostico

        if (erro != null && session == null) {
            return OrbitUiState.Erro(erro, foco)
        }

        return when (snapshot.estado) {
            OrbitState.Idle -> OrbitUiState.Idle
            OrbitState.Collecting -> OrbitUiState.Collecting(mensagem, foco)
            OrbitState.Thinking -> OrbitUiState.Thinking(mensagem, foco)
            OrbitState.Analyzing -> OrbitUiState.Analyzing(mensagem, session, foco)
            OrbitState.AwaitingInput, OrbitState.Success, OrbitState.Warning, OrbitState.Critical -> {
                if (session == null) return OrbitUiState.Idle
                val pendingQ = session.pendingQuestion
                if (pendingQ != null) return OrbitUiState.AwaitingAnswer(session, pendingQ, foco)
                val lastAnalysis = session.analyses.lastOrNull() ?: return OrbitUiState.Idle
                if (snapshot.estado == OrbitState.AwaitingInput) {
                    return OrbitUiState.AwaitingChipSelection(session, session.activeChips, foco)
                }
                OrbitUiState.Result(
                    session = session,
                    latestAnalysis = lastAnalysis,
                    orbitState = snapshot.estado,
                    availableChips = session.activeChips,
                    focoDiagnostico = foco,
                )
            }
        }
    }
}
