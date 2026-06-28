package io.signallq.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors

@Composable
fun AppBorderGlowEffect(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val alphaTarget = if (active) 0.55f else 0f
    val alpha by animateFloatAsState(
        targetValue = alphaTarget,
        animationSpec = tween(600),
        label = "border-alpha",
    )

    val transition = rememberInfiniteTransition(label = "border-glow")
    val hueShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "hue-shift",
    )

    if (alpha > 0f) {
        val baseColor = LkColors.accent
        val hue = baseColor.toHsl().let { (h, s, l) -> Color.hsl((h + hueShift) % 360f, s, l) }
        val strokeColor = hue.copy(alpha = alpha)

        Canvas(modifier = modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            drawRoundRect(
                brush =
                    Brush.sweepGradient(
                        colors = listOf(strokeColor, strokeColor.copy(alpha = 0f), strokeColor),
                        center = Offset(size.width / 2, size.height / 2),
                    ),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(width = stroke),
            )
        }
    }
}

private data class Hsl(
    val h: Float,
    val s: Float,
    val l: Float,
)

private fun Color.toHsl(): Hsl {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Hsl(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h =
        when (max) {
            r -> ((g - b) / d + (if (g < b) 6f else 0f)) / 6f
            g -> ((b - r) / d + 2f) / 6f
            else -> ((r - g) / d + 4f) / 6f
        }
    return Hsl(h * 360f, s, l)
}
