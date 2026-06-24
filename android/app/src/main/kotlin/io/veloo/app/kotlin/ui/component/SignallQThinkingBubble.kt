package io.veloo.app.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import io.veloo.app.feature.diagnostico.pulse.SignallQState
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LkSpacing
import io.veloo.app.ui.LocalLkTokens

private val BUBBLE_SHAPE = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)

@Composable
fun SignallQThinkingBubble(
    mensagem: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "thinking-dots")

    // 3 dots with staggered phase offset
    val scale1 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot1",
    )
    val scale2 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, delayMillis = 167, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot2",
    )
    val scale3 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, delayMillis = 334, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot3",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        SignallQSymbol(
            state = SignallQState.Collecting,
            size = 26.dp,
            modifier = Modifier.padding(top = LkSpacing.xs),
        )

        val tokens = LocalLkTokens.current
        Column {
            Row(
                modifier =
                    Modifier
                        .clip(BUBBLE_SHAPE)
                        .background(tokens.bgSecondary)
                        .padding(horizontal = LkSpacing.lg, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                ThinkingDot(scale = scale1)
                ThinkingDot(scale = scale2)
                ThinkingDot(scale = scale3)
            }

            if (mensagem.isNotBlank()) {
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun ThinkingDot(scale: Float) {
    Box(
        modifier =
            Modifier
                .scale(scale)
                .size(8.dp)
                .clip(CircleShape)
                .background(LkColors.accent.copy(alpha = 0.7f)),
    )
}
