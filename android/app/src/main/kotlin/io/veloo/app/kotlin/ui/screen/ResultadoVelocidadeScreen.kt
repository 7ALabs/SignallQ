package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignals
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ResultadoPdfGenerator
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.LkInfoCallout
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.ads.NativeAdCard
import io.signallq.app.ui.component.ads.NativeAdSource
import io.signallq.app.ui.component.corSemantica
import io.signallq.app.ui.component.labelPt
import kotlinx.coroutines.launch

/**
 * Tela "Resultado do teste" — GH#536.
 *
 * Escopo reduzido de propósito: mostra só o essencial pra responder "minha internet
 * está boa? o que está ruim? o que eu faço agora?" — diagnóstico geral, badge de rede
 * e os 5 cards principais (Download/Upload/Latência/Oscilação/Perda).
 *
 * Resumo pós-teste enxuto, sem despejo de dado técnico (Feature #550, issue #1475):
 * as 3 CTAs abaixo do card "Como sua internet deve funcionar" abrem cada uma seu próprio destino —
 * [DiagnosticoGuiadoScreen] (7 objetivos fechados, motor local + explicação por IA),
 * modo gamer (fluxo próprio, issue #1476) e [DetalhesTecnicosScreen] (métricas cruas).
 * Antes dessa issue, um único CTA abria a sheet automática "Análise detalhada"
 * (`DiagnosticoDetalhadoSheet`, retirada) com banner de IA e recomendação abertos
 * sozinhos ao entrar aqui — decisão do Luiz (comentário de #1474, 2026-07-26): o
 * banner/recomendação só aparece depois que o usuário escolhe um objetivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoVelocidadeScreen(
    resultado: ResultadoSpeedtest,
    snapshotDiagnostico: SnapshotDiagnostico,
    onTestarNovamente: () -> Unit,
    onIrParaHome: () -> Unit,
    onVoltar: () -> Unit = {},
    /** GH#784 — etapa "compartilhou" do funil do teste de velocidade (Uso do App,
     *  admin-worker). Disparado junto do compartilhamento real do PDF, nao antes. */
    onCompartilhar: () -> Unit = {},
    localizacaoServidor: String? = null,
    ispInfo: IspInfo? = null,
    operadoraMovel: String? = null,
    /** Só alimenta o PDF compartilhado (última análise de IA disponível, se houver)
     *  — não dispara mais análise automática nesta tela (issue #1475). */
    analisadorState: AnalisadorState = AnalisadorState.Inativo,
    /** Iniciar diagnóstico guiado por objetivo — issue #1475 ([DiagnosticoGuiadoScreen]). */
    onIniciarDiagnosticoGuiado: () -> Unit = {},
    /** Iniciar modo gamer (jogo → device → resultado) — issue #1476, ainda não
     *  implementado; o CTA já aparece no resumo por paridade com o protótipo #1474. */
    onIniciarModoGamer: () -> Unit = {},
    /** Ver detalhes técnicos (métricas cruas, sem IA/recomendação) — issue #1475
     *  ([DetalhesTecnicosScreen]). */
    onVerDetalhesTecnicos: () -> Unit = {},
    /** Recomendacao do Recommendation Engine (#790/#811/#812) para este diagnostico -- #813.
     *  Usada aqui só como sinal de conteúdo do anúncio nativo (`NativeAdContentSignals`);
     *  o card de recomendação em si mora em [DiagnosticoGuiadoScreen] agora (issue #1475). */
    recommendationDecision: RecommendationDecision? = null,
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
) {
    val c = LocalLkTokens.current
    val scrollState = rememberScrollState()
    val decisao = snapshotDiagnostico.relatorio?.decisao
    val decisaoTitulo = decisao?.titulo
    val decisaoMensagem = decisao?.mensagemUsuario
    var compartilhando by remember { mutableStateOf(false) }
    var metricasDetalhadasAbertas by remember { mutableStateOf(false) }
    // Issue #555 -- dispensar o anuncio e estado de sessao (some ate o proximo resultado
    // recompor a tela do zero); nunca persistido, nunca conta como feedback de recomendacao.
    var nativeAdDismissedResultado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // GH#1221 RF-06 / GH#1225 item C — classificador UNICO (core/diagnostico), a tela nao
    // mantem mais sua propria regua numerica (antes divergia do motor de diagnostico: 3
    // faixas Excelente/Regular/Ruim aqui vs. 6 faixas canonicas em MetricClassifier, com
    // limiares numericos diferentes para a MESMA metrica). "Perda" e rotulada como
    // ESTIMADA — GH#1221 RF-04, o metodo e por timeout de probes HTTP, nao medicao direta
    // de perda de pacotes IP.
    val statusDownload = remember(resultado.downloadMbps) { MetricClassifier.classificarDownload(resultado.downloadMbps) }
    val corDownload = statusDownload.corSemantica(c)
    val veredictoDownload = statusDownload.labelPt()

    val statusUpload =
        remember(resultado.uploadMbps, resultado.uploadNaoDetectado) {
            if (resultado.uploadNaoDetectado) MetricStatus.inconclusivo else MetricClassifier.classificarUpload(resultado.uploadMbps)
        }
    val corUpload = statusUpload.corSemantica(c)
    val veredictoUpload = statusUpload.labelPt()

    val statusPerda = remember(resultado.perdaPercentual) { MetricClassifier.classificarPerdaPacotes(resultado.perdaPercentual) }
    val corPerda = statusPerda.corSemantica(c)
    val veredictoPerda = statusPerda.labelPt()

    val statusLatencia = remember(resultado.latenciaMs) { MetricClassifier.classificarLatencia(resultado.latenciaMs) }
    val corLatencia = statusLatencia.corSemantica(c)
    val veredictoLatencia = statusLatencia.labelPt()

    val statusJitter = remember(resultado.jitterMs) { MetricClassifier.classificarJitter(resultado.jitterMs) }
    val corJitter = statusJitter.corSemantica(c)
    val veredictoJitter = statusJitter.labelPt()

    val statusBufferbloat = remember(resultado.bufferbloatMs) { MetricClassifier.classificarBufferbloat(resultado.bufferbloatMs) }
    val corBufferbloat = statusBufferbloat.corSemantica(c)
    val veredictoBufferbloat = statusBufferbloat.labelPt()

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Resultado do teste", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                    }
                },
                actions = {
                    if (compartilhando) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = c.primary,
                        )
                    } else {
                        IconButton(onClick = {
                            compartilhando = true
                            scope.launch {
                                ResultadoPdfGenerator.gerarECompartilhar(
                                    context = context,
                                    resultado = resultado,
                                    snapshotDiagnostico = snapshotDiagnostico,
                                    analisadorState = analisadorState,
                                    ispInfo = ispInfo,
                                    operadoraMovel = operadoraMovel,
                                    localizacaoServidor = localizacaoServidor,
                                )
                                onCompartilhar()
                                compartilhando = false
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Compartilhar resultado",
                                tint = c.textPrimary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(c.bgPrimary),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(padding)
                        .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Título + mensagem diagnóstico
                Text(
                    text = decisaoTitulo ?: "Resultado do teste",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                )

                if (decisaoMensagem != null) {
                    Spacer(Modifier.height(LkSpacing.sm))
                    Text(
                        text = decisaoMensagem,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                // Badge discreto do tipo de rede
                ChipTipoRede(
                    connectionType = resultado.connectionType,
                    tecnologia = resultado.tecnologia,
                    c = c,
                )

                Spacer(Modifier.height(LkSpacing.xl))

                // Cards principais: Download + Upload
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        label = "Velocidade de download",
                        value = "%.1f".format(resultado.downloadMbps),
                        unit = "Mbps",
                        cor = corDownload,
                        veredito = veredictoDownload,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(LkSpacing.md))
                    MetricCard(
                        label = "Velocidade de upload",
                        value = if (resultado.uploadNaoDetectado) "—" else "%.1f".format(resultado.uploadMbps),
                        unit = if (resultado.uploadNaoDetectado) "Não foi possível medir" else "Mbps",
                        cor = corUpload,
                        veredito = veredictoUpload,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(LkSpacing.md))
                TextButton(onClick = { metricasDetalhadasAbertas = !metricasDetalhadasAbertas }) {
                    Text(
                        text = if (metricasDetalhadasAbertas) "Ocultar detalhes da conexão" else "Ver detalhes da conexão",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(18.dp).rotate(if (metricasDetalhadasAbertas) 180f else 0f),
                    )
                }

                AnimatedVisibility(visible = metricasDetalhadasAbertas) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricCard(
                                label = "Tempo de resposta",
                                value = "%.0f".format(resultado.latenciaMs),
                                unit = "ms",
                                cor = corLatencia,
                                veredito = veredictoLatencia,
                                c = c,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(LkSpacing.md))
                            MetricCard(
                                label = "Variação do tempo de resposta",
                                value = "%.0f".format(resultado.jitterMs),
                                unit = "ms",
                                cor = corJitter,
                                veredito = veredictoJitter,
                                c = c,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(LkSpacing.md))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricCard(
                                // GH#1221 RF-04/#1219 — "perda de pacotes" sugere medicao direta;
                                // o metodo real e taxa de falha/timeout de probes HTTP (ver
                                // ResultadoSpeedtest.packetLossSource == "estimated"). Rotulo
                                // honesto sobre a metodologia, sem prometer mais precisao do
                                // que o teste realmente mede.
                                label = "Falhas estimadas na conexão",
                                value = "%.1f".format(resultado.perdaPercentual),
                                unit = "%",
                                cor = corPerda,
                                veredito = veredictoPerda,
                                c = c,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(LkSpacing.md))
                            MetricCard(
                                label = "Lentidão com a rede ocupada",
                                value = "%.0f".format(resultado.bufferbloatMs),
                                unit = "ms",
                                cor = corBufferbloat,
                                veredito = veredictoBufferbloat,
                                c = c,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (resultado.uploadNaoDetectado) {
                    Spacer(Modifier.height(LkSpacing.md))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(c.warning.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LkInfoCallout(
                            icon = Icons.Outlined.Info,
                            text = "Não consegui medir o envio de dados. Vamos tentar novamente.",
                            iconTint = c.warning,
                        )
                    }
                }

                // Integridade do teste — não é "informação secundária", é sobre a
                // confiabilidade dos números que acabaram de ser mostrados.
                if (resultado.contaminado) {
                    val faseInterrompida = resultado.diagnosticoFases.faseInterrompida
                    val interrompidoPorRedeMudou = faseInterrompida.contains("redeMudou", ignoreCase = true)
                    val mensagemContaminacao =
                        if (interrompidoPorRedeMudou) {
                            "O teste foi interrompido porque a conexão caiu ou mudou durante a medição. Tente novamente quando a rede estabilizar."
                        } else {
                            "Outros aplicativos podem ter afetado o resultado."
                        }
                    Spacer(Modifier.height(LkSpacing.md))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(c.warning.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LkInfoCallout(
                            icon = Icons.Outlined.Warning,
                            text = mensagemContaminacao,
                            iconTint = c.warning,
                        )
                    }
                }

                if (!metricasDetalhadasAbertas) {
                    Spacer(Modifier.height(LkSpacing.sm))
                    if (!nativeAdDismissedResultado) {
                        val nativeAd by rememberNativeAd(
                            adUnitId = AdUnitIds.para(AdSlot.RESULTADO),
                            contentSignal =
                                NativeAdContentSignals.forSlot(
                                    AdSlot.RESULTADO,
                                    recommendationDecision?.matchedTags?.map { it.id }?.toSet() ?: emptySet(),
                                ),
                            eligible = adsEnabled,
                        )
                        NativeAdCard(
                            nativeAd = nativeAd,
                            source = NativeAdSource.ADMOB,
                            onDismiss = { nativeAdDismissedResultado = true },
                        )
                    }
                }

                Spacer(Modifier.height(LkSpacing.xl))
                LkSectionOverline(text = "Como sua internet deve funcionar")
                Spacer(Modifier.height(LkSpacing.sm))
                LkSurfaceCard(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                ) {
                    Column {
                        ImpactoPraticoLinha(
                            label = "Vídeos em alta qualidade",
                            veredito = resultado.diagnosticoQualidade.vereditoStreaming,
                            icon = Icons.Outlined.Tv,
                            c = c,
                        )
                        HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
                        ImpactoPraticoLinha(
                            label = "Jogos online",
                            veredito = resultado.diagnosticoQualidade.vereditoGamer,
                            icon = Icons.Outlined.SportsEsports,
                            c = c,
                        )
                        HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
                        ImpactoPraticoLinha(
                            label = "Chamadas de vídeo",
                            veredito = resultado.diagnosticoQualidade.vereditoVideoChamada,
                            icon = Icons.Outlined.Videocam,
                            c = c,
                        )
                    }
                }

                // 3 CTAs do resumo pós-teste (issue #1475) — nada de dado técnico despejado
                // aqui, cada caminho abre seu próprio destino dedicado.
                Spacer(Modifier.height(LkSpacing.lg))
                Button(
                    onClick = onIniciarDiagnosticoGuiado,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Troubleshoot,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Descobrir o que está acontecendo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(LkSpacing.sm))
                OutlinedButton(
                    onClick = onIniciarModoGamer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SportsEsports,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Analisar jogos online",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                    )
                }
                Spacer(Modifier.height(LkSpacing.sm))
                TextButton(
                    onClick = onVerDetalhesTecnicos,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ManageSearch,
                        contentDescription = null,
                        tint = c.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.xs))
                    Text(
                        text = "Ver detalhes da conexão",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }

                Spacer(Modifier.height(LkSpacing.sm))
                HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
                Spacer(Modifier.height(LkSpacing.sm))

                OutlinedButton(
                    onClick = onTestarNovamente,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Refazer o teste",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                    )
                }
                TextButton(
                    onClick = onIrParaHome,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Voltar ao início",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }

                Spacer(Modifier.height(LkSpacing.xl))
            }
        }
    }
}

@Composable
private fun ChipTipoRede(
    connectionType: String?,
    tecnologia: String?,
    c: LkTokens,
) {
    val (label, icon) =
        remember(connectionType, tecnologia) {
            when {
                connectionType == null -> null
                connectionType.equals("wifi", ignoreCase = true) ->
                    "Teste feito pelo Wi-Fi" to Icons.Rounded.Wifi
                connectionType.equals("movel", ignoreCase = true) -> {
                    val tecLabel =
                        when {
                            tecnologia == null -> "Teste feito pela rede móvel"
                            tecnologia.contains("5G", ignoreCase = true) -> "Teste feito pelo 5G"
                            tecnologia.contains("4G", ignoreCase = true) ||
                                tecnologia.contains("LTE", ignoreCase = true) -> "Teste feito pelo 4G"
                            else -> "Teste feito pela rede móvel"
                        }
                    tecLabel to Icons.Rounded.CellTower
                }
                else -> null
            }
        } ?: return

    Spacer(Modifier.height(LkSpacing.sm))
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        },
        colors =
            SuggestionChipDefaults.suggestionChipColors(
                containerColor = c.surfaceContainer,
                labelColor = c.onSurfaceVariant,
                iconContentColor = c.onSurfaceVariant,
            ),
        border = null,
    )
}

@Composable
private fun ImpactoPraticoLinha(
    label: String,
    veredito: VereditoUso,
    icon: ImageVector,
    c: LkTokens,
) {
    val (cor, badgeLabel) =
        when (veredito) {
            VereditoUso.good -> c.success to "Ótimo"
            VereditoUso.acceptable -> c.warning to "Bom"
            VereditoUso.poor -> c.error to "Ruim"
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(LkSpacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = c.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = badgeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = cor,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(LkRadius.pill))
                    .background(cor.copy(alpha = 0.16f))
                    .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    cor: Color,
    veredito: String,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .padding(LkSpacing.lg)
                .semantics(mergeDescendants = true) { contentDescription = "$label: $value $unit, $veredito" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = cor,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = veredito,
            style = MaterialTheme.typography.labelSmall,
            color = cor,
        )
    }
}
