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
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.RetesteVinculadoSection

private val MISSING_INPUT_LABELS =
    mapOf(
        "dns.latencyMs" to "resposta do DNS",
        "dns_latency_ms" to "resposta do DNS",
        "quality.latencyMs" to "latência",
        "latency_ms" to "latência",
        "quality.jitterMs" to "variação da conexão",
        "jitter_ms" to "variação da conexão",
        "quality.packetLossPercent" to "perda de pacotes",
        "packet_loss_percent" to "perda de pacotes",
        "quality.loadedLatencyMs" to "resposta sob carga",
        "loaded_latency_ms" to "resposta sob carga",
        "quality.bufferbloatMs" to "atraso com a rede ocupada",
        "bufferbloat_ms" to "atraso com a rede ocupada",
        "gateway.rttMs" to "resposta do roteador",
        "gateway_rtt_ms" to "resposta do roteador",
        "gateway.connectedDevices" to "dispositivos conectados",
        "connected_devices" to "dispositivos conectados",
        "wifi.rssiDbm" to "força do Wi-Fi",
        "wifi_rssi_dbm" to "força do Wi-Fi",
        "wifi.band" to "banda do Wi-Fi",
        "wifi_band" to "banda do Wi-Fi",
        "speed.downloadMbps" to "download",
        "download_mbps" to "download",
        "speed.uploadMbps" to "upload",
        "upload_mbps" to "upload",
    )

internal fun tituloAssistSeguro(
    objetivo: ObjetivoDiagnostico,
    status: DiagnosticStatus,
): String =
    when (status) {
        DiagnosticStatus.ok -> "Sua conexão está funcionando bem"
        DiagnosticStatus.info ->
            when (objetivo) {
                ObjetivoDiagnostico.JOGOS_COM_LAG -> "Sua conexão funciona, mas pode apresentar atraso durante o jogo"
                else -> "Sua conexão funciona, mas pode oscilar em alguns momentos"
            }
        DiagnosticStatus.attention -> "Sua conexão apresenta sinais de instabilidade"
        DiagnosticStatus.critical -> "Sua conexão apresenta um problema que precisa de atenção"
        DiagnosticStatus.inconclusive -> "Ainda não há dados suficientes para concluir"
    }

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
        objetivo == ObjetivoDiagnostico.JOGOS_COM_LAG &&
        aumentoSobCarga != null &&
        aumentoSobCarga >= 30.0
    ) {
        return "A resposta aumentou %.0f ms quando a rede ficou ocupada. Isso pode causar atraso perceptível durante as partidas.".format(aumentoSobCarga)
    }
    return when (objetivo) {
        ObjetivoDiagnostico.JOGOS_COM_LAG ->
            when (status) {
                DiagnosticStatus.ok -> "As medições de resposta e estabilidade estão dentro do esperado para jogos online."
                DiagnosticStatus.info -> "A conexão funciona, mas pode oscilar ou apresentar atraso em momentos de disputa."
                DiagnosticStatus.attention, DiagnosticStatus.critical -> "As medições indicam instabilidade que pode causar atrasos ou travadas durante as partidas."
                DiagnosticStatus.inconclusive -> "Não foi possível medir dados suficientes para avaliar a experiência durante o jogo."
            }
        ObjetivoDiagnostico.VIDEOS_TRAVAM -> "A conexão pode perder qualidade quando há tráfego simultâneo, especialmente durante o streaming."
        ObjetivoDiagnostico.INTERNET_CAI_OSCILA -> "As medições indicam que a conexão pode oscilar ou perder estabilidade em alguns momentos."
        ObjetivoDiagnostico.CHAMADAS_CONGELAM -> "A variação da conexão pode causar cortes no áudio ou congelamentos durante chamadas."
        ObjetivoDiagnostico.SITES_DEMORAM -> "A resposta da rede pode atrasar a abertura de sites e aplicativos."
        ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA -> "A velocidade medida pode estar abaixo do esperado para o seu plano ou para o uso atual."
        ObjetivoDiagnostico.WIFI_VS_OPERADORA -> "As medições ajudam a separar um problema no Wi-Fi de um problema no caminho até a operadora."
    }
}

internal fun passosAssistSeguro(
    resultado: ResultadoDiagnosticoGuiado,
    recomendacaoNds: List<String>,
    atrasoSobCarga: Double?,
    latenciaLivre: Double?,
): List<String> {
    if (recomendacaoNds.isNotEmpty()) return recomendacaoNds.take(3)
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

internal fun dadosAusentesEmLinguagemHumana(dadosAusentes: List<String>): String {
    val labels =
        dadosAusentes.map { MISSING_INPUT_LABELS[it] ?: "outras medições avançadas" }.distinct()
    return "Algumas medições avançadas não estavam disponíveis: ${labels.joinToString(", ")}."
}

internal fun confiancaAssist(report: DiagnosticReport): String {
    val confiancaBase =
        when {
            report.confianca >= 0.8 -> "alta"
            report.confianca >= 0.5 -> "média"
            else -> "baixa"
        }
    return if (confiancaBase == "alta" && report.dadosAusentes.isNotEmpty()) {
        "média"
    } else {
        confiancaBase
    }
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
    val evidencia =
        decisao?.evidencia
            ?: resultado.evidencias.firstOrNull()?.let { "${it.label}: ${it.valorExibido}." }
            ?: "A análise não encontrou dados suficientes para concluir."
    val internet = input?.internet
    val latenciaLivre = internet?.latencyMs
    val atrasoSobCarga = internet?.latencyMs?.let { it + (internet.bufferbloatMs ?: 0.0) }
    val titulo =
        if (veioDoNds) tituloAssistSeguro(resultado.objetivo, status) else decisao?.titulo ?: resultado.mensagemMotor
    val mensagem =
        if (veioDoNds) {
            mensagemAssistSegura(resultado.objetivo, status, atrasoSobCarga, latenciaLivre)
        } else {
            decisao?.mensagemUsuario ?: resultado.mensagemMotor
        }
    val passosRecomendacao =
        passosAssistSeguro(
            resultado = resultado,
            recomendacaoNds = diagnosticReport?.decisao?.recomendacaoPassos.orEmpty(),
            atrasoSobCarga = atrasoSobCarga,
            latenciaLivre = latenciaLivre,
        )
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
                text = "Baseado nas medições da sua rede",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
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
        diagnosticReport?.let { report ->
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "Confiança ${confiancaAssist(report)}",
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
                        text = "Sobre esta análise",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = dadosAusentesEmLinguagemHumana(dadosAusentes),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        }
        passosRecomendacao.takeIf { it.isNotEmpty() }?.let { steps ->
            Spacer(Modifier.height(LkSpacing.lg))
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
                contentDescription = if (detalhesAbertos) "Ocultar detalhes técnicos" else "Mostrar detalhes técnicos",
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
            Text("Escolher outra situação", style = MaterialTheme.typography.bodyLarge, color = c.primary)
        }
        Spacer(Modifier.height(LkSpacing.xl))
    }
}
