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

    // COMPORTAMENTO INVERTIDO PELA #1717, de propósito.
    //
    // Estes dois testes afirmavam que a entrada **não aparecia** fora de região GDPR, e isso estava
    // certo enquanto o único destino era o formulário da UMP: fora do GDPR ele vem vazio, e um item
    // que abre tela vazia é pior que item ausente.
    //
    // O que a #1717 mostrou é que a conclusão estava certa e a premissa era incompleta. O app serve
    // anúncio **personalizado** no Brasil, e quem está lá — a maior parte da base — não tinha
    // nenhum controle de anúncio dentro do aplicativo. A saída não é esconder o item: é dar a ele
    // um destino que exista em toda região. Onde a UMP tem formulário, abre o formulário; onde não
    // tem, leva às configurações de anúncios do Android, onde dá para limitar a personalização e
    // redefinir o identificador de publicidade.
    //
    // `mostrarOpcoesAnuncios` continua existindo e continua vindo da UMP — deixou de decidir se o
    // item existe e passou a decidir para onde ele vai (e o subtítulo que anuncia isso).
    @Test
    fun `entrada aparece mesmo sem exigencia da UMP, com destino diferente`() {
        composeRule.setContent {
            SignallQTheme {
                PrivacidadeScreen(onVoltar = {}, mostrarOpcoesAnuncios = false)
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(rotulo))

        composeRule.onNodeWithText(rotulo).assertExists()
        composeRule
            .onNodeWithText("Controlar a personalização dos anúncios nas configurações do Android")
            .assertExists()
    }

    // Mutante que este teste mata: fixar o subtítulo. O item passaria a prometer "rever a escolha
    // que você fez" para quem nunca fez escolha nenhuma — não há formulário da UMP fora do GDPR.
    @Test
    fun `o subtitulo diz para onde o item leva`() {
        composeRule.setContent {
            SignallQTheme {
                PrivacidadeScreen(onVoltar = {}, mostrarOpcoesAnuncios = true)
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(rotulo))

        composeRule
            .onNodeWithText("Rever a escolha que você fez sobre anúncios neste aparelho")
            .assertExists()
        composeRule
            .onNodeWithText("Controlar a personalização dos anúncios nas configurações do Android")
            .assertDoesNotExist()
    }

    @Test
    fun `o default nao esconde mais a entrada`() {
        // Antes da #1717 omitir o parâmetro era o "lado seguro" porque escondia o item. Com o item
        // sempre presente, o default só escolhe o destino — e o seguro passou a ser o destino que
        // funciona em qualquer região.
        composeRule.setContent {
            SignallQTheme { PrivacidadeScreen(onVoltar = {}) }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(rotulo))

        composeRule.onNodeWithText(rotulo).assertExists()
    }

    /**
     * Rola até a última entrada fixa da tela antes de negar a presença da entrada nova.
     *
     * Sem isto os dois testes negativos eram VACUOSOS (bloqueio 1 do parecer de Caio na PR #1709):
     * o item fica fora da viewport, o `LazyColumn` não compõe o que está fora da tela, e o
     * `assertDoesNotExist` passava com a guarda `if (mostrarOpcoesAnuncios)` presente OU removida.
     * A prova está nos próprios testes positivos, que precisam de `performScrollToNode` para achar
     * o rótulo. "Gerenciar dados e privacidade" é o último item antes de onde a entrada nova
     * apareceria — rolar até ela garante que a região já foi composta.
     */
    private fun rolarAteOFimDaLista() {
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Gerenciar dados e privacidade"))
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
