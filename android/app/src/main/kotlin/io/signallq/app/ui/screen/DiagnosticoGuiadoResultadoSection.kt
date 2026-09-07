package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticExplanationProvenance
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine
import io.signallq.app.core.diagnostico.ExplanationProvenanceSource
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.ResultadoDiagnosticoGuiado
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.AiVsMotorExplainer
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.RetesteVinculadoSection
import io.signallq.app.ui.component.corContainer
import io.signallq.app.ui.component.corConteudo
import io.signallq.app.ui.component.corSemantica
import io.signallq.app.ui.component.icone
import io.signallq.app.ui.component.labelPt

internal fun tituloAssistSeguro(
    objetivo: ObjetivoDiagnostico,
    status: DiagnosticStatus,
): String =
    when (status) {
        DiagnosticStatus.ok -> "Sua conexão está funcionando bem"
        DiagnosticStatus.info ->
            when (objetivo) {
                ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS -> "Sua conexão funciona, mas pode apresentar atraso durante o jogo"
                else -> "Sua conexão funciona, mas pode oscilar em alguns momentos"
            }
        DiagnosticStatus.attention -> "Sua conexão apresenta sinais de instabilidade"
        DiagnosticStatus.critical -> "Sua conexão apresenta um problema que precisa de atenção"
        DiagnosticStatus.inconclusive -> "Ainda não há dados suficientes para concluir"
    }

/**
 * No resultado remoto, a conclusão pertence ao NDS. O fallback local só protege uma resposta
 * incompleta; ele não pode reescrever uma causa que o serviço efetivamente encontrou.
 */
internal fun tituloAssistVindoDoNds(
    tituloNds: String?,
    objetivo: ObjetivoDiagnostico,
    status: DiagnosticStatus,
): String = tituloNds?.takeIf(String::isNotBlank) ?: tituloAssistSeguro(objetivo, status)

private fun resumoStatusAssist(status: DiagnosticStatus): String =
    when (status) {
        DiagnosticStatus.ok -> "Nenhum problema importante foi encontrado nas medições."
        DiagnosticStatus.info -> "Há um ponto da conexão que merece atenção."
        DiagnosticStatus.attention -> "Encontramos sinais de instabilidade que podem afetar o uso."
        DiagnosticStatus.critical -> "Encontramos um problema que pode prejudicar sua experiência."
        DiagnosticStatus.inconclusive -> "É preciso fazer uma nova medição para concluir."
    }

internal fun mensagemAssistSegura(
    objetivo: ObjetivoDiagnostico,
    status: DiagnosticStatus,
    atrasoSobCarga: Double?,
    latenciaLivre: Double?,
): String {
    val aumentoSobCarga =
        if (atrasoSobCarga != null && latenciaLivre != null) {
            (atrasoSobCarga - latenciaLivre).coerceAtLeast(0.0)
        } else {
            null
        }
    if (
        objetivo == ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS &&
        aumentoSobCarga != null &&
        aumentoSobCarga >= 30.0
    ) {
        return "A resposta aumentou %.0f ms quando a rede ficou ocupada. Isso pode causar atraso perceptível durante as partidas.".format(aumentoSobCarga)
    }
    return when (objetivo) {
        ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS ->
            when (status) {
                DiagnosticStatus.ok -> "As medições de resposta e estabilidade estão dentro do esperado para jogos online."
                DiagnosticStatus.info -> "A conexão funciona, mas pode oscilar ou apresentar atraso em momentos de disputa."
                DiagnosticStatus.attention, DiagnosticStatus.critical -> "As medições indicam instabilidade que pode causar atrasos ou travadas durante as partidas."
                DiagnosticStatus.inconclusive -> "Não foi possível medir dados suficientes para avaliar a experiência durante o jogo."
            }
        ObjetivoDiagnostico.INSTABILIDADE_QUEDAS -> "As medições indicam que a conexão pode oscilar ou perder estabilidade em alguns momentos."
        ObjetivoDiagnostico.LENTIDAO_GERAL -> "A resposta da rede pode atrasar a abertura de sites e aplicativos ou estar abaixo do esperado."
        ObjetivoDiagnostico.OUTRO_PROBLEMA -> "As medições gerais da sua conexão ajudam a entender o que pode estar acontecendo."
    }
}

/** Mesmo critério do título: a explicação do NDS é a mensagem principal do Assist remoto. */
internal fun mensagemAssistVindaDoNds(
    mensagemNds: String?,
    objetivo: ObjetivoDiagnostico,
    status: DiagnosticStatus,
    atrasoSobCarga: Double?,
    latenciaLivre: Double?,
): String =
    mensagemNds?.takeIf(String::isNotBlank)
        ?: mensagemAssistSegura(objetivo, status, atrasoSobCarga, latenciaLivre)

internal fun passosAssistSeguro(
    resultado: ResultadoDiagnosticoGuiado,
    recomendacaoNds: List<String>,
    atrasoSobCarga: Double?,
    latenciaLivre: Double?,
    usarApenasRecomendacaoNds: Boolean = false,
): List<String> {
    if (recomendacaoNds.isNotEmpty()) return recomendacaoNds.take(3)
    if (usarApenasRecomendacaoNds) return emptyList()
    if (resultado.acoes.isNotEmpty()) return resultado.acoes.take(3)
    val aumentoSobCarga =
        if (atrasoSobCarga != null && latenciaLivre != null) atrasoSobCarga - latenciaLivre else 0.0
    return if (aumentoSobCarga >= 30.0) {
        listOf(
            "Pause downloads, uploads e sincronizações enquanto joga.",
            "Repita a medição com a rede livre para confirmar a melhora.",
        )
    } else {
        emptyList()
    }
}

