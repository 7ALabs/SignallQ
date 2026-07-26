package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.EvidenciaDiagnostico
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.screen.AnalisadorState

/**
 * Componentes de resultado compartilhados entre [io.signallq.app.ui.screen.DiagnosticoGuiadoScreen]
 * (issue #1475, 7 objetivos) e o Modo gamer (issue #1476, categoria+device) — os dois motores
 * ([io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine] e
 * [io.signallq.app.core.diagnostico.ModoGamerEngine]) devolvem o mesmo contrato
 * ([DiagnosticStatus] + [EvidenciaDiagnostico] + ações), então o container visual "Medido pelo
 * motor SignallQ" / "Explicado por IA" (regra não-negociável da Feature #550, protótipo #1474
 * `AiVsMotorExplainer`) é literalmente o mesmo Composable pros dois — extraído aqui pra não
 * duplicar (issue #1476, critério "reaproveita motor de decisão de #1475, sem lógica duplicada").
 */
@Composable
fun DiagnosticoStatusBanner(
    status: DiagnosticStatus,
    mensagem: String,
    c: LkTokens,
) {
    val positivo = status == DiagnosticStatus.ok || status == DiagnosticStatus.info
    val containerColor = if (positivo) c.successContainer else c.errorContainer
    val contentColor = if (positivo) c.onSuccessContainer else c.onErrorContainer
    val icon = if (positivo) Icons.Outlined.CheckCircle else Icons.Outlined.Error
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(containerColor)
                .padding(LkSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        Text(text = mensagem, style = MaterialTheme.typography.bodyMedium, color = contentColor)
    }
}

/**
 * Separação visual explícita entre o que o motor local mede/decide ([evidencias], 100%
 * determinístico) e o que a IA só explica em prosa ([analisadorState]) — regra não-negociável
 * da Feature #550 (protótipo #1474, `AiVsMotorExplainer`). A IA nunca decide o status: quando
 * ainda está carregando ou falhou, o container de evidências continua mostrando os dados reais
 * normalmente, só o container de IA muda de estado.
 */
@Composable
fun AiVsMotorExplainer(
    evidencias: List<EvidenciaDiagnostico>,
    analisadorState: AnalisadorState,
    c: LkTokens,
) {
    Column {
        LkSectionOverline(text = "Como chegamos a esse resultado")
        Spacer(Modifier.height(LkSpacing.sm))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.surfaceContainer)
                    .padding(LkSpacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Verified, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "MEDIDO PELO MOTOR SIGNALLQ",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary,
                    letterSpacing = 0.5.sp,
                )
            }
            if (evidencias.isEmpty()) {
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    text = "Sem métricas suficientes deste teste para esta situação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            } else {
                evidencias.forEach { evidencia ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = evidencia.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = evidencia.valorExibido,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W700,
                            color = evidencia.status.corSemantica(c),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(LkSpacing.sm))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.primary.copy(alpha = 0.06f))
                    .padding(LkSpacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "EXPLICADO POR IA",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.primary,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(Modifier.height(LkSpacing.xs))
            when (analisadorState) {
                is AnalisadorState.Inativo, is AnalisadorState.Analisando ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = c.primary)
                        Text(text = "Preparando a explicação…", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                is AnalisadorState.Resultado -> {
                    val texto = analisadorState.resumo.ifBlank { analisadorState.texto }
                    Text(text = texto, style = MaterialTheme.typography.bodySmall, color = c.textPrimary, lineHeight = 18.sp)
                }
                is AnalisadorState.Erro ->
                    Text(
                        text = "Não foi possível carregar a explicação da IA — o resultado acima já reflete o motor local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
            }
        }

        Spacer(Modifier.height(LkSpacing.sm))
        Row(verticalAlignment = Alignment.Top) {
            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "A IA só explica o resultado — quem decide o status é sempre o motor de diagnóstico local, com base nos números medidos acima.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
fun AcoesRecomendadasCard(
    acoes: List<String>,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(horizontal = LkSpacing.lg),
    ) {
        acoes.forEachIndexed { index, acao ->
            if (index > 0) {
                HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
            }
            Row(modifier = Modifier.padding(vertical = LkSpacing.md), verticalAlignment = Alignment.Top) {
                Icon(imageVector = Icons.Outlined.TaskAlt, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(text = acao, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, lineHeight = 18.sp)
            }
        }
    }
}
