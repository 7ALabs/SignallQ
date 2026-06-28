package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SeveridadeBufferbloat
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ResultadoBitmapGenerator
import io.signallq.app.ui.component.OperadoraBottomSheet
import io.signallq.app.ui.component.OperadoraContactCard
import io.signallq.app.ui.component.rememberTopBarAlpha
import io.signallq.app.ui.screen.AnalisadorState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoVelocidadeScreen(
    resultado: ResultadoSpeedtest,
    snapshotDiagnostico: SnapshotDiagnostico,
    onTestarNovamente: () -> Unit,
    onIrParaHome: () -> Unit,
    onVoltar: () -> Unit = {},
    localizacaoServidor: String? = null,
    ispInfo: IspInfo? = null,
    operadoraMovel: String? = null,
    anatelBannerDismissed: Boolean = true,
    onDismissAnatelBanner: () -> Unit = {},
    analisadorState: AnalisadorState = AnalisadorState.Inativo,
    onAnalisarProblema: (String) -> Unit = {},
    onResetarAnalisador: () -> Unit = {},
) {
    val c = LocalLkTokens.current
    val scrollState = rememberScrollState()
    val topBarAlpha = scrollState.rememberTopBarAlpha()
    val decisao = snapshotDiagnostico.relatorio?.decisao
    val decisaoTitulo = decisao?.titulo
    val decisaoMensagem = decisao?.mensagemUsuario
    val decisaoRecomendacao = decisao?.recomendacao
    var expandida by remember { mutableStateOf(false) }
    var compartilhando by remember { mutableStateOf(false) }
    var showGamerSheet by remember { mutableStateOf(false) }
    var showOperadoraSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val corPerda =
        remember(resultado.perdaPercentual) {
            when {
                resultado.perdaPercentual < 1.0 -> LkColors.success
                resultado.perdaPercentual < 3.0 -> LkColors.warning
                else -> LkColors.error
            }
        }
    val corBloat =
        remember(resultado.severidadeBufferbloat) {
            when (resultado.severidadeBufferbloat) {
                SeveridadeBufferbloat.none -> LkColors.success
                SeveridadeBufferbloat.mild -> LkColors.warning
                SeveridadeBufferbloat.moderate, SeveridadeBufferbloat.severe -> LkColors.error
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

    if (showGamerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGamerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgSecondary,
        ) {
            GamerSheet(
                resultado = resultado,
                ultimaMedicao = null,
                c = c,
                onIrParaTeste = { showGamerSheet = false },
            )
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
                                ResultadoBitmapGenerator.gerarECompartilhar(
                                    context = context,
                                    resultado = resultado,
                                    diagnosticoHeadline = decisaoTitulo,
                                    diagnosticoStatus = decisao?.status,
                                )
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

                // Chip de tipo de rede — posição discreta antes dos valores de velocidade
                ChipTipoRede(
                    connectionType = resultado.connectionType,
                    tecnologia = resultado.tecnologia,
                    c = c,
                )

                Spacer(Modifier.height(LkSpacing.xxl))

                // 3. Row: DL + UL
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        label = "Download",
                        value = "%.1f".format(resultado.downloadMbps),
                        unit = "Mbps",
                        cor = LkColors.success,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(LkSpacing.md))
                    MetricCard(
                        label = "Upload",
                        value = if (resultado.uploadNaoDetectado) "—" else "%.1f".format(resultado.uploadMbps),
                        unit = if (resultado.uploadNaoDetectado) "não detectado" else "Mbps",
                        cor = if (resultado.uploadNaoDetectado) LkColors.warning else LkColors.accent,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(LkSpacing.md))

                // 4. Row: Latência + Jitter
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

                // 5. NOVO: Chip de contaminação (1-B)
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

                // 6. NOVO: Row: Perda + Bufferbloat (1-A)
                Spacer(Modifier.height(LkSpacing.md))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        label = "Perda",
                        value = "%.1f".format(resultado.perdaPercentual),
                        unit = "%",
                        cor = corPerda,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(LkSpacing.md))
                    MetricCard(
                        label = "Bufferbloat",
                        value = "%.0f".format(resultado.bufferbloatMs),
                        unit = "ms",
                        cor = corBloat,
                        c = c,
                        modifier = Modifier.weight(1f),
                    )
                }

                // 7. NOVO: Seção "EXPERIÊNCIA DE USO" (1-C)
                Spacer(Modifier.height(LkSpacing.xxl))
                Text(
                    text = "EXPERIÊNCIA DE USO",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(LkSpacing.sm))
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(c.bgSecondary)
                            .padding(LkSpacing.lg),
                ) {
                    VereditorRow(
                        label = "Streaming",
                        veredito = resultado.diagnosticoQualidade.vereditoStreaming,
                        icon = Icons.Outlined.Tv,
                        c = c,
                    )
                    HorizontalDivider(
                        color = c.border,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = LkSpacing.sm),
                    )
                    VereditorRow(
                        label = "Gaming",
                        veredito = resultado.diagnosticoQualidade.vereditoGamer,
                        icon = Icons.Outlined.SportsEsports,
                        c = c,
                    )
                    HorizontalDivider(
                        color = c.border,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = LkSpacing.sm),
                    )
                    VereditorRow(
                        label = "Vídeo Chamada",
                        veredito = resultado.diagnosticoQualidade.vereditoVideoChamada,
                        icon = Icons.Outlined.Videocam,
                        c = c,
                    )
                }

                // 8. NOVO: Card DNS (condicional) (1-D)
                if (resultado.dnsLatencyMs != null) {
                    Spacer(Modifier.height(LkSpacing.md))
                    val dnsTexto =
                        remember(resultado.dnsLatencyMs, resultado.dnsProvider, resultado.dnsResolverIp) {
                            buildString {
                                append("DNS")
                                val provedor = resultado.dnsProvider ?: resultado.dnsResolverIp
                                if (!provedor.isNullOrBlank()) append(": $provedor")
                                append(" · ${resultado.dnsLatencyMs} ms")
                            }
                        }
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
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = LkColors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.sm))
                        Text(
                            text = dnsTexto,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textSecondary,
                            lineHeight = 18.sp,
                        )
                    }
                }

                // 9. NOVO: Seção expandível "Detalhes avançados" (1-E)
                Spacer(Modifier.height(LkSpacing.md))
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
                                    contentDescription = "Detalhes avançados"
                                    stateDescription = if (expandida) "expandido" else "recolhido"
                                }.clickable { expandida = !expandida }
                                .padding(LkSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Detalhes avançados",
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
                                    .rotate(if (expandida) 180f else 0f),
                        )
                    }
                    AnimatedVisibility(visible = expandida) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = LkSpacing.lg)
                                    .padding(bottom = LkSpacing.lg),
                        ) {
                            HorizontalDivider(color = c.border, thickness = 0.5.dp)
                            Spacer(Modifier.height(LkSpacing.md))
                            DetalheRow("Pico Download", "%.1f Mbps".format(resultado.peakDownloadMbps), c)
                            DetalheRow("Pico Upload", "%.1f Mbps".format(resultado.peakUploadMbps), c)
                            DetalheRow("Latência c/ carga ↓", "%.0f ms".format(resultado.latencyDownloadMs), c)
                            DetalheRow("Latência c/ carga ↑", "%.0f ms".format(resultado.latencyUploadMs), c)
                            if (resultado.stabilityScore in 0.0..1.0) {
                                DetalheRow("Estabilidade", "%.0f%%".format(resultado.stabilityScore * 100), c)
                            }
                        }
                    }
                }

                // 10. NOVO: Linha de servidor (condicional) (1-F)
                if (!localizacaoServidor.isNullOrBlank()) {
                    Spacer(Modifier.height(LkSpacing.md))
                    Text(
                        text = localizacaoServidor,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 11. Card Bufferbloat — migrado da Home #172
                Spacer(Modifier.height(LkSpacing.md))
                BufferbloatCard(bufferbloatMs = resultado.bufferbloatMs.takeIf { it > 0.0 }, c = c)

                // 11b. Card Jogar Online — migrado da Home #173
                Spacer(Modifier.height(LkSpacing.md))
                GamerShortcutCard(c = c, onClick = { showGamerSheet = true })

                // 12. Banner Anatel (dismissível) — migrado da Home #171
                if (!anatelBannerDismissed) {
                    Spacer(Modifier.height(LkSpacing.md))
                    AnatelBanner(onDismiss = onDismissAnatelBanner, c = c)
                }

                // 13. RecomendacaoCard
                if (decisaoRecomendacao != null) {
                    Spacer(Modifier.height(LkSpacing.xxl))
                    RecomendacaoCard(texto = decisaoRecomendacao, c = c)
                }

                // 14. OperadoraContactCard — exibido quando o diagnóstico aponta problema no ISP
                val mostrarContato = decisao?.categoria == "isp"
                if (mostrarContato) {
                    val operadora = remember(ispInfo?.isp) { BancoOperadoras.resolver(ispInfo?.isp) }
                    Spacer(Modifier.height(LkSpacing.md))
                    OperadoraContactCard(operadora = operadora)
                }

                // 15. Analisador de problema (SIG-113)
                Spacer(Modifier.height(LkSpacing.xl))
                AnalisadorProblemaSection(
                    state = analisadorState,
                    onAnalisarProblema = onAnalisarProblema,
                    onResetar = onResetarAnalisador,
                    c = c,
                )

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
                Button(
                    onClick = onTestarNovamente,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = if (resultado.uploadNaoDetectado) "Testar upload novamente" else "Refazer teste",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
                Spacer(Modifier.height(LkSpacing.sm))
                TextButton(onClick = { showOperadoraSheet = true }) {
                    Text(
                        text = "Falar com a operadora",
                        color = c.textSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
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
private fun VereditorRow(
    label: String,
    veredito: VereditoUso,
    icon: ImageVector,
    c: LkTokens,
) {
    val (cor, badgeLabel) =
        when (veredito) {
            VereditoUso.good -> LkColors.success to "Boa"
            VereditoUso.acceptable -> LkColors.warning to "Aceitável"
            VereditoUso.poor -> LkColors.error to "Ruim"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label: $badgeLabel" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(LkSpacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(cor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = cor,
            )
        }
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
                .padding(LkSpacing.lg),
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

private val problemasPredefinidos = listOf(
    "Baixa velocidade",
    "Quedas constantes",
    "Travamentos em streaming ou jogos",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalisadorProblemaSection(
    state: AnalisadorState,
    onAnalisarProblema: (String) -> Unit,
    onResetar: () -> Unit,
    c: LkTokens,
) {
    var selecionandoProblema by remember { mutableStateOf(false) }

    // Quando o estado volta para Inativo (após reset), fecha o seletor de chips.
    if (state is AnalisadorState.Inativo && !selecionandoProblema) {
        OutlinedButton(
            onClick = { selecionandoProblema = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(
                text = "Analisar meu problema",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = c.textPrimary,
            )
        }
        return
    }

    if (state is AnalisadorState.Inativo && selecionandoProblema) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "QUAL É O SEU PROBLEMA?",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                problemasPredefinidos.forEach { problema ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            selecionandoProblema = false
                            onAnalisarProblema(problema)
                        },
                        label = { Text(text = problema, fontSize = 13.sp) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = c.bgSecondary,
                                labelColor = c.textPrimary,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(LkSpacing.xs))
            TextButton(onClick = { selecionandoProblema = false }) {
                Text(text = "Cancelar", color = c.textTertiary, fontSize = 13.sp)
            }
        }
        return
    }

    when (state) {
        is AnalisadorState.Analisando -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LkColors.accent,
                )
                Text(
                    text = "Analisando seu problema...",
                    fontSize = 14.sp,
                    color = c.textSecondary,
                )
            }
        }

        is AnalisadorState.Resultado -> {
            val origemLabel = if (state.origem == "ia") "Análise por IA" else "Diagnóstico local"
            val origemCor = if (state.origem == "ia") LkColors.accent else c.textTertiary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.card),
                colors = CardDefaults.cardColors(containerColor = c.bgSecondary),
            ) {
                Column(modifier = Modifier.padding(LkSpacing.lg)) {
                    Text(
                        text = "DIAGNÓSTICO",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textTertiary,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Text(
                        text = state.texto,
                        fontSize = 14.sp,
                        color = c.textPrimary,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(LkSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = origemLabel,
                            fontSize = 12.sp,
                            color = origemCor,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(onClick = {
                            selecionandoProblema = false
                            onResetar()
                        }) {
                            Text(text = "Nova análise", color = c.textTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        is AnalisadorState.Erro -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = state.mensagem, fontSize = 13.sp, color = LkColors.error)
                Spacer(Modifier.height(LkSpacing.xs))
                TextButton(onClick = {
                    selecionandoProblema = false
                    onResetar()
                }) {
                    Text(text = "Tentar novamente", color = c.textTertiary, fontSize = 13.sp)
                }
            }
        }

        else -> {}
    }
}
