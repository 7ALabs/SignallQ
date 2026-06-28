package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import androidx.compose.ui.res.stringResource
import io.signallq.app.R

@Composable
fun OnDevicePill(
    dark: Boolean = false,
    modelName: String? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val color = if (dark) Color.White.copy(alpha = 0.55f) else c.textTertiary
    val displayName = modelName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.signallq_ia_nome)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.on_device_processado, displayName),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun OnDevicePillLightPreview() {
    SignallQTheme {
        OnDevicePill(dark = false)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun OnDevicePillDarkPreview() {
    SignallQTheme {
        OnDevicePill(dark = true)
    }
}
