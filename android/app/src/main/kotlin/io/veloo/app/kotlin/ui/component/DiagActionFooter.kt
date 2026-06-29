package io.signallq.app.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme

@Composable
fun DiagActionFooter(
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onContactIsp: () -> Unit,
    onAbrirChat: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = c.border, thickness = 1.dp)

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = LkColors.accent,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Refazer teste",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Falar com a operadora",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.Button }
                        .clickable { onContactIsp() }
                        .padding(vertical = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DiagActionFooterPreview() {
    SignallQTheme {
        DiagActionFooter(
            onShare = {},
            onRefresh = {},
            onContactIsp = {},
        )
    }
}
