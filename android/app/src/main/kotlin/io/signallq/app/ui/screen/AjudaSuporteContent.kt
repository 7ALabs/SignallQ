package io.signallq.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SignallQButton
import io.signallq.app.ui.component.SignallQButtonStyle
import io.signallq.app.ui.component.SignallQListRow

internal const val SUPPORT_EMAIL = "suporte@signallq.com"

@Composable
internal fun AjudaSuporteContent(
    onAbrirEmail: () -> Boolean,
    onCopiarEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    var feedback by remember { mutableStateOf<String?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        SelectionContainer {
            SignallQListRow(
                title = SUPPORT_EMAIL,
                subtitle = "Abrir aplicativo de e-mail",
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = {
                    if (!onAbrirEmail()) {
                        feedback = "Nenhum aplicativo de e-mail disponível. Você pode copiar o endereço."
                    }
                },
            )
        }
        SignallQButton(
            label = "Copiar endereço",
            onClick = {
                onCopiarEmail(SUPPORT_EMAIL)
                feedback = "Endereço de suporte copiado."
            },
            style = SignallQButtonStyle.Secondary,
        )
        feedback?.let { message ->
            Text(
                text = message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
            )
        }
    }
}
