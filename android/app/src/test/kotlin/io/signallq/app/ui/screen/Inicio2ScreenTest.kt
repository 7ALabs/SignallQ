package io.signallq.app.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class Inicio2ScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `offline em font scale 2 mostra contexto e um CTA acessivel disparado uma vez`() {
        var analyses = 0
        composeRule.setContent {
            SignallQTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Inicio2Screen(
                        uiState = Inicio2UiState(Inicio2Conexao.Offline, null, Inicio2Analise.SemAnalise),
                        onAnalisarConexao = { analyses++ },
                        onAbrirPerfil = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Sem internet").assertIsDisplayed()
        composeRule.onNodeWithText("Ainda não analisada").assertIsDisplayed()
        composeRule
            .onNodeWithText("Analisar minha conexão")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, analyses)
        composeRule.onNodeWithText("Download").assertDoesNotExist()
        composeRule.onNodeWithText("Ferramentas").assertDoesNotExist()
    }

    @Test
    fun `perfil e estado interrompido permanecem acionaveis`() {
        var profiles = 0
        composeRule.setContent {
            SignallQTheme {
                Inicio2Screen(
                    uiState = Inicio2UiState(Inicio2Conexao.Wifi, "Casa", Inicio2Analise.Interrompida("Contexto preservado.")),
                    onAnalisarConexao = {},
                    onAbrirPerfil = { profiles++ },
                )
            }
        }
        composeRule.onNodeWithText("Análise interrompida").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abrir perfil e ajustes").performClick()
        assertEquals(1, profiles)
    }
}
