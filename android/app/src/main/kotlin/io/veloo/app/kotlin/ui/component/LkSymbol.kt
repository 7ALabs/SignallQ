package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R

/**
 * Material Symbols Outlined — fonte variável (`res/font/material_symbols_outlined.ttf`,
 * Apache 2.0 / Google Fonts) com eixos FILL, wght, GRAD, opsz. Base do design system TO-BE
 * (`Icon()` de `tobe/primitives.jsx`) — substitui `androidx.compose.material.icons.*`
 * (Material Icons fixo) no que for migrado.
 *
 * Renderiza via ligadura OpenType: o [name] é o texto puro do ícone (ex.: "home", "wifi"),
 * substituído pelo glifo correspondente pela feature GSUB `liga` da fonte — não é um enum
 * fechado, é a lista completa de https://fonts.google.com/icons?icon.set=Material+Symbols.
 *
 * Padrão de migração (issue #1008 — fundação; migração tela a tela ainda não iniciada):
 * troque `Icon(imageVector = Icons.Outlined.X, ...)` por
 * `LkSymbol(name = "x_snake_case", ...)`. Para o estado "selecionado" de navegação
 * (bottom nav, chips, tabs), alterne `filled = true` — não use um ImageVector `Filled` /
 * `Outlined` separado, é o MESMO glifo com o eixo FILL diferente.
 *
 * O [name] é renderizado como texto literal (ligadura), não pode vazar pro leitor de tela
 * como se fosse a palavra em si — por padrão o glifo fica invisível pra acessibilidade
 * ([contentDescription] nulo). Passe [contentDescription] só quando o ícone for a ÚNICA
 * pista acessível (sem label visível ao lado, como no antigo `Icon(contentDescription = ...)`).
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun LkSymbol(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    weight: Int = 400,
    grade: Int = 0,
    opticalSize: Int = 24,
    contentDescription: String? = null,
) {
    val fontFamily =
        remember(filled, weight, grade, opticalSize) {
            FontFamily(
                Font(
                    resId = R.font.material_symbols_outlined,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.Setting("FILL", if (filled) 1f else 0f),
                            FontVariation.Setting("wght", weight.toFloat()),
                            FontVariation.Setting("GRAD", grade.toFloat()),
                            FontVariation.Setting("opsz", opticalSize.toFloat()),
                        ),
                ),
            )
        }

    Text(
        text = name,
        modifier =
            modifier.then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.clearAndSetSemantics {}
                },
            ),
        color = tint,
        fontSize = size.value.sp,
        fontFamily = fontFamily,
        textAlign = TextAlign.Center,
        softWrap = false,
        maxLines = 1,
    )
}

@Preview(showBackground = true)
@Composable
private fun LkSymbolPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LkSymbol(name = "home", filled = false)
        LkSymbol(name = "home", filled = true)
        LkSymbol(name = "speed")
        LkSymbol(name = "wifi")
        LkSymbol(name = "history")
        LkSymbol(name = "build")
    }
}
