package io.signallq.app.ui.screen

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
 * Issue #1661 (épico #1647, Task 2.0.13) — decisões de produto do Luiz (comentário de
 * 2026-08-19) aplicadas dentro de `SinalCanalSection.kt`/`SinalWifiSection.kt`:
 *
 * 1. Mesh incerto: sem confirmação de rota, a tela fica em silêncio — nunca afirma a incerteza.
 *    Antes existia o aviso textual "Estrutura estimada por fabricante/sinal — sem confirmação de
 *    rota de rede" (GH#1209 item 8); esta fatia remove o aviso, não o troca por outro texto.
 * 2. Gráfico técnico de canais: removido. A aba Canal mostra só a explicação em linguagem
 *    simples e a lista "Ocupação dos canais" — sem visualização gráfica de espectro/ocupação
 *    (o `SpectrumChart` de curvas Gaussianas em Canvas foi removido, não escondido atrás de
 *    toggle).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class SinalCanalWifiRedesenho2SpecTest {
    @get:Rule
    val composeRule = createComposeRule()

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
                    dispositivosRede = emptyList<DispositivoRede>(),
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
    }

    @Test
    fun `mesh incerto fica em silencio -- aviso de estrutura estimada nao existe mais`() {
        render()
        composeRule.waitForIdle()

        // gateway + noMesh: mesmo OUI, mesma banda, sem confirmação de roteador central —
        // exatamente o cenário que antes disparava o aviso de incerteza (temNoAmbiguo = true,
        // ver SinalScreenMeshApSheetRoutingTest, mesmo fixture).
        composeRule.onNodeWithText("Estrutura estimada por fabricante/sinal — sem confirmação de rota de rede").assertDoesNotExist()
    }

    @Test
    fun `aba Wi-Fi preserva lista rolavel para todas as redes detectadas`() {
        render()
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasScrollAction()).onFirst().assertExists()
    }

    @Test
    fun `aba Canal orienta a iniciar o scan quando ainda nao ha dados`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi =
                        SnapshotScanWifi(
                            estado = EstadoScanWifi.idle,
                            redes = emptyList(),
                            erroMensagem = null,
                        ),
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }

        composeRule.onNodeWithText("Canal").performClick()
        composeRule
            .onNode(hasText("Escanear redes") and hasClickAction())
            .assertExists()
        composeRule.onNodeWithText("Toque para analisar os canais Wi-Fi próximos").assertExists()
    }

    @Test
    fun `aba Canal oferece retentativa quando o scan falha sem dado anterior`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi =
                        SnapshotScanWifi(
                            estado = EstadoScanWifi.erro,
                            redes = emptyList(),
                            erroMensagem = "erroScanWifi",
                        ),
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }

        composeRule.onNodeWithText("Canal").performClick()
        composeRule.onNodeWithText("Não foi possível escanear as redes. Tente novamente.").assertExists()
        composeRule.onNodeWithText("Tentar novamente").assertExists()
    }

    @Test
    fun `aba Canal nao renderiza mais o grafico de espectro`() {
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Canal").performClick()
        composeRule.waitForIdle()

        // O rótulo da seção do gráfico removido não existe mais em nenhuma das duas variantes
        // (com/sem filtro de banda no texto).
        composeRule.onNodeWithText("Intensidade por canal").assertDoesNotExist()
    }

    @Test
    fun `aba Canal continua mostrando explicacao em linguagem simples e lista de ocupacao`() {
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Canal").performClick()
        composeRule.waitForIdle()

        // Lista textual "Ocupação dos canais" (sem gráfico) continua presente — é a fonte de
        // informação por canal exigida pelo critério de aceite original. O rótulo pode vir com
        // sufixo de banda (ex.: "· 2.4GHz") quando o filtro não está em "Todos", daí o substring.
        // Fica abaixo da dobra na LazyColumn (sem gráfico ocupando espaço acima), por isso
        // precisa de scroll explícito antes do assert.
        // LkSectionOverline (o componente do rótulo de seção) sempre uppercase o texto — daí
        // ignoreCase aqui, igual ao badge "AP MESH" em SinalScreenMeshApSheetRoutingTest.
        composeRule
            .onAllNodes(hasScrollAction())
            .onFirst()
            .performScrollToNode(hasText("Ocupação dos canais", substring = true, ignoreCase = true))
        composeRule.onNodeWithText("Ocupação dos canais", substring = true, ignoreCase = true).assertExists()
    }
}
