package io.linka.app.kotlin.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.feature.diagnostico.pulse.QuestionNode
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun ContextualQuestionCard(
    question: QuestionNode,
    onResponder: (OpcaoResposta) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    AnimatedContent(
        targetState = question,
        transitionSpec = {
            (slideInVertically(initialOffsetY = { it / 3 }) + fadeIn()) togetherWith
                (slideOutVertically(targetOffsetY = { -it / 3 }) + fadeOut())
        },
        label = "question-card",
        modifier = modifier,
    ) { q ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.bgCard)
                    .padding(16.dp),
        ) {
            Text(
                q.texto,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.lg))
            Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                q.opcoes.forEach { opcao ->
                    SuggestionChip(
                        onClick = { onResponder(opcao) },
                        label = { Text(opcao.label, style = MaterialTheme.typography.bodyMedium) },
                        colors =
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = LkColors.accent.copy(alpha = 0.08f),
                                labelColor = c.textPrimary,
                            ),
                        border =
                            SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = LkColors.accent.copy(alpha = 0.25f),
                            ),
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    )
                }
            }
        }
    }
}
