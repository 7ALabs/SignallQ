package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.signallq.app.ui.screen.ComparacaoRetesteUiState

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
    val containerColor = status.corContainer(c)
    val contentColor = status.corConteudo(c)
    val icon = status.icone()
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
        LkSectionOverline(text = "Como identifiquei isso")
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
                    text = "DADOS MEDIDOS PELO SIGNALLQ",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary,
                    letterSpacing = 0.5.sp,
                )
            }
            if (evidencias.isEmpty()) {
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    text = "Este teste não trouxe dados suficientes para avaliar essa situação.",
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
                    text = "EXPLICAÇÃO DO RESULTADO",
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
                        Text(text = "Preparando uma explicação simples…", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                is AnalisadorState.Resultado -> {
                    val texto = analisadorState.resumo.ifBlank { analisadorState.texto }
                    Text(text = texto, style = MaterialTheme.typography.bodySmall, color = c.textPrimary, lineHeight = 18.sp)
                    val rotuloConfianca = rotuloConfiancaExibicao(analisadorState.confianca)
                    if (rotuloConfianca.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(text = rotuloConfianca, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
                is AnalisadorState.Erro ->
                    Text(
                        text = "Não consegui carregar a explicação. O resultado acima continua válido.",
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
                text = "A explicação ajuda a entender o resultado. A avaliação é feita com os dados medidos no seu aparelho.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                lineHeight = 15.sp,
            )
        }
    }
}

/**
 * GH#1657 (spec §14.4) / GH#1707 — texto de exibição do rótulo de confiança (`baixa`/`media`/
 * `alta`, ver `RotuloConfianca`). Decisão de Luiz (2026-08-20): só texto, sem número, sem
 * ícone/pill decorativo — por isso este mapeamento devolve string pronta pro `Text`, não um par
 * (ícone, cor) como o resto deste arquivo faz para status.
 */
private fun rotuloConfiancaExibicao(valor: String): String =
    when (valor) {
        "alta" -> "Confiança alta"
        "media" -> "Confiança média"
        "baixa" -> "Confiança baixa"
        else -> ""
    }

/**
 * CTA "Testar novamente" **vinculado** à análise original (spec §8.8, GH#1707 Task 2.0.09e) —
 * nunca "recomeçar do zero" (isso é `ResultadoVelocidadeScreen.onTestarNovamente`, outro fluxo).
 *
 * Só aparece quando a IA recomendou explicitamente um reteste executável no app
 * (`AiAcaoRecomendada.tipo == "reteste" && executavelNoApp`) — o mesmo gancho que já existia sem
 * nenhuma tela consumindo. Enquanto o reteste roda, o botão vira um indicador; ao concluir, vira o
 * veredito em texto (§14.6: "Melhorou"/"Não mudou"/"Piorou"/"Comparação inconclusiva" — nunca
 * números lado a lado, nunca gráfico).
 */
@Composable
fun RetesteVinculadoSection(
    analisadorState: AnalisadorState,
    comparacaoRetesteState: ComparacaoRetesteUiState,
    onTestarNovamente: () -> Unit,
    c: LkTokens,
) {
    val elegivel =
        analisadorState is AnalisadorState.Resultado &&
            analisadorState.acoes.any { acao -> acao.tipo == "reteste" && acao.executavelNoApp }
    if (!elegivel) return

    Spacer(Modifier.height(LkSpacing.sm))
    when (comparacaoRetesteState) {
        is ComparacaoRetesteUiState.Ausente ->
            OutlinedButton(
                onClick = onTestarNovamente,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.primary),
            ) {
                Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(text = "Testar novamente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        is ComparacaoRetesteUiState.EmAndamento ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = c.primary)
                Text(text = "Testando novamente…", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
        is ComparacaoRetesteUiState.Concluido ->
            Text(
                text = comparacaoRetesteState.veredito,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
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
