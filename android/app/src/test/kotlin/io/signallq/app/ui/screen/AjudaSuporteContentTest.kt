package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AjudaSuporteContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `missing mail handler offers copy and announces visible feedback`() {
        var copied: String? = null
        composeRule.setContent {
            SignallQTheme {
                AjudaSuporteContent(
                    onAbrirEmail = { false },
                    onCopiarEmail = { copied = it },
                )
            }
        }

        composeRule.onNodeWithText(SUPPORT_EMAIL).performClick()
        composeRule
            .onNodeWithText("Nenhum aplicativo de e-mail disponível. Você pode copiar o endereço.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Copiar endereço").performClick()

        assertEquals(SUPPORT_EMAIL, copied)
        composeRule.onNodeWithText("Endereço de suporte copiado.").assertIsDisplayed()
    }
}
