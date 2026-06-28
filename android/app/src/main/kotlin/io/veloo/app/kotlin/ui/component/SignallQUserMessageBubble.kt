package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

@Composable
fun SignallQUserMessageBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = LkSpacing.md),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(start = LkSpacing.xxl)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(LkColors.accent.copy(alpha = 0.12f))
                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
        ) {
            Text(
                text = text,
                color = c.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = Int.MAX_VALUE,
            )
        }
    }
}
