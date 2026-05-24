package io.linka.app.kotlin.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.core.database.MedicaoEntity
import io.linka.app.kotlin.feature.history.BlocoUptime
import io.linka.app.kotlin.feature.history.ResumoHistorico
import io.linka.app.kotlin.feature.history.TendenciaEstado
import io.linka.app.kotlin.feature.history.calcularTendencia
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.component.ProfileAvatarButton
import io.linka.app.kotlin.ui.component.rememberTopBarAlpha
import kotlinx.coroutines.launch
import java.util.Calendar

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun qualidadeLabel(m: MedicaoEntity): String {
    val dl = m.downloadMbps ?: return "--"
    return when {
        dl >= 100 -> "Excelente"
        dl >= 25 -> "Bom"
        dl >= 10 -> "Regular"
        else -> "Lento"
    }
}

private fun qualidadeColor(
    m: MedicaoEntity,
    c: LkTokens,
): Color {
    val dl = m.downloadMbps ?: return c.textTertiary
    return when {
        dl >= 100 -> LkColors.success
        dl >= 25 -> LkColors.accent
        dl >= 10 -> LkColors.warning
        else -> LkColors.error
    }
}

private fun networkIcon(m: MedicaoEntity): ImageVector =
    when (m.connectionType) {
        "wifi" -> Icons.Outlined.Wifi
        else -> Icons.Outlined.Speed
    }

