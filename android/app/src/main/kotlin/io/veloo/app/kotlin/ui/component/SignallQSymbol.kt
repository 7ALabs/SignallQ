package io.veloo.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.veloo.app.feature.diagnostico.pulse.SignallQState
import io.veloo.app.ui.LkColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SignallQSymbol(
    state: SignallQState,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    /** Sobrescreve a cor derivada do [state]. Útil para colorir por [ResponseSource]:
     *  INSIGHT/LOCAL → roxo (0xFF6C2BFF), GEMMA → amarelo (0xFFFBBF24). */
    colorOverride: Color? = null,
) {
    val isActive = state == SignallQState.Collecting || state == SignallQState.Thinking || state == SignallQState.Analyzing

    val glowColor =
        colorOverride ?: when (state) {
            SignallQState.Success -> LkColors.success
            SignallQState.Warning -> LkColors.warning
            SignallQState.Critical -> LkColors.error
            else -> LkColors.accent
        }

    val animatedGlowColor by animateColorAsState(
        targetValue = glowColor,
        animationSpec = tween(600),
        label = "glow-color",
    )

    val ringScale by animateFloatAsState(
        targetValue = if (state == SignallQState.Success || state == SignallQState.Warning || state == SignallQState.Critical) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ring-scale",
    )

    val satelliteAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(400),
        label = "satellite-alpha",
    )

    val transition = rememberInfiniteTransition(label = "orbit-breathe")
    val breatheScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathe-scale",
    )
    val outerRingAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "outer-alpha",
    )
    val satelliteAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "satellite-angle",
    )

    val coreScale = if (isActive) breatheScale else ringScale
    val outerAlpha = if (isActive) outerRingAlpha else 0f

    Canvas(modifier = modifier.size(size)) {
        val cx = size.toPx() / 2
        val cy = size.toPx() / 2
        val r = size.toPx() / 2 * 0.65f * coreScale
        val orbitRadius = r * 1.4f

        if (outerAlpha > 0f) {
            drawCircle(
                color = animatedGlowColor.copy(alpha = outerAlpha),
                radius = orbitRadius,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        drawCircle(
            color = animatedGlowColor.copy(alpha = 0.25f),
            radius = r * 1.15f,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = animatedGlowColor,
            radius = r,
            style = Stroke(width = 2.5.dp.toPx()),
        )
        drawCircle(
            color = animatedGlowColor.copy(alpha = 0.08f),
            radius = r * 0.85f,
        )
        drawCircle(
            color = animatedGlowColor,
            radius = 4.dp.toPx(),
        )

        // Satellite dot orbiting the outer ring — radius scales with the symbol
        if (satelliteAlpha > 0f) {
            val satDotRadius = (r * 0.13f + 1.5.dp.toPx()).coerceIn(2.dp.toPx(), 5.dp.toPx())
            val angleRad = Math.toRadians(satelliteAngle.toDouble())
            val satX = cx + (orbitRadius * cos(angleRad)).toFloat()
            val satY = cy + (orbitRadius * sin(angleRad)).toFloat()
            drawCircle(
                color = animatedGlowColor.copy(alpha = satelliteAlpha),
                radius = satDotRadius,
                center = Offset(satX, satY),
            )
            // Soft glow behind the satellite
            drawCircle(
                color = animatedGlowColor.copy(alpha = satelliteAlpha * 0.25f),
                radius = satDotRadius * 1.7f,
                center = Offset(satX, satY),
            )
        }
    }
}

@Deprecated("Use SignallQSymbol", ReplaceWith("SignallQSymbol(state, modifier, size)"))
@Composable
fun SignallQPulseSymbol(
    state: SignallQState,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) = SignallQSymbol(state, modifier, size)
