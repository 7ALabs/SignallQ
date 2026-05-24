package io.linka.app.kotlin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun WifiChannelGuide() {
    val c = LocalLkTokens.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Como mudar o canal da sua rede?",
                fontWeight = FontWeight.W600,
                fontSize = 14.sp,
                color = c.textPrimary,
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LkColors.accent,
            )
        }

        if (isExpanded) {
            HorizontalDivider(color = c.border)

            Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                GuideSection(
                    number = 1,
                    title = "Acesse seu roteador",
                    description = "Abra o navegador e digite o endereço:",
                    details = "• http://192.168.1.1\n• http://admin.local\n• ou consulte o manual do roteador",
                    c = c,
                )

                GuideSection(
                    number = 2,
                    title = "Faça login",
                    description = "Use suas credenciais (padrão: admin/admin)",
                    details = "Se não souber a senha, verifique o adesivo do roteador",
                    c = c,
                )

                GuideSection(
                    number = 3,
                    title = "Abra as configurações Wi-Fi",
                    description = "Procure por:",
                    details = "• Wireless Settings\n• Wi-Fi Configuration\n• 2.4GHz ou 5GHz Settings",
                    c = c,
                )

                GuideSection(
                    number = 4,
                    title = "Altere o canal",
                    description = "Recomendado por banda:",
                    details = "• 2.4GHz: canais 1, 6 ou 11 (não sobrepostos)\n• 5GHz: canais 36, 100, 149 ou 165\n• Evite canais intermediários",
                    c = c,
                )

                GuideSection(
                    number = 5,
                    title = "Aplique e aguarde",
                    description = "Salve as mudanças (Save/Apply)",
                    details = "O roteador pode reiniciar (5-10 segundos)\nSeu mesh/extensor sincronizará automaticamente",
                    c = c,
                )

                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    "Dica: execute um novo scan de canais após a mudança para confirmar a melhoria.",
                    fontSize = 12.sp,
                    color = c.textSecondary,
                    fontWeight = FontWeight.W500,
                )
            }
        }
    }
}

@Composable
internal fun GuideSection(
    number: Int,
    title: String,
    description: String,
    details: String,
    c: LkTokens,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LkColors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                fontSize = 12.sp,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                details,
                fontSize = 12.sp,
                color = c.textTertiary,
                lineHeight = 17.sp,
            )
        }
    }
}
