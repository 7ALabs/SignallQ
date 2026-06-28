package io.signallq.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SignallQSymbolSmall() {
    val color = Color(0xFFFBBF24)
    Canvas(modifier = Modifier.size(16.dp)) {
        val r = size.width * 0.32f
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = r * 1.15f,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = color,
            radius = r,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(color = color, radius = 2.dp.toPx())
    }
}
