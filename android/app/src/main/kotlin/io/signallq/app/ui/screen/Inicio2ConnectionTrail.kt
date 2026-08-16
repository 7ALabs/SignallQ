package io.signallq.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.topologia.engine.TopologiaRedeEngine
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.ui.LkSpacing

internal enum class Inicio2TrailRoute { Equipamento, Wifi, SinalMovel }

internal data class Inicio2TrailNode(
    val title: String,
    val detail: String,
    val route: Inicio2TrailRoute? = null,
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
    ): Inicio2ConnectionTrailState {
        if (snapshotRede.estadoConexao != EstadoConexao.wifi) {
            return mapSemWifi(snapshotRede.estadoConexao)
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
                add(Inicio2TrailNode("Internet", "Conectada"))
                add(Inicio2TrailNode("Equipamento", "Roteador ou modem", Inicio2TrailRoute.Equipamento))
                if (meshConfirmado) add(Inicio2TrailNode("Mesh", "Nó confirmado pela topologia"))
                add(
                    Inicio2TrailNode(
                        "Wi-Fi",
                        snapshotRede.wifiLinkSnapshot?.ssid?.takeIf { it.isNotBlank() } ?: "Rede local",
                        Inicio2TrailRoute.Wifi,
                    ),
                )
                add(Inicio2TrailNode("Este aparelho", "Conectado por Wi-Fi"))
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

    private fun mapSemWifi(estado: EstadoConexao): Inicio2ConnectionTrailState =
        when (estado) {
            EstadoConexao.movel ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", "Conectada"),
                            Inicio2TrailNode("Rede móvel", "Dados móveis ativos", Inicio2TrailRoute.SinalMovel),
                            Inicio2TrailNode("Este aparelho", "Conectado pela rede móvel"),
                        ),
                    supportingMessage = null,
                )
            EstadoConexao.ethernet ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", "Conectada"),
                            Inicio2TrailNode("Equipamento", "Roteador ou modem", Inicio2TrailRoute.Equipamento),
                            Inicio2TrailNode("Ethernet", "Rede cabeada"),
                            Inicio2TrailNode("Este aparelho", "Conectado por cabo"),
                        ),
                    supportingMessage = null,
                )
            EstadoConexao.desconectado ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", "Sem acesso"),
                            Inicio2TrailNode("Este aparelho", "Sem conexão ativa"),
                        ),
                    supportingMessage = "Conecte-se a uma rede para completar a trilha.",
                )
            EstadoConexao.desconhecido ->
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Conexão", "Verificando tipo de rede"),
                            Inicio2TrailNode("Este aparelho", "Aguardando identificação"),
                        ),
                    supportingMessage = "Aguarde enquanto identificamos a conexão.",
                )
            EstadoConexao.wifi -> error("Wi-Fi é mapeado pelo fluxo principal")
        }
}

@Composable
internal fun Inicio2ConnectionTrail(
    state: Inicio2ConnectionTrailState,
    onOpenRoute: (Inicio2TrailRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        Text("Trilha da conexão", style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.fillMaxWidth()) {
            state.nodes.forEach { node ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = LkSpacing.compositionLarge)
                            .then(
                                node.route?.let { route ->
                                    Modifier.clickable(
                                        role = Role.Button,
                                        onClickLabel = "Abrir detalhes de ${node.title}",
                                    ) { onOpenRoute(route) }
                                } ?: Modifier,
                            ).padding(vertical = LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(node.title, style = MaterialTheme.typography.titleMedium)
                        Text(node.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (node.route != null) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
        state.supportingMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
