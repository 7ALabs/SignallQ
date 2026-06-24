package io.veloo.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LocalLkTokens
import io.veloo.app.ui.SignallQTheme

@Composable
fun SignalToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    val bgColor = if (enabled) LkColors.accent.copy(alpha = 0.05f) else Color.White
    val borderColor = if (enabled) LkColors.accent.copy(alpha = 0.25f) else c.border
    val iconColor = if (enabled) LkColors.accent else c.textTertiary

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .clickable { onToggle() }
                .padding(horizontal = 11.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (enabled) {
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(LkColors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, c.border, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SignalToggleCardEnabledPreview() {
    SignallQTheme {
        SignalToggleCard(
            icon = Icons.Rounded.Wifi,
            title = "Wi-Fi & Sinal",
            subtitle = "Intensidade e qualidade do sinal",
            enabled = true,
            onToggle = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SignalToggleCardDisabledPreview() {
    SignallQTheme {
        SignalToggleCard(
            icon = Icons.Rounded.Wifi,
            title = "DNS",
            subtitle = "Resolução de nomes de domínio",
            enabled = false,
            onToggle = {},
        )
    }
}
