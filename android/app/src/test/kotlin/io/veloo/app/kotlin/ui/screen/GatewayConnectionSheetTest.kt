package io.signallq.app.ui.screen

import androidx.compose.material3.SwitchColors
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke tests de UI para [GatewayConnectionSheetContent] (GH#526) usando
 * Robolectric + Compose.
 * Testa a Content diretamente (sem o ModalBottomSheet ao redor) para evitar
 * a complexidade de Popup/janela do Material3 ModalBottomSheet em teste.
 *
 * Tela virtual mais alta que o padrão do Robolectric ([Config.qualifiers]):
 * a sheet tem vários campos + 2 toggles, e o viewport padrão (~470px) corta
 * o formulário antes do botão "Conectar", quebrando performClick (fora da
 * área clicável visível).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1600dp")
class GatewayConnectionSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun renderContent(
        ipInicial: String? = "192.168.1.1",
        usuarioInicial: String = "",
        senhaInicial: String = "",
        lembrarSenhaInicial: Boolean = false,
        manterConectadoInicial: Boolean = false,
        conectar: GatewayConnectionService = GatewayConnectionService { _, _, _ -> GatewayConnectionResultado.Sucesso },
        onConectado: (String, String, String, Boolean, Boolean) -> Unit = { _, _, _, _, _ -> },
    ) {
        composeRule.setContent {
            SignallQTheme {
                GatewayConnectionSheetContent(
                    ipInicial = ipInicial,
                    usuarioInicial = usuarioInicial,
                    senhaInicial = senhaInicial,
                    lembrarSenhaInicial = lembrarSenhaInicial,
                    manterConectadoInicial = manterConectadoInicial,
                    conectar = conectar,
                    onConectado = onConectado,
                    c = LocalLkTokens.current,
                )
            }
        }
    }

    @Test
    fun `campo IP vem pre-preenchido com o gateway detectado`() {
        renderContent(ipInicial = "192.168.15.1")

        composeRule.onNodeWithText("192.168.15.1").assertIsDisplayed()
    }

    @Test
    fun `estado conectando substitui o texto do botao e nao fecha a sheet`() {
        val nuncaResolve = CompletableDeferred<GatewayConnectionResultado>()
        renderContent(
            conectar = GatewayConnectionService { _, _, _ -> nuncaResolve.await() },
        )

        composeRule.onNodeWithTag("gateway_connect_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Conectando…").assertIsDisplayed()
    }

    @Test
    fun `erro de conexao mostra mensagem amigavel e permite tentar novamente sem perder os dados`() {
        val mensagemAmigavel = "Não foi possível conectar ao roteador. Verifique o IP e as credenciais."
        renderContent(
            ipInicial = "192.168.1.1",
            usuarioInicial = "admin",
            conectar = GatewayConnectionService { _, _, _ -> GatewayConnectionResultado.Falha(mensagemAmigavel) },
        )

        composeRule.onNodeWithTag("gateway_connect_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("gateway_error_message").assertIsDisplayed()
        composeRule.onNodeWithText(mensagemAmigavel).assertIsDisplayed()
        // dados digitados nao somem apos o erro
        composeRule.onNodeWithText("192.168.1.1").assertIsDisplayed()
        composeRule.onNodeWithText("admin").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").assertIsDisplayed()
    }

    @Test
    fun `manter conectado ativa lembrar senha automaticamente`() {
        renderContent()

        composeRule.onNodeWithTag("gateway_toggle_lembrar_senha").assertIsOff()
        composeRule.onNodeWithTag("gateway_toggle_manter_conectado").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("gateway_toggle_manter_conectado").assertIsOn()
        composeRule.onNodeWithTag("gateway_toggle_lembrar_senha").assertIsOn()
        composeRule.onNodeWithTag("gateway_toggle_lembrar_senha").assertIsNotEnabled()
    }

    @Test
    fun `switch desabilitado mantem a mesma cor visual do estado habilitado equivalente (GH#848)`() {
        var cores: SwitchColors? = null
        composeRule.setContent {
            SignallQTheme {
                cores = toggleRowSwitchColors(LocalLkTokens.current)
            }
        }
        composeRule.waitForIdle()

        val c = requireNotNull(cores)
        // Sem essa paridade, o switch true+disabled (ex.: "Lembrar senha" apos
        // ativar "Manter conectado") cai nos tokens cinza padrao do Material3
        // e parece desligado mesmo estando checked=true.
        assertEquals(c.checkedThumbColor, c.disabledCheckedThumbColor)
        assertEquals(c.checkedTrackColor, c.disabledCheckedTrackColor)
        assertEquals(c.checkedBorderColor, c.disabledCheckedBorderColor)
        assertEquals(c.uncheckedThumbColor, c.disabledUncheckedThumbColor)
        assertEquals(c.uncheckedTrackColor, c.disabledUncheckedTrackColor)
    }

    @Test
    fun `link de guia de credenciais abre o guia ilustrado sem sair da sheet`() {
        renderContent()

        composeRule.onNodeWithTag("gateway_link_guia_credenciais").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Como encontrar usuário e senha do roteador").assertIsDisplayed()
        // sheet de conexao continua no ar por baixo do guia
        composeRule.onNodeWithTag("gateway_connect_button").assertIsDisplayed()
    }
}
