package io.signallq.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.ConfirmacaoDialog

// ─── Dados locais sheet ───────────────────────────────────────────────────────
// GH#936 — Fase 7 MD3 (6c Dados e privacidade): extraido de AjustesScreen.kt.
//
// Destino unico para as acoes de limpar/apagar/resetar dados -- consolidando o que
// antes eram 3 entradas espalhadas (Zona de risco, Historico e dados, Privacidade),
// cada uma com comportamento de confirmacao diferente. Escalonado por gravidade.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DadosLocaisSheet(
    c: LkTokens,
    onDismiss: () -> Unit,
    onLimparHistorico: () -> Unit,
    onApagarDadosLocais: () -> Unit,
    onResetarApp: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfirmLimpar by remember { mutableStateOf(false) }
    var showConfirmApagar by remember { mutableStateOf(false) }
    var showConfirmResetar by remember { mutableStateOf(false) }

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
                text = "Gerenciar dados e privacidade",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Text(
                text = "Estas ações são irreversíveis. Os dados serão removidos permanentemente do dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                lineHeight = 20.sp,
            )
            OutlinedButton(
                onClick = { showConfirmLimpar = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, LkColors.warning),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LkColors.warning),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Limpar histórico de testes")
            }
            OutlinedButton(
                onClick = { showConfirmApagar = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, LkColors.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LkColors.error),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Apagar dados locais")
            }
            Button(
                onClick = { showConfirmResetar = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.error),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Resetar app")
            }
        }
    }

    if (showConfirmLimpar) {
        ConfirmacaoDialog(
            titulo = "Limpar histórico?",
            mensagem = "Esta ação removerá todos os testes registrados. Não pode ser desfeita.",
            onConfirmar = {
                onLimparHistorico()
                showConfirmLimpar = false
                onDismiss()
            },
            onCancelar = { showConfirmLimpar = false },
        )
    }

    if (showConfirmApagar) {
        ConfirmacaoDialog(
            titulo = "Apagar dados locais?",
            mensagem = "Remove configurações salvas e preferências. Esta ação não pode ser desfeita.",
            onConfirmar = {
                onApagarDadosLocais()
                showConfirmApagar = false
                onDismiss()
            },
            onCancelar = { showConfirmApagar = false },
        )
    }

    if (showConfirmResetar) {
        ConfirmacaoDialog(
            titulo = "Redefinir o app?",
            mensagem =
                "Esta ação apagará todos os dados locais: histórico de testes, configurações salvas e preferências. " +
                    "O app voltará ao estado inicial. Esta ação não pode ser desfeita.",
            onConfirmar = {
                onResetarApp()
                showConfirmResetar = false
                onDismiss()
            },
            onCancelar = { showConfirmResetar = false },
        )
    }
}
