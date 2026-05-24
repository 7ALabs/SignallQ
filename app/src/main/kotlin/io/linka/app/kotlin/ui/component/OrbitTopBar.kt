package io.linka.app.kotlin.ui.component

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
import io.linka.app.kotlin.feature.diagnostico.DiagnosticStatus
import io.linka.app.kotlin.feature.diagnostico.pulse.OrbitState
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.screen.OrbitUiState

private data class OrbitTopBarInfo(
    val orbitState: OrbitState,
    val statusLabel: String,
    val isActive: Boolean,
)

private fun OrbitUiState.toTopBarInfo(): OrbitTopBarInfo =
    when (this) {
        is OrbitUiState.Idle -> OrbitTopBarInfo(OrbitState.Idle, "Pronto", false)
        is OrbitUiState.Collecting -> OrbitTopBarInfo(OrbitState.Collecting, "Medindo...", true)
        is OrbitUiState.Thinking -> OrbitTopBarInfo(OrbitState.Thinking, "Analisando...", true)
        is OrbitUiState.Analyzing -> OrbitTopBarInfo(OrbitState.Analyzing, "Consultando IA...", true)
        is OrbitUiState.AwaitingChipSelection -> OrbitTopBarInfo(OrbitState.AwaitingInput, "Aguardando", false)
        is OrbitUiState.AwaitingAnswer -> OrbitTopBarInfo(OrbitState.AwaitingInput, "Aguardando", false)
        is OrbitUiState.Result -> {
            val state =
                when (session.diagnosticReport?.decisao?.status) {
                    DiagnosticStatus.critical -> OrbitState.Critical
                    DiagnosticStatus.attention -> OrbitState.Warning
                    else -> OrbitState.Success
                }
            OrbitTopBarInfo(state, "Pronto", false)
        }
        is OrbitUiState.Erro -> OrbitTopBarInfo(OrbitState.Idle, "Erro", false)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitTopBar(
    uiState: OrbitUiState,
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
                OrbitSymbol(
                    state = info.orbitState,
                    size = 18.dp,
                )
                androidx.compose.foundation.layout
                    .Spacer(modifier = Modifier.size(LkSpacing.sm))
                Text(
                    text = "Diagnóstico IA",
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
