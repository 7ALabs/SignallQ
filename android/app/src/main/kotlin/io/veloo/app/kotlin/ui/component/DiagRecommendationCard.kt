package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import androidx.compose.ui.res.stringResource
import io.signallq.app.R

@Composable
fun DiagRecommendationCard(
    index: Int,
    title: String,
    description: String,
    priority: String,
    priorityColor: Color,
    onStepByStep: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(16.dp))
                .padding(13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(LkColors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PriorityPill(label = priority, color = priorityColor)
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = description,
            fontSize = 11.5.sp,
            color = c.textSecondary,
            modifier = Modifier.padding(start = 32.dp),
        )

        if (onStepByStep != null) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = stringResource(R.string.diag_rec_ver_passo),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LkColors.accent,
                modifier =
                    Modifier
                        .padding(start = 32.dp)
                        .clickable { onStepByStep() },
            )
        }
    }
}

@Composable
private fun PriorityPill(
    label: String,
    color: Color,
) {
    Text(
        text = label,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 0.3.sp,
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = 0.1f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DiagRecommendationCardHighPreview() {
    SignallQTheme {
        DiagRecommendationCard(
            index = 1,
            title = "Aproxime o aparelho do roteador ou use 5 GHz",
            description = "A −74 dBm o sinal está no limite. A poucos metros o download mais que dobra.",
            priority = "ALTA",
            priorityColor = LkColors.error,
            onStepByStep = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DiagRecommendationCardMediumPreview() {
    SignallQTheme {
        DiagRecommendationCard(
            index = 3,
            title = "Mude o Wi-Fi 2.4 GHz do canal 6 para o 1 ou 11",
            description = "Há 6 redes vizinhas no canal 6 disputando espaço.",
            priority = "MÉDIA",
            priorityColor = LkColors.warning,
        )
    }
}
