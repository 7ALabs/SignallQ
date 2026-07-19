package io.signallq.pro.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Variante visual do [ProButton] -- mapeia direto os 4 estilos do CSS real (`sqp-btn--*`). */
enum class ProButtonVariant { PRIMARIO, SECUNDARIO, TEXTO, DESTRUTIVO }

/**
 * Botao pill do Pro (#1170 item 1) -- shape 999dp, min-height 48dp. Nome `ProButton` (nao
 * `Button` puro) para nao colidir/confundir com `androidx.compose.material3.Button` nos
 * imports de quem consumir. Opacidade de estado desabilitado (0.38) fica a cargo do
 * comportamento padrao do M3 `Button`/`TextButton`/`OutlinedButton` -- nao redescoberto aqui.
 */
@Composable
fun ProButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ProButtonVariant = ProButtonVariant.PRIMARIO,
    habilitado: Boolean = true,
    icone: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(ProRadius.pill)
    val contentPadding =
        PaddingValues(
            horizontal = if (variant == ProButtonVariant.TEXTO) 12.dp else 24.dp,
            vertical = 12.dp,
        )
    val alturaMinima = Modifier.heightIn(min = 48.dp)

    when (variant) {
        ProButtonVariant.PRIMARIO ->
            Button(
                onClick = onClick,
                modifier = modifier.then(alturaMinima),
                enabled = habilitado,
                shape = shape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                contentPadding = contentPadding,
            ) { ConteudoProButton(texto, icone) }
        ProButtonVariant.SECUNDARIO ->
            Button(
                onClick = onClick,
                modifier = modifier.then(alturaMinima),
                enabled = habilitado,
                shape = shape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                contentPadding = contentPadding,
            ) { ConteudoProButton(texto, icone) }
        ProButtonVariant.TEXTO ->
            TextButton(
                onClick = onClick,
                modifier = modifier.then(alturaMinima),
                enabled = habilitado,
                shape = shape,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                contentPadding = contentPadding,
            ) { ConteudoProButton(texto, icone) }
        ProButtonVariant.DESTRUTIVO ->
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.then(alturaMinima),
                enabled = habilitado,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            if (habilitado) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            },
                    ),
                contentPadding = contentPadding,
            ) { ConteudoProButton(texto, icone) }
    }
}

@Composable
private fun ConteudoProButton(
    texto: String,
    icone: (@Composable () -> Unit)?,
) {
    if (icone != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) { icone() }
            Text(texto)
        }
    } else {
        Text(texto)
    }
}
