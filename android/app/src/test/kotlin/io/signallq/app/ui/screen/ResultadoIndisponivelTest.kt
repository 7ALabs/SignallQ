package io.signallq.app.ui.screen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Estado vazio de resultado indisponível — issue #1714.
 *
 * O bug: `AppShellNavigator.Saver` faz a pilha de overlays sobreviver ao process death, mas o
 * `ResultadoSpeedtest` não sobrevive (vem de um `@Singleton` em memória). Na volta, três overlays
 * ficavam simultaneamente **na pilha** e **sem resultado**, e o `AnimatedVisibility` não compunha
 * nada — o usuário via a tela de trás e o back consumia um `pop()` invisível.
 *
 * O que estes testes travam é a inversão: a ausência de resultado deixou de **esconder** o overlay
 * e passou a **decidir o que mostrar**. O mutante que interessa é voltar a guarda para o `visible`
 * do `AnimatedVisibility` — que é literalmente o código anterior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResultadoIndisponivelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val titulo = "Este resultado não está mais disponível"

    // ─── A tela em si ──────────────────────────────────────────────────────────

    @Test
    fun `explica sem jargao e sem culpar o usuario`() {
        // A copy é o produto aqui: o usuário não fez nada errado, e "o app foi fechado em segundo
        // plano" é a linguagem que ele reconhece do próprio aparelho. Trocar isso por "estado não
        // persistido" ou "sessão expirada" seria regressão de produto, não de código.
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Resultado", onVoltar = {}) }
        }
        composeRule.onNodeWithText(titulo).assertExists()
        composeRule.onNodeWithText("fechado em segundo plano", substring = true).assertExists()
    }

    @Test
    fun `o titulo e parametrizado por overlay`() {
        // Uma tela, três consumidores. Se o título fosse fixo, o usuário veria "Resultado" ao
        // voltar de Detalhes técnicos.
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Detalhes da conexão", onVoltar = {}) }
        }
        composeRule.onNodeWithText("Detalhes da conexão").assertExists()
    }

    @Test
    fun `voltar aciona o callback`() {
        var voltou = false
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Resultado", onVoltar = { voltou = true }) }
        }
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertTrue(voltou) }
    }

    @Test
    fun `botao medir agora so aparece quando ha para onde ir`() {
        // `onMedirNovamente` é opcional de propósito: o Detalhes técnicos não tem destino de nova
        // medição, e oferecer um botão que não leva a lugar nenhum seria pior que não oferecer.
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Detalhes da conexão", onVoltar = {}) }
        }
        composeRule.onNodeWithText("Medir agora").assertDoesNotExist()
    }

    @Test
    fun `botao medir agora aciona o callback quando oferecido`() {
        var mediu = false
        composeRule.setContent {
            SignallQTheme {
                ResultadoIndisponivelScreen(
                    titulo = "Resultado",
                    onVoltar = {},
                    onMedirNovamente = { mediu = true },
                )
            }
        }
        composeRule.onNodeWithText("Medir agora").performClick()
        composeRule.runOnIdle { assertTrue(mediu) }
    }

    // ─── A ligação no overlay: é aqui que o bug morava ─────────────────────────

    @Test
    fun `detalhes tecnicos na pilha sem resultado mostra o estado vazio, nao um container mudo`() {
        // ESTE é o teste do bug. Mutante: devolver `&& resultadoSpeedtest != null` ao `visible` do
        // AnimatedVisibility — o container volta a não compor e o `testTag` some.
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = null,
                    localizacaoServidor = null,
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertExists()
        composeRule.onNodeWithText(titulo).assertExists()
    }

    @Test
    fun `voltar do estado vazio remove o overlay da pilha`() {
        // Fecha o outro lado do defeito: antes, o back consumia um `pop()` que não correspondia a
        // nada visível. Agora o usuário vê o que está fechando.
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = null,
                    localizacaoServidor = null,
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertFalse(AppShellOverlay.DetalhesTecnicos in stack) }
    }

    @Test
    fun `fora da pilha continua sem compor, mesmo sem resultado`() {
        // A inversão não pode ter ido longe demais: sem estar na pilha, nada aparece. Um mutante
        // que trocasse o `visible` por `true` passaria pelos testes acima e falharia aqui.
        val stack = mutableStateListOf<AppShellOverlay>()
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = null,
                    localizacaoServidor = null,
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(emptyList<AppShellOverlay>(), stack.toList()) }
    }
}
