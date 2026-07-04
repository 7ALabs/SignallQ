package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.SignallQTheme

@Composable
fun DiagVerdictHeroCard(
    titulo: String,
    veredito: String,
    statusLabel: String,
    statusColor: Color,
    confianca: String,
    dark: Boolean = true,
    modelName: String? = null,
    modifier: Modifier = Modifier,
) {
    val gradient =
        Brush.linearGradient(
            colors = listOf(LkColors.signallQDarkSurface, LkColors.signallQBlack),
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(gradient)
                .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = titulo,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f),
            )
            StatusPill(label = statusLabel, color = statusColor)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = veredito,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = LkColors.signallQTextOnDark,
            lineHeight = (16 * 1.42).sp,
        )

        Spacer(Modifier.height(14.dp))

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            thickness = 1.dp,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "● $confianca",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = LkColors.success,
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .semantics { contentDescription = confianca },
            )
            Spacer(Modifier.width(8.dp))
            OnDevicePill(dark = true, modelName = modelName)
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color,
) {
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DiagVerdictHeroCardPreview() {
    SignallQTheme {
        DiagVerdictHeroCard(
            titulo = "DIAGNÓSTICO IA",
            veredito =
                "Seu Wi-Fi chega fraco neste cômodo e a fila de download entope a conexão. " +
                    "É por isso que chamadas travam e páginas demoram — o plano em si está ok.",
            statusLabel = "ATENÇÃO",
            statusColor = LkColors.warning,
            confianca = "Confiança alta",
        )
    }
}

// GH#409: previews com fontScale ampliado para validar que o card nao quebra
// (texto cortado, pill sobreposta) com fonte grande do sistema.
@Preview(name = "Font scale 150%", fontScale = 1.5f, showBackground = true)
@Composable
private fun DiagVerdictHeroCardFontScale150Preview() {
    SignallQTheme {
        DiagVerdictHeroCard(
            titulo = "DIAGNÓSTICO IA",
            veredito =
                "Seu Wi-Fi chega fraco neste cômodo e a fila de download entope a conexão. " +
                    "É por isso que chamadas travam e páginas demoram — o plano em si está ok.",
            statusLabel = "ATENÇÃO",
            statusColor = LkColors.warning,
            confianca = "Confiança alta",
        )
    }
}

@Preview(name = "Font scale 200%", fontScale = 2.0f, showBackground = true)
@Composable
private fun DiagVerdictHeroCardFontScale200Preview() {
    SignallQTheme {
        DiagVerdictHeroCard(
            titulo = "DIAGNÓSTICO IA",
            veredito =
                "Seu Wi-Fi chega fraco neste cômodo e a fila de download entope a conexão. " +
                    "É por isso que chamadas travam e páginas demoram — o plano em si está ok.",
            statusLabel = "ATENÇÃO",
            statusColor = LkColors.warning,
            confianca = "Confiança alta",
        )
    }
}
