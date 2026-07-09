package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke tests de UI para [GatewayCredentialsGuideSheetContent] (GH#529, epic #525).
 * Mesmo padrao de [GatewayConnectionSheetTest]: testa a Content diretamente,
 * sem o ModalBottomSheet ao redor.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1600dp")
class GatewayCredentialsGuideSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun renderContent() {
        composeRule.setContent {
            SignallQTheme {
                GatewayCredentialsGuideSheetContent(c = LocalLkTokens.current)
            }
        }
    }

    @Test
    fun `guia mostra titulo e os 4 passos ilustrados`() {
        renderContent()

        composeRule.onNodeWithText("Como encontrar usuário e senha do roteador").assertIsDisplayed()
        composeRule.onNodeWithText("Vire o roteador").assertIsDisplayed()
        composeRule.onNodeWithText("Encontre a etiqueta").assertIsDisplayed()
        composeRule.onNodeWithText("Usuário e senha padrão estão ali").assertIsDisplayed()
        composeRule.onNodeWithText("Se já foi alterado, use o que você configurou").assertIsDisplayed()
    }
}
