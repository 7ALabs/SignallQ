package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

sealed interface SignallQScreenState<out T> {
    data object Loading : SignallQScreenState<Nothing>

    data class Content<T>(
        val value: T,
    ) : SignallQScreenState<T>

    data class Empty(
        val title: String,
        val message: String,
    ) : SignallQScreenState<Nothing>

    data class Offline(
        val title: String,
        val message: String,
    ) : SignallQScreenState<Nothing>

    data class PermissionRequired(
        val title: String,
        val message: String,
    ) : SignallQScreenState<Nothing>

    data class RecoverableError(
        val title: String,
        val message: String,
    ) : SignallQScreenState<Nothing>
}

@Composable
fun <T> SignallQStatefulScreen(
    state: SignallQScreenState<T>,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        SignallQScreenState.Loading -> SignallQSkeleton(modifier = modifier.fillMaxSize().padding(LkSpacing.xl), lines = 5)
        is SignallQScreenState.Content -> content(state.value)
        is SignallQScreenState.Empty -> SignallQFullScreenState(state.title, state.message, Icons.Outlined.Inbox, modifier, actionLabel, onAction)
        is SignallQScreenState.Offline -> SignallQFullScreenState(state.title, state.message, Icons.Outlined.WifiOff, modifier, actionLabel, onAction)
        is SignallQScreenState.PermissionRequired ->
            SignallQFullScreenState(
                state.title,
                state.message,
                Icons.Outlined.LocationOff,
                modifier,
                actionLabel,
                onAction,
            )
        is SignallQScreenState.RecoverableError ->
            SignallQFullScreenState(
                state.title,
                state.message,
                Icons.Outlined.ErrorOutline,
                modifier,
                actionLabel,
                onAction,
            )
    }
}

@Composable
private fun SignallQFullScreenState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LkSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.lg, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = c.onSurfaceVariant)
        Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) SignallQButton(actionLabel, onAction)
    }
}

@Composable
fun SignallQOfflineBanner(
    modifier: Modifier = Modifier,
    message: String = "Sem conexão ativa. Recursos locais continuam disponíveis.",
) {
    SignallQBanner(
        title = "Você está offline",
        message = message,
        modifier = modifier,
        tone = SignallQFeedbackTone.Warning,
    )
}
