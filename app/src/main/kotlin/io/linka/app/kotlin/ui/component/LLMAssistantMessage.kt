package io.linka.app.kotlin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.ui.LinkaTheme
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun LLMAssistantMessage(
    content: String,
    isStreaming: Boolean = false,
    onActionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

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

        val displayText = if (isStreaming && content.isNotEmpty()) "$content▌" else content

        Text(
            text = displayText,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = (14 * 1.6).sp,
                    color = c.textPrimary,
                ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LLMAssistantMessagePreview() {
    LinkaTheme {
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
    LinkaTheme {
        LLMAssistantMessage(
            content = "Analisando sua conexão",
            isStreaming = true,
        )
    }
}
