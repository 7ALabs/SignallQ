package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.EquipamentoItemTecnico
import io.signallq.app.ui.component.LkSurfaceCard

/**
 * "Informações técnicas" — passo 13 da narrativa (bug #6, spec Lia): card
 * colapsável que já inclui "Acesso ao equipamento", sem duplicar essa linha
 * em nenhum outro card. Extraído de `EquipamentoInternetScreen.kt` (dívida
 * crítica, ver `.claude/rules/higiene-e-padronizacao-repositorio.md` seção
 * 4.6).
 */
@Composable
internal fun DeviceInfoSectionCard(
    linhas: List<Pair<String, String>>,
    acesso: String,
    acessoColor: Color,
    c: LkTokens,
) {
    var expandido by remember { mutableStateOf(false) }
    LkSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Router, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                text = "Equipamento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expandido) "Ocultar informações" else "Ver informações",
                style = MaterialTheme.typography.labelLarge,
                color = c.primary,
                modifier = Modifier.clickable { expandido = !expandido },
            )
        }
        if (expandido) {
            Spacer(Modifier.height(LkSpacing.sm))
            linhas.forEachIndexed { index, linha ->
                DataRowCard(item = EquipamentoItemTecnico(linha.first, linha.second), c = c)
                if (index < linhas.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(LkSpacing.sm))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.outlineVariant),
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "Acesso ao equipamento",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = acesso,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = acessoColor,
                textAlign = TextAlign.End,
            )
        }
    }
}
