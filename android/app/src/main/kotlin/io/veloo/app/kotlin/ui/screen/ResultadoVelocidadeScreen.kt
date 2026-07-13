package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignals
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationType
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ResultadoPdfGenerator
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.AnaliseDetalhadaBottomSheet
import io.signallq.app.ui.component.LocalDeviceSection
import io.signallq.app.ui.component.OperadoraBottomSheet
import io.signallq.app.ui.component.OperadoraContactCard
import io.signallq.app.ui.component.Overline
import io.signallq.app.ui.component.ads.NativeAdCard
import io.signallq.app.ui.component.ads.NativeAdSource
import io.signallq.app.ui.component.mapLocalDeviceSectionUiState
import io.signallq.app.ui.component.rememberTopBarAlpha
import kotlinx.coroutines.launch

/**
 * Tela "Resultado do teste" — GH#536.
 *
 * Escopo reduzido de propósito: mostra só o essencial pra responder "minha internet
 * está boa? o que está ruim? o que eu faço agora?" — diagnóstico geral, badge de rede
 * e os 5 cards principais (Download/Upload/Latência/Oscilação/Perda). Tudo que era
 * relatório empilhado (experiência de uso, DNS, detalhes avançados, atalho de gaming,
 * aviso Anatel pesado, contato permanente com operadora) foi para dentro do bottom
 * sheet de diagnóstico detalhado por IA, aberto pelo CTA principal.
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
    analisadorState: AnalisadorState = AnalisadorState.Inativo,
    onAnalisarProblema: (String) -> Unit = {},
    onResetarAnalisador: () -> Unit = {},
    /** Snapshot do equipamento local (ONT/roteador), quando disponivel — GH#544,
     *  epic #547. Null ate a leitura opcional de equipamento (GH#543) ser
     *  produzida; a secao "Equipamento local" do diagnostico detalhado renderiza
     *  o estado "nenhum encontrado" nesse caso, nunca um card vazio. */
    localDevice: LocalNetworkDeviceSnapshot? = null,
    /** Recomendacao do Recommendation Engine (#790/#811/#812) para este diagnostico -- #813.
     *  null quando nao ha nada elegivel ou o usuario ja ocultou. */
    recommendationDecision: RecommendationDecision? = null,
    recommendationFeedback: RecommendationFeedbackType? = null,
    onRecommendationShown: () -> Unit = {},
    onRecommendationClicked: () -> Unit = {},
    onRecommendationFeedback: (RecommendationFeedbackType) -> Unit = {},
    onRecommendationDismissed: () -> Unit = {},
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
) {
    val c = LocalLkTokens.current
    val scrollState = rememberScrollState()
    val topBarAlpha = scrollState.rememberTopBarAlpha()
    val decisao = snapshotDiagnostico.relatorio?.decisao
    val decisaoTitulo = decisao?.titulo
    val decisaoMensagem = decisao?.mensagemUsuario
    val decisaoRecomendacao = decisao?.recomendacao
    var compartilhando by remember { mutableStateOf(false) }
    var showDiagnosticoSheet by remember { mutableStateOf(false) }
    var showOperadoraSheet by remember { mutableStateOf(false) }
    // 1a (GH#931) — "Analisar meu problema com IA" isolado em sheet proprio, aberto a partir
    // do sheet de diagnostico detalhado (nao mais inline dentro dele).
    var showAnalisadorSheet by remember { mutableStateOf(false) }
    // Issue #555 -- dispensar o anuncio e estado de sessao (some ate o proximo resultado
    // recompor a tela do zero); nunca persistido, nunca conta como feedback de recomendacao.
    var nativeAdDismissedResultado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val corDownload =
        remember(resultado.downloadMbps) {
            when {
                resultado.downloadMbps >= 50.0 -> LkColors.success
                resultado.downloadMbps >= 25.0 -> LkColors.warning
                else -> LkColors.error
            }
        }
    val corUpload =
        remember(resultado.uploadMbps, resultado.uploadNaoDetectado) {
            when {
                resultado.uploadNaoDetectado -> LkColors.warning
                resultado.uploadMbps >= 10.0 -> LkColors.success
                resultado.uploadMbps >= 3.0 -> LkColors.warning
                else -> LkColors.error
            }
        }
    val corPerda =
        remember(resultado.perdaPercentual) {
            when {
                resultado.perdaPercentual < 1.0 -> LkColors.success
                resultado.perdaPercentual < 3.0 -> LkColors.warning
                else -> LkColors.error
            }
        }
    val corLatencia =
        remember(resultado.latenciaMs) {
            when {
                resultado.latenciaMs < 20.0 -> LkColors.success
                resultado.latenciaMs < 60.0 -> LkColors.warning
                else -> LkColors.error
            }
        }
    val corJitter =
        remember(resultado.jitterMs) {
            when {
                resultado.jitterMs < 10.0 -> LkColors.success
                resultado.jitterMs < 30.0 -> LkColors.warning
                else -> LkColors.error
            }
        }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.graphicsLayer { alpha = topBarAlpha },
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
                            color = LkColors.accent,
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
                                contentDescription = "Compartilhar PDF do resultado",
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
                    text = decisaoTitulo ?: "Resultado do Teste",
                    style = MaterialTheme.typography.headlineMedium,
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
                        lineHeight = 20.sp,
                    )
                }

                // Badge discreto do tipo de rede
                ChipTipoRede(
                    connectionType = resultado.connectionType,
                    tecnologia = resultado.tecnologia,
                    c = c,
                )

                Spacer(Modifier.height(LkSpacing.xxl))

                // Cards principais: Download + Upload
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        label = "Download",
                        value = "%.1f".format(resultado.downloadMbps),
                        unit = "Mbps",
                        cor = corDownload,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(LkSpacing.md))
                    MetricCard(
                        label = "Upload",
                        value = if (resultado.uploadNaoDetectado) "—" else "%.1f".format(resultado.uploadMbps),
                        unit = if (resultado.uploadNaoDetectado) "não detectado" else "Mbps",
                        cor = corUpload,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(LkSpacing.md))

                // Cards principais: Latência + Oscilação
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        label = "Latência",
                        value = "%.0f".format(resultado.latenciaMs),
                        unit = "ms",
                        cor = corLatencia,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(LkSpacing.md))
                    MetricCard(
                        label = "Oscilação",
                        value = "%.0f".format(resultado.jitterMs),
                        unit = "ms",
                        cor = corJitter,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (resultado.uploadNaoDetectado) {
                    Spacer(Modifier.height(LkSpacing.md))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(LkColors.warning.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = LkColors.warning,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            text = "Upload não detectado — verifique a conexão",
                            style = MaterialTheme.typography.bodySmall,
                            color = LkColors.warning,
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
                            "Resultado pode conter interferência de outros apps"
                        }
                    Spacer(Modifier.height(LkSpacing.md))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(LkColors.warning.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = LkColors.warning,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            text = mensagemContaminacao,
                            style = MaterialTheme.typography.bodySmall,
                            color = LkColors.warning,
                        )
                    }
                }

                // Card principal: Perda
                Spacer(Modifier.height(LkSpacing.md))
                MetricCard(
                    label = "Perda",
                    value = "%.1f".format(resultado.perdaPercentual),
                    unit = "%",
                    cor = corPerda,
                    c = c,
                    modifier = Modifier.fillMaxWidth(),
                )

                // CTA principal — abre o diagnóstico detalhado por IA (bottom sheet).
                Spacer(Modifier.height(LkSpacing.xxl))
                Button(
                    onClick = { showDiagnosticoSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Ver recomendações para melhorar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Slot de anuncio nativo (issue #555) -- depois de todos os CTAs
                // organicos (aqui: "Ver recomendacoes para melhorar" acima; "Refazer
                // teste" e o disclaimer Anatel ficam na barra fixa do rodape, fora do
                // scroll -- colocar o anuncio la competiria com o CTA primario sempre
                // visivel, entao fica no fim do conteudo rolavel). Fonte da oferta:
                // tags reais do diagnostico quando o Recommendation Engine achou uma
                // recomendacao gratuita elegivel para este resultado; sem isso, o slot
                // ainda aparece (AdMob e sempre ativo), so sem sinal contextual extra.
                Spacer(Modifier.height(LkSpacing.xl))
                val tagIdsResultado =
                    remember(recommendationDecision) {
                        recommendationDecision?.matchedTags?.map { it.id }?.toSet() ?: emptySet()
                    }
                val nativeAdResultado by
                    rememberNativeAd(
                        adUnitId = AdUnitIds.para(AdSlot.RESULTADO),
                        contentSignal = NativeAdContentSignals.forSlot(AdSlot.RESULTADO, tagIdsResultado),
                        eligible = adsEnabled && !nativeAdDismissedResultado,
                    )
                if (!nativeAdDismissedResultado) {
                    NativeAdCard(
                        nativeAd = nativeAdResultado,
                        source = NativeAdSource.ADMOB,
                        onDismiss = { nativeAdDismissedResultado = true },
                    )
                }

                Spacer(Modifier.height(LkSpacing.xl))
            }

            HorizontalDivider(color = c.border, thickness = 1.dp)

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)
                        .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                        text = if (resultado.uploadNaoDetectado) "Testar upload novamente" else "Refazer teste",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                    )
                }
                // Disclaimer Anatel discreto — não é mais um alerta com fundo colorido,
                // só o lembrete de que velocidade pode variar por regulação vigente.
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    text = "Resultados de velocidade podem diferir do contratado conforme regulação Anatel vigente.",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showDiagnosticoSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showDiagnosticoSheet = false
                onRecommendationDismissed()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgPrimary,
        ) {
            DiagnosticoDetalhadoSheet(
                resultado = resultado,
                decisaoTitulo = decisaoTitulo,
                decisaoMensagem = decisaoMensagem,
                decisaoRecomendacao = decisaoRecomendacao,
                categoria = decisao?.categoriaOrigem,
                ispInfo = ispInfo,
                localizacaoServidor = localizacaoServidor,
                localDevice = localDevice,
                analisadorState = analisadorState,
                onAbrirAnalisador = { showAnalisadorSheet = true },
                onFalarComOperadora = { showOperadoraSheet = true },
                recommendationDecision = recommendationDecision,
                recommendationFeedback = recommendationFeedback,
                onRecommendationShown = onRecommendationShown,
                onRecommendationClicked = onRecommendationClicked,
                onRecommendationFeedback = onRecommendationFeedback,
                c = c,
            )
        }
    }

    if (showOperadoraSheet) {
        OperadoraBottomSheet(
            connectionType = resultado.connectionType,
            ispNome = ispInfo?.isp,
            operadoraMovel = operadoraMovel,
            onDismiss = { showOperadoraSheet = false },
        )
    }

    if (showAnalisadorSheet) {
        AnaliseDetalhadaBottomSheet(
            state = analisadorState,
            onAnalisarProblema = onAnalisarProblema,
            onResetar = onResetarAnalisador,
            onDismiss = { showAnalisadorSheet = false },
        )
    }
}

