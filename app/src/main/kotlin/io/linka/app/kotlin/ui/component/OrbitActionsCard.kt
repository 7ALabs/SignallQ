package io.linka.app.kotlin.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.linka.app.kotlin.feature.diagnostico.ai.AiAcaoRecomendada
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun OrbitActionsCard(
    actions: List<AiAcaoRecomendada>,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bgSecondary)
                .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = LkColors.warning,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "O que fazer",
                style = MaterialTheme.typography.labelLarge,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        actions.forEach { action ->
            ActionRow(action = action)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionRow(action: AiAcaoRecomendada) {
    val c = LocalLkTokens.current
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(LkColors.accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(action.titulo, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            if (action.descricao.isNotBlank()) {
                Text(action.descricao, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}
