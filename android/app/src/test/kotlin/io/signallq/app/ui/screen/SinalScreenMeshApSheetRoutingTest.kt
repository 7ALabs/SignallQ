package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * GH#1025 (3c) — cobre o roteamento de clique nos nós da árvore de topologia (aba Wi-Fi):
 * - nó classificado como AP/mesh que correlaciona com um [DispositivoRede] real do scan LAN abre
 *   `MeshApSheet` com dado real (nunca fabricado);
 * - o nó conectado nunca abre `MeshApSheet`, mesmo quando o motor de topologia classifica ele
 *   como NO_MESH/SISTEMA_MESH_PROVAVEL por falta de confirmação de roteador central — "sua
 *   conexão" continua abrindo `NetworkDetailSheet`;
 * - rede vizinha comum (sem classificação de AP/mesh) continua abrindo `NetworkDetailSheet`.
 *
 * Qualifiers de viewport maior (`w411dp-h891dp`) — o default do Robolectric (320x470dp) é pequeno
 * demais pra árvore de topologia com 2 nós e faz o clique cair fora dos limites da janela.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class SinalScreenMeshApSheetRoutingTest {
    @get:Rule
    val composeRule = createComposeRule()

    // Mesmo cenário de TopologiaRedeEngineTest ("mesh real sem confirmação de rota"): mesmo OUI,
    // mesma banda, 2 nós — sem confirmação de roteador central o motor classifica AMBOS
    // (inclusive o conectado) como SISTEMA_MESH_PROVAVEL/NO_MESH. Serve pra provar tanto que o nó
    // conectado nunca abre MeshApSheet quanto que o nó secundário abre quando correlaciona.
    private val gateway =
        RedeVizinha(
            ssid = "CasaSilva",
            bssid = "50:C7:BF:00:00:01",
            rssiDbm = -50,
            frequenciaMhz = 2412,
            seguranca = SegurancaWifi.wpa2,
            larguraCanalMhz = 20,
            oui = "50C7BF",
        )
    private val noMesh =
        RedeVizinha(
            ssid = "CasaSilva",
            bssid = "50:C7:BF:00:00:02",
            rssiDbm = -65,
            frequenciaMhz = 2412,
            seguranca = SegurancaWifi.wpa2,
            larguraCanalMhz = 20,
            oui = "50C7BF",
        )
    private val vizinha =
        RedeVizinha(
            ssid = "WifiDoVizinho",
            bssid = "AA:BB:CC:11:22:33",
            rssiDbm = -80,
            frequenciaMhz = 2437,
            seguranca = SegurancaWifi.wpa2,
            larguraCanalMhz = 20,
        )

    private val dispositivoDoNoMesh =
        DispositivoRede(
            id = "dev-no-mesh",
            ip = "192.168.1.2",
            mac = "50:C7:BF:00:00:02",
            nomeExibicao = "Nó Mesh Sala",
            fonteNome = "arp",
        )

    private fun render() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi =
                        SnapshotScanWifi(
                            estado = EstadoScanWifi.concluido,
                            redes = listOf(gateway, noMesh, vizinha),
                            erroMensagem = null,
                        ),
                    connectedNetwork = gateway,
                    estadoConexao = EstadoConexao.wifi,
                    dispositivosRede = listOf(dispositivoDoNoMesh),
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
    }

    @Test
    fun `no mesh correlacionado abre MeshApSheet com dado real do dispositivo`() {
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Nó #1").performClick()
        composeRule.waitForIdle()

        // Badge "AP Mesh" renderiza em UPPERCASE (transformação visual do BadgePill).
        composeRule.onNodeWithText("AP MESH").assertExists()
        // "Nó Mesh Sala" aparece 2x (título + placeholder do campo Apelido vazio) — confere só
        // que existe ao menos 1, não usa onNodeWithText (exige exatamente 1 match).
        composeRule.onAllNodesWithText("Nó Mesh Sala").onFirst().assertExists()
    }

    @Test
    fun `no conectado nunca abre MeshApSheet mesmo classificado como mesh`() {
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Conectado agora").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("AP MESH").assertDoesNotExist()
        composeRule.onNodeWithText("BSSID").assertExists()
    }

    @Test
    fun `rede vizinha comum continua abrindo NetworkDetailSheet`() {
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("WifiDoVizinho").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("AP MESH").assertDoesNotExist()
        composeRule.onNodeWithText("BSSID").assertExists()
    }
}
