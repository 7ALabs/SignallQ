package io.signallq.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme

data class ImpactItem(
    val icon: ImageVector,
    val label: String,
    val status: String,
    val statusColor: Color,
    /** Texto de detalhe exibido no "ver detalhes" (mesmo padrao expandir/colapsar
     *  do [io.signallq.app.ui.component.DiagMetricsGrid]). Null = item sem detalhes,
     *  sem affordance de expansao (comportamento atual preservado). */
    val detalhes: String? = null,
)

@Composable
fun DiagImpactCard(
    items: List<ImpactItem>,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "IMPACTO NO USO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = c.textTertiary,
            letterSpacing = 0.5.sp,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.bgSecondary),
        ) {
            items.forEachIndexed { index, item ->
                ImpactRow(item = item)
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = c.border,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImpactRow(item: ImpactItem) {
    val c = LocalLkTokens.current
    var expanded by remember { mutableStateOf(false) }
    val temDetalhes = !item.detalhes.isNullOrBlank()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .semantics(mergeDescendants = true) {}
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.label,
                fontSize = 12.sp,
                color = c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(label = item.status, color = item.statusColor)
        }

        if (temDetalhes) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (expanded) "Ocultar detalhes" else "Ver detalhes",
                fontSize = 11.sp,
                color = c.textTertiary,
                modifier =
                    Modifier
                        .semantics { role = Role.Button }
                        .clickable { expanded = !expanded },
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Text(
                    text = item.detalhes.orEmpty(),
                    fontSize = 11.5.sp,
                    color = c.textSecondary,
                    lineHeight = 16.sp,
                    modifier =
                        Modifier
                            .padding(top = 6.dp)
                            .semantics { contentDescription = "Detalhes de ${item.label}: ${item.detalhes}" },
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color,
) {
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DiagImpactCardPreview() {
    SignallQTheme {
        DiagImpactCard(
            items =
                listOf(
                    ImpactItem(Icons.Rounded.PlayCircle, "Streaming / vídeo", "Ok", LkColors.success),
                    ImpactItem(Icons.Rounded.Videocam, "Chamadas de vídeo", "Travando", LkColors.error),
                    ImpactItem(Icons.Rounded.Games, "Jogos online", "Ruim", LkColors.error),
                ),
        )
    }
}
