package io.signallq.pro.feature.visita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.pro.core.database.cliente.ClienteRepository
import io.signallq.pro.core.database.visita.StatusVisita
import io.signallq.pro.core.database.visita.VisitaEntity
import io.signallq.pro.core.database.visita.VisitaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AtendimentoResumoUiState(
    val visitaId: String,
    val clienteNome: String,
    val tipo: String,
    val emAndamento: Boolean,
)

data class PainelUiState(
    val carregando: Boolean = true,
    val visitaEmAndamento: AtendimentoResumoUiState? = null,
    val proximosAtendimentos: List<AtendimentoResumoUiState> = emptyList(),
) {
    val vazio: Boolean get() = !carregando && visitaEmAndamento == null && proximosAtendimentos.isEmpty()
}

/**
 * Tela 2.1 -- painel do Pro. NAO replica os 3 cards de metrica de vaidade do prototipo
 * (Atendimentos hoje/Clientes/Laudos emitidos, ver handoff Fase 2 #1161). Lidera com
 * "Proximos atendimentos" (lista) + as 4 acoes rapidas (ver [PainelScreen]).
 */
@HiltViewModel
class PainelViewModel
    @Inject
    constructor(
        visitaRepository: VisitaRepository,
        clienteRepository: ClienteRepository,
    ) : ViewModel() {
        val uiState: StateFlow<PainelUiState> =
            combine(
                visitaRepository.observarRecentes(limite = 10),
                clienteRepository.observarClientes(),
            ) { visitas, clientes ->
                val nomesPorId = clientes.associate { it.id to it.nome }

                fun paraResumo(visita: VisitaEntity) =
                    AtendimentoResumoUiState(
                        visitaId = visita.id,
                        clienteNome = nomesPorId[visita.clienteId] ?: "Cliente",
                        tipo = visita.tipo.name,
                        emAndamento = visita.status != StatusVisita.CONCLUIDA,
                    )
                val emAndamento =
                    visitas.firstOrNull { it.status == StatusVisita.EM_ANDAMENTO || it.status == StatusVisita.INTERROMPIDA }
                PainelUiState(
                    carregando = false,
                    visitaEmAndamento = emAndamento?.let(::paraResumo),
                    proximosAtendimentos =
                        visitas.filter { it.id != emAndamento?.id }.map(::paraResumo),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PainelUiState())
    }
