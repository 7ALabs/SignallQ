package io.signallq.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import androidx.compose.ui.res.stringResource
import io.signallq.app.R

enum class MetricStatus {
    OK,
    WARN,
    BAD,
    ;

    fun color(): Color =
        when (this) {
            OK -> LkColors.success
            WARN -> LkColors.warning
            BAD -> LkColors.error
        }
}

data class MetricItem(
    val label: String,
    val value: String,
    val status: MetricStatus,
    val note: String? = null,
)

@Composable
fun DiagMetricsGrid(
    metrics: List<MetricItem>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.diag_metrics_sinais),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = c.textTertiary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) stringResource(R.string.diag_metrics_recolher) else stringResource(R.string.diag_metrics_expandir),
                fontSize = 11.sp,
                color = c.textTertiary,
                modifier = Modifier.clickable { onToggleExpand() },
            )
        }

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            MetricsGridContent(metrics = metrics)
        }
    }
}

@Composable
private fun MetricsGridContent(metrics: List<MetricItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { metric ->
                    MetricCell(metric = metric, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCell(
    metric: MetricItem,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val statusColor = metric.status.color()

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(c.bgSecondary)
                .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = metric.label,
                fontSize = 10.sp,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = metric.value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
        )
        if (metric.note != null) {
            Text(
                text = metric.note,
                fontSize = 9.5.sp,
                color = statusColor,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DiagMetricsGridExpandedPreview() {
    SignallQTheme {
        DiagMetricsGrid(
            metrics =
                listOf(
                    MetricItem("Download", "38.2 Mbps", MetricStatus.BAD, "19% do plano (200)"),
                    MetricItem("Upload", "41.8 Mbps", MetricStatus.OK),
                    MetricItem("Latência ociosa", "22 ms", MetricStatus.OK),
                    MetricItem("Bufferbloat", "+182 ms", MetricStatus.BAD),
                    MetricItem("Wi-Fi RSSI", "−74 dBm", MetricStatus.BAD),
                    MetricItem("Perda de pacotes", "1.4 %", MetricStatus.WARN),
                ),
            expanded = true,
            onToggleExpand = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DiagMetricsGridCollapsedPreview() {
    SignallQTheme {
        DiagMetricsGrid(
            metrics =
                listOf(
                    MetricItem("Download", "38.2 Mbps", MetricStatus.BAD, "19% do plano (200)"),
                    MetricItem("Upload", "41.8 Mbps", MetricStatus.OK),
                ),
            expanded = false,
            onToggleExpand = {},
        )
    }
}
