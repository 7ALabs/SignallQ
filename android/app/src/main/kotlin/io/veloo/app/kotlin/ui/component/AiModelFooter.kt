package io.signallq.app.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.signallq.app.feature.diagnostico.ai.ModeloIa
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

/**
 * Rodapé discreto com o nome do motor de análise usado (Gemma/SignallQ IA/local).
 * Sempre exibe texto — fallback "Motor de análise: SignallQ IA" quando modeloIa é null.
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
            ?: "SignallQ IA"

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = c.textSecondary.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = LkSpacing.xl, vertical = LkSpacing.xs),
    )
}
