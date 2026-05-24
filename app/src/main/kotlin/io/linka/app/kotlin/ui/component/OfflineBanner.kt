package io.linka.app.kotlin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(LkColors.warning.copy(alpha = 0.12f))
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = LkColors.warning,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(LkSpacing.sm))
        Text(
            "Sem conexão ativa",
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = LkColors.warning,
        )
    }
}
