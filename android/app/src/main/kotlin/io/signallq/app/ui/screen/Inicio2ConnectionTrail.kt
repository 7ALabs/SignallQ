package io.signallq.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.topologia.engine.TopologiaRedeEngine
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.ui.LkSpacing

internal data class Inicio2TrailNode(
    val id: String,
    val label: String,
    val detail: String,
)

internal data class Inicio2ConnectionTrailState(
    val nodes: List<Inicio2TrailNode>,
    val supportingMessage: String?,
)

internal object Inicio2ConnectionTrailMapper {
    fun map(
        snapshotRede: SnapshotRede,
        snapshotWifi: SnapshotScanWifi,
        temPermissaoLocalizacao: Boolean,
        temConfirmacaoRoteadorCentral: Boolean = false,
        ispName: String? = null,
        equipmentName: String? = null,
        deviceName: String? = null,
    ): Inicio2ConnectionTrailState {
        if (snapshotRede.estadoConexao != EstadoConexao.wifi) {
            return mapSemWifi(snapshotRede.estadoConexao, ispName, equipmentName, deviceName)
        }
        val classificacoes =
            if (temPermissaoLocalizacao && snapshotWifi.estado == EstadoScanWifi.concluido) {
                TopologiaRedeEngine.classificar(
                    redes = snapshotWifi.redes,
                    connectedBssid = snapshotRede.wifiLinkSnapshot?.bssid,
                    temConfirmacaoRoteadorCentral = temConfirmacaoRoteadorCentral,
                )
            } else {
                emptyList()
            }
        val meshConfirmado =
            temConfirmacaoRoteadorCentral &&
                classificacoes.any { (_, classificacao) ->
                    classificacao.papelProvavel == PapelTopologia.NO_MESH &&
                        classificacao.confianca == NivelConfianca.ALTA &&
                        classificacao.conflitos.isEmpty()
                }
        val nodes =
            buildList {
                add(Inicio2TrailNode("Internet", ispName?.takeIf { it.isNotBlank() } ?: "Internet", "Conectada"))
                add(Inicio2TrailNode("Equipamento", equipmentName?.takeIf { it.isNotBlank() } ?: "Equipamento principal", "Roteador ou modem"))
                if (meshConfirmado) add(Inicio2TrailNode("Mesh", "Nó mesh da sala", "Nó confirmado pela topologia"))
                add(
                    Inicio2TrailNode(
                        "Wi-Fi",
                        snapshotRede.wifiLinkSnapshot?.ssid?.takeIf { it.isNotBlank() } ?: "Wi-Fi",
                        "Rede local",
                    ),
                )
                add(Inicio2TrailNode("Este aparelho", deviceName?.takeIf { it.isNotBlank() } ?: "Este aparelho", "Conectado por Wi-Fi"))
            }
        val supportingMessage =
            when {
                !temPermissaoLocalizacao -> "Permita redes próximas para completar a trilha."
                snapshotWifi.estado == EstadoScanWifi.scanning -> "Atualizando os detalhes da rede…"
                snapshotWifi.estado == EstadoScanWifi.erro -> "Alguns detalhes da rede não estão disponíveis."
                else -> null
            }
        return Inicio2ConnectionTrailState(nodes, supportingMessage)
    }

    private fun mapSemWifi(
        estado: EstadoConexao,
        ispName: String?,
        equipmentName: String?,
        deviceName: String?,
    ): Inicio2ConnectionTrailState =
        when (estado) {
            EstadoConexao.movel ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", ispName?.takeIf { it.isNotBlank() } ?: "Internet", "Conectada"),
                            Inicio2TrailNode("Rede móvel", "Dados móveis ativos", "Dados móveis ativos"),
                            Inicio2TrailNode("Este aparelho", deviceName?.takeIf { it.isNotBlank() } ?: "Este aparelho", "Conectado pela rede móvel"),
                        ),
                    supportingMessage = null,
                )
            EstadoConexao.ethernet ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", ispName?.takeIf { it.isNotBlank() } ?: "Internet", "Conectada"),
                            Inicio2TrailNode("Equipamento", equipmentName?.takeIf { it.isNotBlank() } ?: "Equipamento principal", "Roteador ou modem"),
                            Inicio2TrailNode("Ethernet", "Ethernet", "Rede cabeada"),
                            Inicio2TrailNode("Este aparelho", deviceName?.takeIf { it.isNotBlank() } ?: "Este aparelho", "Conectado por cabo"),
                        ),
                    supportingMessage = null,
                )
            EstadoConexao.desconectado ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", "Internet", "Sem acesso"),
                            Inicio2TrailNode("Este aparelho", deviceName?.takeIf { it.isNotBlank() } ?: "Este aparelho", "Sem conexão ativa"),
                        ),
                    supportingMessage = "Conecte-se a uma rede para completar a trilha.",
                )
            EstadoConexao.desconhecido ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Conexão", "Conexão", "Verificando tipo de rede"),
                            Inicio2TrailNode("Este aparelho", deviceName?.takeIf { it.isNotBlank() } ?: "Este aparelho", "Aguardando identificação"),
                        ),
                    supportingMessage = "Aguarde enquanto identificamos a conexão.",
                )
            EstadoConexao.wifi -> error("Wi-Fi é mapeado pelo fluxo principal")
        }
}

@Composable
internal fun Inicio2ConnectionTrail(
    state: Inicio2ConnectionTrailState,
    modifier: Modifier = Modifier,
) {
    val c = io.signallq.app.ui.LocalLkTokens.current
    val nodes = state.nodes.take(5)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
        Box(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.TopCenter),
            ) {
                val nodeCount = nodes.size
                repeat((nodeCount - 1).coerceAtLeast(0)) { index ->
                    val currentCenter = size.width * (index + 0.5f) / nodeCount
                    val nextCenter = size.width * (index + 1.5f) / nodeCount
                    drawLine(
                        color = c.outlineVariant,
                        start =
                            androidx.compose.ui.geometry.Offset(
                                currentCenter + 20.dp.toPx(),
                                size.height / 2,
                            ),
                        end =
                            androidx.compose.ui.geometry.Offset(
                                nextCenter - 20.dp.toPx(),
                                size.height / 2,
                            ),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                nodes.forEach { node ->
                    Inicio2TrailItem(
                        node = node,
                        color = c,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        state.supportingMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun Inicio2TrailItem(
    node: Inicio2TrailNode,
    color: io.signallq.app.ui.LkTokens,
    modifier: Modifier = Modifier,
) {
    val icon =
        when {
            node.id == "Internet" -> Icons.Outlined.Public
            node.id == "Equipamento" -> Icons.Outlined.Router
            node.id == "Mesh" || node.id == "Nó mesh" -> Icons.Outlined.Hub
            node.id == "Wi-Fi" -> Icons.Outlined.Wifi
            node.id == "Este aparelho" -> Icons.Outlined.Smartphone
            else -> Icons.Outlined.Wifi
        }
    Column(
        modifier = modifier.padding(horizontal = LkSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        Box(
            modifier = Modifier.size(LkSpacing.xxl),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color.textSecondary, modifier = Modifier.size(LkSpacing.lg))
        }
        Text(
            text = node.label,
            style = MaterialTheme.typography.labelSmall,
            color = color.textPrimary,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
