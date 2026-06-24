package io.veloo.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.veloo.app.feature.diagnostico.DiagnosticStatus
import io.veloo.app.feature.diagnostico.pulse.SignallQState
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LkSpacing
import io.veloo.app.ui.LocalLkTokens
import io.veloo.app.ui.screen.SignallQUiState

private data class SignallQTopBarInfo(
    val signallQState: SignallQState,
    val statusLabel: String,
    val isActive: Boolean,
)

private fun SignallQUiState.toTopBarInfo(): SignallQTopBarInfo =
    when (this) {
        is SignallQUiState.Idle -> SignallQTopBarInfo(SignallQState.Idle, "Pronto", false)
        is SignallQUiState.Collecting -> SignallQTopBarInfo(SignallQState.Collecting, "Medindo...", true)
        is SignallQUiState.Thinking -> SignallQTopBarInfo(SignallQState.Thinking, "Analisando...", true)
        is SignallQUiState.Analyzing -> SignallQTopBarInfo(SignallQState.Analyzing, "Consultando IA...", true)
        is SignallQUiState.AwaitingChipSelection -> SignallQTopBarInfo(SignallQState.AwaitingInput, "Aguardando", false)
        is SignallQUiState.AwaitingAnswer -> SignallQTopBarInfo(SignallQState.AwaitingInput, "Aguardando", false)
        is SignallQUiState.Result -> {
            val state =
                when (session.diagnosticReport?.decisao?.status) {
                    DiagnosticStatus.critical -> SignallQState.Critical
                    DiagnosticStatus.attention -> SignallQState.Warning
                    else -> SignallQState.Success
                }
            SignallQTopBarInfo(state, "Pronto", false)
        }
        is SignallQUiState.Erro -> SignallQTopBarInfo(SignallQState.Idle, "Erro", false)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignallQTopBar(
    uiState: SignallQUiState,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val info = uiState.toTopBarInfo()

    val statusColor by animateColorAsState(
        targetValue =
            when {
                info.isActive -> LkColors.accent
                info.statusLabel == "Pronto" -> LkColors.success
                info.statusLabel == "Erro" -> LkColors.error
                else -> c.textTertiary
            },
        animationSpec = tween(400),
        label = "status-color",
    )

    CenterAlignedTopAppBar(
        modifier = modifier.background(c.bgPrimary),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignallQSymbol(
                    state = info.signallQState,
                    size = 18.dp,
                )
                androidx.compose.foundation.layout
                    .Spacer(modifier = Modifier.size(LkSpacing.sm))
                Text(
                    text = "Assistente SignallQ",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                )
            }
        },
        actions = {
            Text(
                text = info.statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
            )
            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.size(LkSpacing.md))
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
    )
}
