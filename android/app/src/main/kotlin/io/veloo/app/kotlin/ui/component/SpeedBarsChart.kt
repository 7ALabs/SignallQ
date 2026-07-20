package io.signallq.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens

// ── Filtro de tipo de conexão ──────────────────────────────────────────────────

private enum class FiltroConexao(
    val label: String,
) {
    TODOS("Todos"),
    WIFI("Wi-Fi"),
    MOVEL("Móvel"),
}

// ── Barra individual animada ───────────────────────────────────────────────────

@Composable
private fun SpeedBar(
    fraction: Float,
    color: Color,
    label: String,
    valueLabel: String,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(fraction) { triggered = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (triggered) fraction else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "bar_$label",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Label de valor no topo da barra
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelSmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        // A barra em si
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height((120 * animatedFraction).coerceAtLeast(2f).dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(color),
        )
        Spacer(Modifier.height(4.dp))
        // Label de tipo abaixo
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Shimmer placeholder ────────────────────────────────────────────────────────

@Composable
private fun SpeedBarsShimmer(
    medicoes: List<MedicaoEntity>,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg)
                .height(160.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(medicoes.size.coerceAtMost(20)) { index ->
            val fakeHeight = listOf(60, 90, 45, 110, 70, 85, 50, 100, 65, 80)[index % 10]
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(fakeHeight.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(c.border),
            )
        }
    }
}

// ── Estado vazio contextual ────────────────────────────────────────────────────

@Composable
private fun SpeedBarsEmpty(
    filtro: FiltroConexao,
    c: LkTokens,
) {
    val mensagem =
        when (filtro) {
            FiltroConexao.WIFI -> "Nenhum teste Wi-Fi registrado ainda."
            FiltroConexao.MOVEL -> "Nenhum teste móvel registrado ainda."
            FiltroConexao.TODOS -> "Nenhum teste registrado ainda."
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Componente público ─────────────────────────────────────────────────────────

@Composable
fun SpeedBarsChart(
    medicoes: List<MedicaoEntity>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val c = LocalLkTokens.current
    var filtroSelecionado by remember { mutableStateOf(FiltroConexao.TODOS) }

    val medicoesFiltradas =
        remember(medicoes, filtroSelecionado) {
            val base =
                when (filtroSelecionado) {
                    FiltroConexao.TODOS -> medicoes
                    FiltroConexao.WIFI -> medicoes.filter { it.connectionType == "wifi" }
                    FiltroConexao.MOVEL -> medicoes.filter { it.connectionType == EstadoConexao.movel.name }
                }
            base.takeLast(20)
        }

    val maxMbps =
        remember(medicoesFiltradas) {
            medicoesFiltradas.mapNotNull { it.downloadMbps }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .padding(vertical = LkSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Velocidade (Mbps)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                // Legenda de cores
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendaDot(color = c.primary, label = "Wi-Fi", c = c)
                    LegendaDot(color = c.secondary, label = "Móvel", c = c)
                }
            }

            // Chips de filtro
            Row(
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                FiltroConexao.entries.forEach { filtro ->
                    FilterChip(
                        selected = filtroSelecionado == filtro,
                        onClick = { filtroSelecionado = filtro },
                        label = {
                            Text(
                                filtro.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.primary.copy(alpha = 0.15f),
                                selectedLabelColor = c.primary,
                            ),
                        border =
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filtroSelecionado == filtro,
                                borderColor = c.border,
                                selectedBorderColor = c.primary,
                            ),
                    )
                }
            }

            // Gráfico
            when {
                isLoading -> SpeedBarsShimmer(medicoes, c)
                medicoesFiltradas.isEmpty() -> SpeedBarsEmpty(filtroSelecionado, c)
                else -> {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = LkSpacing.lg),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        medicoesFiltradas.forEach { medicao ->
                            val dl = medicao.downloadMbps
                            val fraction = if (dl != null) (dl / maxMbps).toFloat().coerceIn(0f, 1f) else 0f
                            val isWifi = medicao.connectionType == "wifi"
                            val barColor = if (isWifi) c.primary else c.secondary
                            val typeLabel = if (isWifi) "W" else medicao.operadoraMovel?.take(4) ?: "M"
                            val valueLabel = if (dl != null) "%.0f".format(dl) else "--"

                            SpeedBar(
                                fraction = fraction,
                                color = barColor,
                                label = typeLabel,
                                valueLabel = valueLabel,
                                c = c,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Rodapé com aviso de operadora
            if (!isLoading && medicoesFiltradas.any { it.connectionType == EstadoConexao.movel.name }) {
                Text(
                    text = "Testes móveis identificados pelo tipo de rede.",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    modifier = Modifier.padding(horizontal = LkSpacing.lg),
                )
            }
        }
    }
}

// ── Helper: ponto de legenda ───────────────────────────────────────────────────

@Composable
private fun LegendaDot(
    color: Color,
    label: String,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
    }
}
