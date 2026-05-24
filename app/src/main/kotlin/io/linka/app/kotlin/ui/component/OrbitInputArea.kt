package io.linka.app.kotlin.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens

private const val CHAR_LIMIT = 280
private const val CHAR_COUNTER_THRESHOLD = 200

@Composable
fun OrbitInputArea(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onEnviarMensagem: () -> Unit,
    chips: List<OpcaoResposta>,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    modifier: Modifier = Modifier,
    // T2.3: chips só aparecem antes da primeira resposta da IA
    hasAiResponse: Boolean = false,
    // T6.3: limite de 5 turnos por sessão — desabilita o input quando atingido
    isLimitReached: Boolean = false,
) {
    val c = LocalLkTokens.current
    val charCount = value.text.length
    val isOverLimit = charCount >= CHAR_LIMIT
    val showCounter = charCount >= CHAR_COUNTER_THRESHOLD

    // Input efetivamente desabilitado quando limite atingido OU sobrepassa o char limit
    val inputEnabled = !isLimitReached

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            // T2.3: chips desaparecem após receber a primeira resposta da IA
            // T6.3: chips também ficam ocultos quando limite atingido
            if (chips.isNotEmpty() && !hasAiResponse && !isLimitReached) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = LkSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    items(chips, key = { it.id }) { chip ->
                        SuggestionChip(
                            onClick = { onSelecionarChip(chip) },
                            label = { Text(chip.label) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = value,
                    // T2.4: hard block acima de 280 chars
                    // T6.3: ignorar input quando limite de turnos atingido
                    onValueChange = { new ->
                        if (!isLimitReached && new.text.length <= CHAR_LIMIT) onValueChange(new)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = inputEnabled,
                    minLines = 1,
                    maxLines = 6,
                    placeholder = {
                        Text(
                            if (isLimitReached) "Limite de mensagens atingido" else "Digite sua mensagem",
                        )
                    },
                    // T2.4: contador de caracteres como supportingText
                    // T6.3: mensagem de limite substitui o contador quando bloqueado
                    supportingText =
                        when {
                            isLimitReached -> {
                                {
                                    Text(
                                        text = "Limite atingido. Inicie um novo diagnóstico para continuar.",
                                        color = c.textTertiary,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            showCounter -> {
                                {
                                    Text(
                                        text = "$charCount/$CHAR_LIMIT",
                                        color = if (isOverLimit) MaterialTheme.colorScheme.error else c.textSecondary,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            else -> null
                        },
                )
                IconButton(
                    onClick = onEnviarMensagem,
                    enabled = inputEnabled && value.text.isNotBlank() && !isOverLimit,
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar mensagem",
                    )
                }
            }
        }
    }
}
