package io.signallq.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignals
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.feature.diagnostico.MetricClassifier
import io.signallq.app.feature.diagnostico.MetricStatus
import io.signallq.app.feature.history.ResumoHistorico
import io.signallq.app.feature.history.TendenciaEstado
import io.signallq.app.feature.history.calcularTendencia
import io.signallq.app.ui.FiltroConexaoHistorico
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.ProfileAvatarButton
import io.signallq.app.ui.component.ads.NativeAdCard
import io.signallq.app.ui.component.ads.NativeAdSource
import io.signallq.app.ui.component.rememberTopBarAlpha
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

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
        EstadoConexao.movel.name -> "Celular"
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
private fun bufferbloatVeredito(deltaMs: Double): Pair<String, Color> =
    when (MetricClassifier.classificarBufferbloat(deltaMs)) {
        MetricStatus.excelente, MetricStatus.bom -> "Baixo" to LkColors.success
        MetricStatus.regular -> "Moderado" to LkColors.warning
        MetricStatus.ruim, MetricStatus.critico -> "Alto" to LkColors.error
        MetricStatus.inconclusivo -> "—" to LkColors.accent
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

// ─── Gráfico de linha ─────────────────────────────────────────────────────────

private data class TapLabel(
    val text: String,
    val x: Float,
    val y: Float,
)

@Composable
private fun LineChartGrafico(
    medicoes: List<MedicaoEntity>,
    c: LkTokens,
) {
    if (medicoes.isEmpty()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Sem dados para o filtro selecionado",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val ordered = remember(medicoes) { medicoes.sortedBy { it.timestampEpochMs } }

    val maxMbps =
        remember(ordered) {
            ordered
                .flatMap { listOf(it.downloadMbps ?: 0.0, it.uploadMbps ?: 0.0) }
                .maxOrNull()
                ?.coerceAtLeast(1.0) ?: 1.0
        }

    var tapLabel by remember { mutableStateOf<TapLabel?>(null) }

    val density = LocalDensity.current
    val chartHeightDp = 160.dp
    val yAxisWidthDp = 36.dp
    val dotRadiusDp = 4.dp
    val tapThresholdDp = 24.dp

    val dlPoints = remember(ordered) { mutableListOf<Pair<Float, Float>>() }
    val ulPoints = remember(ordered) { mutableListOf<Pair<Float, Float>>() }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Legenda
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = LkSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(LkColors.accent))
                Text("↓ Download", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(LkColors.accentBlue))
                Text("↑ Upload", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            }
        }

        Spacer(Modifier.height(LkSpacing.xs))

        Box(modifier = Modifier.fillMaxWidth().height(chartHeightDp)) {
            val yAxisWidthPx = with(density) { yAxisWidthDp.toPx() }
            val dotRadiusPx = with(density) { dotRadiusDp.toPx() }
            val tapThresholdPx = with(density) { tapThresholdDp.toPx() }
            val gridLevels = listOf(0.0, 0.33, 0.67, 1.0)

            androidx.compose.foundation.Canvas(
                modifier =
                    Modifier.fillMaxSize().pointerInput(ordered) {
                        detectTapGestures { tapOffset ->
                            if (ordered.isEmpty()) return@detectTapGestures
                            var closestDist = Float.MAX_VALUE
                            var closestLabel: TapLabel? = null
                            dlPoints.forEachIndexed { i, (px, py) ->
                                val dist = abs(tapOffset.x - px) + abs(tapOffset.y - py)
                                if (dist < closestDist && dist < tapThresholdPx * 2) {
                                    closestDist = dist
                                    val v = ordered.getOrNull(i)?.downloadMbps
                                    if (v != null) closestLabel = TapLabel("↓ ${"%.0f".format(v)} Mbps", px, py)
                                }
                            }
                            ulPoints.forEachIndexed { i, (px, py) ->
                                val dist = abs(tapOffset.x - px) + abs(tapOffset.y - py)
                                if (dist < closestDist && dist < tapThresholdPx * 2) {
                                    closestDist = dist
                                    val v = ordered.getOrNull(i)?.uploadMbps
                                    if (v != null) closestLabel = TapLabel("↑ ${"%.0f".format(v)} Mbps", px, py)
                                }
                            }
                            tapLabel = closestLabel
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val chartW = w - yAxisWidthPx
                val n = ordered.size
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                gridLevels.forEach { level ->
                    val y = h - (level * h).toFloat()
                    drawLine(
                        color = c.border,
                        start = Offset(yAxisWidthPx, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                }

                fun drawPolyline(
                    values: List<Double?>,
                    color: Color,
                    points: MutableList<Pair<Float, Float>>,
                ) {
                    points.clear()
                    val path = Path()
                    var firstPoint = true
                    values.forEachIndexed { i, v ->
                        val x = if (n == 1) yAxisWidthPx + chartW / 2f else yAxisWidthPx + (i.toFloat() / (n - 1)) * chartW
                        val frac = ((v ?: 0.0) / maxMbps).coerceIn(0.0, 1.0).toFloat()
                        val y = h - frac * h * 0.85f - h * 0.05f
                        points.add(Pair(x, y))
                        if (v != null) {
                            if (firstPoint) {
                                path.moveTo(x, y)
                                firstPoint = false
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                    }
                    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
                    values.forEachIndexed { i, v ->
                        if (v != null) {
                            val (px, py) = points[i]
                            drawCircle(color = color, radius = dotRadiusPx, center = Offset(px, py))
                        }
                    }
                }

                drawPolyline(ordered.map { it.downloadMbps }, LkColors.accent, dlPoints)
                drawPolyline(ordered.map { it.uploadMbps }, LkColors.accentBlue, ulPoints)
            }

            // Y-axis labels
            gridLevels.forEach { level ->
                val valueMbps = (level * maxMbps).roundToInt()
                val yFrac = (1.0 - level).toFloat()
                val yOffsetDp = chartHeightDp * yFrac
                Text(
                    text = "$valueMbps",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(top = yOffsetDp.coerceAtMost(chartHeightDp - 14.dp)),
                )
            }

            // Tap label popup
            val tl = tapLabel
            if (tl != null) {
                with(density) {
                    Surface(
                        modifier =
                            Modifier
                                .offset {
                                    IntOffset(
                                        (tl.x - 40.dp.toPx()).roundToInt().coerceAtLeast(0),
                                        (tl.y - 32.dp.toPx()).roundToInt().coerceAtLeast(0),
                                    )
                                }.clickable { tapLabel = null },
                        shape = RoundedCornerShape(6.dp),
                        color = c.bgCard,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, c.border),
                    ) {
                        Text(
                            text = tl.text,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // Eixo de tempo: sem isso, o gráfico não diz se cobre 3 dias ou 3 meses (P1)
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = yAxisWidthDp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatDateShort(ordered.first().timestampEpochMs),
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
            if (ordered.size > 1) {
                Text(
                    formatDateShort(ordered.last().timestampEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                )
            }
        }
    }
}

private fun formatDateShort(epochMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hoje"
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Ontem"
        else -> "%02d/%02d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
    }
}

// ─── Cards de média ───────────────────────────────────────────────────────────

@Composable
private fun MediaCards(
    medicoes: List<MedicaoEntity>,
    c: LkTokens,
) {
    val validas = remember(medicoes) { medicoes.filter { !it.contaminado } }
    val mediaDl =
        remember(validas) {
            val vals = validas.mapNotNull { it.downloadMbps }
            if (vals.isEmpty()) null else vals.average()
        }
    val mediaUl =
        remember(validas) {
            val vals = validas.mapNotNull { it.uploadMbps }
            if (vals.isEmpty()) null else vals.average()
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        MediaCard(
            label = "DOWNLOAD MÉDIO",
            value = mediaDl?.let { "%.0f Mbps".format(it) } ?: "--",
            color = LkColors.accent,
            modifier = Modifier.weight(1f),
            c = c,
        )
        MediaCard(
            label = "UPLOAD MÉDIO",
            value = mediaUl?.let { "%.0f Mbps".format(it) } ?: "--",
            color = LkColors.accentBlue,
            modifier = Modifier.weight(1f),
            c = c,
        )
    }
}

@Composable
private fun MediaCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    c: LkTokens,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = c.bgCard),
        border = BorderStroke(1.dp, c.border),
        shape = RoundedCornerShape(LkRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ─── Filtros de conexão ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosConexao(
    filtroSelecionado: FiltroTipo,
    onFiltroChange: (FiltroTipo) -> Unit,
    operadorasDisponiveis: List<String>,
    filtroOperadora: String?,
    onFiltroOperadoraChange: (String?) -> Unit,
    c: LkTokens,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
            FiltroTipo.entries.forEach { filtro ->
                FilterChip(
                    selected = filtroSelecionado == filtro,
                    onClick = { onFiltroChange(filtro) },
                    label = { Text(filtro.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LkColors.accent.copy(alpha = 0.15f),
                            selectedLabelColor = LkColors.accent,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filtroSelecionado == filtro,
                            borderColor = c.border,
                            selectedBorderColor = LkColors.accent,
                        ),
                )
            }
        }

        if (filtroSelecionado == FiltroTipo.MOVEL && operadorasDisponiveis.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                FilterChip(
                    selected = filtroOperadora == null,
                    onClick = { onFiltroOperadoraChange(null) },
                    label = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LkColors.accentBlue.copy(alpha = 0.15f),
                            selectedLabelColor = LkColors.accentBlue,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filtroOperadora == null,
                            borderColor = c.border,
                            selectedBorderColor = LkColors.accentBlue,
                        ),
                )
                operadorasDisponiveis.forEach { op ->
                    FilterChip(
                        selected = filtroOperadora == op,
                        onClick = { onFiltroOperadoraChange(op) },
                        label = { Text(op, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LkColors.accentBlue.copy(alpha = 0.15f),
                                selectedLabelColor = LkColors.accentBlue,
                            ),
                        border =
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filtroOperadora == op,
                                borderColor = c.border,
                                selectedBorderColor = LkColors.accentBlue,
                            ),
                    )
                }
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
    val topBarAlpha = listState.rememberTopBarAlpha()

    val operadorasDisponiveisAtivas =
        if (filtroConexao != null) {
            operadorasDisponiveis
        } else {
            remember(historico) {
                historico
                    .filter { it.connectionType == EstadoConexao.movel.name }
                    .mapNotNull { it.operadoraMovel }
                    .distinct()
                    .sorted()
            }
        }

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
                historico
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
                modifier = Modifier.graphicsLayer { alpha = topBarAlpha },
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
                            imageVector = Icons.Outlined.SaveAlt,
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
            val maxValue =
                remember(historicoFiltrado) {
                    maxOf(
                        historicoFiltrado.flatMap { listOf(it.downloadMbps ?: 0.0, it.uploadMbps ?: 0.0) }.maxOrNull() ?: 100.0,
                        100.0,
                    )
                }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                if (listaParaExibir.isNotEmpty()) {
                    item(key = "grafico_linha") {
                        LineChartGrafico(medicoes = historicoFiltrado, c = c)
                    }
                    item(key = "media_cards") {
                        MediaCards(medicoes = historicoFiltrado, c = c)
                    }
                }
                item(key = "filtros_conexao") {
                    FiltrosConexao(
                        filtroSelecionado = filtroConexaoAtivo,
                        onFiltroChange = { novo ->
                            if (filtroConexao != null) {
                                onFiltroConexaoChange(novo)
                            } else {
                                filtroConexaoInterno = novo
                                if (novo != FiltroTipo.MOVEL) filtroOperadoraInterno = null
                            }
                        },
                        operadorasDisponiveis = operadorasDisponiveisAtivas,
                        filtroOperadora = filtroOperadoraAtivo,
                        onFiltroOperadoraChange = { op ->
                            if (filtroConexao != null) {
                                onFiltroOperadoraChange(op)
                            } else {
                                filtroOperadoraInterno = op
                            }
                        },
                        c = c,
                    )
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
                    item(key = "medir_agora") {
                        Button(
                            onClick = onIniciarTeste,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(LkRadius.card),
                            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                            contentPadding = PaddingValues(vertical = 14.dp),
                        ) {
                            Text("Medir agora", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600)
                        }
                    }
                    resumoHistorico?.let { resumo ->
                        if (resumo.totalMedicoes >= 2) {
                            item(key = "tendencia") {
                                TendenciaCard(resumo = resumo, c = c)
                            }
                        }
                    }
                    // Slot de anuncio nativo (issue #555) -- depois do resumo de
                    // estabilidade (Tendencia), antes das medicoes recentes.
                    if (!nativeAdDismissedHistorico) {
                        item(key = "native_ad_historico") {
                            val nativeAd by
                                rememberNativeAd(
                                    adUnitId = AdUnitIds.para(AdSlot.HISTORICO),
                                    contentSignal = NativeAdContentSignals.forSlot(AdSlot.HISTORICO),
                                    eligible = adsEnabled,
                                )
                            NativeAdCard(
                                nativeAd = nativeAd,
                                source = NativeAdSource.ADMOB,
                                onDismiss = { nativeAdDismissedHistorico = true },
                                modifier = Modifier.padding(top = LkSpacing.xs),
                            )
                        }
                    }
                    items(historicoFiltrado, key = { it.id }) { medicao ->
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
                    IaBadge()
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
            SpeedBar(value = medicao.downloadMbps, maxValue = maxValue, color = LkColors.accentBlue, arrowLabel = "↓")
            SpeedBar(value = medicao.uploadMbps, maxValue = maxValue, color = LkColors.accent, arrowLabel = "↑")
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
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = barDesc },
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

/**
 * Badge de origem "diagnóstico gerado por IA" -- autoexplicativo por design: o icone
 * de sparkle (mesmo usado em todo o app pra IA, ver DiagVerdictHeroCard)
 * + rotulo "IA" comunicam a origem sem precisar de nome de marca nem texto explicativo.
 */
@Composable
private fun IaBadge() {
    // accent puro sobre fundo escuro cai a ~3.1:1 (falha WCAG AA) — em dark
    // theme usa a variante clara accentOnDark p/ texto/ícone (ver GH#505).
    val accentText = if (isSystemInDarkTheme()) LkColors.accentOnDark else LkColors.accent
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(LkColors.accent.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = accentText, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(2.dp))
        Text("IA", fontSize = 10.sp, fontWeight = FontWeight.W700, color = accentText)
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
                Row(Modifier.fillMaxWidth().padding(horizontal = LkSpacing.xl)) {
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
                Row(Modifier.fillMaxWidth().padding(horizontal = LkSpacing.xl)) {
                    SecondaryMetric("Latência", latency?.let { "%.0f ms".format(it) } ?: "--", Modifier.weight(1f))
                    SecondaryMetric("Oscilação", jitter?.let { "%.1f ms".format(it) } ?: "--", Modifier.weight(1f))
                    SecondaryMetric("Perda", perda?.let { "%.1f%%".format(it) } ?: "--", Modifier.weight(1f))
                }
                Spacer(Modifier.height(LkSpacing.lg))
                HorizontalDivider(color = c.border)
            }
        }
        if (medicao.fonte == "orbit") {
            item { SheetRow("Origem", "Diagnóstico gerado por IA", valueColor = LkColors.accent) }
            item { HorizontalDivider(color = c.border) }
        }
        item { SheetRow("Tipo de rede", tipoLabel(medicao)) }
        item { HorizontalDivider(color = c.border) }
        if (medicao.contaminado) {
            item { SheetRow("Resultado", "Pode não ser confiável", valueColor = LkColors.error) }
            item { HorizontalDivider(color = c.border) }
        }
        if (bufferbloat != null) {
            val (bloatVeredito, bloatCor) = bufferbloatVeredito(bufferbloat)
            item {
                SheetRow("Bufferbloat", "${"%.0f".format(bufferbloat)} ms — $bloatVeredito", valueColor = bloatCor)
            }
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
        val diagTexto = medicao.diagnosticoTexto
        if (!diagTexto.isNullOrBlank()) {
            item {
                DiagnosticoHistoricoSection(
                    texto = diagTexto,
                    origem = medicao.diagnosticoOrigem,
                    problemas = medicao.diagnosticoProblemas,
                    c = c,
                )
            }
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
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        Text(
            text = "DIAGNÓSTICO",
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.bgSecondary)
                    .padding(LkSpacing.lg),
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textPrimary,
                lineHeight = 20.sp,
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
                                    .background(LkColors.warning),
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
            Text(
                text = if (origem == "ia") "Gerado por IA" else "Diagnóstico local",
                style = MaterialTheme.typography.labelSmall,
                color = if (origem == "ia") LkColors.accent else c.textTertiary,
                fontWeight = FontWeight.W500,
            )
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
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
                letterSpacing = (-1).sp,
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

@Composable
private fun SheetRow(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    val c = LocalLkTokens.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
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
