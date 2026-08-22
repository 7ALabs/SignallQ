package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine
import io.signallq.app.core.diagnostico.ResultadoDiagnosticoGuiado
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.AiVsMotorExplainer
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.RetesteVinculadoSection

/**
 * Diagnóstico guiado por objetivo — Feature #550, issue #1475. 7 objetivos fechados,
 * cada um com um roteiro próprio de perguntas fechadas (nunca chat livre) e um
 * resultado que separa visualmente o que o motor local mede/decide
 * ([DiagnosticoGuiadoEngine], 100% determinístico) do que a IA só explica em prosa
 * ([AnalisadorState], mesmo mecanismo já usado no resto do app — nunca decide status,
 * nunca inventa evidência, nunca sugere compra sem recorrência).
 *
 * Substitui a antiga sheet automática "Análise detalhada"
 * (`DiagnosticoDetalhadoSheet`, retirada nesta issue): o banner de veredito e a
 * recomendação da IA deixam de abrir sozinhos ao entrar no resultado do teste — só
 * aparecem depois que o usuário escolhe um objetivo aqui (decisão do Luiz, comentário
 * de #1474 em 2026-07-26). Protótipo: `diagnostico-guiado.jsx` (#1483).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticoGuiadoResultadoSection(
    modifier: Modifier,
    resultado: ResultadoDiagnosticoGuiado,
    diagnosticReport: DiagnosticReport?,
    input: DiagnosticInput?,
    analisadorState: AnalisadorState,
    onEscolherOutraSituacao: () -> Unit,
    onIrParaHome: () -> Unit,
    categoria: String?,
    ispNome: String?,
    connectionType: String?,
    operadoraMovel: String?,
    recommendationDecision: RecommendationDecision?,
    recommendationFeedback: RecommendationFeedbackType?,
    onRecommendationShown: () -> Unit,
    onRecommendationClicked: () -> Unit,
    onRecommendationFeedback: (RecommendationFeedbackType) -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact,
    onIniciarModoGamer: (() -> Unit)?,
    onAbrirFerramentaSugerida: (TipoFerramenta) -> Unit,
    onTestarNovamenteVinculado: () -> Unit,
    comparacaoRetesteState: ComparacaoRetesteUiState,
    c: LkTokens,
    cabecalho: (@Composable () -> Unit)? = null,
) {
    val decisao = diagnosticReport?.decisao
    val status = decisao?.status ?: resultado.status
    val titulo = decisao?.titulo ?: resultado.mensagemMotor
    val mensagem = decisao?.mensagemUsuario ?: resultado.mensagemMotor
    val evidencia =
        decisao?.evidencia
            ?: resultado.evidencias.firstOrNull()?.let { "${it.label}: ${it.valorExibido}." }
            ?: "A análise não encontrou dados suficientes para concluir."
    val veioDoNds = diagnosticReport?.evaluationSource == DiagnosticEvaluationSource.REMOTE
    val internet = input?.internet
    val latenciaLivre = internet?.latencyMs
    val atrasoSobCarga = internet?.latencyMs?.let { it + (internet.bufferbloatMs ?: 0.0) }
    val ferramentaSugerida = remember(resultado.objetivo) { resultado.objetivo.ferramentaSugerida() }
    var detalhesAbertos by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        cabecalho?.let {
            it()
            Spacer(Modifier.height(LkSpacing.lg))
        }
        if (!veioDoNds) {
            resultado.evidencias.forEach { evidenciaLocal ->
                Text(evidenciaLocal.label, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            }
        }
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(c.warning.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            val positivo = status == DiagnosticStatus.ok
            Icon(
                imageVector = if (positivo) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = if (positivo) c.success else c.warning,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(LkSpacing.xl))
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!veioDoNds) {
            Spacer(Modifier.height(LkSpacing.lg))
            resultado.evidencias.forEach { evidenciaLocal ->
                Text(evidenciaLocal.label, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            }
            AiVsMotorExplainer(
                evidencias = resultado.evidencias,
                analisadorState = analisadorState,
                c = c,
                sectionTitle = "Medições",
            )
        }
        if (veioDoNds) {
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "Análise feita pelo NDS",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.height(LkSpacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(LkSpacing.md))
            Text(
                when (status) {
                    DiagnosticStatus.ok -> "Sua conexão está funcionando bem"
                    DiagnosticStatus.info -> "Sua conexão funciona, mas há espaço para melhorar"
                    DiagnosticStatus.attention -> "Encontramos sinais de instabilidade"
                    DiagnosticStatus.critical -> "Encontramos um problema que precisa de atenção"
                    DiagnosticStatus.inconclusive -> "Ainda não há dados suficientes para concluir"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
            )
        }
        diagnosticReport?.let { report ->
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "Confiança ${if (report.confianca >= 0.8) {
                    "alta"
                } else if (report.confianca >= 0.5) {
                    "média"
                } else {
                    "baixa"
                }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.height(LkSpacing.xl))
        LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = false) {
            Column(Modifier.padding(LkSpacing.lg)) {
                Text(
                    text = "O que encontramos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(LkSpacing.sm))
                Text(evidencia, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                if (latenciaLivre != null && atrasoSobCarga != null) {
                    Spacer(Modifier.height(LkSpacing.lg))
                    Row(Modifier.fillMaxWidth()) {
                        ResultadoMetricaCard(
                            value = "%.0f ms".format(latenciaLivre),
                            label = "Rede livre · resposta estável",
                            c = c,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(LkSpacing.md))
                        ResultadoMetricaCard(
                            value = "%.0f ms".format(atrasoSobCarga),
                            label = "Sob carga · atraso perceptível",
                            c = c,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                diagnosticReport?.dadosAusentes?.takeIf { it.isNotEmpty() }?.let { dadosAusentes ->
                    Spacer(Modifier.height(LkSpacing.md))
                    Text(
                        text = "O que faltou para refinar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = dadosAusentes.joinToString(", ") { it.replace('.', ' ') },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        }
        diagnosticReport?.decisao?.recomendacaoPassos?.takeIf { veioDoNds && it.isNotEmpty() }?.let { steps ->
            Spacer(Modifier.height(LkSpacing.lg))
            LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = false) {
                Column(Modifier.padding(LkSpacing.lg)) {
                    Text("Como resolver", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.textPrimary)
                    Spacer(Modifier.height(LkSpacing.sm))
                    steps.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                        if (index < steps.lastIndex) Spacer(Modifier.height(LkSpacing.xs))
                    }
                }
            }
        }
        if (resultado.evidencias.size > 1) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = LkSpacing.md)) {
                resultado.evidencias.drop(1).forEach { evidenciaSecundaria ->
                    Text(
                        text = evidenciaSecundaria.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(vertical = LkSpacing.xs),
                    )
                }
            }
        }
        RetesteVinculadoSection(
            analisadorState = analisadorState,
            comparacaoRetesteState = comparacaoRetesteState,
            onTestarNovamente = onTestarNovamenteVinculado,
            c = c,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { detalhesAbertos = !detalhesAbertos },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ver detalhes técnicos",
                style = MaterialTheme.typography.titleMedium,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.rotate(if (detalhesAbertos) 180f else 0f),
            )
        }
        Spacer(Modifier.height(LkSpacing.xl))
        Button(
            onClick = {
                if (ferramentaSugerida != null) {
                    onAbrirFerramentaSugerida(ferramentaSugerida)
                } else {
                    onEscolherOutraSituacao()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
        ) {
            Text("Ver como melhorar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onEscolherOutraSituacao, modifier = Modifier.fillMaxWidth()) {
            Text("Ver resultado inconclusivo", style = MaterialTheme.typography.bodyLarge, color = c.primary)
        }
        Spacer(Modifier.height(LkSpacing.xl))
    }
}
