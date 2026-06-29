package io.signallq.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme

@Composable
fun LLMAssistantMessage(
    content: String,
    isStreaming: Boolean = false,
    onActionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val (thinkingText, responseText) = remember(content) { parseThinkingContent(content) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .background(
                            color = LkColors.accent,
                            shape = RoundedCornerShape(999.dp),
                        ),
            )
            Spacer(modifier = Modifier.size(LkSpacing.sm))
            Text(
                text = "LINKA",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = c.textTertiary,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(LkSpacing.md))

        if (thinkingText != null) {
            ThinkingCompletedSection(thinkingText, c)
        }

        val textToShow = if (thinkingText != null) responseText else content
        val displayText = if (isStreaming && textToShow.isNotEmpty()) "$textToShow▌" else textToShow

        if (displayText.isNotBlank()) {
            Text(
                text = displayText,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = (14 * 1.6).sp,
                        color = c.textPrimary,
                    ),
                maxLines = Int.MAX_VALUE,
            )
        }
    }
}

private fun parseThinkingContent(text: String): Pair<String?, String> {
    val thinkStart = text.indexOf("<think>")
    if (thinkStart == -1) return Pair(null, text)

    val thinkEnd = text.indexOf("</think>", thinkStart)
    return if (thinkEnd == -1) {
        val partial = text.substring(thinkStart + 7).trim()
        Pair(partial.ifBlank { null }, "")
    } else {
        val thinkContent = text.substring(thinkStart + 7, thinkEnd).trim()
        val responseContent = (text.substring(0, thinkStart) + text.substring(thinkEnd + 8)).trim()
        Pair(thinkContent.ifBlank { null }, responseContent)
    }
}

@Composable
private fun ThinkingCompletedSection(
    thinkingText: String,
    c: io.signallq.app.ui.LkTokens,
) {
    var expanded by remember { mutableStateOf(false) }
    val estimatedSeconds = (thinkingText.length / 100).coerceAtLeast(1)
    val pensouDesc = if (expanded) stringResource(R.string.cd_pensamento_expandido) else stringResource(R.string.cd_pensamento_recolhido)

    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        contentDescription = pensouDesc
                    }.clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = c.textTertiary,
            )
            Text(
                text = "Pensou por ${estimatedSeconds}s",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)),
        ) {
            Text(
                text = thinkingText,
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                modifier = Modifier.padding(start = 20.dp, top = LkSpacing.xs),
                maxLines = Int.MAX_VALUE,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LLMAssistantMessagePreview() {
    SignallQTheme {
        LLMAssistantMessage(
            content =
                "Sua internet pode ficar lenta à noite por alguns motivos comuns:\n\n" +
                    "1. Horário de pico — muitos vizinhos usando a rede ao mesmo tempo.\n" +
                    "2. Wi-Fi 2.4 GHz cheio — banda congestionada no período noturno.\n" +
                    "3. Atualizações em segundo plano — apps atualizando enquanto você usa.\n\n" +
                    "Tente rodar um teste agora para confirmar.",
            isStreaming = false,
            modifier = Modifier,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LLMAssistantMessageStreamingPreview() {
    SignallQTheme {
        LLMAssistantMessage(
            content = "Analisando sua conexão",
            isStreaming = true,
        )
    }
}
