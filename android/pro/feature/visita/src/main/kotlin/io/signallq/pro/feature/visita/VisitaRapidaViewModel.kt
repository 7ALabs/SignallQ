package io.signallq.pro.feature.visita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.pro.core.database.cliente.ClienteEntity
import io.signallq.pro.core.database.cliente.ClienteRepository
import io.signallq.pro.core.database.visita.EtapaVisita
import io.signallq.pro.core.database.visita.TipoVisita
import io.signallq.pro.core.database.visita.VisitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitaRapidaUiState(
    val clientes: List<ClienteEntity> = emptyList(),
    val clienteSelecionadoId: String? = null,
    val visitaIdCriada: String? = null,
)

/**
 * Tela 2.13 -- modo de visita rapida. Reduz campos de verdade (so cliente, sem tipo/
 * checklist): visita nasce em modoRapido=true, tipo fixo SUPORTE, etapa inicial AMBIENTES
 * (pula CHECKLIST) -- handoff Fase 2, #1161.
 */
@HiltViewModel
class VisitaRapidaViewModel
    @Inject
    constructor(
        private val visitaRepository: VisitaRepository,
        clienteRepository: ClienteRepository,
    ) : ViewModel() {
        private val clienteSelecionadoIdFlow = MutableStateFlow<String?>(null)
        private val visitaIdCriadaFlow = MutableStateFlow<String?>(null)

        val uiState: StateFlow<VisitaRapidaUiState> =
            combine(
                clienteRepository.observarClientes(),
                clienteSelecionadoIdFlow,
                visitaIdCriadaFlow,
            ) { clientes, clienteId, visitaId ->
                VisitaRapidaUiState(clientes, clienteId, visitaId)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VisitaRapidaUiState())

        fun selecionarCliente(id: String) {
            clienteSelecionadoIdFlow.value = id
        }

        fun iniciarVisitaRapida() {
            val clienteId = clienteSelecionadoIdFlow.value ?: return
            viewModelScope.launch {
                val id =
                    visitaRepository.criarVisita(
                        clienteId = clienteId,
                        tipo = TipoVisita.SUPORTE,
                        modoRapido = true,
                    )
                visitaRepository.avancarEtapa(id, EtapaVisita.AMBIENTES)
                visitaIdCriadaFlow.value = id
            }
        }
    }
