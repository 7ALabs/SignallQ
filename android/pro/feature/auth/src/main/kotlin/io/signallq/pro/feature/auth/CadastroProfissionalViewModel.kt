package io.signallq.pro.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.pro.core.database.profissional.ProfissionalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CadastroProfissionalUiState(
    val nome: String = "",
    val erroNomeVazio: Boolean = false,
    val salvo: Boolean = false,
)

@HiltViewModel
class CadastroProfissionalViewModel
    @Inject
    constructor(
        private val profissionalRepository: ProfissionalRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CadastroProfissionalUiState())
        val uiState: StateFlow<CadastroProfissionalUiState> = _uiState

        fun atualizarNome(nome: String) {
            _uiState.update { it.copy(nome = nome, erroNomeVazio = false) }
        }

        fun salvar() {
            val nome = _uiState.value.nome.trim()
            if (nome.isEmpty()) {
                _uiState.update { it.copy(erroNomeVazio = true) }
                return
            }
            viewModelScope.launch {
                profissionalRepository.salvarPerfil(nome = nome, logoUri = null)
                _uiState.update { it.copy(salvo = true) }
            }
        }
    }
