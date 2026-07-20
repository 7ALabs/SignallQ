package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignals
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.feature.history.ResumoHistorico
import io.signallq.app.ui.FiltroConexaoHistorico
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSheetDivider
import io.signallq.app.ui.component.LkSheetFrame
import io.signallq.app.ui.component.LkSheetInfoRow
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.Overline
import io.signallq.app.ui.component.ProfileAvatarButton
import io.signallq.app.ui.component.ads.NativeAdCard
import io.signallq.app.ui.component.ads.NativeAdSource
import kotlinx.coroutines.launch
import java.util.Calendar

// ─── Filtro enum ──────────────────────────────────────────────────────────────

private typealias FiltroTipo = FiltroConexaoHistorico

private val FiltroConexaoHistorico.label: String
    get() =
        when (this) {
            FiltroConexaoHistorico.TODOS -> "Todos"
            FiltroConexaoHistorico.WIFI -> "Wi-Fi"
            FiltroConexaoHistorico.MOVEL -> "Rede móvel"
        }

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Suppress("unused")
private fun qualidadeLabel(m: MedicaoEntity): String {
    val dl = m.downloadMbps ?: return "--"
    return when {
        dl >= 100 -> "Excelente"
        dl >= 25 -> "Bom"
        dl >= 10 -> "Regular"
        else -> "Lento"
    }
}

@Suppress("unused")
private fun qualidadeColor(
    m: MedicaoEntity,
    c: LkTokens,
): Color {
    val dl = m.downloadMbps ?: return c.textTertiary
    return when {
        dl >= 100 -> c.success
        dl >= 25 -> c.primary
        dl >= 10 -> c.warning
        else -> c.error
    }
}

private fun networkIcon(m: MedicaoEntity): ImageVector =
    when (m.connectionType) {
        "wifi" -> Icons.Outlined.Wifi
        else -> Icons.Outlined.Speed
    }

internal fun tipoLabel(m: MedicaoEntity): String =
    when (m.connectionType) {
        "wifi" -> "Wi-Fi" + bandaWifiSufixo(m.bandaWifi)
        EstadoConexao.movel.name -> "Celular"
        "ethernet" -> "Cabo"
        else -> m.connectionType
    }

/** GH#1027: " · 5GHz"/" · 2.4GHz" quando a banda foi capturada na medição, "" quando não (medição antiga). */
internal fun bandaWifiSufixo(bandaWifi: String?): String =
    when (bandaWifi) {
        BandaWifi.ghz5.name -> " · 5GHz"
        BandaWifi.ghz24.name -> " · 2.4GHz"
        else -> ""
    }

private fun formatDate(epochMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val h = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
    val m = "%02d".format(cal.get(Calendar.MINUTE))
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hoje $h:$m"
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Ontem $h:$m"
        else -> {
            val d = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
            val mo = "%02d".format(cal.get(Calendar.MONTH) + 1)
            "$d/$mo $h:$m"
        }
    }
}

private fun formatFullDate(epochMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val d = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
    val mo = "%02d".format(cal.get(Calendar.MONTH) + 1)
    val y = cal.get(Calendar.YEAR)
    val h = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
    val m = "%02d".format(cal.get(Calendar.MINUTE))
    return "$d/$mo/$y às $h:$m"
}

@Suppress("unused")
private fun mbpsStr(v: Double?): String = v?.let { "%.0f".format(it) } ?: "--"

private fun vereditoLabel(v: String?): String? =
    when (v) {
        "good" -> "Bom"
        "acceptable" -> "Aceitável"
        "poor" -> "Ruim"
        null -> null
        else -> v
    }

/** Veredito humano do bufferbloat -- mesma regua canonica de [MetricClassifier.classificarBufferbloat]. */
private fun bufferbloatVeredito(
    deltaMs: Double,
    c: LkTokens,
): Pair<String, Color> =
    when (MetricClassifier.classificarBufferbloat(deltaMs)) {
        MetricStatus.excelente, MetricStatus.bom -> "Baixo" to c.success
        MetricStatus.regular -> "Moderado" to c.warning
        MetricStatus.ruim, MetricStatus.critico -> "Alto" to c.error
        MetricStatus.inconclusivo -> "—" to c.primary
    }

