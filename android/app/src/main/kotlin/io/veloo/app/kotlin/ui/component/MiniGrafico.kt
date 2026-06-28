package io.signallq.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.PontoAoVivo

@Composable
fun MiniGrafico(
    pontos: List<PontoAoVivo>,
    fase: FaseSpeedtest,
    corFase: Color,
) {
    val isDl = fase == FaseSpeedtest.download
    val isUl = fase == FaseSpeedtest.upload
    if (!isDl && !isUl) return

    val valores: List<Float> =
        pontos.mapNotNull { p ->
            if (isDl) p.dl?.toFloat() else p.ul?.toFloat()
        }
    if (valores.size < 2) return

    val maxVal = valores.max().takeIf { it > 0f } ?: return

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .height(64.dp),
    ) {
        val w = size.width
        val h = size.height
        val padX = 2f
        val padYBottom = 4f
        val padYTop = 4f
        val drawH = h - padYBottom - padYTop
        val n = valores.size

        val path = Path()
        valores.forEachIndexed { i, v ->
            val x = padX + (i.toFloat() / (n - 1).toFloat()) * (w - padX * 2f)
            val y = (h - padYBottom) - (v / maxVal) * drawH
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = corFase,
            style =
                Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
        )
    }
}
