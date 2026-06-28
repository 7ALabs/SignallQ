package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.state.UiState

/**
 * Composable generico que renderiza o estado correto com base em [UiState].
 *
 * - Loading: CircularProgressIndicator centralizado
 * - Empty: icone + titulo + subtitulo + CTA opcional
 * - Error: icone de erro + titulo fixo + mensagem + botao retry obrigatorio
 * - Success: slot [content] com os dados
 *
 * O caller e responsavel por sanitizar mensagens de erro antes de passar
 * para [UiState.Error]. Nunca passe stack traces diretamente.
 */
@Composable
fun <T> StatefulScreen(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector = Icons.Outlined.Inbox,
    emptyIconDescription: String? = null,
    emptyTitle: String = "Nada por aqui",
    emptySubtitle: String = "Nenhum dado disponivel no momento.",
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val c = LocalLkTokens.current

    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                            .semantics { contentDescription = "Carregando" },
                    color = LkColors.accent,
                    strokeWidth = 3.dp,
                )
            }

            is UiState.Empty -> {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = LkSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = emptyIcon,
                        contentDescription = emptyIconDescription ?: emptyTitle,
                        modifier = Modifier.size(56.dp),
                        tint = c.textTertiary,
                    )
                    Spacer(modifier = Modifier.height(LkSpacing.lg))
                    Text(
                        text = emptyTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(LkSpacing.sm))
                    Text(
                        text = emptySubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    if (emptyActionLabel != null && onEmptyAction != null) {
                        Spacer(modifier = Modifier.height(LkSpacing.xl))
                        OutlinedButton(onClick = onEmptyAction) {
                            Text(text = emptyActionLabel)
                        }
                    }
                }
            }

            is UiState.Error -> {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = LkSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Erro",
                        modifier = Modifier.size(56.dp),
                        tint = LkColors.error,
                    )
                    Spacer(modifier = Modifier.height(LkSpacing.lg))
                    Text(
                        text = "Algo deu errado",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(LkSpacing.sm))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(LkSpacing.xl))
                    Button(
                        onClick = onRetry,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = LkColors.accent,
                            ),
                    ) {
                        Text(text = "Tentar novamente")
                    }
                }
            }

            is UiState.Success -> {
                content(state.data)
            }
        }
    }
}
