package io.signallq.pro.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

/**
 * Campo de texto do Pro (#1170 item 1) -- label estatico ACIMA do campo, nao floating como
 * o `OutlinedTextField` nativo do M3 (o design pede um controle mais simples). Implementado
 * com `BasicTextField` porque o `OutlinedTextField` padrao nao tem essa variante de label.
 * Nome `ProTextField` (nao `TextField` puro) pelo mesmo motivo do `ProButton`.
 */
@Composable
fun ProTextField(
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    rotulo: String? = null,
    placeholder: String? = null,
    textoAjuda: String? = null,
    erro: Boolean = false,
    habilitado: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var focado by remember { mutableStateOf(false) }
    val corBorda =
        when {
            erro -> MaterialTheme.colorScheme.error
            focado -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }
    val corAjuda = if (erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val corFundo =
        when {
            !habilitado -> MaterialTheme.colorScheme.surfaceContainerLow
            focado -> MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
            else -> Color.Transparent
        }

    Column(
        modifier = modifier.widthIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (rotulo != null) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .alpha(if (habilitado) 1f else 0.38f)
                    .background(corFundo, RoundedCornerShape(ProRadius.extraSmall))
                    .border(1.dp, corBorda, RoundedCornerShape(ProRadius.extraSmall))
                    .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = valor,
                onValueChange = onValorChange,
                enabled = habilitado,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = keyboardOptions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focado = it.isFocused },
                decorationBox = { campoInterno ->
                    if (valor.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    campoInterno()
                },
            )
        }
        if (textoAjuda != null) {
            Text(text = textoAjuda, style = MaterialTheme.typography.labelMedium, color = corAjuda)
        }
    }
}
