package io.signallq.pro.feature.medicaodiagnostico

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.pro.core.database.evidencia.EvidenciaEntity
import io.signallq.pro.core.database.evidencia.EvidenciaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val QUALIDADE_JPEG = 85

data class EvidenciasUiState(
    val itens: List<EvidenciaEntity> = emptyList(),
)

/**
 * Tela 2.12 -- evidencia visual (foto) OU nota textual, nunca obrigatoria uma sobre a
 * outra (handoff Fase 2, #1161: "permitir nota textual como alternativa").
 */
@HiltViewModel
class EvidenciasViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val evidenciaRepository: EvidenciaRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val ambienteId: String = checkNotNull(savedStateHandle["ambienteId"])

        val uiState: StateFlow<EvidenciasUiState> =
            evidenciaRepository
                .observarPorAmbiente(ambienteId)
                .map { EvidenciasUiState(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EvidenciasUiState())

        fun salvarFoto(bitmap: Bitmap) {
            viewModelScope.launch {
                val arquivo = File(context.filesDir, "evidencia_${System.currentTimeMillis()}.jpg")
                FileOutputStream(arquivo).use { saida ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida)
                }
                evidenciaRepository.adicionarFoto(ambienteId, arquivo.absolutePath)
            }
        }

        fun salvarNota(texto: String) {
            if (texto.isBlank()) return
            viewModelScope.launch { evidenciaRepository.adicionarNota(ambienteId, texto.trim()) }
        }

        fun excluir(evidencia: EvidenciaEntity) {
            viewModelScope.launch { evidenciaRepository.excluir(evidencia) }
        }
    }
