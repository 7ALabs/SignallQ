package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.LkSectionOverline

/**
 * Identidade do equipamento — passos 1 e 2 da narrativa da tela "Equipamento
 * de internet" (bug #6, spec Lia): quem é o equipamento antes de qualquer
 * veredito de status. Extraído de `EquipamentoInternetScreen.kt` (dívida
 * crítica, ver `.claude/rules/higiene-e-padronizacao-repositorio.md` seção
 * 4.6).
 *
 * ## Card próprio removido (revisão Lia 2026-07-25)
 * Wireframe aprovado pelo Luiz (reorganização de hierarquia, artifact
 * `547faba7-3981-42d0-a82f-58e0eba64da8`): `IdentificacaoEquipamentoCard`
 * (nome do device em manchete própria, competindo com "Conectado ao
 * equipamento") foi removido — [equipamentoIdentidadeLabel] gera só o texto,
 * consumido como subtítulo dentro de `StatusEquipamentoCard`
 * (`EquipamentoStatusPanel.kt`). `deviceType` deixou de aparecer nesta linha
 * (não fazia parte do subtítulo aprovado no wireframe); segue disponível via
 * seletor de equipamento quando há mais de um painel.
 */
internal fun equipamentoIdentidadeLabel(
    vendor: String?,
    modelo: String?,
    atualizadoEm: String,
): String {
    val titulo =
        listOfNotNull(vendor?.takeIf { it.isNotBlank() }, modelo?.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .ifBlank { "Seu equipamento de internet" }
    return "$titulo · $atualizadoEm"
}

@Composable
internal fun DeviceSelectorCard(
    paineis: List<EquipmentPanelUi>,
    selecionadoId: String,
    onSelecionar: (String) -> Unit,
    c: LkTokens,
) {
    var expandido by remember { mutableStateOf(false) }
    val selecionado = paineis.first { it.id == selecionadoId }

    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        LkSectionOverline(text = "Equipamento")
        OutlinedButton(
            onClick = { expandido = !expandido },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.input),
        ) {
            Text(
                text = "${selecionado.vendor} ${selecionado.modelo} · ${selecionado.papel}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = if (expandido) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textSecondary,
            )
        }

        if (expandido) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.input))
                        .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.input)),
            ) {
                paineis.forEachIndexed { index, painel ->
                    val ativo = painel.id == selecionadoId
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(if (ativo) c.primaryContainer else c.surfaceContainerLow)
                                .clickable {
                                    onSelecionar(painel.id)
                                    expandido = false
                                }.padding(horizontal = LkSpacing.base, vertical = LkSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${painel.vendor} ${painel.modelo} · ${painel.papel}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ativo) c.onPrimaryContainer else c.textPrimary,
                        )
                    }
                    if (index < paineis.lastIndex) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(c.outlineVariant),
                        )
                    }
                }
            }
        }
    }
}
