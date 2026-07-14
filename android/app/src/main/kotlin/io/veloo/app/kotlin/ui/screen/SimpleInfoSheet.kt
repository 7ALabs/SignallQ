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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

// GH#936 — Fase 7 MD3 (6f Sobre): extraido de AjustesScreen.kt. Wrapper generico
// de sheet informativa (titulo + linhas de InfoRow), hoje usado so pelo SobreSheet
// abaixo, mas mantido reutilizavel pelo mesmo motivo de antes (DiagnosticoAppSheet
// tem forma parecida mas conteudo proprio, nao migrado aqui por nao fazer parte do
// escopo 6a-6f).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SimpleInfoSheet(
    c: LkTokens,
    titulo: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
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
                    .verticalScroll(rememberScrollState())
                    .padding(top = LkSpacing.md)
                    .padding(bottom = LkSpacing.xxl)
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
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
            )
            Spacer(Modifier.height(LkSpacing.md))
            content()
        }
    }
}

// GH#936 — SheetInfoRow (spec): label bodyMedium/onSurfaceVariant (muted) + valor
// titleSmall/onSurface (forte), alinhado a direita. Corrigido em 2026-07: estava
// invertido (label forte, valor fraco).
@Composable
internal fun InfoRow(
    c: LkTokens,
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
    }
}

// ─── Sobre (6f) ─────────────────────────────────────────────────────────────
// Conteudo estatico: versao do app, desenvolvedor, contato de suporte e
// licencas de terceiros. "Plataforma" cortada na reestruturacao de 2026-07
// (redundante, sem motivo legal). "Licencas de terceiros" MANTIDA por decisao
// da Claudete (2026-07): risco de compliance real pesa mais que fidelidade
// estrita as 3 linhas da spec To-Be — Apache 2.0 de AndroidX/Kotlin/Firebase
// pede preservacao de NOTICE, e esta e a unica superficie visivel ao usuario
// hoje (a doc completa em docs_ai/technical/THIRD_PARTY_LICENSES.md e interna,
// nao aparece no app). Seguranca juridica vence design nesse caso especifico.
@Composable
internal fun SobreSheet(
    c: LkTokens,
    appVersion: String,
    onDismiss: () -> Unit,
) {
    SimpleInfoSheet(
        c = c,
        titulo = "Sobre o SignallQ",
        onDismiss = onDismiss,
    ) {
        InfoRow(c, "Versão", "v$appVersion")
        HorizontalDivider(color = c.border, thickness = 1.dp)
        InfoRow(c, "Desenvolvido por", "Equipe SignallQ")
        HorizontalDivider(color = c.border, thickness = 1.dp)
        InfoRow(c, "Suporte", "suporte@signallq.app")
        HorizontalDivider(color = c.border, thickness = 1.dp)
        InfoRow(c, "Licenças de terceiros", "Google Sans Flex · SIL OFL 1.1")
    }
}