/**
 * Conteúdo do bottom sheet "Diagnóstico detalhado" — GH#536. Não é chat livre: é
 * uma leitura objetiva do mesmo diagnóstico já calculado (causa provável, impacto
 * prático, recomendações, orientação por tipo de rede) mais um atalho opcional pra
 * um diagnóstico mais específico por problema relatado (SIG-113, reaproveitado aqui
 * em vez de recriado).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticoDetalhadoSheet(
    resultado: ResultadoSpeedtest,
    decisaoTitulo: String?,
    decisaoMensagem: String?,
    decisaoRecomendacao: String?,
    categoria: String?,
    ispInfo: IspInfo?,
    localizacaoServidor: String?,
    localDevice: LocalNetworkDeviceSnapshot?,
    analisadorState: AnalisadorState,
    onAbrirAnalisador: () -> Unit,
    onFalarComOperadora: () -> Unit,
    recommendationDecision: RecommendationDecision?,
    recommendationFeedback: RecommendationFeedbackType?,
    onRecommendationShown: () -> Unit,
    onRecommendationClicked: () -> Unit,
    onRecommendationFeedback: (RecommendationFeedbackType) -> Unit,
    c: LkTokens,
) {
    var detalhesTecnicosExpandido by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = LkSpacing.xxl)
                .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column {
                Text(
                    text = "Diagnóstico detalhado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.W700,
                    color = c.textPrimary,
                )
                Text(
                    text = "Leitura objetiva do resultado — sem conversa livre",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.xl))

        // CAUSA PROVÁVEL — a informação mais importante da tela: peso tipográfico
        // reforçado (titleLarge) pra não competir com o resto (#833).
        if (decisaoTitulo != null || decisaoMensagem != null) {
            Overline(texto = "Causa provável", color = c.textTertiary)
            Spacer(Modifier.height(LkSpacing.sm))
            if (decisaoTitulo != null) {
                Text(
                    text = decisaoTitulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (decisaoMensagem != null) {
                Text(
                    text = decisaoMensagem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    lineHeight = 20.sp,
                )
            }
            Spacer(Modifier.height(LkSpacing.xl))
        }

        // IMPACTO PRÁTICO — compactado numa linha só (#833): os 3 vereditos cabem
        // lado a lado, sem precisar de 3 linhas + 2 divisores pra mesma informação.
        Overline(texto = "Impacto prático", color = c.textTertiary)
        Spacer(Modifier.height(LkSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            ImpactoPraticoChip(
                label = "Streaming",
                veredito = resultado.diagnosticoQualidade.vereditoStreaming,
                icon = Icons.Outlined.Tv,
                c = c,
                modifier = Modifier.weight(1f),
            )
            ImpactoPraticoChip(
                label = "Gaming",
                veredito = resultado.diagnosticoQualidade.vereditoGamer,
                icon = Icons.Outlined.SportsEsports,
                c = c,
                modifier = Modifier.weight(1f),
            )
            ImpactoPraticoChip(
                label = "Vídeo Chamada",
                veredito = resultado.diagnosticoQualidade.vereditoVideoChamada,
                icon = Icons.Outlined.Videocam,
                c = c,
                modifier = Modifier.weight(1f),
            )
        }

        // RECOMENDAÇÕES
        Spacer(Modifier.height(LkSpacing.xl))
        Overline(texto = "Recomendações", color = c.textTertiary)
        Spacer(Modifier.height(LkSpacing.sm))
        if (!decisaoRecomendacao.isNullOrBlank()) {
            RecomendacaoCard(texto = decisaoRecomendacao, c = c)
            Spacer(Modifier.height(LkSpacing.md))
        }

        // Recomendacao do Recommendation Engine (#790/#811/#812) — #813. Se o engine nao
        // achou nada elegivel, recommendationDecision e null e a secao inteira nao renderiza
        // (sem card vazio, sem placeholder).
        if (recommendationDecision != null) {
            RecommendationEngineCard(
                decision = recommendationDecision,
                feedback = recommendationFeedback,
                onShown = onRecommendationShown,
                onClicked = onRecommendationClicked,
                onFeedback = onRecommendationFeedback,
                c = c,
            )
            Spacer(Modifier.height(LkSpacing.md))
        }

        AnalisadorEntryRow(
            state = analisadorState,
            onAbrir = onAbrirAnalisador,
            c = c,
        )

        val mostrarContato = categoria == "isp" || categoria == "fibra"
        if (mostrarContato) {
            Spacer(Modifier.height(LkSpacing.md))
            val operadora = remember(ispInfo?.isp) { BancoOperadoras.resolver(ispInfo?.isp) }
            OperadoraContactCard(operadora = operadora)
            Spacer(Modifier.height(LkSpacing.xs))
            TextButton(onClick = onFalarComOperadora) {
                Text(text = "Falar com a operadora", color = c.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // DETALHES TÉCNICOS (expansível) — inclui Orientação por tipo de rede como
        // primeiro item interno (#833): é texto de referência/boilerplate por tipo
        // de conexão, não personalizado como a causa provável ou o diagnóstico da IA,
        // então não precisa competir na primeira dobra.
        Spacer(Modifier.height(LkSpacing.xl))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.bgSecondary),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            role = Role.Button
                            contentDescription = "Detalhes técnicos"
                            stateDescription = if (detalhesTecnicosExpandido) "expandido" else "recolhido"
                        }.clickable { detalhesTecnicosExpandido = !detalhesTecnicosExpandido }
                        .padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Detalhes técnicos",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier =
                        Modifier
                            .size(20.dp)
                            .rotate(if (detalhesTecnicosExpandido) 180f else 0f),
                )
            }
            AnimatedVisibility(visible = detalhesTecnicosExpandido) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg)
                            .padding(bottom = LkSpacing.lg),
                ) {
                    HorizontalDivider(color = c.border, thickness = 0.5.dp)
                    Spacer(Modifier.height(LkSpacing.md))
                    Overline(texto = "Orientação por tipo de rede", color = c.textTertiary)
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = orientacaoPorTipoDeRede(resultado.connectionType, resultado.tecnologia),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(LkSpacing.md))
                    HorizontalDivider(color = c.border, thickness = 0.5.dp)
                    Spacer(Modifier.height(LkSpacing.md))
                    DetalheRow("Bufferbloat", "%.0f ms".format(resultado.bufferbloatMs), c)
                    DetalheRow("Pico Download", "%.1f Mbps".format(resultado.peakDownloadMbps), c)
                    DetalheRow("Pico Upload", "%.1f Mbps".format(resultado.peakUploadMbps), c)
                    DetalheRow("Latência c/ carga ↓", "%.0f ms".format(resultado.latencyDownloadMs), c)
                    DetalheRow("Latência c/ carga ↑", "%.0f ms".format(resultado.latencyUploadMs), c)
                    if (resultado.stabilityScore in 0.0..1.0) {
                        DetalheRow("Estabilidade", "%.0f%%".format(resultado.stabilityScore * 100), c)
                    }
                    if (resultado.dnsLatencyMs != null) {
                        val dnsProvedor = resultado.dnsProvider ?: resultado.dnsResolverIp
                        DetalheRow(
                            "DNS" + (dnsProvedor?.let { " ($it)" } ?: ""),
                            "${resultado.dnsLatencyMs} ms",
                            c,
                        )
                    }
                    if (!localizacaoServidor.isNullOrBlank()) {
                        DetalheRow("Servidor", localizacaoServidor, c)
                    }
                    Spacer(Modifier.height(LkSpacing.md))
                    LocalDeviceSection(state = mapLocalDeviceSectionUiState(localDevice))
                }
            }
        }
    }
}

/** Orientação curta por tipo de conexão — não é chat, é texto fixo condicionado
 * ao tipo detectado. Extraída como função pura pra ser testável isoladamente. */
