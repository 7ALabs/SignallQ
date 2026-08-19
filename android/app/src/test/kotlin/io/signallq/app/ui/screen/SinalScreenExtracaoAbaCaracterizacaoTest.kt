package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Testes de caracterização da issue #1660 (épico #1647, Task 2.0.12) — provam que a extração
 * de `SinalScreen.kt` (3503 linhas, dívida crítica §4.8b) em `SinalScreen.kt` (scaffold),
 * `SinalWifiSection.kt`, `SinalCanalSection.kt`, `SinalMovelSection.kt` e
 * `SinalSharedComponents.kt` não mudou nenhum comportamento observável. Cobre os estados
 * citados na issue: seleção/restauração de aba, permissões (localização/telefonia), scan
 * vazio, aba Móvel ativa e alternância entre as três abas.
 *
 * Não cobre auto-refresh (já em [SinalScreenAutoRefreshTest]), preservação de dado em erro
 * (já em [SinalScreenErroPreservaDadoTest]) nem roteamento pro MeshApSheet (já em
 * [SinalScreenMeshApSheetRoutingTest]) — esses três já caracterizavam a tela antes desta PR e
 * continuam verdes após a extração (evidência de que o comportamento se manteve).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalScreenExtracaoAbaCaracterizacaoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotWifiVazio =
        SnapshotScanWifi(estado = EstadoScanWifi.concluido, redes = emptyList(), erroMensagem = null)

    private val snapshotWifiIdle =
        SnapshotScanWifi(estado = EstadoScanWifi.idle, redes = emptyList(), erroMensagem = null)

    @Test
    fun `aba Wi-Fi comeca selecionada quando conectado em wifi e mostra scan vazio`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    temPermissaoLocalizacao = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        // As três abas existem e a tela renderiza o estado de "nenhuma rede encontrada" da
        // aba Wi-Fi (selecionada por padrao quando estadoConexao == wifi).
        composeRule.onNodeWithText("Wi-Fi").assertExists()
        composeRule.onNodeWithText("Canal").assertExists()
        composeRule.onNodeWithText("Móvel").assertExists()
        composeRule.onNodeWithText("Nenhuma rede encontrada").assertExists()
    }

    @Test
    fun `auto-seleciona aba Movel quando a conexao nao e wifi`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    temPermissaoTelefonia = false,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Sem permissao de telefonia, a aba Movel (auto-selecionada) mostra o empty state
        // de permissao — prova que RedesTab/CanalTab (que exigiriam Wi-Fi) nao renderizaram.
        composeRule.onNodeWithText("Permissão necessária").assertExists()
    }

    @Test
    fun `banner e sheet de permissao de localizacao aparecem na aba Wi-Fi sem permissao`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    temPermissaoLocalizacao = false,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Permissão de localização necessária para escanear redes").assertExists()
        composeRule.onNodeWithText("Por que precisamos da localização?").assertExists()
    }

    @Test
    fun `sheet de permissao de telefonia aparece ao entrar na aba Movel sem permissao`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    temPermissaoLocalizacao = true,
                    temPermissaoTelefonia = false,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Móvel").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Permissão necessária").assertExists()
        composeRule.onNodeWithText("Seu aparelho está sem permissão para ler\nas informações de rede móvel.").assertExists()
    }

    @Test
    fun `alternar entre as tres abas troca o conteudo exibido`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiIdle,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    temPermissaoLocalizacao = true,
                    temPermissaoTelefonia = false,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Wi-Fi (default): SinalWifiSection.EmptyStateWifi
        composeRule.onNodeWithText("Nenhuma rede encontrada").assertExists()

        // Canal: SinalCanalSection.CanalIdleState (estado idle + redes vazia)
        composeRule.onNodeWithText("Canal").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Toque para analisar os canais Wi-Fi próximos").assertExists()

        // Movel: SinalMovelSection.EmptyStatePermissaoTelefonia
        composeRule.onNodeWithText("Móvel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Permissão necessária").assertExists()

        // Volta pra Wi-Fi: estado preservado, sem regressao de composicao entre arquivos.
        composeRule.onNodeWithText("Wi-Fi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nenhuma rede encontrada").assertExists()
    }
}
