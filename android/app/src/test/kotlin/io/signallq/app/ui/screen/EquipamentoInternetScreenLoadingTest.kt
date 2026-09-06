package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1090 — a tela "Equipamento de internet" travava mostrando "Sessão expirada" com 5
 * skeletons vazios (sem nenhuma acao) quando a primeira tentativa de conexao nunca
 * resolvia (nem sucesso, nem erro). Causa raiz dupla:
 * 1. `mapAcessoEquipamento()` nao trata idle/conectando (por design, ver KDoc da funcao) e
 *    cai em SESSAO_EXPIRADA por eliminacao — o subtitulo do TopAppBar usava esse valor sem
 *    checar se a tela ainda estava carregando.
 * 2. A tela de carregamento (skeletons) nao tinha nenhum botao de saida — se a tentativa
 *    travasse (sem virar nem `erro` nem `concluido`), o usuario ficava sem rota de volta.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class EquipamentoInternetScreenLoadingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun snapshotIdle() =
        SnapshotFibra(estado = EstadoFibra.idle, gpon = null, wan = null, ppp = null, deviceInfo = null, erroMensagem = null)

    @Test
    fun `estado idle mostra Conectando no subtitulo, nunca Sessao expirada`() {
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotIdle(),
                    localDevice = null,
                    natStatus = null,
                    modemHost = "192.168.1.1",
                    modemUsername = "admin",
                    modemPassword = "admin",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Conectando…").assertExists()
        composeRule.onNodeWithText("Sessão expirada").assertDoesNotExist()
    }

    @Test
    fun `carregamento demorado oferece Tentar novamente e Revisar configuracoes`() {
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotIdle(),
                    localDevice = null,
                    natStatus = null,
                    modemHost = "192.168.1.1",
                    modemUsername = "admin",
                    modemPassword = "admin",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tentar novamente").assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(13_000L)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tentar novamente").assertExists()
        composeRule.onNodeWithText("Revisar configurações").assertExists()
    }
}