private fun gargaloLabel(g: String?): String? =
    when (g) {
        null, "none" -> null
        "download" -> "Download"
        "upload" -> "Upload"
        "latency" -> "Latência"
        "jitter" -> "Oscilação"
        "packetLoss" -> "Perda de pacotes"
        "bufferbloat" -> "Bufferbloat"
        else -> g
    }

// ─── Filtros de conexão ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosConexao(
    filtroSelecionado: FiltroTipo,
    onFiltroChange: (FiltroTipo) -> Unit,
    c: LkTokens,
    compact: Boolean = false,
) {
    Row(
        modifier =
            if (compact) {
                // #1131 (bug 2) — largura fixa (220.dp) dividida em 3 pills de peso igual
                // forcava "Rede movel" a quebrar em duas linhas (nao cabia no espaco
                // reservado). Cada pill agora usa a largura do proprio conteudo em vez de
                // peso igual, e a Row inteira acompanha a soma dos filhos.
                Modifier
                    .clip(RoundedCornerShape(LkRadius.pill))
                    .background(c.surfaceContainer)
                    .padding(LkSpacing.xs)
            } else {
                Modifier
            },
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        FiltroTipo.entries.forEach { filtro ->
            if (compact) {
                Surface(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(LkRadius.pill))
                            .clickable { onFiltroChange(filtro) },
                    color = if (filtroSelecionado == filtro) c.secondaryContainer else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            filtro.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filtroSelecionado == filtro) c.onSecondaryContainer else c.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            } else {
                FilterChip(
                    selected = filtroSelecionado == filtro,
                    onClick = { onFiltroChange(filtro) },
                    label = { Text(filtro.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = c.secondaryContainer,
                            selectedLabelColor = c.onSecondaryContainer,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filtroSelecionado == filtro,
                            borderColor = c.border,
                            selectedBorderColor = c.secondaryContainer,
                        ),
                )
            }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    historico: List<MedicaoEntity>,
    resumoHistorico: ResumoHistorico? = null,
    nomeUsuario: String = "",
    fotoUri: String? = null,
    onAbrirPerfil: () -> Unit = {},
    onIniciarTeste: () -> Unit = {},
    filtroConexao: FiltroConexaoHistorico? = null,
    onFiltroConexaoChange: (FiltroConexaoHistorico) -> Unit = {},
    filtroOperadora: String? = null,
    onFiltroOperadoraChange: (String?) -> Unit = {},
    operadorasDisponiveis: List<String> = emptyList(),
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()

    var selectedMedicao by remember { mutableStateOf<MedicaoEntity?>(null) }
    var mostrarExport by remember { mutableStateOf(false) }
    // Issue #555 -- dispensar o anuncio e estado de sessao, nunca persistido.
    var nativeAdDismissedHistorico by remember { mutableStateOf(false) }

    // Controlled mode: use external state from AppShell/ViewModel
    // Uncontrolled mode: use internal session state
    var filtroConexaoInterno by remember { mutableStateOf(FiltroTipo.TODOS) }
    var filtroOperadoraInterno by remember { mutableStateOf<String?>(null) }
    val filtroConexaoAtivo = filtroConexao ?: filtroConexaoInterno
    val filtroOperadoraAtivo = if (filtroConexao != null) filtroOperadora else filtroOperadoraInterno

    val sheetDetalheState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetExportState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Modo controlado: AppShell/ViewModel já pré-filtrou a lista — não re-filtrar aqui,
    // pois isso causaria double-filter e lista sempre vazia ao selecionar MOVEL.
    // Modo não-controlado: aplica filtro local (sessão interna sem ViewModel).
    val historicoFiltrado =
        remember(historico, filtroConexaoAtivo, filtroOperadoraAtivo) {
            if (filtroConexao != null) {
                // Modo controlado: lista já vem filtrada do ViewModel.
                historico
            } else {
                // Modo não-controlado: filtra internamente.
                // #1096 -- exclui medicoes sinteticas do MonitoramentoWorker (fonte="monitor"),
                // que nao tem download/upload e nao devem aparecer na lista do Historico.
                historico
                    .filter { m -> m.fonte != "monitor" }
                    .filter { m ->
                        when (filtroConexaoAtivo) {
                            FiltroTipo.TODOS -> true
                            FiltroTipo.WIFI -> m.connectionType == "wifi"
                            FiltroTipo.MOVEL -> m.connectionType == EstadoConexao.movel.name
                        }
                    }.filter { m -> filtroOperadoraAtivo == null || m.operadoraMovel == filtroOperadoraAtivo }
            }
        }

    Scaffold(
        containerColor = c.bgPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = c.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text("Histórico", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W600, color = c.textPrimary)
                    }
                },
                navigationIcon = {
                    ProfileAvatarButton(nomeUsuario = nomeUsuario, fotoUri = fotoUri, onClick = onAbrirPerfil)
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (selectedMedicao != null) {
                                    sheetDetalheState.hide()
                                    selectedMedicao = null
                                }
                                mostrarExport = true
                            }
                        },
                        enabled = historicoFiltrado.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Exportar histórico",
                            tint = if (historicoFiltrado.isNotEmpty()) c.textPrimary else c.textTertiary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        val listaParaExibir = historicoFiltrado
        val filtroAtivo = filtroConexaoAtivo != FiltroTipo.TODOS

        if (listaParaExibir.isEmpty() && !filtroAtivo) {
            EmptyHistorico(
                modifier = Modifier.fillMaxSize().padding(padding),
                onIniciarTeste = onIniciarTeste,
                filtroAtivo = false,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                if (!nativeAdDismissedHistorico) {
                    item(key = "native_ad_historico") {
                        val nativeAd by rememberNativeAd(
                            adUnitId = AdUnitIds.para(AdSlot.HISTORICO),
                            contentSignal = NativeAdContentSignals.forSlot(AdSlot.HISTORICO),
                            eligible = adsEnabled,
                        )
                        NativeAdCard(
                            nativeAd = nativeAd,
                            source = NativeAdSource.ADMOB,
                            onDismiss = { nativeAdDismissedHistorico = true },
                        )
                    }
                }
                item(key = "medicoes_header") {
                    // GH: filtros ficavam na mesma Row do overline (SpaceBetween) e estouravam a
                    // largura em telas menores, cortando o grupo "Todos | WiFi | Rede Móvel".
                    // Movidos para abaixo do subtítulo, em largura total.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LkSectionOverline("Medições recentes")
                        Spacer(Modifier.height(LkSpacing.sm))
                        FiltrosConexao(
                            filtroSelecionado = filtroConexaoAtivo,
                            onFiltroChange = { novo ->
                                if (filtroConexao != null) {
                                    onFiltroConexaoChange(novo)
                                } else {
                                    filtroConexaoInterno = novo
                                }
                            },
                            c = c,
                            compact = true,
                        )
                    }
                }
                if (listaParaExibir.isEmpty()) {
                    item(key = "empty_filtro") {
                        EmptyHistorico(
                            modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.xxl),
                            onIniciarTeste = onIniciarTeste,
                            filtroAtivo = true,
                        )
                    }
                } else {
                    items(historicoFiltrado, key = { it.id }) { medicao ->
                        HistoricoCard(
                            medicao = medicao,
                            onClick = {
                                scope.launch {
                                    if (mostrarExport) {
                                        sheetExportState.hide()
                                        mostrarExport = false
                                    }
                                    selectedMedicao = medicao
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    val sel = selectedMedicao
    if (sel != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMedicao = null },
            sheetState = sheetDetalheState,
            containerColor = c.bgCard,
            dragHandle = {},
        ) {
            HistoricoDetailSheet(medicao = sel)
        }
    }

    if (mostrarExport) {
        ModalBottomSheet(
            onDismissRequest = { mostrarExport = false },
            sheetState = sheetExportState,
            containerColor = c.bgCard,
            dragHandle = {},
        ) {
            ExportHistoricoBottomSheet(
                historico = historicoFiltrado,
                snackbarHostState = snackbarHostState,
                onDismiss = { mostrarExport = false },
                onRetry = {
                    scope.launch {
                        sheetExportState.hide()
                        mostrarExport = false
                        mostrarExport = true
                    }
                },
            )
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyHistorico(
    modifier: Modifier = Modifier,
    onIniciarTeste: () -> Unit = {},
    filtroAtivo: Boolean = false,
) {
    val c = LocalLkTokens.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.History, null, tint = c.textTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(LkSpacing.lg))
            Text(
                if (filtroAtivo) "Nenhum teste para este filtro" else "Nenhum teste realizado ainda",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.W500,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                if (filtroAtivo) {
                    "Não há medições para o filtro selecionado.\nTente selecionar outro tipo de conexão."
                } else {
                    "Os resultados dos testes de velocidade\naparecerão aqui."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.lg))
            Button(onClick = onIniciarTeste) {
                Text(if (filtroAtivo) "Medir agora" else "Fazer primeiro teste")
            }
        }
    }
}

// ─── List item ────────────────────────────────────────────────────────────────

@Composable
private fun HistoricoCard(
    medicao: MedicaoEntity,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    val dl = medicao.downloadMbps
    val cardDesc = "Medição de ${formatDate(medicao.timestampEpochMs)}, download ${dl?.let { "%.0f".format(it) } ?: "sem dados"} Mbps"
    val valorPrincipal = dl ?: medicao.uploadMbps
    val valorCor =
        if ((valorPrincipal ?: 0.0) >= 30.0) c.success else c.warning

    LkSurfaceCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    contentDescription = cardDesc
                }.clickable(onClick = onClick),
        outlined = false,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(LkSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Icon(networkIcon(medicao), null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Text(
                formatDate(medicao.timestampEpochMs),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valorPrincipal?.let { "${"%.1f".format(it)} Mbps" } ?: "-- Mbps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W700,
                color = valorCor,
            )
        }
    }
}

// ─── Detail sheet ─────────────────────────────────────────────────────────────

@Composable
private fun HistoricoDetailSheet(medicao: MedicaoEntity) {
    val c = LocalLkTokens.current
    val dl = medicao.downloadMbps
    val ul = medicao.uploadMbps
    val latency = medicao.latencyMs
    val jitter = medicao.jitterMs
    val perda = medicao.perdaPercentual
    val bufferbloat = medicao.bufferbloatMs
    val streaming = vereditoLabel(medicao.vereditoStreaming)
    val gamer = vereditoLabel(medicao.vereditoGamer)
    val videoChamada = vereditoLabel(medicao.vereditoVideoChamada)
    val gargalo = gargaloLabel(medicao.gargaloPrimario)

    LkSheetFrame(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Detalhes do teste",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
        )
        Text(
            formatFullDate(medicao.timestampEpochMs),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        LkSheetDivider()
        Spacer(Modifier.height(LkSpacing.lg))
        Row(Modifier.fillMaxWidth()) {
            PrimaryMetric(
                arrow = "↓",
                arrowColor = c.primary,
                value = dl?.let { "%.1f".format(it) } ?: "--",
                label = "Download",
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(72.dp)
                    .background(c.outlineVariant)
                    .align(Alignment.CenterVertically),
            )
            PrimaryMetric(
                arrow = "↑",
                arrowColor = c.success,
                value = ul?.let { "%.1f".format(it) } ?: "--",
                label = "Upload",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(LkSpacing.lg))
        Row(Modifier.fillMaxWidth()) {
            SecondaryMetric("Latência", latency?.let { "%.0f ms".format(it) } ?: "--", Modifier.weight(1f))
            SecondaryMetric("Oscilação", jitter?.let { "%.1f ms".format(it) } ?: "--", Modifier.weight(1f))
            SecondaryMetric("Perda", perda?.let { "%.1f%%".format(it) } ?: "--", Modifier.weight(1f))
        }
        Spacer(Modifier.height(LkSpacing.lg))
        LkSheetDivider()

        if (medicao.fonte == "orbit") {
            // GH#505: accent puro sobre fundo escuro cai a ~3.1:1 (falha WCAG AA) — c.primary
            // já resolve para a variante clara em dark theme (SignallQTheme.kt), sem check manual.
            val origemColor = c.primary
            LkSheetInfoRow("Origem", "Diagnóstico gerado por IA", valueColor = origemColor)
            LkSheetDivider()
        }
        LkSheetInfoRow("Tipo de rede", tipoLabel(medicao))
        LkSheetDivider()
        if (medicao.contaminado) {
            LkSheetInfoRow("Resultado", "Pode não ser confiável", valueColor = c.warning)
            LkSheetDivider()
        }
        if (bufferbloat != null) {
            val (bloatVeredito, bloatColor) = bufferbloatVeredito(bufferbloat, c)
            LkSheetInfoRow("Bufferbloat", "${"%.0f".format(bufferbloat)} ms — $bloatVeredito", valueColor = bloatColor)
            LkSheetDivider()
        }
        if (streaming != null) {
            LkSheetInfoRow("Streaming", streaming, valueColor = historicoVerdictColor(streaming, c))
            LkSheetDivider()
        }
        if (gamer != null) {
            LkSheetInfoRow("Games", gamer, valueColor = historicoVerdictColor(gamer, c))
            LkSheetDivider()
        }
        if (videoChamada != null) {
            LkSheetInfoRow("Vídeo chamada", videoChamada, valueColor = historicoVerdictColor(videoChamada, c))
            LkSheetDivider()
        }
        if (gargalo != null) {
            LkSheetInfoRow("Gargalo identificado", gargalo, valueColor = c.warning)
        }

        val diagTexto = medicao.diagnosticoTexto
        if (!diagTexto.isNullOrBlank()) {
            Spacer(Modifier.height(LkSpacing.lg))
            DiagnosticoHistoricoSection(
                texto = diagTexto,
                origem = medicao.diagnosticoOrigem,
                problemas = medicao.diagnosticoProblemas,
                c = c,
            )
        }
    }
}

@Composable
private fun DiagnosticoHistoricoSection(
    texto: String,
    origem: String?,
    problemas: String?,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .padding(LkSpacing.lg),
    ) {
        Overline(texto = "Diagnóstico", color = c.textTertiary)
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = c.textPrimary,
            lineHeight = 22.sp,
        )
        if (!problemas.isNullOrBlank()) {
            val lista = problemas.split(";").filter { it.isNotBlank() }
            if (lista.isNotEmpty()) {
                Spacer(Modifier.height(LkSpacing.sm))
                lista.forEach { problema ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(c.warning),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            text = problema,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(LkSpacing.sm))
        // GH#505: accent puro sobre fundo escuro cai a ~3.1:1 (falha WCAG AA) — c.primary
        // já resolve para a variante clara em dark theme (SignallQTheme.kt), sem check manual.
        val origemColor = if (origem == "ia") c.primary else c.textTertiary
        Text(
            text = if (origem == "ia") "Gerado por IA" else "Diagnóstico local",
            style = MaterialTheme.typography.labelMedium,
            color = origemColor,
            fontWeight = FontWeight.W500,
        )
    }
}

private fun historicoVerdictColor(
    label: String,
    c: LkTokens,
): Color =
    when (label) {
        "Bom" -> c.success
        "Aceitável" -> c.warning
        "Ruim" -> c.error
        else -> c.warning
    }

@Composable
private fun PrimaryMetric(
    arrow: String,
    arrowColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(arrow, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.W700, color = arrowColor)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
            Spacer(Modifier.width(4.dp))
            Text("Mbps", style = MaterialTheme.typography.bodySmall, color = c.textSecondary, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
    }
}

@Composable
private fun SecondaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W600, color = c.textPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
    }
}
