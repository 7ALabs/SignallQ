package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSectionOverline

/**
 * "Ações disponíveis" — passo 14, último da narrativa (bug #6, spec Lia).
 * Inclui o fallback "Reiniciar equipamento" (quando não há outras ações
 * habilitadas) e o diálogo de confirmação. Extraído de
 * `EquipamentoInternetScreen.kt` (dívida crítica, ver
 * `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.6).
 */
@Composable
internal fun ActionsSectionCard(
    actions: List<EquipmentActionUi>,
    onSolicitarReiniciar: () -> Unit,
    onVerDispositivos: () -> Unit,
    onExecutarDiagnostico: () -> Unit,
    onVerDetalhesWifi: () -> Unit,
    c: LkTokens,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        LkSectionOverline(text = "Ações disponíveis")
        actions.forEach { action ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceContainer)
                        .border(1.dp, c.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable(enabled = action.enabled) {
                            when (action.id) {
                                "restart" -> onSolicitarReiniciar()
                                "devices" -> onVerDispositivos()
                                "diagnosis" -> onExecutarDiagnostico()
                                "wifi" -> onVerDetalhesWifi()
                            }
                        }.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(action.icon, contentDescription = null, tint = if (action.danger) LkColors.error else c.textSecondary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(LkSpacing.md))
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleSmall,
                    color =
                        when {
                            !action.enabled -> c.textTertiary
                            action.danger -> LkColors.error
                            else -> c.textPrimary
                        },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = if (action.enabled) c.textTertiary else c.outlineVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun ReiniciarEquipamentoRow(
    onClick: () -> Unit,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text("Reiniciar equipamento", fontSize = 13.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
            Text(
                "O equipamento fica indisponível por alguns minutos após reiniciar.",
                fontSize = 11.sp,
                // GH#937: mesma correção de contraste (ver acessoLabel).
                color = c.textSecondary,
            )
        }
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(14.dp)) { Text("Reiniciar", color = LkColors.error) }
    }
}

@Composable
internal fun ReiniciarEquipamentoDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = LocalLkTokens.current.surfaceContainerHigh,
        shape = RoundedCornerShape(LkRadius.dialog),
        title = { Text("Reiniciar equipamento?", fontWeight = FontWeight.W600) },
        text = {
            Text(
                "O equipamento vai desligar e ligar novamente. Durante esse tempo — geralmente " +
                    "de 1 a 3 minutos — você fica sem internet e sem acesso a esta tela, até ele " +
                    "voltar a responder.",
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Reiniciar", color = LkColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
