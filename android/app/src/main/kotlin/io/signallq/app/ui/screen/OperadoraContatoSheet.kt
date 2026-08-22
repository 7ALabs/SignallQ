package io.signallq.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.signallq.app.ui.ContatoOperadora
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SignallQButton
import io.signallq.app.ui.component.SignallQButtonStyle
import io.signallq.app.ui.component.SignallQListRow
import io.signallq.app.ui.component.SignallQSheet

/** Dados necessários para abrir o contato sem acoplar a aba móvel à navegação do AppShell. */
internal data class ContatoOperadoraSelecionada(
    val operadora: ContatoOperadora?,
    val nomeOperadora: String?,
)

@Composable
internal fun OperadoraContatoSheet(
    contato: ContatoOperadoraSelecionada,
    onDismiss: () -> Unit,
    onAbrirSite: () -> Unit,
    onLigar: (() -> Unit)?,
    onAbrirLaudo: () -> Unit,
) {
    val c = LocalLkTokens.current
    val nomeOperadora = contato.operadora?.nome ?: contato.nomeOperadora ?: "sua operadora"
    SignallQSheet(
        onDismissRequest = onDismiss,
        title = "Contato da operadora",
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                Text(
                    text = "Falar com a $nomeOperadora",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.onSurface,
                )
                Text(
                    text = "Use um canal oficial e tenha os dados da medição à mão.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                )
            }
            SignallQListRow(
                title = "Suporte online",
                subtitle = if (contato.operadora == null) "Buscar canal oficial" else "Abrir site oficial",
                icon = Icons.Outlined.Language,
                onClick = onAbrirSite,
            )
            onLigar?.let { ligar ->
                SignallQListRow(
                    title = "Central de atendimento",
                    subtitle = contato.operadora?.sac.orEmpty(),
                    icon = Icons.Outlined.Call,
                    onClick = ligar,
                )
            }
            SignallQListRow(
                title = "Compartilhe o laudo",
                subtitle = "Explique o problema com mais precisão",
                icon = Icons.Outlined.Description,
            )
            SignallQButton(
                label = "Abrir laudo",
                onClick = onAbrirLaudo,
                style = SignallQButtonStyle.Secondary,
                modifier = Modifier,
            )
        }
    }
}
