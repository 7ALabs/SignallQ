package io.signallq.app.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssistScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `mapeamento explicito cobre sete objetivos e pergunta apenas quando altera diagnostico`() {
        val relevantes = ObjetivoDiagnostico.entries.filter { it.contextoQueAlteraDiagnostico() != null }

        assertEquals(setOf(ObjetivoDiagnostico.JOGOS_COM_LAG, ObjetivoDiagnostico.WIFI_VS_OPERADORA), relevantes.toSet())
        assertEquals(
            7,
            ObjetivoDiagnostico.entries
                .map { it.analyticsId() }
                .distinct()
                .size,
        )
        assertNull(ObjetivoDiagnostico.INTERNET_CAI_OSCILA.contextoQueAlteraDiagnostico())
    }

    @Test
    fun `neutro e sete objetivos aparecem com texto ampliado`() {
        setContent()

        composeRule.onNodeWithText("Quero verificar minha conexão").assertIsDisplayed()
        ObjetivoDiagnostico.entries.forEach { composeRule.onNodeWithText(it.titulo).fetchSemanticsNode() }
    }

    @Test
    fun `objetivo relevante confirma pergunta fechada e conclui uma vez`() {
        val objetivos = mutableListOf<ObjetivoDiagnostico?>()
        val respostas = mutableListOf<Int>()
        var conclusoes = 0
        setContent(
            onObjetivo = { objetivo, _ -> objetivos += objetivo },
            onResposta = { _, _, resposta, _ -> respostas += resposta },
            onConcluir = { _, _ -> conclusoes += 1 },
        )

        composeRule.onNodeWithText(ObjetivoDiagnostico.JOGOS_COM_LAG.titulo).performScrollTo().performClick()
        composeRule.onNodeWithText("Em qual conexão você joga?").assertIsDisplayed()
        composeRule.onNodeWithText("Cabo de rede").performClick()
        composeRule.onNodeWithText("Continuar").assertIsEnabled().performClick()

        assertEquals(listOf(ObjetivoDiagnostico.JOGOS_COM_LAG), objetivos)
        assertEquals(listOf(1), respostas)
        assertEquals(1, conclusoes)
    }

    @Test
    fun `estado salvo restaura contexto sem repetir confirmacao`() {
        val original = AssistScreenState(ObjetivoDiagnostico.WIFI_VS_OPERADORA.name, 2)
        val restored = AssistScreenState.restoreSnapshot(AssistScreenState.snapshot(original))

        assertEquals(ObjetivoDiagnostico.WIFI_VS_OPERADORA, restored.objetivo)
        assertEquals(2, restored.selectedAnswer)
        assertTrue(restored.retomada)
    }

    @Test
    fun `toque repetido no objetivo neutro inicia uma unica vez`() {
        var objetivos = 0
        var conclusoes = 0
        setContent(
            onObjetivo = { _, _ -> objetivos += 1 },
            onConcluir = { _, _ -> conclusoes += 1 },
        )

        composeRule.onNodeWithText("Quero verificar minha conexão").performClick().performClick()

        assertEquals(1, objetivos)
        assertEquals(1, conclusoes)
    }

    @Test
    fun `segundo objetivo relevante, nao sei onde esta o problema, tambem confirma pergunta fechada`() {
        val objetivos = mutableListOf<ObjetivoDiagnostico?>()
        val respostas = mutableListOf<Int>()
        setContent(
            onObjetivo = { objetivo, _ -> objetivos += objetivo },
            onResposta = { _, _, resposta, _ -> respostas += resposta },
        )

        composeRule.onNodeWithText(ObjetivoDiagnostico.WIFI_VS_OPERADORA.titulo).performScrollTo().performClick()
        composeRule
            .onNodeWithText("A internet melhora quando você desliga o Wi-Fi e usa a rede móvel?")
            .assertIsDisplayed()
        // Pergunta com 4 opções + texto mais longo empurra "Continuar" para fora da viewport
        // simulada do Robolectric — precisa de performScrollTo() antes do clique (diferente do
        // teste do Jogos acima, cujo conteúdo mais curto cabe sem rolar).
        composeRule.onNodeWithText("Sim, um pouco").performScrollTo().performClick()
        composeRule
            .onNodeWithText("Continuar")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf(ObjetivoDiagnostico.WIFI_VS_OPERADORA), objetivos)
        assertEquals(listOf(1), respostas)
    }

    @Test
    fun `objetivo sem pergunta contextual conclui direto, sem abrir a etapa de contexto`() {
        var conclusoes = 0
        val objetivosConcluidos = mutableListOf<ObjetivoDiagnostico?>()
        setContent(
            onConcluir = { objetivo, respostas ->
                conclusoes += 1
                objetivosConcluidos += objetivo
                assertTrue(respostas.isEmpty())
            },
        )

        composeRule.onNodeWithText(ObjetivoDiagnostico.SITES_DEMORAM.titulo).performScrollTo().performClick()

        assertEquals(1, conclusoes)
        assertEquals(listOf(ObjetivoDiagnostico.SITES_DEMORAM), objetivosConcluidos)
    }

    @Test
    fun `voltar antes de escolher objetivo abandona sem objetivo e permite retomar depois`() {
        var abandonos = 0
        var objetivoAbandonado: ObjetivoDiagnostico? = ObjetivoDiagnostico.SITES_DEMORAM
        var retomavel = true
        composeRule.setContent {
            SignallQTheme {
                AssistScreen(
                    onObjetivoConfirmado = { _, _ -> },
                    onRespostaConfirmada = { _, _, _, _ -> },
                    onConcluir = { _, _ -> },
                    onAbandonar = { objetivo, podeRetomar ->
                        abandonos += 1
                        objetivoAbandonado = objetivo
                        retomavel = podeRetomar
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Voltar").performClick()

        assertEquals(1, abandonos)
        assertNull(objetivoAbandonado)
        assertEquals(false, retomavel)
    }

    @Test
    fun `titulo muda de sintoma para Assist ao entrar na pergunta contextual`() {
        setContent()

        composeRule.onNodeWithText("O que está acontecendo?").assertIsDisplayed()

        composeRule.onNodeWithText(ObjetivoDiagnostico.JOGOS_COM_LAG.titulo).performScrollTo().performClick()

        composeRule.onNodeWithText("Assist").assertIsDisplayed()
    }

    private fun setContent(
        onObjetivo: (ObjetivoDiagnostico?, Boolean) -> Unit = { _, _ -> },
        onResposta: (ObjetivoDiagnostico, AssistContexto, Int, Boolean) -> Unit = { _, _, _, _ -> },
        onConcluir: (ObjetivoDiagnostico?, List<Int>) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            SignallQTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    AssistScreen(onObjetivo, onResposta, onConcluir) { _, _ -> }
                }
            }
        }
    }
}
