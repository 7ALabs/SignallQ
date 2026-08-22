package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

private fun ObjetivoDiagnostico.iconeAssist(): ImageVector =
    when (this) {
        ObjetivoDiagnostico.INTERNET_CAI_OSCILA -> Icons.Outlined.WifiOff
        ObjetivoDiagnostico.VIDEOS_TRAVAM -> Icons.Outlined.Tv
        ObjetivoDiagnostico.JOGOS_COM_LAG -> Icons.Outlined.SportsEsports
        ObjetivoDiagnostico.CHAMADAS_CONGELAM -> Icons.Outlined.Videocam
        ObjetivoDiagnostico.SITES_DEMORAM -> Icons.Outlined.Language
        ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA -> Icons.Outlined.Speed
        ObjetivoDiagnostico.WIFI_VS_OPERADORA -> Icons.Outlined.CompareArrows
    }

@Composable
internal fun DiagnosticoGuiadoListaObjetivosSection(
    modifier: Modifier = Modifier,
    onSelect: (ObjetivoDiagnostico?) -> Unit,
    c: LkTokens,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(c.bgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        Text("O que está acontecendo com sua internet?", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.size(LkSpacing.lg))
        Text(
            "Quero verificar minha conexão",
            style = MaterialTheme.typography.titleSmall,
            color = c.primary,
            modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = LkSpacing.md),
        )
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(objetivo) }.padding(LkSpacing.lg)) {
                Icon(objetivo.iconeAssist(), contentDescription = null, tint = c.primary, modifier = Modifier.size(40.dp).padding(9.dp))
                Spacer(Modifier.width(LkSpacing.md))
                Column(Modifier.weight(1f)) {
                    Text(objetivo.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
                    Text(objetivo.subtitulo, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.textTertiary)
            }
        }
    }
}
