package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val conectarIndisponivel =
    GatewayConnectionService { _, _, _ -> GatewayConnectionResultado.Indisponivel }

/**
 * GH#1806 — a etapa "Conectar equipamento" só existe enquanto não há endereço salvo (ver
 * `onAbrirEquipamentoInternetOverlay` em AppShell.kt). Cobre as duas variantes de entrada
 * (com e sem detecção automática) e o "Pular por enquanto".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class EquipamentoConectarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `sem endereco detectado oferece configuracao manual e pular chama onPular`() {
        var pulou = false
        composeRule.setContent {
            SignallQTheme {
                EquipamentoConectarScreen(
                    enderecoDetectado = null,
                    conectar = conectarIndisponivel,
                    onVoltar = {},
                    onAbrirMenu = {},
                    onConectado = { _, _, _, _, _ -> },
                    onPular = { pulou = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Nenhum equipamento detectado automaticamente").assertIsDisplayed()
        composeRule.onNodeWithText("Configurar conexão").assertIsDisplayed()
        composeRule.onNodeWithText("Veja se o seu aparelho está na lista").assertIsDisplayed()

        composeRule.onNodeWithText("Pular por enquanto").performClick()
        assert(pulou) { "onPular deveria ter sido chamado ao tocar em 'Pular por enquanto'." }
    }

    @Test
    fun `com endereco detectado mostra o endereco e oferece conectar`() {
        composeRule.setContent {
            SignallQTheme {
                EquipamentoConectarScreen(
                    enderecoDetectado = "192.168.1.1",
                    conectar = conectarIndisponivel,
                    onVoltar = {},
                    onAbrirMenu = {},
                    onConectado = { _, _, _, _, _ -> },
                    onPular = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Endereço detectado nesta rede").assertIsDisplayed()
        composeRule.onNodeWithText("192.168.1.1 · fabricante ainda não identificado").assertIsDisplayed()
        composeRule.onNodeWithText("Conectar").assertIsDisplayed()
    }
}
