package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
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
 *
 * Review da PR #1683 (bloqueio 4): `assertIsEnabled()` sozinho só prova que *alguma*
 * resposta chegou (o botão fica habilitado com qualquer índice >= 0), não que chegou a
 * resposta *certa* — trocar `respostaPreSelecionadaPasso0` por qualquer outro índice
 * manteria esse teste verde. Os dois testes abaixo completam o roteiro até o resultado e
 * comparam a evidência renderizada (presença/ausência de "Força do sinal Wi-Fi"), que é
 * exatamente o que muda entre índice 0 ("Wi-Fi") e 1 ("Cabo de rede") em
 * `DiagnosticoGuiadoEngine.avaliarJogosComLag` — a mesma distinção que o WIP original do
 * Codex descartava em silêncio.
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

    @Test
    fun `resposta pre-selecionada 'cabo de rede' (indice 1) suprime a evidencia de wifi no resultado`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 1, // "Cabo de rede"
            input = inputJogosComWifiFraco(),
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertDoesNotExist()
    }

    @Test
    fun `resposta pre-selecionada 'wi-fi' (indice 0) mantem a evidencia de wifi no resultado`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0, // "Wi-Fi"
            input = inputJogosComWifiFraco(),
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertIsDisplayed()
    }

    /** Avança da primeira pergunta (já pré-preenchida pelo Assist) até o resultado,
     *  respondendo a segunda pergunta do roteiro de Jogos com lag normalmente. */
    private fun completarSegundaPerguntaJogos() {
        composeRule.onNodeWithText("Em qual conexão você joga?").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Com que frequência isso acontece?").assertIsDisplayed()
        composeRule.onNodeWithText("Quase sempre").performClick()
        composeRule.onNodeWithText("Ver o que identifiquei").performClick()
    }

    /** Mesmos valores do caso `jogos com lag fica critica com latencia alta` em
     *  `DiagnosticoGuiadoEngineTest` — Wi-Fi fraco o bastante para gerar a evidência
     *  quando não suprimida por `jogaPorCabo`. */
    private fun inputJogosComWifiFraco() =
        DiagnosticInput(
            internet =
                InternetDiagnosticInput(
                    downloadMbps = 80.0,
                    uploadMbps = 20.0,
                    latencyMs = 187.0,
                    jitterMs = 44.0,
                    perdaPercentual = 0.5,
                ),
            wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
        )

    @Suppress("LongParameterList")
    private fun setContent(
        objetivoPreSelecionado: ObjetivoDiagnostico? = null,
        respostaPreSelecionadaPasso0: Int? = null,
        input: DiagnosticInput? = null,
    ) {
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoGuiadoScreen(
                    input = input,
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
