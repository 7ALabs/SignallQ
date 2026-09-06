package io.signallq.app.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

/**
 * Ponto pulsante + rótulo -- indicador genérico de "isto está atualizando ao vivo". Generalizado
 * a partir do antigo `SilentSpeedtestIndicator` (GH#1668, sem consumidor até então -- 0 call
 * sites) para servir tanto o speedtest silencioso quanto `SinalWifiScreen`, em vez de duplicar o
 * mesmo ponto pulsante em dois arquivos.
 *
 * @param estatico quando `true`, remove a animação de pulso e mostra o ponto sólido -- critério
 * de aceite "redução de movimento mantém leitura" (issue #1668). O rótulo continua atualizando
 * normalmente por recomposição; só o efeito decorativo é removido. Ver
 * [io.signallq.app.ui.component.animacoesDoSistemaDesativadas].
 */
@Composable
fun LkLiveIndicator(
    label: String = "Teste em andamento",
    estatico: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        if (estatico) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(c.primary),
            )
        } else {
            val transition = rememberInfiniteTransition(label = "live-dot")
            val alpha by
                transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
                    label = "dot-alpha",
                )
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .alpha(alpha)
                        .background(c.primary),
            )
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
    }
}