/** Ferramentas locais são continuidade do motor local, não recomendação do resultado remoto. */
internal fun deveExibirCtaMelhoriaLocal(veioDoNds: Boolean): Boolean = !veioDoNds

/**
 * O card "O que encontramos" deve exibir somente fatos que a medição coletou.
 * A explicação em prosa já é apresentada no topo do resultado; repeti-la no
 * card tira destaque das métricas e torna a leitura mais cansativa.
 */
internal fun deveExibirMetricasEncontradas(
    latenciaLivre: Double?,
    atrasoSobCarga: Double?,
): Boolean = latenciaLivre != null && atrasoSobCarga != null

internal fun rotuloProcedenciaExplicacao(provenance: DiagnosticExplanationProvenance?): String? =
    when (provenance?.source) {
        ExplanationProvenanceSource.AI ->
            provenance.modelLabel?.let { "Explicação por IA · $it" } ?: "Explicação por IA"
        ExplanationProvenanceSource.COPY_CATALOG -> "Explicação validada do Assist"
        ExplanationProvenanceSource.DETERMINISTIC -> "Baseado nas regras do diagnóstico"
        null -> null
    }

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
    val veioDoNds = diagnosticReport?.evaluationSource == DiagnosticEvaluationSource.REMOTE
    val internet = input?.internet
    val latenciaLivre = internet?.latencyMs
    val atrasoSobCarga = internet?.latencyMs?.let { it + (internet.bufferbloatMs ?: 0.0) }
    val titulo =
        if (veioDoNds) {
            tituloAssistVindoDoNds(decisao?.titulo, resultado.objetivo, status)
        } else {
            decisao?.titulo ?: resultado.mensagemMotor
        }
    val mensagem =
        if (veioDoNds) {
            mensagemAssistVindaDoNds(
                mensagemNds = decisao?.mensagemUsuario,
                objetivo = resultado.objetivo,
                status = status,
                atrasoSobCarga = atrasoSobCarga,
                latenciaLivre = latenciaLivre,
            )
        } else {
            decisao?.mensagemUsuario ?: resultado.mensagemMotor
        }
    val passosRecomendacao =
        passosAssistSeguro(
            resultado = resultado,
            recomendacaoNds = diagnosticReport?.decisao?.recomendacaoPassos.orEmpty(),
            atrasoSobCarga = atrasoSobCarga,
            latenciaLivre = latenciaLivre,
            usarApenasRecomendacaoNds = veioDoNds,
        )
    val ferramentaSugerida = remember(resultado.objetivo) { resultado.objetivo.ferramentaSugerida() }
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
        if (veioDoNds) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = status.icone(),
                    contentDescription = null,
                    tint = status.corSemantica(c),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(LkSpacing.xs))
                Text(
                    text = status.labelPt().uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = status.corSemantica(c),
                )
            }
            Spacer(Modifier.height(LkSpacing.md))
        } else {
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
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LkSpacing.md))
        if (veioDoNds) {
            LkSectionOverline(text = "O que isso significa para você")
            Spacer(Modifier.height(LkSpacing.sm))
        }
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
                text = "Baseado nas medições da sua rede",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            rotuloProcedenciaExplicacao(diagnosticReport.explanationProvenance)?.let { procedencia ->
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    text = procedencia,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                )
            }
        }
        if (!veioDoNds) {
            Spacer(Modifier.height(LkSpacing.lg))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Resumo do diagnóstico",
                    tint = c.textSecondary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(LkSpacing.md))
                Text(
                    resumoStatusAssist(status),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary,
                )
            }
        }
        if (deveExibirMetricasEncontradas(latenciaLivre, atrasoSobCarga)) {
            Spacer(Modifier.height(LkSpacing.xl))
            LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = false) {
                Column(Modifier.padding(LkSpacing.lg)) {
                    Text(
                        text = "O que encontramos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                    )
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
            }
        }
        passosRecomendacao.takeIf { it.isNotEmpty() }?.let { steps ->
            Spacer(Modifier.height(LkSpacing.lg))
            if (veioDoNds) {
                LkSectionOverline(text = if (steps.size > 1) "Próximos passos" else "Próximo passo")
                Spacer(Modifier.height(LkSpacing.sm))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(status.corContainer(c))
                            .padding(LkSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TaskAlt,
                        contentDescription = null,
                        tint = status.corConteudo(c),
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        steps.forEachIndexed { index, step ->
                            Text(
                                text = if (steps.size > 1) "${index + 1}. $step" else step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = status.corConteudo(c),
                            )
                            if (index < steps.lastIndex) Spacer(Modifier.height(LkSpacing.xs))
                        }
                    }
                }
            } else {
                LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = false) {
                    Column(Modifier.padding(LkSpacing.lg)) {
                        Text("O que você pode fazer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.textPrimary)
                        Spacer(Modifier.height(LkSpacing.sm))
                        steps.forEachIndexed { index, step ->
                            Text("${index + 1}. $step", style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                            if (index < steps.lastIndex) Spacer(Modifier.height(LkSpacing.xs))
                        }
                    }
                }
            }
        }
        if (resultado.evidencias.size > 1 && !veioDoNds) {
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
        Spacer(Modifier.height(LkSpacing.xl))
        if (deveExibirCtaMelhoriaLocal(veioDoNds)) {
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
        }
        TextButton(onClick = onEscolherOutraSituacao, modifier = Modifier.fillMaxWidth()) {
            Text("Escolher outra situação", style = MaterialTheme.typography.bodyLarge, color = c.primary)
        }
        Spacer(Modifier.height(LkSpacing.xl))
    }
}
