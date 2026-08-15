package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

private const val SKELETON_LAST_LINE_FRACTION = 0.68f

enum class SignallQFeedbackTone { Neutral, Success, Warning, Error }

@Composable
fun SignallQBanner(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    tone: SignallQFeedbackTone = SignallQFeedbackTone.Neutral,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current
    val visual =
        when (tone) {
            SignallQFeedbackTone.Neutral -> Triple(c.surfaceContainerHigh, c.onSurface, Icons.Outlined.Info)
            SignallQFeedbackTone.Success -> Triple(c.successContainer, c.onSuccessContainer, Icons.Outlined.CheckCircle)
            SignallQFeedbackTone.Warning -> Triple(c.warningContainer, c.onWarningContainer, Icons.Outlined.WarningAmber)
            SignallQFeedbackTone.Error -> Triple(c.errorContainer, c.onErrorContainer, Icons.Outlined.ErrorOutline)
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = visual.first,
        contentColor = visual.second,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(LkSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(visual.third, contentDescription = tone.accessibleLabel(), modifier = Modifier.size(LkSpacing.xl))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (actionLabel != null && onAction != null) {
                    SignallQButton(actionLabel, onAction, style = SignallQButtonStyle.Text)
                }
            }
        }
    }
}

internal fun SignallQFeedbackTone.accessibleLabel(): String =
    when (this) {
        SignallQFeedbackTone.Neutral -> "Informação"
        SignallQFeedbackTone.Success -> "Sucesso"
        SignallQFeedbackTone.Warning -> "Atenção"
        SignallQFeedbackTone.Error -> "Erro"
    }

@Composable
fun SignallQProgress(
    label: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (progress == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics { contentDescription = label })
        } else {
            val safeProgress = progress.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { safeProgress },
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = label
                        progressBarRangeInfo = ProgressBarRangeInfo(safeProgress, 0f..1f)
                    },
            )
        }
    }
}

@Composable
fun SignallQResultBlock(
    conclusion: String,
    explanation: String,
    modifier: Modifier = Modifier,
    tone: SignallQFeedbackTone = SignallQFeedbackTone.Neutral,
    nextStep: String? = null,
) {
    val c = LocalLkTokens.current
    Surface(
        modifier = modifier,
        color = c.cardSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(LkSpacing.cardContent),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            SignallQBadge(label = tone.accessibleLabel(), tone = tone.toBadgeTone())
            Text(conclusion, style = MaterialTheme.typography.headlineSmall)
            Text(explanation, style = MaterialTheme.typography.bodyLarge)
            nextStep?.let {
                Text("Próximo passo", style = MaterialTheme.typography.titleSmall)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun SignallQFeedbackTone.toBadgeTone() =
    when (this) {
        SignallQFeedbackTone.Neutral -> SignallQBadgeTone.Neutral
        SignallQFeedbackTone.Success -> SignallQBadgeTone.Success
        SignallQFeedbackTone.Warning -> SignallQBadgeTone.Warning
        SignallQFeedbackTone.Error -> SignallQBadgeTone.Error
    }

@Composable
fun SignallQTranslatedMetric(
    label: String,
    value: String,
    meaning: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        Text(meaning, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
    }
}

@Composable
fun SignallQSkeleton(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    val c = LocalLkTokens.current
    Column(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "Conteúdo carregando" },
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        repeat(lines.coerceAtLeast(1)) { index ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(if (index == lines - 1) SKELETON_LAST_LINE_FRACTION else 1f)
                        .height(LkSpacing.lg)
                        .clip(MaterialTheme.shapes.small)
                        .background(c.surfaceContainerHigh),
            )
        }
    }
}
