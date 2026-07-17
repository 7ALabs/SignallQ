package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.ResponsiveActionsRow

@Composable
fun PermissaoTelefoniaContextoSheet(
    onConceder: () -> Unit,
    onAgoraNao: () -> Unit,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.border),
        )
        Spacer(Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Outlined.CellTower,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Text(
            "Por que precisamos desta permissão?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            "Para identificar sua operadora, o tipo de rede (4G, 5G) e a qualidade do sinal da torre.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            "Não acessamos chamadas, mensagens ou dados pessoais.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.xl))
        ResponsiveActionsRow(
            secondary = { m ->
                TextButton(onClick = onAgoraNao, modifier = m) {
                    Text("Agora não", color = c.textSecondary)
                }
            },
            primary = { m ->
                Button(
                    onClick = onConceder,
                    modifier = m,
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                ) {
                    Text("Entendi, conceder")
                }
            },
        )
        Spacer(Modifier.height(LkSpacing.md))
    }
}