internal fun orientacaoPorTipoDeRede(
    connectionType: String?,
    tecnologia: String?,
): String =
    when {
        connectionType.equals("wifi", ignoreCase = true) ->
            "Conexão via Wi-Fi. Se o resultado ficou abaixo do esperado, teste perto do roteador " +
                "ou com um cabo de rede pra isolar se o problema é do Wi-Fi ou da internet contratada."
        connectionType.equals("movel", ignoreCase = true) -> {
            val tecLabel =
                when {
                    tecnologia?.contains("5G", ignoreCase = true) == true -> "5G"
                    tecnologia?.contains("4G", ignoreCase = true) == true ||
                        tecnologia?.contains("LTE", ignoreCase = true) == true -> "4G"
                    else -> "rede móvel"
                }
            "Conexão via $tecLabel. Sinal fraco e congestionamento da torre variam por local e horário — " +
                "repita o teste em outro ponto ou horário antes de concluir que há um problema fixo."
        }
        else ->
            "Tipo de conexão não identificado neste teste. Repita o teste conectado por Wi-Fi ou dados " +
                "móveis pra ter uma orientação mais precisa."
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
                    "Via Wi-Fi" to Icons.Rounded.Wifi
                connectionType.equals("movel", ignoreCase = true) -> {
                    val tecLabel =
                        when {
                            tecnologia == null -> "Via Rede Móvel"
                            tecnologia.contains("5G", ignoreCase = true) -> "Via 5G"
                            tecnologia.contains("4G", ignoreCase = true) ||
                                tecnologia.contains("LTE", ignoreCase = true) -> "Via 4G"
                            else -> "Via Rede Móvel"
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
                containerColor = c.bgSecondary,
                labelColor = c.textSecondary,
                iconContentColor = c.textSecondary,
            ),
        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = c.border),
    )
}

