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
 * CTA "Diagnosticar problema" dentro do `SignallQOfflineBanner` (issue #1811) e sua navegação
 * padrão para o diálogo real de diagnóstico offline (issue #1818, `DiagnosticoOfflineDialog`).
 *
 * O teste sem callback externo abre o diálogo real, que dispara `DiagnosticoOfflineViewModel
 * .iniciar()` de verdade (achado de revisão do Caio na PR #1821) — não uma sondagem de rede real:
 * sob Robolectric, sem rede Wi-Fi ativa configurada, `DiagnosticoOfflineExecutorReal` encerra em
 * "sem rede Wi-Fi ativa" na primeira etapa (ver `capturarContextoRedeWifiPadrao` retornando
 * `null`), então nenhuma chamada de I/O real acontece. Este teste cobre só a navegação (o título
 * do diálogo aparece); `DiagnosticoOfflineDialogTest` cobre o conteúdo do diálogo isoladamente,
 * sem depender do ViewModel/Factory reais.
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
    fun `tap no cta sem callback externo abre o dialogo real de diagnostico`() {
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
    fun `tap no cta com callback externo usa navegacao real em vez do dialogo padrao`() {
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
