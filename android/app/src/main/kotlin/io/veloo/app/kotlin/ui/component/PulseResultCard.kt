package io.signallq.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.feature.diagnostico.pulse.AiAnalysisEntry
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

@Composable
fun PulseResultCard(
    analysis: AiAnalysisEntry,
    modifier: Modifier = Modifier,
) {
    // Guarda: não renderiza card vazio — evita aparência cinza sem conteúdo
    if (analysis.content.isBlank()) return

    val c = LocalLkTokens.current
    var expanded by remember { mutableStateOf(true) }
    val preview = analysis.content.take(120).let { if (analysis.content.length > 120) "$it…" else it }
    val cardStateDesc = if (expanded) stringResource(R.string.cd_analise_ia_expandida) else stringResource(R.string.cd_analise_ia_recolhida)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .semantics { stateDescription = cardStateDesc }
                .clickable { expanded = !expanded }
                .padding(LkSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "Análise IA",
                style = MaterialTheme.typography.labelLarge,
                color = c.primary,
                modifier = Modifier.weight(1f),
            )
            if (analysis.isFallback) {
                Text("local", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.height(LkSpacing.sm))

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(analysis.content, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, lineHeight = 20.sp)
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(preview, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}
