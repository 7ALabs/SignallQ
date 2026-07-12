package io.signallq.app.ui.component.ads

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Borda tracejada -- diferenciador visual do card de anuncio nativo em relacao a
 * qualquer Card organico do app (issue #555, guideline de disclosure do Google).
 * Compose nao tem `Modifier.border` tracejado nativo; desenhado manualmente.
 */
fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.dp,
): Modifier =
    drawBehind {
        val stroke =
            Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            )
        drawRoundRect(
            color = color,
            style = stroke,
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
