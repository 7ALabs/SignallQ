package io.signallq.pro.feature.visita

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.pro.core.database.checklist.ChecklistItemEntity
import io.signallq.pro.core.database.checklist.ChecklistItemRepository
import io.signallq.pro.core.database.visita.EtapaVisita
import io.signallq.pro.core.database.visita.VisitaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChecklistTipoVisitaUiState(
    val itens: List<ChecklistItemEntity> = emptyList(),
) {
    val progresso: Float
        get() = if (itens.isEmpty()) 0f else itens.count { it.concluido }.toFloat() / itens.size
}

/** Tela 2.14 -- checklist por tipo de servico, densidade alta (ListRow, nao card por item). */
@HiltViewModel
class ChecklistTipoVisitaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val checklistItemRepository: ChecklistItemRepository,
        private val visitaRepository: VisitaRepository,
    ) : ViewModel() {
        private val visitaId: String = checkNotNull(savedStateHandle["visitaId"])

        val uiState: StateFlow<ChecklistTipoVisitaUiState> =
            checklistItemRepository
                .observarPorVisita(visitaId)
                .map { itens -> ChecklistTipoVisitaUiState(itens) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistTipoVisitaUiState())

        fun alternarItem(
            id: String,
            concluido: Boolean,
        ) {
            viewModelScope.launch {
                checklistItemRepository.marcarConcluido(id, concluido)
            }
        }

        fun avancarParaAmbientes() {
            viewModelScope.launch {
                visitaRepository.avancarEtapa(visitaId, EtapaVisita.AMBIENTES)
            }
        }
    }
