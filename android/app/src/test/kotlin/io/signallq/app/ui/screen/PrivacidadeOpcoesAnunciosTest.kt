package io.signallq.app.ui.screen

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Entrada de opções de privacidade da UMP na tela Privacidade — issue #1703.
 *
 * O app coletava consentimento de anúncio (`ConsentManager.atualizarEMostrarSeNecessario`) sem
 * oferecer caminho para revisá-lo depois. Isso é exigência da própria UMP quando
 * `privacyOptionsRequirementStatus == REQUIRED` (regiões sob GDPR), não preferência de produto.
 *
 * A condicionalidade é o ponto delicado e é o que estes testes travam: a entrada **precisa**
 * aparecer quando exigida e **precisa** sumir quando não exigida. Mostrar sempre abriria um
 * formulário vazio fora da região; nunca mostrar mantém o descumprimento que a issue corrige.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivacidadeOpcoesAnunciosTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val rotulo = "Preferências de anúncios"

    @Test
    fun `entrada aparece quando a UMP exige opcoes de privacidade`() {
        composeRule.setContent {
            SignallQTheme {
                PrivacidadeScreen(onVoltar = {}, mostrarOpcoesAnuncios = true)
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(rotulo))
        composeRule.onNodeWithText(rotulo).assertExists()
    }

    @Test
    fun `entrada nao aparece quando a UMP nao exige`() {
        // Mutante que este teste mata: ignorar `mostrarOpcoesAnuncios` e renderizar sempre.
        // Fora de região GDPR o formulário da UMP não tem o que mostrar — um item que abre tela
        // vazia é pior que item ausente.
        composeRule.setContent {
            SignallQTheme {
                PrivacidadeScreen(onVoltar = {}, mostrarOpcoesAnuncios = false)
            }
        }
        composeRule.onNodeWithText(rotulo).assertDoesNotExist()
    }

    @Test
    fun `default e nao mostrar`() {
        // Omitir o parâmetro não pode ligar a entrada por acidente: as chamadas que não sabem o
        // status da UMP (previews, outros call sites) devem cair no lado seguro.
        composeRule.setContent {
            SignallQTheme { PrivacidadeScreen(onVoltar = {}) }
        }
        composeRule.onNodeWithText(rotulo).assertDoesNotExist()
    }

    @Test
    fun `tocar a entrada aciona o callback e nao o de gerenciar dados`() {
        // Mutante que este teste mata: ligar `onAbrirOpcoesAnuncios` no `onAbrirGerenciarDados`.
        // Compila, e o usuário que quer rever consentimento de anúncio cairia na sheet de apagar
        // dados locais — dois destinos vizinhos na mesma tela, fáceis de trocar.
        val acionados = mutableListOf<String>()
        composeRule.setContent {
            SignallQTheme {
                PrivacidadeScreen(
                    onVoltar = {},
                    onAbrirGerenciarDados = { acionados += "gerenciarDados" },
                    mostrarOpcoesAnuncios = true,
                    onAbrirOpcoesAnuncios = { acionados += "opcoesAnuncios" },
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(rotulo))
        composeRule.onNodeWithText(rotulo).performClick()
        composeRule.runOnIdle { assertEquals(listOf("opcoesAnuncios"), acionados) }
    }

    @Test
    fun `entrada de gerenciar dados continua funcionando com a nova entrada visivel`() {
        // Caracterização: a entrada nova não pode roubar o toque da que já existia.
        val acionados = mutableListOf<String>()
        composeRule.setContent {
            SignallQTheme {
                PrivacidadeScreen(
                    onVoltar = {},
                    onAbrirGerenciarDados = { acionados += "gerenciarDados" },
                    mostrarOpcoesAnuncios = true,
                    onAbrirOpcoesAnuncios = { acionados += "opcoesAnuncios" },
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Gerenciar dados e privacidade"))
        composeRule.onNodeWithText("Gerenciar dados e privacidade").performClick()
        composeRule.runOnIdle { assertTrue(acionados == listOf("gerenciarDados")) }
    }
}
