package io.veloo.app.pulse

import io.veloo.app.feature.diagnostico.pulse.SignallQSnapshot
import io.veloo.app.feature.diagnostico.pulse.SignallQState
import io.veloo.app.ui.screen.SignallQUiState

object SignallQUiStateMapper {
    fun from(snapshot: SignallQSnapshot): SignallQUiState {
        val session = snapshot.session
        val mensagem = snapshot.mensagemAtual ?: ""
        val erro = snapshot.erro
        val foco = snapshot.focoDiagnostico

        if (erro != null && session == null) {
            return SignallQUiState.Erro(erro, foco)
        }

        return when (snapshot.estado) {
            SignallQState.Idle -> SignallQUiState.Idle
            SignallQState.Collecting -> SignallQUiState.Collecting(mensagem, foco)
            SignallQState.Thinking -> SignallQUiState.Thinking(mensagem, foco)
            SignallQState.Analyzing -> SignallQUiState.Analyzing(mensagem, session, foco)
            SignallQState.AwaitingInput, SignallQState.Success, SignallQState.Warning, SignallQState.Critical -> {
                if (session == null) return SignallQUiState.Idle
                val pendingQ = session.pendingQuestion
                if (pendingQ != null) return SignallQUiState.AwaitingAnswer(session, pendingQ, foco)
                val lastAnalysis = session.analyses.lastOrNull() ?: return SignallQUiState.Idle
                if (snapshot.estado == SignallQState.AwaitingInput) {
                    return SignallQUiState.AwaitingChipSelection(session, session.activeChips, foco)
                }
                SignallQUiState.Result(
                    session = session,
                    latestAnalysis = lastAnalysis,
                    signallQState = snapshot.estado,
                    availableChips = session.activeChips,
                    focoDiagnostico = foco,
                )
            }
        }
    }
}
