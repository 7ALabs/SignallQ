package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #1672 (épico #1647, Task 2.0.24) — prova que `SinalScreen` migrou do `OfflineBanner()`
 * legado (pré-2.0) para `SignallQOfflineBanner()`, o mesmo componente de estado 2.0 já usado em
 * `DispositivosScreen` (piloto do #1663). Cobre a rota crítica offline citada na matriz da issue:
 * mesmo sem conexão ativa, a tela continua exibindo o que consegue calcular localmente (aqui, o
 * scan de Wi-Fi já coletado) em vez de bloquear a tela — decisão de produto do Luiz em 2026-08-19
 * registrada na issue.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalScreenOfflineBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotWifiVazio =
        SnapshotScanWifi(estado = EstadoScanWifi.concluido, redes = emptyList(), erroMensagem = null)

    @Test
    fun `banner 2 ponto 0 aparece offline e conteudo local continua visivel`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    conectado = false,
                    temPermissaoLocalizacao = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Texto do SignallQOfflineBanner (SignallQScreenState.kt) — explica situação e deixa
        // claro que os recursos locais continuam disponíveis, em vez de esvaziar a tela.
        composeRule.onNodeWithText("Você está offline").assertExists()
        // O conteúdo local (scan de Wi-Fi já coletado) continua acessível abaixo do banner.
        composeRule.onNodeWithText("Nenhuma rede encontrada").assertExists()
    }

    @Test
    fun `banner nao aparece quando conectado`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    conectado = true,
                    temPermissaoLocalizacao = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Você está offline").assertDoesNotExist()
    }
}
