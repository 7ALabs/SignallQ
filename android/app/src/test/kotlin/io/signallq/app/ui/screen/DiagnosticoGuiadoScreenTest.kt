package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de caracterização da pré-seleção vinda do SignallQ Assist (issue #1656) —
 * [DiagnosticoGuiadoScreen.objetivoPreSelecionado]/[DiagnosticoGuiadoScreen.respostaPreSelecionadaPasso0]
 * são a ponte entre "sintoma escolhido antes do teste" e o "plano existente" (o roteiro
 * fechado de perguntas + [io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine]) —
 * sem essa ponte a resposta do Assist não mudaria nada além de analytics, o que
 * contraria o critério de aceite "pergunta só existe se muda plano, recomendação ou
 * confiança". Não cobre o fluxo padrão completo (objetivo escolhido na própria tela,
 * sem Assist) — esse já era implícito no comportamento anterior e não muda aqui.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiagnosticoGuiadoScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `sem pre-selecao mostra a lista de objetivos como antes`() {
        setContent()

        composeRule.onNodeWithText("Vamos descobrir o que está acontecendo").assertIsDisplayed()
        composeRule.onNodeWithText(ObjetivoDiagnostico.JOGOS_COM_LAG.titulo).assertIsDisplayed()
    }

    @Test
    fun `objetivo e resposta pre-selecionados pulam a lista e abrem a primeira pergunta respondida`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 1,
        )

        // Pula direto pro roteiro do objetivo escolhido no Assist — não pede de novo.
        composeRule.onNodeWithText("Em qual conexão você joga?").assertIsDisplayed()
        // Resposta do Assist já preenchida: o usuário só confirma, não responde de novo.
        composeRule.onNodeWithText("Continuar").assertIsEnabled()
    }

    @Test
    fun `objetivo pre-selecionado sem resposta pula a lista mas exige responder normalmente`() {
        setContent(objetivoPreSelecionado = ObjetivoDiagnostico.SITES_DEMORAM)

        composeRule.onNodeWithText("Isso acontece em quais sites?").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Suppress("LongParameterList")
    private fun setContent(
        objetivoPreSelecionado: ObjetivoDiagnostico? = null,
        respostaPreSelecionadaPasso0: Int? = null,
    ) {
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoGuiadoScreen(
                    input = null,
                    resultadoValidoParaConclusao = true,
                    objetivoPreSelecionado = objetivoPreSelecionado,
                    respostaPreSelecionadaPasso0 = respostaPreSelecionadaPasso0,
                    analisadorState = AnalisadorState.Inativo,
                    onAnalisarProblema = {},
                    onResetarAnalisador = {},
                    onVoltar = {},
                    onIrParaHome = {},
                    categoria = null,
                    ispNome = null,
                    connectionType = null,
                    operadoraMovel = null,
                    recommendationDecision = null,
                    recommendationFeedback = null,
                    onRecommendationShown = {},
                    onRecommendationClicked = {},
                    onRecommendationFeedback = {},
                    resolveOperadoraIdentidadeLocal = { _, _ -> null },
                    resolveOperadoraContatoLocal = { _, _ -> null },
                    resolveOperadoraIdentidadeRemota = { _, _ -> error("categoria=null nunca chama resolução de operadora") },
                    resolveOperadoraContatoRemoto = { _, _ -> error("categoria=null nunca chama resolução de operadora") },
                )
            }
        }
    }
}
