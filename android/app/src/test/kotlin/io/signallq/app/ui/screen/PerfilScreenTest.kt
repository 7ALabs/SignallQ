package io.signallq.app.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerfilScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `perfil exposes administrative destinations without account affordance at font scale 2`() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            SignallQTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    PerfilScreen(
                        appVersion = "9.8.7",
                        onVoltar = { opened += "voltar" },
                        onAbrirAjustes = { opened += "ajustes" },
                        onAbrirPrivacidade = { opened += "privacidade" },
                        onAbrirNovidades = { opened += "novidades" },
                        onAbrirAjuda = { opened += "ajuda" },
                        onAbrirTermos = { opened += "termos" },
                        onAbrirSobre = { opened += "sobre" },
                    )
                }
            }
        }

        composeRule
            .onNodeWithText("Ajustes")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Privacidade").performClick()
        composeRule.onNodeWithText("Novidades").performClick()
        composeRule.onNodeWithText("Ajuda e suporte").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Termos de uso"))
        composeRule.onNodeWithText("Termos de uso").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Sobre o SignallQ"))
        composeRule.onNodeWithText("Sobre o SignallQ").performClick()
        composeRule.onNodeWithContentDescription("Voltar").assertHasClickAction().performClick()

        assertEquals(listOf("ajustes", "privacidade", "novidades", "ajuda", "termos", "sobre", "voltar"), opened)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Versão 9.8.7"))
        composeRule.onNodeWithText("Versão 9.8.7").assertIsDisplayed()
        composeRule.onNodeWithText("Entrar").assertDoesNotExist()
        composeRule.onNodeWithText("Criar conta").assertDoesNotExist()
    }
}
