package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

// ─── Preferências sheet (Alertas de qualidade) ─────────────────────────────────
// GH#936 — Fase 7 MD3: restaurado como arquivo dedicado. Estava definido em
// AjustesScreen.kt mas sem nenhum ponto de acesso na UI havia tempo (comentário
// original: "showPreferenciasSheet removido — dead code, nunca aberto via
// LazyColumn"); a persistência (limiteAlertaMbps, PreferenciasAppRepository) e
// toda a cadeia de callbacks (MainViewModel.salvarLimiteAlerta, MainActivity,
// AppShell) continuavam vivas e funcionando — só faltava a entrada na UI. PR do
// gate de Done (Rhodolfo/#945) apontou a perda de funcionalidade real; a entrada
// foi restaurada em AjustesScreen.kt (linha "Alertas de qualidade" na seção
// Notificações), não é código novo/inventado — mesmo conteúdo de antes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreferenciasSheet(
    c: LkTokens,
    limiteAtual: Int,
    onDismiss: () -> Unit,
    onSalvar: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var limiteInput by remember { mutableStateOf(if (limiteAtual > 0) limiteAtual.toString() else "") }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.primary,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.lg)
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
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
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                "Alertas de qualidade",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Text(
                "Defina um limite mínimo de download. Quando sua conexão ficar abaixo desse valor, o SignallQ pode alertar você.",
                style = MaterialTheme.typography.titleSmall,
                color = c.textSecondary,
                lineHeight = 18.sp,
            )
            OutlinedTextField(
                value = limiteInput,
                onValueChange = { limiteInput = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Mínimo de download (Mbps)") },
                placeholder = { Text("Ex: 50", color = c.textTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                shape = RoundedCornerShape(LkRadius.input),
            )
            if (limiteInput.isBlank()) {
                Text(
                    "Deixe em branco para desativar os alertas.",
                    style = MaterialTheme.typography.bodySmall,
                    // GH#937: textTertiary sobre branco ~2.5:1 (fail AA). textSecondary ~4.8:1.
                    color = c.textSecondary,
                )
            }
            Button(
                onClick = { onSalvar(limiteInput.toIntOrNull() ?: 0) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
            ) {
                Text("Salvar")
            }
        }
    }
}
