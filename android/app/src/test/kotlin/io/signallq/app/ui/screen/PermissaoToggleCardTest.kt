package io.signallq.app.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * GH#1092: quando a permissao ja foi concedida, o toggle trava (Android nao permite revogar
 * programaticamente) mas precisa comunicar visualmente o motivo — chip "Concedida" ao lado do
 * Switch, em vez de deixar so acinzentado sem explicacao.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissaoToggleCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun renderCard(
        concedida: Boolean,
        marcado: Boolean = concedida,
    ) {
        composeRule.setContent {
            SignallQTheme {
                PermissaoToggleCard(
                    icon = Icons.Outlined.Wifi,
                    titulo = "Wi-Fi por perto",
                    descricao = "Para encontrar e analisar as redes Wi-Fi ao seu redor.",
                    marcado = marcado,
                    concedida = concedida,
                    onMarcadoChange = {},
                    c = LocalLkTokens.current,
                )
            }
        }
    }

    @Test
    fun `permissao concedida mostra chip Concedida e trava o switch`() {
        renderCard(concedida = true)

        composeRule.onNodeWithText("Concedida", ignoreCase = true).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Wi-Fi por perto: concedida, ative pelas configurações do sistema para alterar")
            .assertIsNotEnabled()
    }

    @Test
    fun `permissao nao concedida nao mostra chip e switch fica habilitado`() {
        renderCard(concedida = false, marcado = false)

        composeRule.onNodeWithText("Concedida", ignoreCase = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Wi-Fi por perto: não marcado").assertIsEnabled()
    }
}
