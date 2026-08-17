package io.signallq.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
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

    private var analiseIniciada = 0
    private var analiseCancelada = 0

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

    // ---------------------------------------------------------------------------------------
    // Rota `Analise` (spec 2.0 §8.5) — GH#1704 parte 4/4.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `fim do roteiro sem medicao entra na analise e dispara a medicao`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.NaoIniciada,
            statusMedicao = null,
        )

        completarSegundaPerguntaJogos()

        assertEquals(1, analiseIniciada)
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertExists()
    }

    // Este é o teste que prova o objetivo da fatia: antes dela, `resultadoValidoParaConclusao =
    // false` fazia o banner de resultado inválido substituir a tela INTEIRA já na entrada — nem a
    // lista de objetivos aparecia, e não havia ação nenhuma disponível para a pessoa.
    @Test
    fun `sem medicao valida a entrada mostra a lista de objetivos, nao o banner`() {
        setContent(
            estadoAnalise = EstadoAnaliseGuiada.NaoIniciada,
            statusMedicao = null,
        )

        composeRule.onNodeWithText("Vamos descobrir o que está acontecendo").assertIsDisplayed()
        composeRule.onNodeWithText(ObjetivoDiagnostico.JOGOS_COM_LAG.titulo).assertIsDisplayed()
    }

    // COMPORTAMENTO SUBSTITUÍDO PELA #1705, de propósito. Este teste afirmava que uma medição
    // concluída porém inválida fazia o fluxo **remedir sozinho** — foi o que a #1704 entregou e o
    // que Caio aprovou naquela rodada.
    //
    // A #1705 mostra por que aquilo era defeito: remedir em silêncio repete uma medição que acabou
    // de falhar e não diz nada à pessoa. Agora o fluxo CHEGA à conclusão e o que ela encontra lá é
    // a explicação do status e um botão. Quem decide remedir é ela, com o motivo na tela.
    @Test
    fun `medicao concluida porem invalida explica e oferece acao, em vez de remedir sozinha`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
            statusMedicao = MeasurementStatus.CONTAMINATED,
        )

        completarSegundaPerguntaJogos()

        assertEquals("não pode disparar medição sozinha", 0, analiseIniciada)
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_CONTINUIDADE_MEDICAO_ACAO).assertExists()
    }

    // Mutante: remover a cláusula `podeConcluirSemMedir` do `onAvancar`, deixando todo fim de
    // roteiro cair na análise. Rodado — este teste falha: quem chega ao fluxo já medido mediria
    // de novo, que é a regressão de jornada mais provável desta fatia.
    @Test
    fun `medicao ja concluida e valida vai direto para a conclusao sem remedir`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            input = inputJogosComWifiFraco(),
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
        )

        completarSegundaPerguntaJogos()

        assertEquals(0, analiseIniciada)
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertDoesNotExist()
        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertIsDisplayed()
    }

    // Mutante: remover a cláusula `emAnalise ->` de `voltarUmPasso`. Rodado — este teste falha
    // com a tela ainda na análise: o back "vazaria" para a cláusula seguinte e voltaria uma
    // pergunta sem nunca cancelar a medição, deixando o executor rodando sozinho.
    @Test
    fun `voltar durante a analise cancela a medicao e retorna ao roteiro`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.NaoIniciada,
            statusMedicao = null,
        )
        completarSegundaPerguntaJogos()
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertExists()

        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA_CANCELAR).performClick()

        assertEquals(1, analiseCancelada)
        composeRule.onNodeWithText("Com que frequência isso acontece?").assertIsDisplayed()
    }

    // BLOQUEIO B1 do parecer de Caio na PR #1719. Todos os testes acima passam `estadoAnalise`
    // ESTÁTICO — e o `LaunchedEffect` + `medicaoObservadaEmCurso` existem exatamente para tratar
    // mudança no tempo. Com estado parado, esvaziar o ramo
    // `medicaoObservadaEmCurso -> { emAnalise = false; mostrarResultado = true }` sobrevivia à
    // suíte: o fluxo NUNCA sairia da rota Analise — a promessa inteira da via B — sem nada
    // reclamar. Pior: o teste `medicao concluida porem invalida remede` assere que a tela FICA na
    // análise, ou seja, é a asserção do estado travado.
    //
    // Este teste dirige o estado por `mutableStateOf`: NaoIniciada → EmAndamento → Concluida.
    // Rodado com o mutante: falha. Rodado sem: passa.
    @Test
    fun `analise que conclui no tempo leva a tela para a conclusao`() {
        var estado by mutableStateOf<EstadoAnaliseGuiada>(EstadoAnaliseGuiada.NaoIniciada)
        var status by mutableStateOf<MeasurementStatus?>(null)
        composeRule.setContent {
            SignallQTheme {
                TelaDeTeste(
                    objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
                    respostaPreSelecionadaPasso0 = 0,
                    input = inputJogosComWifiFraco(),
                    estadoAnalise = estado,
                    statusMedicao = status,
                )
            }
        }

        completarSegundaPerguntaJogos()
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertExists()

        estado = EstadoAnaliseGuiada.EmAndamento(0.4f, "Medindo a velocidade de recebimento")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertExists()

        estado = EstadoAnaliseGuiada.Concluida
        status = MeasurementStatus.COMPLETE
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertDoesNotExist()
        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertIsDisplayed()
    }

    // RESSALVA RS1 do parecer. `etapaEmLinguagemHumana` era testada como função pura e nunca como
    // pixel — fixar a linha da etapa em "Preparando a análise" sobrevivia à suíte, e a tradução
    // podia nunca chegar à tela sem nada acusar.
    //
    // Armadilha que o parecer documenta e que este teste respeita: o `clearAndSetSemantics` do
    // componente apaga a semântica de texto, então `onNodeWithText` NÃO alcança a etapa — só
    // `onNodeWithContentDescription`.
    @Test
    fun `a etapa em linguagem humana chega a tela, nao so a funcao pura`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.EmAndamento(0.4f, "Medindo a velocidade de recebimento"),
            statusMedicao = null,
        )

        completarSegundaPerguntaJogos()

        composeRule
            .onNodeWithContentDescription("Analisando: Medindo a velocidade de recebimento")
            .assertExists()
    }

    // RESSALVA RS13. A RS3 nasceu de eu ter aceitado "função pura testada" como prova de
    // comportamento — o `progresso` era calculado, testado e nunca chegava a pixel. Aceitar agora
    // "é visual, confia" repetiria o mesmo erro com outro nome.
    //
    // O que se testa aqui é a ESCOLHA DO RAMO, que é lógica: `EmAndamento` → indicador determinado,
    // qualquer outro → indeterminado. Animação, easing e cor ficam de fora de propósito.
    //
    // Por tag e não por `ProgressBarRangeInfo`: o `clearAndSetSemantics` que torna o indicador
    // decorativo para o TalkBack apaga justamente essa propriedade. Tentei o matcher de semântica
    // primeiro e ele não acha o nó — mudar a acessibilidade para acomodar o teste seria trocar o
    // certo pelo conveniente.
    @Test
    fun `com progresso o indicador e determinado, sem progresso e indeterminado`() {
        var estado by mutableStateOf<EstadoAnaliseGuiada>(EstadoAnaliseGuiada.NaoIniciada)
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoGuiadoAnaliseSection(
                    estado = estado,
                    onCancelar = {},
                    onTentarNovamente = {},
                )
            }
        }

        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA_INDETERMINADO).assertExists()
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA_DETERMINADO).assertDoesNotExist()

        estado = EstadoAnaliseGuiada.EmAndamento(0.4f, "Medindo a velocidade de recebimento")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA_DETERMINADO).assertExists()
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA_INDETERMINADO).assertDoesNotExist()
    }

    @Test
    fun `falha na analise oferece tentar de novo e redispara a medicao`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.Falhou("sem conexão"),
            statusMedicao = null,
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithText("sem conexão").assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA_TENTAR_NOVAMENTE).performClick()
        assertEquals(2, analiseIniciada) // 1 ao entrar na rota + 1 no "Tentar de novo"
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

    // ---------------------------------------------------------------------------------------
    // Continuidade por status de medição — GH#1705 (2.0.09c).
    // ---------------------------------------------------------------------------------------

    // Mutante que estes quatro matam: voltar `statusMedicao` a um `Boolean`, isto é, tratar os 4
    // status não-completos com o mesmo texto e a mesma ação. Cada teste assere o título ESPECÍFICO
    // do seu status — checar só "existe um banner" passaria com o achatamento de volta.
    @Test
    fun `contaminado explica a troca de rede e oferece refazer`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
            statusMedicao = MeasurementStatus.CONTAMINATED,
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithText("Sua rede mudou durante a medição").assertIsDisplayed()
        composeRule.onNodeWithText("Refazer na mesma rede").assertIsDisplayed()
        // Sem conclusão junto: o dado veio de outra rede.
        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertDoesNotExist()
    }

    @Test
    fun `inconclusivo declara a insuficiencia e oferece medir de novo`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
            statusMedicao = MeasurementStatus.INCONCLUSIVE,
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithText("Não consegui medir o suficiente").assertIsDisplayed()
        composeRule.onNodeWithText("Medir de novo").assertIsDisplayed()
    }

    // §9 da spec: "mostra o que foi possível concluir E o teste necessário para avançar". Antes da
    // #1705 o parcial caía no mesmo banner do contaminado e a conclusão nem era exibida.
    @Test
    fun `parcial mostra a conclusao possivel junto com o que falta`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            input = inputJogosComWifiFraco(),
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
            statusMedicao = MeasurementStatus.PARTIAL,
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithText("Consegui medir parte da sua conexão").assertIsDisplayed()
        composeRule.onNodeWithText("Completar a medição").assertIsDisplayed()
        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertIsDisplayed()
    }

    @Test
    fun `cancelado preserva a evidencia medida e permite seguir`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            input = inputJogosComWifiFraco(),
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
            statusMedicao = MeasurementStatus.CANCELLED,
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithText("Você interrompeu a medição").assertIsDisplayed()
        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertIsDisplayed()
    }

    @Test
    fun `completo nao mostra continuidade nenhuma`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            input = inputJogosComWifiFraco(),
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
            statusMedicao = MeasurementStatus.COMPLETE,
        )

        completarSegundaPerguntaJogos()

        composeRule.onNodeWithTag(TAG_CONTINUIDADE_MEDICAO).assertDoesNotExist()
        composeRule.onNodeWithText("Força do sinal Wi-Fi").assertIsDisplayed()
    }

    // O beco sem saída que a issue descreve: o banner anterior era só texto. Este teste prova que
    // a ação leva de volta à rota `Analise` e dispara medição — não termina em nada.
    @Test
    fun `a acao da continuidade remede pelo proprio fluxo`() {
        setContent(
            objetivoPreSelecionado = ObjetivoDiagnostico.JOGOS_COM_LAG,
            respostaPreSelecionadaPasso0 = 0,
            estadoAnalise = EstadoAnaliseGuiada.Concluida,
            statusMedicao = MeasurementStatus.INCONCLUSIVE,
        )
        completarSegundaPerguntaJogos()
        val disparosAntes = analiseIniciada

        composeRule.onNodeWithTag(TAG_CONTINUIDADE_MEDICAO_ACAO).performClick()

        assertEquals(disparosAntes + 1, analiseIniciada)
        composeRule.onNodeWithTag(TAG_ANALISE_GUIADA).assertExists()
    }

    @Suppress("LongParameterList")
    private fun setContent(
        objetivoPreSelecionado: ObjetivoDiagnostico? = null,
        respostaPreSelecionadaPasso0: Int? = null,
        input: DiagnosticInput? = null,
        /** GH#1704 — `Concluida` e o caminho de quem chega ao fluxo ja com medicao feita, que e
         *  o pressuposto de todos os testes anteriores a esta fatia. Os testes da rota `Analise`
         *  passam os outros estados explicitamente. */
        estadoAnalise: EstadoAnaliseGuiada = EstadoAnaliseGuiada.Concluida,
        statusMedicao: MeasurementStatus? = MeasurementStatus.COMPLETE,
    ) {
        composeRule.setContent {
            SignallQTheme {
                TelaDeTeste(
                    objetivoPreSelecionado = objetivoPreSelecionado,
                    respostaPreSelecionadaPasso0 = respostaPreSelecionadaPasso0,
                    input = input,
                    estadoAnalise = estadoAnalise,
                    statusMedicao = statusMedicao,
                )
            }
        }
    }

    /**
     * A tela sob teste. Extraida de [setContent] para que o teste do bloqueio B1 possa dirigir
     * `estadoAnalise` por `mutableStateOf` — [setContent] recebe valores fixos no momento da
     * chamada e nao serve para exercitar transicao.
     */
    @Composable
    private fun TelaDeTeste(
        objetivoPreSelecionado: ObjetivoDiagnostico?,
        respostaPreSelecionadaPasso0: Int?,
        input: DiagnosticInput?,
        estadoAnalise: EstadoAnaliseGuiada,
        statusMedicao: MeasurementStatus?,
    ) {
        DiagnosticoGuiadoScreen(
            input = input,
            statusMedicao = statusMedicao,
            analise =
                AnaliseGuiadaContrato(
                    estado = estadoAnalise,
                    onIniciar = { analiseIniciada += 1 },
                    onCancelar = { analiseCancelada += 1 },
                ),
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
            resolveOperadoraIdentidadeRemota = { _, _ -> error("categoria=null nunca chama resolucao de operadora") },
            resolveOperadoraContatoRemoto = { _, _ -> error("categoria=null nunca chama resolucao de operadora") },
        )
    }
}
