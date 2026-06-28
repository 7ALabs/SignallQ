package io.signallq.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.feature.diagnostico.pulse.OpcaoResposta
import io.signallq.app.feature.diagnostico.pulse.SignallQState
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import kotlinx.coroutines.delay

private val BUBBLE_SHAPE = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)

/**
 * SignallQ "faz uma pergunta" como turno da conversa — a pergunta aparece como
 * um bubble à esquerda com as opções de resposta inline (chips).
 */
@Composable
fun SignallQInlineQuestion(
    texto: String,
    opcoes: List<OpcaoResposta>,
    onResponder: (OpcaoResposta) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        SignallQSymbol(
            state = SignallQState.AwaitingInput,
            size = 26.dp,
            modifier = Modifier.padding(top = LkSpacing.xs),
        )

        val tokens = LocalLkTokens.current
        Column {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(BUBBLE_SHAPE)
                        .background(tokens.bgSecondary)
                        .padding(horizontal = 16.dp, vertical = LkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textPrimary,
                    lineHeight = 20.sp,
                )

                if (opcoes.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    ) {
                        itemsIndexed(opcoes) { index, opcao ->
                            var visible by remember(opcao.id) { mutableStateOf(false) }
                            LaunchedEffect(opcao.id) {
                                delay(index * 80L)
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)),
                            ) {
                                SuggestionChip(
                                    onClick = { onResponder(opcao) },
                                    label = { Text(opcao.label, style = MaterialTheme.typography.labelLarge) },
                                    colors =
                                        SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = LkColors.accent.copy(alpha = 0.12f),
                                            labelColor = tokens.textPrimary,
                                        ),
                                    border =
                                        SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = LkColors.accent.copy(alpha = 0.4f),
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = "SignallQ IA",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Spacer(Modifier.width(40.dp))
    }
}
