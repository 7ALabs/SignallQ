package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

// ─── Tema sheet ─────────────────────────────────────────────────────────────
// O row "Tema" (Ajustes > Aparência) abria showPreferenciasSheet por engano —
// PreferenciasSheet é o sheet de "Alertas de qualidade" (limite de Mbps),
// sem relação com tema. ThemeSelector já existia pronto (ícones Sistema/Claro/
// Escuro, onDefinirTemaSelecionado já chegava até aqui) mas nunca teve ponto de
// entrada real na UI. Movido de AjustesScreen.kt pra arquivo dedicado, seguindo
// o mesmo padrão de PreferenciasSheet.kt/DadosLocaisSheet.kt.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemaSheet(
    c: LkTokens,
    temaSelecionado: String,
    onSelecionarTema: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {},
        containerColor = c.bgSecondary,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.border)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = "Arrastar para fechar" },
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                text = "Tema",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
            )
            Spacer(Modifier.height(LkSpacing.md))
            ThemeSelector(
                selecionado = temaSelecionado,
                onSelect = { valor ->
                    onSelecionarTema(valor)
                    onDismiss()
                },
                c = c,
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    selecionado: String,
    onSelect: (String) -> Unit,
    c: LkTokens,
) {
    val opcoes =
        listOf(
            Triple("sistema", "Sistema", Icons.Outlined.Settings),
            Triple("claro", "Claro", Icons.Outlined.LightMode),
            Triple("escuro", "Escuro", Icons.Outlined.DarkMode),
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opcoes.forEach { (valor, label, icone) ->
            val selecionada = selecionado == valor
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.surfaceContainer)
                        .border(
                            width = if (selecionada) 2.dp else 1.dp,
                            color = if (selecionada) LkColors.accent else c.border,
                            shape = RoundedCornerShape(LkRadius.card),
                        ).clickable { onSelect(valor) }
                        .padding(vertical = LkSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = icone,
                        contentDescription = label,
                        tint = if (selecionada) LkColors.accent else c.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selecionada) FontWeight.W600 else FontWeight.W400,
                        color = if (selecionada) LkColors.accent else c.textSecondary,
                    )
                }
            }
        }
    }
}
