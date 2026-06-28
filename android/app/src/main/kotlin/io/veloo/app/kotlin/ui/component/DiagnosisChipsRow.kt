package io.signallq.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.signallq.app.feature.diagnostico.pulse.OpcaoResposta
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import kotlinx.coroutines.delay

@Composable
fun DiagnosisChipsRow(
    chips: List<OpcaoResposta>,
    onSelect: (OpcaoResposta) -> Unit,
) {
    val c = LocalLkTokens.current

    LazyRow(
        contentPadding = PaddingValues(horizontal = LkSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        itemsIndexed(chips) { index, chip ->
            var visible by remember(chip.id) { mutableStateOf(false) }
            LaunchedEffect(chip.id) {
                delay(index * 80L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter =
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)),
            ) {
                SuggestionChip(
                    onClick = { onSelect(chip) },
                    label = { Text(chip.label, style = MaterialTheme.typography.labelLarge) },
                    colors =
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = LkColors.accent.copy(alpha = 0.1f),
                            labelColor = c.textPrimary,
                        ),
                    border =
                        SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = LkColors.accent.copy(alpha = 0.3f),
                        ),
                )
            }
        }
    }
}
