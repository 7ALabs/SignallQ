package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.ResponsiveActionsRow

@Composable
fun PermissaoLocalizacaoContextoSheet(
    onConceder: () -> Unit,
    onAgoraNao: () -> Unit,
    // #155/9.3: quando true, exibe mensagem de permissão bloqueada permanentemente
    bloqueadaPermanentemente: Boolean = false,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.border),
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(LkSpacing.sm),
        ) {
            listOf("Solicitar", "Bloqueada").forEach { label ->
                val selecionado =
                    when (label) {
                        "Solicitar" -> !bloqueadaPermanentemente
                        else -> bloqueadaPermanentemente
                    }
                FilterChip(
                    selected = selecionado,
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    label = { Text(label) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = c.primary.copy(alpha = 0.15f),
                            selectedLabelColor = c.primary,
                            disabledSelectedContainerColor = c.primary.copy(alpha = 0.15f),
                            disabledContainerColor = c.bgSecondary,
                            disabledLabelColor = c.textTertiary,
                        ),
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.xl))
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(LkSpacing.lg))
        if (bloqueadaPermanentemente) {
            // Estado 9.3 — permissão bloqueada permanentemente
            Text(
                "Permissão bloqueada",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                "A permissão foi bloqueada nas configurações do Android. Para ativar, abra os ajustes do app.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.xl))
            ResponsiveActionsRow(
                secondary = { m ->
                    TextButton(onClick = onAgoraNao, modifier = m) {
                        Text("Agora não", color = c.textSecondary)
                    }
                },
                primary = { m ->
                    Button(
                        onClick = onConceder,
                        modifier = m,
                        shape = RoundedCornerShape(LkRadius.button),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                    ) {
                        Text("Abrir ajustes do Android")
                    }
                },
            )
        } else {
            // Estado 9.2 — permissão não concedida, pode solicitar
            Text(
                "Por que precisamos da localização?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                "O Android exige permissão de localização para identificar as redes Wi-Fi ao redor e analisar canais de interferência.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                "Não usamos sua localização para rastrear onde você está. Ela nunca sai do dispositivo.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.xl))
            ResponsiveActionsRow(
                secondary = { m ->
                    TextButton(onClick = onAgoraNao, modifier = m) {
                        Text("Agora não", color = c.textSecondary)
                    }
                },
                primary = { m ->
                    Button(
                        onClick = onConceder,
                        modifier = m,
                        shape = RoundedCornerShape(LkRadius.button),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                    ) {
                        Text("Entendi, conceder")
                    }
                },
            )
        }
        Spacer(Modifier.height(LkSpacing.md))
    }
}
