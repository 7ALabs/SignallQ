package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OperadoraContatoSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `operadora catalogada oferece site SAC e acesso ao laudo`() {
        var abriuSite = false
        var ligou = false
        val vivo = requireNotNull(BancoOperadoras.resolverMovel("Vivo"))

        composeRule.setContent {
            SignallQTheme {
                OperadoraContatoSheet(
                    contato = ContatoOperadoraSelecionada(vivo, "Vivo"),
                    onDismiss = {},
                    onAbrirSite = { abriuSite = true },
                    onLigar = { ligou = true },
                    onAbrirLaudo = {},
                )
            }
        }

        composeRule.onNodeWithText("Falar com a Vivo").assertIsDisplayed()
        composeRule.onNodeWithText("Suporte online").performClick()
        composeRule.onNodeWithText("Central de atendimento").performClick()
        composeRule.onNodeWithText("Abrir laudo").performScrollTo().assertIsDisplayed()

        assertEquals(true, abriuSite)
        assertEquals(true, ligou)
    }
}
