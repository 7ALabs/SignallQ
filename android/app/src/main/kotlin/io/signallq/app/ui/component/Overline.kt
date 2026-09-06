package io.signallq.app.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme

/**
 * Label "overline" do design system SignallQ — padrão recorrente de caption em
 * UPPERCASE sobre cor terciária, usado como cabeçalho curto de seção dentro de
 * cards e sheets (ex.: "CAUSA PROVÁVEL", "IMPACTO PRÁTICO").
 *
 * Fonte de verdade: `.claude/skills/linka-design/colors_and_type.css` (`.overline`).
 * Antes desta extração (GH#929), o mesmo estilo era reimplementado manualmente em
 * pelo menos 17 arquivos (`letterSpacing`/`textTransform` inline, valores
 * levemente divergentes entre eles) — este componente é a versão única e
 * reutilizável, mas migrar os usos existentes fica fora do escopo desta fase.
 */
@Composable
fun Overline(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = LocalLkTokens.current.textTertiary,
) {
    Text(
        text = texto.uppercase(),
        modifier = modifier,
        color = color,
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
    )
}

@Preview(name = "Overline — claro", showBackground = true)
@Composable
private fun OverlinePreview() {
    SignallQTheme {
        Overline(texto = "Causa provável")
    }
}

@Preview(name = "Overline — escuro", showBackground = true)
@Composable
private fun OverlineDarkPreview() {
    SignallQTheme(darkTheme = true) {
        Overline(texto = "Impacto prático")
    }
}
