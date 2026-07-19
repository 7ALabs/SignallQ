package io.signallq.pro.feature.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.pro.core.database.cliente.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NovoClienteUiState(
    val nome: String = "",
    val telefone: String = "",
    val endereco: String = "",
    val erroNomeVazio: Boolean = false,
    val clienteIdCriado: String? = null,
)

/**
 * Tela 2.3 -- so o nome e obrigatorio (doc 09 §11: cadastro rapido). O cliente ganha um local
 * "Principal" junto no cadastro (issue #1166) -- endereco fica opcional aqui, "pode ser
 * concluido depois" (doc 09 §11), sem bloquear o fluxo rapido.
 */
@HiltViewModel
class NovoClienteViewModel
    @Inject
    constructor(
        private val clienteRepository: ClienteRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NovoClienteUiState())
        val uiState: StateFlow<NovoClienteUiState> = _uiState

        fun atualizarNome(nome: String) {
            _uiState.update { it.copy(nome = nome, erroNomeVazio = false) }
        }

        fun atualizarTelefone(telefone: String) {
            _uiState.update { it.copy(telefone = telefone) }
        }

        fun atualizarEndereco(endereco: String) {
            _uiState.update { it.copy(endereco = endereco) }
        }

        fun salvar() {
            val nome = _uiState.value.nome.trim()
            if (nome.isEmpty()) {
                _uiState.update { it.copy(erroNomeVazio = true) }
                return
            }
            viewModelScope.launch {
                val telefone =
                    _uiState.value.telefone
                        .trim()
                        .ifBlank { null }
                val endereco = _uiState.value.endereco.trim()
                val id = clienteRepository.criarCliente(nome = nome, telefone = telefone, endereco = endereco)
                _uiState.update { it.copy(clienteIdCriado = id) }
            }
        }
    }
