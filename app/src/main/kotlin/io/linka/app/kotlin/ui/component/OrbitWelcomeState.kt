package io.linka.app.kotlin.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun OrbitWelcomeState(
    onIniciarOrbit: (foco: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalLkTokens.current

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))

        HorizontalDivider(color = tokens.border, thickness = 0.5.dp)

        IntentRow(
            icon = Icons.Filled.SignalCellularAlt,
            label = "Internet lenta",
            onClick = { onIniciarOrbit("Foco: Internet lenta") },
        )
        IntentRow(
            icon = Icons.Filled.PlayCircle,
            label = "Streaming ruim",
            onClick = { onIniciarOrbit("Foco: Streaming ruim") },
        )
        IntentRow(
            icon = Icons.Filled.SportsEsports,
            label = "Jogos travando",
            onClick = { onIniciarOrbit("Foco: Jogos travando") },
        )
        IntentRow(
            icon = Icons.Filled.PhoneDisabled,
            label = "Chamadas falhando",
            onClick = { onIniciarOrbit("Foco: Chamadas falhando") },
        )
        IntentRow(
            icon = Icons.Filled.Search,
            label = "Diagnóstico geral",
            onClick = { onIniciarOrbit(null) },
        )

        Spacer(Modifier.padding(bottom = LkSpacing.lg))
    }
}

@Composable
private fun IntentRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LkColors.accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(LkSpacing.lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textPrimary,
        )
    }
}
