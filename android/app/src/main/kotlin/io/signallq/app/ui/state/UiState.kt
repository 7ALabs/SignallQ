package io.signallq.app.ui.state

/**
 * Estado de UI generico e selado para uso em ViewModels e Composables.
 *
 * Uso:
 *   - Loading: exibir indicador de progresso
 *   - Success: exibir dados
 *   - Empty: lista/dado vazio sem erro
 *   - Error: falha recuperavel — exibir mensagem e botao de retry
 *
 * ATENCAO: a mensagem de Error deve ser traduzida pelo caller antes de ser
 * passada aqui. Nunca passe stack traces diretamente para o usuario.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T,
    ) : UiState<T>

    data object Empty : UiState<Nothing>

    data class Error(
        val message: String,
    ) : UiState<Nothing>
}
