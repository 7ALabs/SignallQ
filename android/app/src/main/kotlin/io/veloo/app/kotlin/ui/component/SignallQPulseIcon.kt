package io.signallq.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.signallq.app.feature.diagnostico.pulse.SignallQState
import io.signallq.app.ui.LkColors

/**
 * SignallQSymbol com camadas extras de glow ambiente para uso sobre fundos escuros
 * (tela SignallQ IA). O tamanho do contêiner é 2× o tamanho do ícone para acomodar
 * o halo externo sem cortar.
 */
@Composable
fun SignallQPulseIcon(
    state: SignallQState,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    val glowColor =
        when (state) {
            SignallQState.Success -> LkColors.success
            SignallQState.Warning -> LkColors.warning
            SignallQState.Critical -> LkColors.error
            else -> LkColors.accent
        }
    val container = size * 2.0f

    Box(
        modifier = modifier.size(container),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(container)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            // Outermost soft halo
            drawCircle(
                color = glowColor.copy(alpha = 0.04f),
                radius = size.toPx() * 0.95f,
                center = Offset(cx, cy),
            )
            // Mid glow
            drawCircle(
                color = glowColor.copy(alpha = 0.07f),
                radius = size.toPx() * 0.68f,
                center = Offset(cx, cy),
            )
            // Inner warm halo
            drawCircle(
                color = glowColor.copy(alpha = 0.11f),
                radius = size.toPx() * 0.45f,
                center = Offset(cx, cy),
            )
        }
        SignallQSymbol(state = state, size = size)
    }
}
