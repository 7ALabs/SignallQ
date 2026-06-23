package io.veloo.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.veloo.app.feature.diagnostico.DiagnosticStatus
import io.veloo.app.feature.diagnostico.ai.AiAcaoRecomendada
import io.veloo.app.feature.diagnostico.pulse.AiAnalysisEntry
import io.veloo.app.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.veloo.app.feature.diagnostico.pulse.ResponseSource
import io.veloo.app.feature.diagnostico.pulse.SignallQState
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LkSpacing
import io.veloo.app.ui.LocalLkTokens
import java.util.Calendar

@Composable
fun SignallQAiMessageBubble(
    analysis: AiAnalysisEntry,
    isLatest: Boolean,
    modifier: Modifier = Modifier,
    session: IntelligentDiagnosticSession? = null,
    isProgressMessage: Boolean = false,
) {
    val tokens = LocalLkTokens.current

    // Cor do símbolo por fonte da resposta:
    // INSIGHT/LOCAL → roxo (accent), GEMMA → amarelo (amber)
    val symbolColor =
        when (analysis.source) {
            ResponseSource.GEMMA -> Color(0xFFFBBF24)
            ResponseSource.INSIGHT, ResponseSource.LOCAL -> Color(0xFF6C2BFF)
        }
    val timeStr =
        remember(analysis.timestamp) {
            val cal = Calendar.getInstance().apply { timeInMillis = analysis.timestamp }
            "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }

    val sourceLabel =
        if (isProgressMessage) {
            "Diagnóstico IA · $timeStr"
        } else {
            when {
                !analysis.isFallback && analysis.fullResult?.source?.contains("cloudflare", ignoreCase = true) == true ->
                    "SignallQ IA · $timeStr"
                !analysis.isFallback && analysis.fullResult?.source == "cache" -> "SignallQ IA (cache) · $timeStr"
                else -> "Diagnóstico do dispositivo · $timeStr"
            }
        }

    val signallQSymbolState =
        when (session?.diagnosticReport?.decisao?.status) {
            DiagnosticStatus.ok, DiagnosticStatus.info -> SignallQState.Success
            DiagnosticStatus.attention -> SignallQState.Warning
            DiagnosticStatus.critical -> SignallQState.Critical
            else -> SignallQState.Idle
        }

    val statusColor =
        when (session?.diagnosticReport?.decisao?.status) {
            DiagnosticStatus.ok, DiagnosticStatus.info -> LkColors.success
            DiagnosticStatus.attention -> LkColors.warning
            DiagnosticStatus.critical -> LkColors.error
            else -> tokens.textSecondary
        }
    val statusLabel =
        when (session?.diagnosticReport?.decisao?.status) {
            DiagnosticStatus.ok, DiagnosticStatus.info -> "Rede OK"
            DiagnosticStatus.attention -> "Atenção"
            DiagnosticStatus.critical -> "Problema crítico"
            else -> "Inconclusivo"
        }

    val actions = analysis.fullResult?.acoesRecomendadas
    val technicalDetails = buildTechnicalSummary(analysis, session)
    var showTechnical by rememberSaveable(analysis.timestamp) { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = LkSpacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        // Símbolo colorido — cor determinada por ResponseSource
        SignallQSymbol(
            state = signallQSymbolState,
            size = 20.dp,
            colorOverride = symbolColor,
            modifier = Modifier.padding(top = 2.dp),
        )

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(tokens.bgSecondary)
                    .padding(LkSpacing.md),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                // Texto principal — sem background, sem borda, sem shape
                val textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = tokens.textPrimary,
                        lineHeight = 21.sp,
                    )
                if (isLatest) {
                    TypewriterText(
                        text = analysis.content,
                        style = textStyle,
                    )
                } else {
                    Text(text = analysis.content, style = textStyle)
                }

                // Inline metrics — sem card, layout compacto
                if (!isProgressMessage && session != null && session.hasAnyData()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(statusColor.copy(alpha = 0.12f))
                                    .padding(horizontal = LkSpacing.xs, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(statusColor),
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                            )
                        }
                        session.speedtestDownloadMbps?.let {
                            Text(
                                "↓ ${"%.0f".format(it)} Mbps",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LkColors.phaseDownload,
                            )
                        }
                        session.speedtestUploadMbps?.let {
                            Text(
                                "↑ ${"%.0f".format(it)} Mbps",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LkColors.phaseUpload,
                            )
                        }
                        session.speedtestLatencyMs?.let {
                            Text(
                                "${"%.0f".format(it)} ms",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LkColors.phaseLatencia,
                            )
                        }
                    }
                }

                // Ações recomendadas — lista simples de bullets, sem card wrapper
                if (!isProgressMessage && !actions.isNullOrEmpty()) {
                    BulletActionList(actions = actions)
                }

                // Detalhes técnicos colapsáveis — sem borda, sem card
                if (!isProgressMessage && technicalDetails != null) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { showTechnical = !showTechnical }
                                .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                    ) {
                        Text(
                            text = "Ver detalhes técnicos",
                            style = MaterialTheme.typography.labelMedium,
                            color = LkColors.accent.copy(alpha = 0.8f),
                        )
                        Icon(
                            imageVector = if (showTechnical) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = LkColors.accent.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    AnimatedVisibility(
                        visible = showTechnical,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200)),
                    ) {
                        Text(
                            text = technicalDetails,
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textSecondary,
                            lineHeight = 16.sp,
                        )
                    }
                }

                // Footer discreto: "SignallQ IA · HH:MM" em textTertiary
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun BulletActionList(actions: List<AiAcaoRecomendada>) {
    val tokens = LocalLkTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        Text(
            text = "O QUE FAZER",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.textSecondary.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp,
        )
        actions.take(4).forEach { action ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = LkSpacing.xs)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(LkColors.accent),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.titulo,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary,
                    )
                    if (action.descricao.isNotBlank()) {
                        Text(
                            text = action.descricao,
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textSecondary,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun buildTechnicalSummary(
    analysis: AiAnalysisEntry,
    session: IntelligentDiagnosticSession?,
): String? {
    val result = analysis.fullResult ?: return null
    val lines =
        buildList {
            val p = result.problemaPrincipal
            if (p.descricao.isNotBlank()) {
                add("Problema: ${p.descricao} (confiança: ${(p.confianca * 100).toInt()}%)")
            }
            result.evidencias.take(3).forEach { add("• ${it.label}: ${it.valor} — ${it.interpretacao}") }
            session?.wifiSsid?.let { add("Wi-Fi: $it") }
            val nomeModelo = result.modeloIa.nomeExibicao.takeIf { it.isNotBlank() }
            if (nomeModelo != null) add("Motor: $nomeModelo")
        }
    return if (lines.isEmpty()) null else lines.joinToString("\n")
}
