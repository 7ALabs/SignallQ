package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
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
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationType
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.diagnostico.ai.ordenadasPorPrioridade
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.ResultadoPdfGenerator
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.AnaliseDetalhadaBottomSheet
import io.signallq.app.ui.component.LkInfoCallout
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.LocalDeviceSection
import io.signallq.app.ui.component.OperadoraBadge
import io.signallq.app.ui.component.OperadoraBottomSheet
import io.signallq.app.ui.component.ads.NativeAdCard
import io.signallq.app.ui.component.ads.NativeAdSource
import io.signallq.app.ui.component.corSemantica
import io.signallq.app.ui.component.labelPt
import io.signallq.app.ui.component.mapLocalDeviceSectionUiState
import io.signallq.app.ui.component.rememberResolvedOperadoraContact
import io.signallq.app.ui.component.rememberResolvedOperadoraIdentity
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
    /** `null` quando acionado automaticamente ao abrir a tela 1a (Análise detalhada,
     *  sem sintoma escolhido); preenchido quando vem do fluxo por sintoma
     *  (`AnaliseDetalhadaBottomSheet`). Mesmo mecanismo (GH#design-tobe-alinhamento,
     *  decisão do Luiz 2026-07-14) — ver `AppShellDiagnosticoState.onAnalisarProblema`. */
    onAnalisarProblema: (String?) -> Unit = {},
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
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
    /** GH#970 — resolucao de identidade/contato de operadora (nivel 1, catalogo local,
     *  sincrono). Sem I/O, sem corrotina — mesmo comportamento de sempre pras ~12
     *  operadoras principais. */
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? =
        { _, _ -> null },
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact? =
        { _, _ -> null },
    /** GH#970 — cadeia completa (local -> diretorio remoto do worker signallq-diagnostic ->
     *  fallback generico), so chamada quando o nivel 1 acima nao encontrou. */
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity =
        { nome, _ ->
            ResolvedOperadoraIdentity(
                displayName = nome ?: "Operadora",
                monograma = nome?.firstOrNull()?.uppercase() ?: "?",
                corMarca = null,
                logoRes = null,
                logoUrl = null,
                source = OperadoraSource.FALLBACK,
            )
        },
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact =
        { nome, _ ->
            ResolvedOperadoraContact(
                displayName = nome ?: "Operadora",
                sacPhone = null,
                whatsapp = null,
                site = null,
                source = OperadoraSource.FALLBACK,
            )
        },
) {
    val c = LocalLkTokens.current
    val scrollState = rememberScrollState()
    val decisao = snapshotDiagnostico.relatorio?.decisao
    val decisaoTitulo = decisao?.titulo
    val decisaoMensagem = decisao?.mensagemUsuario
    val decisaoRecomendacao = decisao?.recomendacao
    var compartilhando by remember { mutableStateOf(false) }
    var showDiagnosticoSheet by remember { mutableStateOf(false) }
    var showOperadoraSheet by remember { mutableStateOf(false) }
    var metricasDetalhadasAbertas by remember { mutableStateOf(false) }
    // 1a (GH#931) — "Analisar meu problema com IA" isolado em sheet proprio, aberto a partir
    // do sheet de diagnostico detalhado (nao mais inline dentro dele).
    var showAnalisadorSheet by remember { mutableStateOf(false) }
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
                    Text("Resultado", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
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
                    text = decisaoTitulo ?: "Resultado",
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
                        label = "Download",
                        value = "%.1f".format(resultado.downloadMbps),
                        unit = "Mbps",
                        cor = corDownload,
                        veredito = veredictoDownload,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(LkSpacing.md))
                    MetricCard(
                        label = "Upload",
                        value = if (resultado.uploadNaoDetectado) "—" else "%.1f".format(resultado.uploadMbps),
                        unit = if (resultado.uploadNaoDetectado) "não detectado" else "Mbps",
                        cor = corUpload,
                        veredito = veredictoUpload,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(LkSpacing.md))
                TextButton(onClick = { metricasDetalhadasAbertas = !metricasDetalhadasAbertas }) {
                    Text(
                        text = if (metricasDetalhadasAbertas) "Ocultar métricas detalhadas" else "Ver métricas detalhadas",
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
                                label = "Latência",
                                value = "%.0f".format(resultado.latenciaMs),
                                unit = "ms",
                                cor = corLatencia,
                                veredito = veredictoLatencia,
                                c = c,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(LkSpacing.md))
                            MetricCard(
                                label = "Oscilação",
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
                                label = "Perda estimada",
                                value = "%.1f".format(resultado.perdaPercentual),
                                unit = "%",
                                cor = corPerda,
                                veredito = veredictoPerda,
                                c = c,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(LkSpacing.md))
                            MetricCard(
                                label = "Atraso sob carga",
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
                            text = "Upload não detectado — verifique a conexão",
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
                            "Resultado pode conter interferência de outros apps"
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
                LkSectionOverline(text = "Experiência de uso")
                Spacer(Modifier.height(LkSpacing.sm))
                LkSurfaceCard(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                ) {
                    Column {
                        ImpactoPraticoLinha(
                            label = "Vídeos em 4K",
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
                            label = "Videochamadas",
                            veredito = resultado.diagnosticoQualidade.vereditoVideoChamada,
                            icon = Icons.Outlined.Videocam,
                            c = c,
                        )
                    }
                }

                // CTA principal — abre o diagnóstico detalhado por IA (bottom sheet).
                Spacer(Modifier.height(LkSpacing.lg))
                Button(
                    onClick = { showDiagnosticoSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Ver análise detalhada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

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
                        text = if (resultado.uploadNaoDetectado) "Testar novamente" else "Testar novamente",
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
                        text = "Ir para o início",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }

                Spacer(Modifier.height(LkSpacing.xl))
            }
        }
    }

    if (showDiagnosticoSheet) {
        ModalBottomSheet(
            // GH#1225 item H — fechar esta sheet (swipe, tap fora, voltar) NAO e uma acao
            // explicita sobre a recomendacao especifica: o usuario pode ter fechado sem
            // nunca ter rolado ate o card, ou so terminado de ler o resto do diagnostico.
            // O dismiss so deve ser registrado pelo botao "Ocultar" dentro do proprio
            // RecommendationEngineCard (ver onFeedback(HIDE) abaixo).
            onDismissRequest = { showDiagnosticoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surfaceContainerLow,
        ) {
            DiagnosticoDetalhadoSheet(
                resultado = resultado,
                decisaoTitulo = decisaoTitulo,
                decisaoMensagem = decisaoMensagem,
                decisaoRecomendacao = decisaoRecomendacao,
                decisaoStatus = decisao?.status,
                categoria = decisao?.categoriaOrigem,
                ispInfo = ispInfo,
                localizacaoServidor = localizacaoServidor,
                localDevice = localDevice,
                analisadorState = analisadorState,
                onAnalisarProblema = onAnalisarProblema,
                onAbrirAnalisador = { showAnalisadorSheet = true },
                onFalarComOperadora = { showOperadoraSheet = true },
                recommendationDecision = recommendationDecision,
                recommendationFeedback = recommendationFeedback,
                onRecommendationShown = onRecommendationShown,
                onRecommendationClicked = onRecommendationClicked,
                onRecommendationFeedback = onRecommendationFeedback,
                c = c,
                resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                resolveOperadoraContatoLocal = resolveOperadoraContatoLocal,
                resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
                resolveOperadoraContatoRemoto = resolveOperadoraContatoRemoto,
            )
        }
    }

    if (showOperadoraSheet) {
        OperadoraBottomSheet(
            connectionType = resultado.connectionType,
            ispNome = ispInfo?.isp,
            operadoraMovel = operadoraMovel,
            onDismiss = { showOperadoraSheet = false },
            resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
            resolveOperadoraContatoLocal = resolveOperadoraContatoLocal,
            resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
            resolveOperadoraContatoRemoto = resolveOperadoraContatoRemoto,
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
 *
 * Título/mensagem/recomendação do banner e do card de Recomendações são gerados pela
 * IA (`AnalisadorState.Resultado.titulo`/`resumo`/`acoes`, disparado automaticamente
 * ao abrir via `onAnalisarProblema(null)` — decisão do Luiz, 2026-07-16). O motor
 * determinístico local continua decidindo o veredito (`decisaoStatus`, cor/ícone do
 * banner) e serve de fallback textual (`decisaoTitulo`/`decisaoMensagem`/
 * `decisaoRecomendacao`) enquanto a IA carrega ou se a chamada falhar sem cair no
 * fallback local do próprio repositório.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticoDetalhadoSheet(
    resultado: ResultadoSpeedtest,
    decisaoTitulo: String?,
    decisaoMensagem: String?,
    decisaoRecomendacao: String?,
    decisaoStatus: DiagnosticStatus?,
    categoria: String?,
    ispInfo: IspInfo?,
    localizacaoServidor: String?,
    localDevice: LocalNetworkDeviceSnapshot?,
    analisadorState: AnalisadorState,
    onAnalisarProblema: (String?) -> Unit,
    onAbrirAnalisador: () -> Unit,
    onFalarComOperadora: () -> Unit,
    recommendationDecision: RecommendationDecision?,
    recommendationFeedback: RecommendationFeedbackType?,
    onRecommendationShown: () -> Unit,
    onRecommendationClicked: () -> Unit,
    onRecommendationFeedback: (RecommendationFeedbackType) -> Unit,
    c: LkTokens,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact,
) {
    var detalhesTecnicosExpandido by remember { mutableStateOf(false) }

    // GH#1225 criterio D — resultado CONTAMINATED/INCONCLUSIVE/PARTIAL nao pode alimentar
    // IA, Recommendation Engine ou contato com operadora como se fosse uma conclusao
    // confiavel. So MeasurementStatus.COMPLETE libera a analise conclusiva desta sheet.
    val resultadoValidoParaConclusao = resultado.status.liberaConclusaoCompleta

    // Dispara a analise por IA automaticamente ao abrir a sheet -- so quando ainda
    // nao ha resultado (Inativo) E o resultado e valido o suficiente para uma conclusao.
    // Reaproveita o MESMO estado/mecanismo do fluxo "Analisar meu problema com IA"
    // (problema = null aqui vs. sintoma escolhido la).
    LaunchedEffect(Unit) {
        if (resultadoValidoParaConclusao && analisadorState is AnalisadorState.Inativo) {
            onAnalisarProblema(null)
        }
    }

    val analiseIa = analisadorState as? AnalisadorState.Resultado
    val carregandoAnalise = analisadorState is AnalisadorState.Inativo || analisadorState is AnalisadorState.Analisando
    val tituloExibido = analiseIa?.titulo?.ifBlank { null } ?: decisaoTitulo
    val mensagemExibida = analiseIa?.resumo?.ifBlank { null } ?: analiseIa?.texto?.ifBlank { null } ?: decisaoMensagem
    val acaoPrincipal = analiseIa?.acoes?.ordenadasPorPrioridade()?.firstOrNull()
    val recomendacaoExibida = acaoPrincipal?.descricao?.ifBlank { null } ?: decisaoRecomendacao

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column {
                Text(
                    text = "Análise detalhada",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.textPrimary,
                )
                Text(
                    text = "Leitura objetiva do resultado",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.xl))

        if (!resultadoValidoParaConclusao) {
            ResultadoInvalidoBanner(status = resultado.status, c = c)
        } else {
            DiagnosticoStatusBanner(
                status = decisaoStatus,
                titulo = tituloExibido,
                mensagem = mensagemExibida,
                carregando = carregandoAnalise,
                c = c,
            )
        }

        Spacer(Modifier.height(LkSpacing.xl))

        // RECOMENDAÇÕES — o protótipo to-be abre direto daqui após o banner, sem
        // blocos intermediários de causa/impacto dentro desta sheet. Enquanto a IA
        // carrega, nao mostra recomendacao (evita exibir texto deterministico como
        // se ja fosse a leitura final). GH#1225 criterio D — resultado invalido
        // (contaminado/inconclusivo/parcial) nunca mostra recomendacao como conclusiva.
        if (resultadoValidoParaConclusao && !carregandoAnalise) {
            LkSectionOverline(text = "Recomendações")
            Spacer(Modifier.height(LkSpacing.sm))
            if (!recomendacaoExibida.isNullOrBlank()) {
                RecomendacaoCard(texto = recomendacaoExibida, c = c)
                Spacer(Modifier.height(LkSpacing.md))
            }
        }

        if (resultadoValidoParaConclusao && recommendationDecision != null) {
            LkSectionOverline(text = "Configurações")
            Spacer(Modifier.height(LkSpacing.sm))
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

        // GH#1245 -- quando o banner de veredito acima ja esta mostrando "carregando"
        // (mesmo `analisadorState`), este card mostraria o mesmo spinner/copy logo
        // abaixo -- duplicacao visual sem informacao nova. So esconde quando o
        // resultado e valido E o banner acima esta na fase de carregamento; no caso de
        // resultado invalido (banner acima nao renderiza loading, mostra
        // ResultadoInvalidoBanner) o convite manual continua visivel normalmente.
        val ocultarEntryRowPorDuplicacao = resultadoValidoParaConclusao && carregandoAnalise
        if (!ocultarEntryRowPorDuplicacao) {
            AnalisadorEntryRow(
                state = analisadorState,
                onAbrir = onAbrirAnalisador,
                c = c,
            )
        }

        // GH#1225 criterio D/K — contato com operadora exige resultado completo (nunca
        // sugerir escalonamento a partir de teste contaminado/inconclusivo/parcial).
        val mostrarContato = resultadoValidoParaConclusao && (categoria == "isp" || categoria == "fibra")
        if (mostrarContato) {
            Spacer(Modifier.height(LkSpacing.md))
            // GH#970 — local (sincrono, sem mudanca pras ~12 operadoras principais) ->
            // diretorio remoto (worker signallq-diagnostic) -> fallback generico.
            val identidade =
                rememberResolvedOperadoraIdentity(
                    ispNomeBruto = ispInfo?.isp,
                    viaMovel = false,
                    resolveLocal = resolveOperadoraIdentidadeLocal,
                    resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
                )
            val contato =
                rememberResolvedOperadoraContact(
                    ispNomeBruto = ispInfo?.isp,
                    viaMovel = false,
                    resolveLocal = resolveOperadoraContatoLocal,
                    resolveRemoteOrFallback = resolveOperadoraContatoRemoto,
                )
            OperadoraResumoCard(
                identidade = identidade,
                contato = contato,
                c = c,
            )
            Spacer(Modifier.height(LkSpacing.xs))
            OutlinedButton(
                onClick = onFalarComOperadora,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(text = "Falar com a operadora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600)
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
                    .background(c.surfaceContainer),
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
                    HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
                    Spacer(Modifier.height(LkSpacing.md))
                    LkSectionOverline(text = "Orientação por tipo de rede")
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = orientacaoPorTipoDeRede(resultado.connectionType, resultado.tecnologia),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(LkSpacing.md))
                    HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
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
                    "Teste realizado via Wi-Fi" to Icons.Rounded.Wifi
                connectionType.equals("movel", ignoreCase = true) -> {
                    val tecLabel =
                        when {
                            tecnologia == null -> "Teste realizado via rede móvel"
                            tecnologia.contains("5G", ignoreCase = true) -> "Teste realizado via 5G"
                            tecnologia.contains("4G", ignoreCase = true) ||
                                tecnologia.contains("LTE", ignoreCase = true) -> "Teste realizado via 4G"
                            else -> "Teste realizado via rede móvel"
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
private fun ImpactoPraticoChip(
    label: String,
    veredito: VereditoUso,
    icon: ImageVector,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val (cor, badgeLabel) =
        when (veredito) {
            VereditoUso.good -> c.success to "Boa"
            VereditoUso.acceptable -> c.warning to "Aceitável"
            VereditoUso.poor -> c.error to "Ruim"
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
private fun OperadoraResumoCard(
    identidade: ResolvedOperadoraIdentity?,
    contato: ResolvedOperadoraContact?,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (identidade != null) {
            OperadoraBadge(identidade = identidade, size = 40.dp)
        } else {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contato?.displayName ?: "Operadora",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                text = "Atendimento oficial disponível",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

/**
 * Banner de veredito da tela 1a. `status` vem do motor determinístico local
 * (`DiagnosticStatus` — ok/info/attention/critical/inconclusive) e decide cor/ícone;
 * `titulo`/`mensagem` vêm humanizados pela IA (`AnalisadorState.Resultado`,
 * ver [DiagnosticoDetalhadoSheet]). Antes, a cor era decidida por matching de texto
 * em cima do próprio título/mensagem (`textoBase.contains("saud")`...) — gambiarra
 * frágil que quebraria de vez com texto livre gerado pela IA; substituída por
 * `status`, que é sempre o veredito determinístico real.
 */
@Composable
private fun DiagnosticoStatusBanner(
    status: DiagnosticStatus?,
    titulo: String?,
    mensagem: String?,
    carregando: Boolean,
    c: LkTokens,
) {
    if (carregando) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.surfaceContainer)
                    .padding(LkSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = c.primary,
            )
            Text(
                text = "Analisando seu resultado com IA…",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }
        return
    }

    val positivo = status == DiagnosticStatus.ok || status == DiagnosticStatus.info
    val containerColor = if (positivo) c.successContainer else c.errorContainer
    val contentColor = if (positivo) c.onSuccessContainer else c.onErrorContainer
    val icon = if (positivo) Icons.Outlined.CheckCircle else Icons.Outlined.Error
    val tituloExibido = titulo ?: if (positivo) "Conexão saudável" else "Sinais de sobrecarga identificados"
    val mensagemExibida =
        mensagem ?: if (positivo) {
            "Não encontramos perda de pacotes, instabilidade ou latência fora do esperado nesta análise."
        } else {
            "A latência sob carga subiu além do esperado, indicando disputa de banda ou saturação da conexão."
        }

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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                text = tituloExibido,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = contentColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mensagemExibida,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

/**
 * GH#1225 criterio D — banner exibido no lugar de [DiagnosticoStatusBanner] quando o
 * resultado NAO e [MetricStatus] COMPLETE (a analise por IA/Recommendation Engine/contato
 * com operadora nem chega a ser disparada nesse caso — ver `resultadoValidoParaConclusao`
 * em [DiagnosticoDetalhadoSheet]). Mensagem varia por [MeasurementStatus] pra nao tratar
 * "rede mudou" e "poucas amostras" como o mesmo problema.
 */
@Composable
private fun ResultadoInvalidoBanner(
    status: MeasurementStatus,
    c: LkTokens,
) {
    val mensagem =
        when (status) {
            MeasurementStatus.CONTAMINATED ->
                "A conexão mudou durante o teste. Execute novamente mantendo a mesma rede para obter um resultado confiável."
            MeasurementStatus.INCONCLUSIVE ->
                "Poucas respostas válidas para calcular um resultado com confiança. Execute o teste novamente."
            MeasurementStatus.PARTIAL ->
                "Uma das fases do teste não foi concluída — os números acima refletem só o que foi medido de fato."
            MeasurementStatus.CANCELLED, MeasurementStatus.COMPLETE ->
                "Não foi possível concluir uma análise confiável para este teste."
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.warningContainer)
                .padding(LkSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = c.onWarningContainer,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = c.onWarningContainer,
        )
    }
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
                .background(c.surfaceContainer)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = c.primary,
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
                tint = c.primary,
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
                // GH#1225 item H — unica acao EXPLICITA que deve registrar "dismiss" desta
                // recomendacao (nao fechar a sheet de diagnostico como um todo, que pode nem
                // ter chegado a mostrar este card na tela).
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
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(LkRadius.pill),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        border = BorderStroke(1.dp, c.border),
    ) {
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

/** Título do card de resultado do [AnalisadorEntryRow] — distingue o laudo disparado
 * automaticamente pela tela 1a (`problemaRelatado == null`) da análise que o usuário pediu
 * por sintoma (`problemaRelatado != null`). Extraída como função pura pra ser testável
 * isoladamente (follow-up Lia, PR #1013). */
internal fun tituloResultadoAnalisadorEntryRow(problemaRelatado: String?): String =
    if (problemaRelatado == null) "Laudo pronto — toque para ver" else "Ver análise completa"

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
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Analisar meu problema com IA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Abrir diagnóstico específico por problema relatado",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
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
                    color = c.primary,
                )
                Text(
                    text = "Preparando o diagnóstico da IA…",
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
                    LkSectionOverline(text = "Diagnóstico da IA")
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = tituloResultadoAnalisadorEntryRow(state.problemaRelatado),
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
                        .background(c.error.copy(alpha = 0.08f))
                        .clickable(onClick = onAbrir)
                        .padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = c.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "A análise falhou — toque para tentar novamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.error,
                )
            }
        }
    }
}
