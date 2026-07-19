package io.signallq.pro.feature.visita

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.pro.core.database.cliente.ClienteRepository
import io.signallq.pro.core.database.visita.EtapaVisita
import io.signallq.pro.core.database.visita.StatusVisita
import io.signallq.pro.core.database.visita.VisitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AtendimentoUiState(
    val carregando: Boolean = true,
    val clienteNome: String = "",
    val tipo: String = "",
    val etapaAtual: EtapaVisita = EtapaVisita.CHECKLIST,
    val status: StatusVisita = StatusVisita.EM_ANDAMENTO,
)

/**
 * Tela 2.5 -- hub da visita. E aqui que a retomada de visita interrompida acontece de
 * verdade (criterio de saida do MVP0, issue #1119): ao abrir com um [visitaId] cuja
 * [StatusVisita] e EM_ANDAMENTO/INTERROMPIDA, mostra a etapa salva e o botao "Continuar"
 * leva direto pra ela -- nada e perdido.
 */
@HiltViewModel
class AtendimentoViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val visitaRepository: VisitaRepository,
        private val clienteRepository: ClienteRepository,
    ) : ViewModel() {
        val visitaId: String = checkNotNull(savedStateHandle["visitaId"])

        private val _uiState = MutableStateFlow(AtendimentoUiState())
        val uiState: StateFlow<AtendimentoUiState> = _uiState

        init {
            viewModelScope.launch {
                val visita = visitaRepository.buscarVisita(visitaId) ?: return@launch
                val cliente = clienteRepository.buscarPorId(visita.clienteId)
                _uiState.update {
                    it.copy(
                        carregando = false,
                        clienteNome = cliente?.nome ?: "Cliente",
                        tipo = visita.tipo.name,
                        etapaAtual = visita.etapaAtual,
                        status = visita.status,
                    )
                }
                if (visita.status == StatusVisita.INTERROMPIDA) {
                    visitaRepository.retomarVisita(visitaId)
                }
            }
        }

        fun marcarInterrompidaAoSair() {
            if (_uiState.value.status == StatusVisita.CONCLUIDA) return
            viewModelScope.launch { visitaRepository.marcarInterrompida(visitaId) }
        }
    }