@Composable
private fun ImpactoPraticoChip(
    label: String,
    veredito: VereditoUso,
    icon: ImageVector,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val (cor, badgeLabel) =
        when (veredito) {
            VereditoUso.good -> LkColors.success to "Boa"
            VereditoUso.acceptable -> LkColors.warning to "Aceitável"
            VereditoUso.poor -> LkColors.error to "Ruim"
        }
    // Compacta os 3 vereditos de Impacto prático numa única linha (#833): o nome
    // completo ("Streaming", "Gaming"...) só existe pro leitor de tela, na tela
    // aparece ícone + veredito curto, cor semântica já carrega o significado.
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .background(cor.copy(alpha = 0.12f))
                .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.sm)
                .semantics(mergeDescendants = true) { contentDescription = "$label: $badgeLabel" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = cor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = badgeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.W600,
            color = cor,
        )
    }
}

@Composable
private fun DetalheRow(
    label: String,
    valor: String,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    cor: Color,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(LkSpacing.lg)
                .semantics(mergeDescendants = true) { contentDescription = "$label: $value $unit" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = cor,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
        )
    }
}

@Composable
private fun RecomendacaoCard(
    texto: String,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = LkColors.accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(LkSpacing.sm))
        Text(
            text = texto,
            style = MaterialTheme.typography.titleSmall,
            color = c.textSecondary,
            lineHeight = 18.sp,
        )
    }
}

