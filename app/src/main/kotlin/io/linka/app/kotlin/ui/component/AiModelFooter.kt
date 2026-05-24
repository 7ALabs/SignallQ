package io.linka.app.kotlin.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.linka.app.kotlin.feature.diagnostico.ai.ModeloIa
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens

/**
 * Rodapé discreto com o nome do motor de análise usado (Gemma/Linka IA/local).
 * Sempre exibe texto — fallback "Motor de análise: Linka IA" quando modeloIa é null.
 * NUNCA exibe o id interno do modelo — apenas o textoRodape comercial.
 */
@Composable
fun AiModelFooter(
    modeloIa: ModeloIa?,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val text =
        modeloIa?.nomeExibicao?.takeIf { it.isNotBlank() }
            ?: modeloIa?.nomeCompletoComercial?.takeIf { it.isNotBlank() }
            ?: "Linka IA"

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = c.textSecondary.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = LkSpacing.xl, vertical = LkSpacing.xs),
    )
}
