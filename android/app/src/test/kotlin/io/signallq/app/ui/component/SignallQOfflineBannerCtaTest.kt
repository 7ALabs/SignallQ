package io.signallq.app.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #1811 (Task 3/4) — CTA "Diagnosticar problema" dentro do `SignallQOfflineBanner`.
 *
 * A navegação real para o fluxo de diagnóstico offline (Task 2, `DiagnosticoOfflineViewModel`,
 * branch `feat/1811-diagnostico-offline-state` / PR #1814) ainda não está mergeada em `main`, por
 * isso este teste cobre o comportamento atual: o CTA existe, tem a microcopy esperada e, sem
 * callback externo, abre o stub de navegação placeholder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignallQOfflineBannerCtaTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `cta diagnosticar problema aparece no banner offline`() {
        composeRule.setContent {
            SignallQTheme {
                SignallQOfflineBanner()
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Você está offline").assertExists()
        composeRule.onNodeWithText("Diagnosticar problema").assertExists()
    }

    @Test
    fun `tap no cta sem callback externo abre o stub de navegacao`() {
        composeRule.setContent {
            SignallQTheme {
                SignallQOfflineBanner()
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Diagnosticar problema").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Diagnóstico guiado").assertExists()
    }

    @Test
    fun `tap no cta com callback externo usa navegacao real em vez do stub`() {
        var chamadas = 0
        composeRule.setContent {
            SignallQTheme {
                SignallQOfflineBanner(onDiagnosticarProblema = { chamadas++ })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Diagnosticar problema").performClick()
        composeRule.waitForIdle()

        assert(chamadas == 1) { "esperava 1 chamada ao callback externo, houve $chamadas" }
        composeRule.onNodeWithText("Diagnóstico guiado").assertDoesNotExist()
    }
}
