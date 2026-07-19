package io.signallq.pro.feature.visita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.pro.core.database.cliente.ClienteEntity
import io.signallq.pro.core.database.cliente.ClienteRepository
import io.signallq.pro.core.database.visita.TipoVisita
import io.signallq.pro.core.database.visita.VisitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class SelecaoVisita(
    val clienteId: String? = null,
    val tipo: TipoVisita = TipoVisita.INSTALACAO,
)

data class NovaVisitaUiState(
    val clientes: List<ClienteEntity> = emptyList(),
    val clienteSelecionadoId: String? = null,
    val tipoSelecionado: TipoVisita = TipoVisita.INSTALACAO,
    val erroSemCliente: Boolean = false,
    val visitaIdCriada: String? = null,
)

/** Tela 2.4 -- erro quando cliente/local nao selecionado (handoff Fase 2, #1161). */
@HiltViewModel
class NovaVisitaViewModel
    @Inject
    constructor(
        private val visitaRepository: VisitaRepository,
        clienteRepository: ClienteRepository,
    ) : ViewModel() {
        private val selecaoFlow = MutableStateFlow(SelecaoVisita())
        private val erroSemClienteFlow = MutableStateFlow(false)
        private val visitaIdCriadaFlow = MutableStateFlow<String?>(null)

        val uiState: StateFlow<NovaVisitaUiState> =
            combine(
                clienteRepository.observarClientes(),
                selecaoFlow,
                erroSemClienteFlow,
                visitaIdCriadaFlow,
            ) { clientes, selecao, erro, visitaId ->
                NovaVisitaUiState(
                    clientes = clientes,
                    clienteSelecionadoId = selecao.clienteId,
                    tipoSelecionado = selecao.tipo,
                    erroSemCliente = erro,
                    visitaIdCriada = visitaId,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NovaVisitaUiState())

        fun selecionarCliente(id: String) {
            selecaoFlow.update { it.copy(clienteId = id) }
            erroSemClienteFlow.value = false
        }

        fun selecionarTipo(tipo: TipoVisita) {
            selecaoFlow.update { it.copy(tipo = tipo) }
        }

        fun iniciarVisita() {
            val selecao = selecaoFlow.value
            val clienteId = selecao.clienteId
            if (clienteId == null) {
                erroSemClienteFlow.value = true
                return
            }
            viewModelScope.launch {
                val id = visitaRepository.criarVisita(clienteId = clienteId, tipo = selecao.tipo)
                visitaIdCriadaFlow.value = id
            }
        }
    }
