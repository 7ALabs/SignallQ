package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DadosLocaisSheetResumoTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `exibe contagens reais e encaminha para exportar o histórico`() {
        var abriuHistorico = false
        composeRule.setContent {
            SignallQTheme {
                DadosLocaisSheet(
                    c = lightTokens(),
                    onDismiss = {},
                    onLimparHistorico = {},
                    onApagarDadosLocais = {},
                    onResetarApp = {},
                    quantidadeHistorico = 18,
                    quantidadeApelidos = 3,
                    onAbrirHistorico = { abriuHistorico = true },
                )
            }
        }

        composeRule.onNodeWithText("Dados neste aparelho").assertIsDisplayed()
        composeRule.onNodeWithText("Histórico de medições").assertIsDisplayed()
        composeRule.onNodeWithText("18 itens").assertIsDisplayed()
        composeRule.onNodeWithText("3 itens").assertIsDisplayed()
        composeRule.onNodeWithText("Exportar histórico").performScrollTo().performClick()

        assertEquals(true, abriuHistorico)
    }
}