/**
 * Recomendacao escolhida pelo Recommendation Engine (`RecommendationEngine.choose`,
 * `coreRecommendation`, issues #790/#811/#812) — GH#813. Mostra titulo, tipo e motivo
 * da decisao, com as 3 acoes de feedback do usuario (util / não útil / ocultar).
 *
 * `onShown` dispara uma unica vez por [RecommendationDecision.trackingId] — LaunchedEffect
 * so reexecuta se a key mudar, e o ViewModel tem uma guarda de idempotencia adicional, então
 * recomposição do Compose nunca duplica o evento `recommendation_shown`.
 */
@Composable
private fun RecommendationEngineCard(
    decision: RecommendationDecision,
    feedback: RecommendationFeedbackType?,
    onShown: () -> Unit,
    onClicked: () -> Unit,
    onFeedback: (RecommendationFeedbackType) -> Unit,
    c: LkTokens,
) {
    LaunchedEffect(decision.trackingId) { onShown() }
    var motivoExpandido by remember(decision.trackingId) { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .semantics {
                    role = Role.Button
                    contentDescription = "Recomendação: ${decision.recommendation.title}"
                    stateDescription = if (motivoExpandido) "expandido" else "recolhido"
                }.clickable {
                    if (!motivoExpandido) onClicked()
                    motivoExpandido = !motivoExpandido
                }.padding(LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendationTypeLabel(decision.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = decision.recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    lineHeight = 18.sp,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textTertiary,
                modifier =
                    Modifier
                        .size(20.dp)
                        .rotate(if (motivoExpandido) 180f else 0f),
            )
        }

        AnimatedVisibility(visible = motivoExpandido) {
            Text(
                text = decision.reason,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = LkSpacing.sm),
            )
        }

        Spacer(Modifier.height(LkSpacing.md))
        if (feedback == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                RecommendationFeedbackButton(
                    texto = "Útil",
                    icon = Icons.Outlined.ThumbUp,
                    c = c,
                    onClick = { onFeedback(RecommendationFeedbackType.HELPFUL) },
                )
                RecommendationFeedbackButton(
                    texto = "Não útil",
                    icon = Icons.Outlined.ThumbDown,
                    c = c,
                    onClick = { onFeedback(RecommendationFeedbackType.NOT_HELPFUL) },
                )
                RecommendationFeedbackButton(
                    texto = "Ocultar",
                    icon = Icons.Outlined.VisibilityOff,
                    c = c,
                    onClick = { onFeedback(RecommendationFeedbackType.HIDE) },
                )
            }
        } else {
            Text(
                text = "Obrigado pelo feedback — isso ajuda a melhorar as próximas recomendações.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
        }
    }
}

