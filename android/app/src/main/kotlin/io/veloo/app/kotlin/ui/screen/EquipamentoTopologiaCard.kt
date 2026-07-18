package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.LkSurfaceCard

/**
 * "Como sua rede está conectada" — passo 8 da narrativa (bug #6, spec Lia):
 * peça central da tela, entre o painel de status/uso e os módulos técnicos.
 * Extraído de `EquipamentoInternetScreen.kt` (dívida crítica, ver
 * `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.6).
 */
@Composable
internal fun TopologiaRedeCard(
    paineis: List<EquipmentPanelUi>,
    selecionadoId: String,
    warning: String?,
    c: LkTokens,
) {
    val compacto = paineis.size <= 1
    LkSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountTree, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                text = "Como sua rede está conectada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
        }
        Spacer(Modifier.height(LkSpacing.md))
        if (compacto) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactTopologyNode(
                    label = "Internet",
                    icon = Icons.Outlined.AccountTree,
                    highlighted = false,
                    c = c,
                )
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(15.dp))
                CompactTopologyNode(
                    label = paineis.first().topologyLabel,
                    icon = Icons.Outlined.Router,
                    highlighted = true,
                    c = c,
                )
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(15.dp))
                CompactTopologyNode(
                    label = "Este celular",
                    icon = Icons.Outlined.Smartphone,
                    highlighted = false,
                    c = c,
                )
            }
        } else {
            val nos = buildTopologyNodes(paineis, selecionadoId)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                nos.forEachIndexed { index, no ->
                    if (index > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(start = 16.dp)
                                    .width(1.dp)
                                    .height(14.dp)
                                    .background(c.outlineVariant),
                        )
                    }
                    TopologyNodeRow(node = no, c = c)
                }
            }
        }
        warning?.let {
            Spacer(Modifier.height(LkSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = c.warning, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.warning)
            }
        }
    }
}

@Composable
private fun CompactTopologyNode(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    c: LkTokens,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (highlighted) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) c.primary else c.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) c.primary else c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopologyNodeRow(
    node: TopologyNodeUi,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (node.highlighted) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = node.icon,
                contentDescription = null,
                tint = if (node.highlighted) c.primary else c.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Column {
            Text(node.label, style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
            node.sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
        }
    }
}

private fun buildTopologyNodes(
    paineis: List<EquipmentPanelUi>,
    selecionadoId: String,
): List<TopologyNodeUi> =
    buildList {
        add(TopologyNodeUi(label = "Internet", icon = Icons.Outlined.AccountTree))
        paineis.forEach { painel ->
            add(
                TopologyNodeUi(
                    label = "${painel.vendor} ${painel.modelo}",
                    sub = painel.papel.replaceFirstChar { it.titlecase() },
                    icon =
                        when (painel.deviceTypeLabel) {
                            "Equipamento de fibra" -> Icons.Outlined.Router
                            "Ponto de acesso / mesh" -> Icons.Outlined.Devices
                            else -> Icons.Outlined.Router
                        },
                    highlighted = painel.id == selecionadoId,
                ),
            )
        }
        add(TopologyNodeUi(label = "Este celular", icon = Icons.Outlined.Smartphone))
    }

private data class TopologyNodeUi(
    val label: String,
    val sub: String? = null,
    val icon: ImageVector,
    val highlighted: Boolean = false,
)
