package io.linka.app.kotlin.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun GaugeCircular(
    progressoGlobal: Float,
    rotulo: String,
    velocidadeMbps: Float,
    corFase: Color,
    unidade: String,
    tamanho: Dp = 220.dp,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val density = LocalDensity.current

    val progressoAnimado by animateFloatAsState(
        targetValue = progressoGlobal.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "gaugeProgress",
    )
    val corAnimada by animateColorAsState(
        targetValue = corFase,
        animationSpec = tween(durationMillis = 400),
        label = "gaugeColor",
    )

    val strokeWidthDp = 8.dp
    val strokeWidthPx = remember(density) { with(density) { strokeWidthDp.toPx() } }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(tamanho)) {
        Canvas(modifier = Modifier.size(tamanho)) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Arco de fundo
            drawArc(
                color = c.border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            if (progressoAnimado > 0f) {
                val sweep = 360f * progressoAnimado

                // Glow: arco mais largo e transparente sob o arco ativo
                drawArc(
                    color = corAnimada.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx * 3f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = corAnimada.copy(alpha = 0.30f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx * 1.8f, cap = StrokeCap.Round),
                )

                // Arco ativo principal
                drawArc(
                    color = corAnimada,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }

        // Centro: label + número + unidade
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (rotulo.isNotBlank()) {
                Text(
                    text = rotulo,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.W600,
                    color = corAnimada,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = formatarVelocidade(velocidadeMbps),
                // MD3 exception: hero display metric for speed gauge — intentionally exceeds displayLarge (57sp)
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = corAnimada,
                textAlign = TextAlign.Center,
                lineHeight = 72.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = unidadeDisplay(velocidadeMbps, unidade),
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = c.textTertiary,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

private fun formatarVelocidade(mbps: Float): String =
    when {
        mbps <= 0f -> "—"
        mbps >= 1000f -> "%.1f".format(mbps / 1000f)
        mbps >= 100f -> "%.0f".format(mbps)
        else -> "%.1f".format(mbps)
    }

private fun unidadeDisplay(
    mbps: Float,
    unidadeBase: String,
): String = if (mbps >= 1000f) "Gbps" else unidadeBase