@Composable
private fun RecommendationFeedbackButton(
    texto: String,
    icon: ImageVector,
    c: LkTokens,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, contentPadding = ButtonDefaults.TextButtonContentPadding) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
    }
}

internal fun recommendationTypeLabel(type: RecommendationType): String =
    when (type) {
        RecommendationType.FREE_TIP -> "DICA GRATUITA"
        RecommendationType.TUTORIAL -> "TUTORIAL"
        RecommendationType.CONFIGURATION -> "CONFIGURAÇÃO"
        // Tipos monetizados desligados nas RecommendationFlags desta entrega (#813) --
        // labels aqui só por exaustividade do when, não devem aparecer na UI ainda.
        RecommendationType.AFFILIATE_PRODUCT -> "PRODUTO RECOMENDADO"
        RecommendationType.PARTNER_OFFER -> "OFERTA PARCEIRA"
        RecommendationType.OPERATOR_OFFER -> "OFERTA DA OPERADORA"
        RecommendationType.NATIVE_AD_FALLBACK -> "PUBLICIDADE"
    }

/**
 * Entrada compacta do fluxo "Analisar meu problema com IA" dentro do sheet de diagnóstico
 * detalhado — GH#931 (Fase 2 MD3). O fluxo completo (seletor de problema, loading, resultado,
 * erro) mora em `AnaliseDetalhadaBottomSheet`, sheet dedicado aberto por [onAbrir]; aqui só o
 * resumo do estado atual, pra não competir visualmente com o resto do diagnóstico.
 */
@Composable
private fun AnalisadorEntryRow(
    state: AnalisadorState,
    onAbrir: () -> Unit,
    c: LkTokens,
) {
    when (state) {
        is AnalisadorState.Inativo -> {
            OutlinedButton(
                onClick = onAbrir,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Text(
                    text = "Analisar meu problema com IA",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary,
                )
            }
        }
        is AnalisadorState.Analisando -> {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.bgSecondary)
                        .clickable(onClick = onAbrir)
                        .padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LkColors.accent,
                )
                Text(
                    text = "Analisando seu problema…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }
        is AnalisadorState.Resultado -> {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.bgSecondary)
                        .clickable(onClick = onAbrir)
                        .padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Overline(texto = "Diagnóstico da IA", color = c.textTertiary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Ver análise completa",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.rotate(-90f),
                )
            }
        }
        is AnalisadorState.Erro -> {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(LkColors.error.copy(alpha = 0.08f))
                        .clickable(onClick = onAbrir)
                        .padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = LkColors.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "A análise falhou — toque para tentar novamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LkColors.error,
                )
            }
        }
    }
}
