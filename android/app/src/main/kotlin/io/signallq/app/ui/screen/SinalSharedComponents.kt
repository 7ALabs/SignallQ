package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSectionOverline

// ─── Sinal: componentes compartilhados entre abas ─────────────────────────────

@Composable
internal fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    LkSectionOverline(text = text, modifier = modifier)
}

@Composable
internal fun BandFilterRow(
    selected: String,
    bands: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    counts: Map<String, Int>? = null,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.pill))
                .background(c.surfaceContainer)
                .padding(LkSpacing.xs)
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        bands.forEach { band ->
            val active = selected == band
            val label = counts?.get(band)?.let { "$band ($it)" } ?: band
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(LkRadius.pill))
                        .background(if (active) c.secondaryContainer else Color.Transparent)
                        .minimumInteractiveComponentSize()
                        .clickable { onSelect(band) }
                        .padding(horizontal = LkSpacing.base, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.W600 else FontWeight.W500,
                    color = if (active) c.onSecondaryContainer else c.textSecondary,
                )
            }
        }
    }
}