private fun tipoLabel(m: MedicaoEntity): String =
    when (m.connectionType) {
        "wifi" -> "Wi-Fi"
        "cellular" -> "Celular"
        "ethernet" -> "Cabo"
        else -> m.connectionType
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

private fun mbpsStr(v: Double?): String = v?.let { "%.0f".format(it) } ?: "--"

private fun vereditoLabel(v: String?): String? =
    when (v) {
        "good" -> "Bom"
        "acceptable" -> "Aceitável"
        "poor" -> "Ruim"
        null -> null
        else -> v
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

// ─── Tendência ────────────────────────────────────────────────────────────────

@Composable
private fun TendenciaCard(
    resumo: ResumoHistorico,
    c: LkTokens,
) {
    val tendencia = calcularTendencia(resumo) ?: return
    val (estado, percentual) = tendencia
    val icon =
        when (estado) {
            TendenciaEstado.MELHOROU -> Icons.Outlined.TrendingUp
            TendenciaEstado.PIOROU -> Icons.Outlined.TrendingDown
            TendenciaEstado.ESTAVEL -> Icons.Outlined.TrendingFlat
        }
    val iconColor =
        when (estado) {
            TendenciaEstado.MELHOROU -> LkColors.success
            TendenciaEstado.PIOROU -> LkColors.error
            TendenciaEstado.ESTAVEL -> c.textSecondary
        }
    val titulo =
        when (estado) {
            TendenciaEstado.MELHOROU -> "Download $percentual% acima da sua média"
            TendenciaEstado.PIOROU -> "Download $percentual% abaixo da sua média"
            TendenciaEstado.ESTAVEL -> "Velocidade dentro do esperado"
        }
    val subtexto =
        when (estado) {
            TendenciaEstado.MELHOROU, TendenciaEstado.PIOROU -> "Comparado às últimas 5 medições"
            TendenciaEstado.ESTAVEL -> "Consistente com as últimas 5 medições"
        }
    val semanticDesc =
        when (estado) {
            TendenciaEstado.MELHOROU -> "Tendência de velocidade: download $percentual por cento acima da média"
            TendenciaEstado.PIOROU -> "Tendência de velocidade: download $percentual por cento abaixo da média"
            TendenciaEstado.ESTAVEL -> "Tendência de velocidade: estável"
        }
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = semanticDesc },
        border = BorderStroke(1.dp, c.border),
        colors = CardDefaults.cardColors(containerColor = c.bgCard),
        shape = RoundedCornerShape(LkRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(LkSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, color = iconColor)
                Text(subtexto, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    historico: List<MedicaoEntity>,
    blocoUptime: List<BlocoUptime> = emptyList(),
    narrativaUptime: String = "",
    resumoHistorico: ResumoHistorico? = null,
    nomeUsuario: String = "",
    fotoUri: String? = null,
    onAbrirPerfil: () -> Unit = {},
    onIniciarTeste: () -> Unit = {},
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()

    // ── Estados dos dois sheets exclusivos ──
    var selectedMedicao by remember { mutableStateOf<MedicaoEntity?>(null) }
    var mostrarExport by remember { mutableStateOf(false) }

    val sheetDetalheState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetExportState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val topBarAlpha = listState.rememberTopBarAlpha()

    Scaffold(
        containerColor = c.bgPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.graphicsLayer { alpha = topBarAlpha },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.History, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text("Histórico", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W600, color = c.textPrimary)
                    }
                },
                navigationIcon = {
                    ProfileAvatarButton(
                        nomeUsuario = nomeUsuario,
                        fotoUri = fotoUri,
                        onClick = onAbrirPerfil,
                    )
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
                        enabled = historico.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SaveAlt,
                            contentDescription = "Exportar histórico",
                            tint = if (historico.isNotEmpty()) c.textPrimary else c.textTertiary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        if (historico.isEmpty()) {
            EmptyHistorico(modifier = Modifier.fillMaxSize().padding(padding), onIniciarTeste = onIniciarTeste)
        } else {
            val maxValue =
                remember(historico) {
                    maxOf(
                        historico.flatMap { listOf(it.downloadMbps ?: 0.0, it.uploadMbps ?: 0.0) }.maxOrNull() ?: 100.0,
                        100.0,
                    )
                }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                // ── Botão medir agora ──
                item(key = "medir_agora") {
                    Button(
                        onClick = onIniciarTeste,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LkRadius.card),
                        colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text(
                            "Medir agora",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W600,
                        )
                    }
                }

                // ── TendenciaCard (aparece antes do uptime quando há >= 2 medições) ──
                resumoHistorico?.let { resumo ->
                    if (resumo.totalMedicoes >= 2) {
                        item(key = "tendencia") {
                            TendenciaCard(resumo = resumo, c = c)
                        }
                    }
                }

                items(historico, key = { it.id }) { medicao ->
                    HistoricoCard(
                        medicao = medicao,
                        maxValue = maxValue,
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

    // ── Sheet de detalhe de medicao ──
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

    // ── Sheet de export ──
    if (mostrarExport) {
        ModalBottomSheet(
            onDismissRequest = { mostrarExport = false },
            sheetState = sheetExportState,
            containerColor = c.bgCard,
            dragHandle = {},
        ) {
            ExportHistoricoBottomSheet(
                historico = historico,
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
) {
    val c = LocalLkTokens.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.History, null, tint = c.textTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(LkSpacing.lg))
            Text(
                "Nenhum teste realizado ainda",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.W500,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                "Os resultados dos testes de velocidade\naparecerão aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.lg))
            Button(onClick = onIniciarTeste) {
                Text("Fazer primeiro teste")
            }
        }
    }
}

// ─── List item ────────────────────────────────────────────────────────────────

@Composable
private fun HistoricoCard(
    medicao: MedicaoEntity,
    maxValue: Double,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    val dl = medicao.downloadMbps
    val cardDesc = "Medição de ${formatDate(medicao.timestampEpochMs)}, download ${dl?.let { "%.0f".format(it) } ?: "sem dados"} Mbps"

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    contentDescription = cardDesc
                }.clickable(onClick = onClick),
        shape = RoundedCornerShape(LkRadius.card),
        colors = CardDefaults.cardColors(containerColor = c.bgCard),
        border = BorderStroke(1.dp, c.border),
    ) {
        Column(
            modifier = Modifier.padding(LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            // Header: data + badges + rede
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatDate(medicao.timestampEpochMs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                if (medicao.fonte == "orbit") {
                    QualityBadge("ORBIT", LkColors.accent)
                    Spacer(Modifier.width(LkSpacing.xs))
                }
                if (medicao.contaminado) {
                    QualityBadge("ERRO", LkColors.error)
                    Spacer(Modifier.width(LkSpacing.xs))
                }
                Icon(networkIcon(medicao), null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text(tipoLabel(medicao), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            }

            // Barras de velocidade
            SpeedBar(value = medicao.downloadMbps, maxValue = maxValue, color = LkColors.accentBlue, arrowLabel = "↓")
            SpeedBar(value = medicao.uploadMbps, maxValue = maxValue, color = LkColors.accent, arrowLabel = "↑")

            // Footer: latência
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Language, null, tint = c.textTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                val latStr = medicao.latencyMs?.let { "%.0f ms".format(it) }
                val footerText = if (latStr != null) "${tipoLabel(medicao)} | $latStr" else tipoLabel(medicao)
                Text(footerText, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }
        }
    }
}

@Composable
private fun SpeedBar(
    value: Double?,
    maxValue: Double,
    color: Color,
    arrowLabel: String,
) {
    val c = LocalLkTokens.current
    val progress = if (value != null && maxValue > 0) (value / maxValue).coerceIn(0.0, 1.0).toFloat() else 0f
    val valueStr = value?.let { "$arrowLabel ${"%.1f".format(it)} Mbps" } ?: "$arrowLabel -- Mbps"
    val label = if (arrowLabel == "↓") "Download" else "Upload"
    val barDesc = "$label ${value?.let { "%.1f".format(it) } ?: "sem dados"} Mbps"

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = barDesc },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.bgSecondary),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color),
            )
        }
        Text(
            valueStr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.W600,
            color = color,
            maxLines = 1,
            modifier = Modifier.width(100.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun QualityBadge(
    label: String,
    color: Color,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.W700, color = color)
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

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = LkSpacing.xxl),
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                // Drag handle
                Box(
                    Modifier
                        .padding(vertical = LkSpacing.sm)
                        .width(36.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.border),
                )
                Spacer(Modifier.height(LkSpacing.sm))

                // Header
                Text(
                    "Detalhes do teste",
                    modifier = Modifier.padding(horizontal = LkSpacing.xl),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Text(
                    formatFullDate(medicao.timestampEpochMs),
                    modifier = Modifier.padding(horizontal = LkSpacing.xl),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )

                Spacer(Modifier.height(LkSpacing.lg))
                HorizontalDivider(color = c.border)
                Spacer(Modifier.height(LkSpacing.lg))

                // Primary metrics
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.xl),
                ) {
                    PrimaryMetric(
                        arrow = "↓",
                        arrowColor = LkColors.accent,
                        value = dl?.let { "%.1f".format(it) } ?: "--",
                        label = "Download",
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(c.border)
                            .align(Alignment.CenterVertically),
                    )
                    PrimaryMetric(
                        arrow = "↑",
                        arrowColor = LkColors.success,
                        value = ul?.let { "%.1f".format(it) } ?: "--",
                        label = "Upload",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(LkSpacing.lg))

                // Secondary metrics
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.xl),
                ) {
                    SecondaryMetric("Latência", latency?.let { "%.0f ms".format(it) } ?: "--", Modifier.weight(1f))
                    SecondaryMetric("Oscilação", jitter?.let { "%.1f ms".format(it) } ?: "--", Modifier.weight(1f))
                    SecondaryMetric("Perda", perda?.let { "%.1f%%".format(it) } ?: "--", Modifier.weight(1f))
                }

                Spacer(Modifier.height(LkSpacing.lg))
                HorizontalDivider(color = c.border)
            }
        }

        if (medicao.fonte == "orbit") {
            item { SheetRow("Origem", "Orbit (IA)", valueColor = LkColors.accent) }
            item { HorizontalDivider(color = c.border) }
        }
        item { SheetRow("Tipo de rede", tipoLabel(medicao)) }
        item { HorizontalDivider(color = c.border) }
        if (medicao.contaminado) {
            item { SheetRow("Resultado", "Contaminado", valueColor = LkColors.error) }
            item { HorizontalDivider(color = c.border) }
        }
        if (bufferbloat != null) {
            item { SheetRow("Bufferbloat", "%.0f ms".format(bufferbloat)) }
            item { HorizontalDivider(color = c.border) }
        }
        if (streaming != null) {
            item { SheetRow("Streaming", streaming) }
            item { HorizontalDivider(color = c.border) }
        }
        if (gamer != null) {
            item { SheetRow("Games", gamer) }
            item { HorizontalDivider(color = c.border) }
        }
        if (videoChamada != null) {
            item { SheetRow("Vídeo chamada", videoChamada) }
            item { HorizontalDivider(color = c.border) }
        }
        if (gargalo != null) {
            item { SheetRow("Gargalo identificado", gargalo, valueColor = LkColors.warning) }
            item { HorizontalDivider(color = c.border) }
        }
    }
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
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.W700, color = c.textPrimary, letterSpacing = (-1).sp)
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

@Composable
private fun SheetRow(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    val c = LocalLkTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: c.textPrimary,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
