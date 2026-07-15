package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import io.signallq.app.feature.wifi.EstadoScanWifi
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SnapshotScanWifi
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regressão (#893): um erro de scan temporário não pode apagar a última lista
 * de redes válida nem tomar a tela inteira quando já existe dado pra mostrar.
 * Antes desta correção, [io.signallq.app.feature.wifi.ScannerRedesWifi.escanear]
 * zerava `redes` no estado `erro`, e o `CanalTab` sempre tomava a tela inteira
 * nesse estado — mesmo com dado em cache.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalScreenErroPreservaDadoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val redeExemplo =
        RedeVizinha(
            ssid = "CasaWifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssiDbm = -55,
            frequenciaMhz = 2437,
            seguranca = SegurancaWifi.wpa2,
            larguraCanalMhz = 20,
        )

    private fun renderComErro() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi =
                        SnapshotScanWifi(
                            estado = EstadoScanWifi.erro,
                            redes = listOf(redeExemplo),
                            erroMensagem = "erroScanWifi",
                        ),
                    connectedNetwork = redeExemplo,
                    estadoConexao = EstadoConexao.wifi,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
    }

    @Test
    fun `aba wifi continua mostrando a rede em cache durante erro`() {
        renderComErro()
        composeRule.waitForIdle()

        // A rede continua visivel — nao ha wipe pra lista vazia.
        composeRule.onNodeWithText("CasaWifi").assertExists()
    }

    @Test
    fun `aba canal nao toma a tela inteira quando ha dado em cache durante erro`() {
        renderComErro()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Canal").performClick()
        composeRule.waitForIdle()

        // Nao caiu no CanalErroState (tela cheia) — o filtro de banda do conteudo
        // normal continua presente, e o erro vira so um aviso inline.
        composeRule.onNodeWithText("Não foi possível escanear as redes. Tente novamente.").assertDoesNotExist()
        composeRule.onNodeWithText("Todos (1)").assertExists()
        composeRule.onNodeWithText("Não foi possível atualizar agora. Mostrando o último dado válido.").assertExists()
    }
}
