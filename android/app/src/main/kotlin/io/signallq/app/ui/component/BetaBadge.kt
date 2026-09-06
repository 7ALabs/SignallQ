package io.signallq.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

/**
 * Selo "BETA" -- pill com contorno e fundo transparente, cor adaptada ao tema
 * via [LocalLkTokens]. Usado para sinalizar recursos ainda em avaliação.
 */
@Composable
fun BetaBadge(modifier: Modifier = Modifier) {
    val c = LocalLkTokens.current
    Text(
        text = "BETA",
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.pill))
                .border(BorderStroke(1.dp, c.border), RoundedCornerShape(LkRadius.pill))
                .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = c.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
