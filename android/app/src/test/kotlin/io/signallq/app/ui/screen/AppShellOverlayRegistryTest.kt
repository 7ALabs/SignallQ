package io.signallq.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.dns.EstadoBenchmarkDns
import io.signallq.app.feature.dns.SnapshotBenchmarkDns
import io.signallq.app.feature.speedtest.DiagnosticoFasesSpeedtest
import io.signallq.app.feature.speedtest.DiagnosticoQualidadeSpeedtest
import io.signallq.app.feature.speedtest.GargaloPrimario
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SeveridadeBufferbloat
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.ResolvedOperadoraIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de caracterização do ponto de extensão criado pela issue #1695 (épico #1647).
 *
 * Cada overlay migrado (`AppShellXxxOverlay.kt`) precisa continuar se comportando
 * EXATAMENTE como o bloco `AnimatedVisibility` que vivia inline em `AppShell.kt`: visível só
 * quando o [AppShellOverlay] correspondente está na pilha, `onVoltar` remove só o seu próprio
 * overlay (nunca outro), e overlays independentes convivem sem interferir uns nos outros. O
 * risco descrito na issue — regressão silenciosa de pilha/estado — é exatamente o que estes
 * testes travam.
 *
 * Duas camadas de cobertura, deliberadamente redundantes:
 * - por overlay (`AppShellXxxOverlay` chamado diretamente) — cobre a condição de visibilidade e
 *   o `onVoltar` de cada um isoladamente;
 * - por registro (`AppShellOverlayRegistry` chamado inteiro, via [RegistryDeTeste]) — cobre que a
 *   chamada para aquele overlay realmente existe dentro do agregador. Achado do parecer de
 *   revisão da PR #1697: sem essa segunda camada, apagar a chamada de um overlay de dentro de
 *   `AppShellOverlayRegistry` deixava a suíte verde (a tela simplesmente sumia do app sem
 *   nenhum teste vermelho). Todas as 8 entradas do registro (`Assist`, `Termos`, `Novidades`,
 *   `Privacidade`, `DetalhesTecnicos`, `SinalWifi`, `Ping`, `Dns`) têm um teste desta camada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShellOverlayRegistryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun resultadoSpeedtestDeTeste(): ResultadoSpeedtest =
        ResultadoSpeedtest(
            timestampEpochMs = 0L,
            specVersion = "1",
            modo = ModoSpeedtest.complete,
            connectionTypeStart = "wifi",
            connectionTypeEnd = "wifi",
            contaminado = false,
            latenciaMs = 10.0,
            jitterMs = 1.0,
            perdaPercentual = 0.0,
            bufferbloatMs = 5.0,
            severidadeBufferbloat = SeveridadeBufferbloat.none,
            downloadMbps = 100.0,
            uploadMbps = 50.0,
            latencyDownloadMs = 10.0,
            latencyUploadMs = 10.0,
            stabilityScore = 1.0,
            peakDownloadMbps = 110.0,
            peakUploadMbps = 55.0,
            packetLossSource = "download",
            dnsLatencyMs = null,
            dnsResolverIp = null,
            dnsProvider = null,
            diagnosticoQualidade =
                DiagnosticoQualidadeSpeedtest(
                    vereditoStreaming = VereditoUso.good,
                    vereditoGamer = VereditoUso.good,
                    vereditoVideoChamada = VereditoUso.good,
                    gargaloPrimario = GargaloPrimario.none,
                ),
            diagnosticoFases =
                DiagnosticoFasesSpeedtest(
                    faseInterrompida = "",
                    latenciaAmostrasTotais = 0,
                    latenciaAmostrasValidas = 0,
                    latenciaTimeouts = 0,
                    downloadBytesTotal = 0L,
                    downloadAmostrasValidas = 0,
                    downloadRequisicoesSucesso = 0,
                    downloadRequisicoesErro = 0,
                    downloadEncerradaPor = "",
                    downloadThroughputOrigem = "",
                    downloadUltimoErro = null,
                    uploadBytesTotal = 0L,
                    uploadAmostrasValidas = 0,
                    uploadRequisicoesSucesso = 0,
                    uploadRequisicoesErro = 0,
                    uploadEncerradaPor = "",
                    uploadThroughputOrigem = "",
                    uploadUltimoErro = null,
                    dnsErroMensagem = null,
                ),
            status = MeasurementStatus.COMPLETE,
        )

    private fun snapshotDnsDeTeste(): SnapshotBenchmarkDns =
        SnapshotBenchmarkDns(
            estado = EstadoBenchmarkDns.idle,
            progressoPercentual = 0,
            resultados = emptyList(),
            erroMensagem = null,
        )

    /**
     * Entrada do diagnóstico guiado para os testes — issue #1704. Primeira entrada do registro no
     * formato agrupado (`AppShellXxxEntry`), padrão que a ressalva 3 de Caio (PR #1697) tornou
     * obrigatório para toda migração de overlay a partir daqui.
     */
    private fun diagnosticoGuiadoDeTeste(
        resultado: ResultadoSpeedtest? = null,
        disparos: MutableList<String> = mutableListOf(),
        identidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? = { _, _ -> IDENTIDADE_DE_TESTE },
    ) = AppShellDiagnosticoGuiadoEntry(
        dados =
            AppShellDiagnosticoGuiadoDados(
                input = null,
                resultado = resultado,
                analisadorState = AnalisadorState.Inativo,
                objetivoPreSelecionado = null,
                respostaPreSelecionadaPasso0 = null,
                categoria = null,
                ispNome = "ISP de teste",
                operadoraMovel = null,
                recommendationDecision = null,
                recommendationFeedback = null,
            ),
        operadora =
            AppShellOperadoraResolvers(
                identidadeLocal = identidadeLocal,
                contatoLocal = { _, _ -> null },
                // Identidade DISTINTA da local de propósito (ressalva RS2 de Caio na re-revisão
                // da PR #1708): se as duas devolvessem o mesmo objeto, o teste de tela que vai
                // cobrir a resolução de operadora não conseguiria distinguir local de remota —
                // ligar identidadeLocal em identidadeRemota passaria. É o mesmo defeito do
                // achado R1, um nível abaixo, evitado antes de o teste existir.
                identidadeRemota = { _, _ -> IDENTIDADE_REMOTA_DE_TESTE },
                contatoRemoto = { _, _ -> error("nao usado neste teste") },
            ),
        // Cada lambda grava um id PRÓPRIO em vez de `{}`. Achado R1 do parecer de Caio na
        // PR #1702, reincidente na PR #1708: com todos os campos fixados no mesmo valor neutro,
        // trocar dois de lugar no data class (ex.: `onVoltar` recebendo `onIrParaHome`) não é
        // detectável por nenhuma asserção — e essa troca muda comportamento de verdade, porque
        // `onIrParaHome` limpa o estado do Assist e navega para a raiz.
        acoes =
            AppShellDiagnosticoGuiadoAcoes(
                onAnalisarProblema = { disparos += "analisarProblema" },
                onResetarAnalisador = { disparos += "resetarAnalisador" },
                onVoltar = { disparos += "voltar" },
                onIrParaHome = { disparos += "irParaHome" },
                onIniciarModoGamer = { disparos += "iniciarModoGamer" },
                onAbrirFerramentaSugerida = { disparos += "abrirFerramentaSugerida" },
                onMedirNovamente = { disparos += "medirNovamente" },
                onRecommendationShown = { disparos += "recommendationShown" },
                onRecommendationClicked = { disparos += "recommendationClicked" },
                onRecommendationFeedback = { disparos += "recommendationFeedback" },
            ),
    )

    /**
     * Wiring padrão do [AppShellOverlayRegistry] para os testes desta classe — os parâmetros que
     * um teste precisa customizar (pilha, resultado de speedtest, callback de gerenciar dados,
     * entrada do diagnóstico guiado) ficam explícitos; o resto é o mínimo neutro para compor sem
     * crash.
     */
    @Composable
    private fun RegistryDeTeste(
        stack: MutableList<AppShellOverlay>,
        resultadoSpeedtest: ResultadoSpeedtest? = null,
        onAbrirGerenciarDados: () -> Unit = {},
        diagnosticoGuiado: AppShellDiagnosticoGuiadoEntry = diagnosticoGuiadoDeTeste(),
    ) {
        AppShellOverlayRegistry(
            overlayStack = stack,
            onAssistObjetivo = {},
            onAssistResposta = {},
            onAssistAbandono = {},
            onPreSelecaoParaDiagnosticoGuiado = { _, _ -> },
            onSolicitarDiagnostico = { null },
            appVersion = "1.0.0",
            onAbrirGerenciarDados = onAbrirGerenciarDados,
            resultadoSpeedtest = resultadoSpeedtest,
            localizacaoServidor = null,
            localDevice = null,
            temPermissaoLocalizacao = true,
            localizacaoBloqueadaPermanentemente = false,
            onSolicitarPermissaoLocalizacao = {},
            snapshotDns = snapshotDnsDeTeste(),
            dnsResolverIp = null,
            snapshotRede = SnapshotRede.desconectado(0L),
            onIniciarBenchmarkDns = {},
            diagnosticoGuiado = diagnosticoGuiado,
        )
    }

    // ─── Diagnóstico guiado (issue #1704) ──────────────────────────────────────

    @Test
    fun `diagnostico guiado sem resultado compoe o estado vazio, nao um container mudo`() {
        // COMPORTAMENTO INVERTIDO PELA #1714, de propósito. A versão anterior deste teste afirmava
        // que o container NÃO compunha sem resultado — e isso era justamente o bug: a pilha de
        // overlays sobrevive ao process death, o `ResultadoSpeedtest` não, e o usuário voltava com
        // um overlay na pilha que não desenhava nada. O back consumia um `pop()` invisível.
        //
        // Agora a ausência de resultado decide O QUE mostrar em vez de esconder. O `testTag`
        // continua sendo o que distingue "compôs" de "não compôs" — segue valendo o achado de Caio
        // no DetalhesTecnicos (PR #1697), só que agora a asserção é a oposta.
        val stack = mutableStateListOf(AppShellOverlay.DiagnosticoGuiado)
        composeRule.setContent { RegistryDeTeste(stack = stack) }
        composeRule.onNodeWithTag(TAG_OVERLAY_DIAGNOSTICO_GUIADO).assertExists()
        composeRule.onNodeWithText("Este resultado não está mais disponível").assertExists()
    }

    @Test
    fun `registry compoe diagnostico guiado quando esta na pilha e ha resultado`() {
        // Mutante que este teste mata: remover a chamada de AppShellDiagnosticoGuiadoOverlay de
        // dentro do registro — a tela sumiria do app com a suíte verde.
        val stack = mutableStateListOf(AppShellOverlay.DiagnosticoGuiado)
        composeRule.setContent {
            RegistryDeTeste(
                stack = stack,
                diagnosticoGuiado = diagnosticoGuiadoDeTeste(resultado = resultadoSpeedtestDeTeste()),
            )
        }
        composeRule.onNodeWithTag(TAG_OVERLAY_DIAGNOSTICO_GUIADO).assertExists()
    }

    @Test
    fun `diagnostico guiado repassa onVoltar e nao onIrParaHome ao botao voltar`() {
        // Mutante que este teste mata: trocar `onVoltar` com `onIrParaHome` dentro de
        // AppShellDiagnosticoGuiadoAcoes. Compila, e o efeito é grave — "voltar" passaria a
        // limpar o estado do Assist e navegar para a raiz Home, em vez de só fechar o overlay.
        //
        // Sobrevivia à suíte inteira do :app antes deste teste, porque o helper fixava os 9
        // lambdas como `{}` e nenhuma asserção olhava para qual disparou. É o achado R1 do
        // parecer de Caio na PR #1702, reincidente aqui.
        val disparos = mutableListOf<String>()
        val stack = mutableStateListOf(AppShellOverlay.DiagnosticoGuiado)
        composeRule.setContent {
            RegistryDeTeste(
                stack = stack,
                diagnosticoGuiado =
                    diagnosticoGuiadoDeTeste(
                        resultado = resultadoSpeedtestDeTeste(),
                        disparos = disparos,
                    ),
            )
        }
        // Estado inicial da tela guiada é a lista de objetivos; ali `voltarUmPasso()` cai no
        // ramo `else -> onVoltar()`.
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertEquals(listOf("voltar"), disparos) }
    }

    @Test
    fun `diagnostico guiado fora da pilha nao compoe`() {
        val stack = mutableStateListOf<AppShellOverlay>()
        composeRule.setContent {
            RegistryDeTeste(
                stack = stack,
                diagnosticoGuiado = diagnosticoGuiadoDeTeste(resultado = resultadoSpeedtestDeTeste()),
            )
        }
        composeRule.onNodeWithTag(TAG_OVERLAY_DIAGNOSTICO_GUIADO).assertDoesNotExist()
    }

    // ─── Por overlay (visibilidade + onVoltar isolados) ─────────────────────────

    @Test
    fun `termos overlay aparece so quando esta na pilha e onVoltar remove so o proprio overlay`() {
        val stack = mutableStateListOf(AppShellOverlay.Perfil)
        composeRule.setContent { AppShellTermosOverlay(overlayStack = stack) }

        composeRule.onNodeWithText("Termos de Uso").assertDoesNotExist()

        composeRule.runOnIdle { stack.add(AppShellOverlay.Termos) }
        composeRule.onNodeWithText("Termos de Uso").assertExists()

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle {
            assertFalse(AppShellOverlay.Termos in stack)
            assertTrue(AppShellOverlay.Perfil in stack)
        }
    }

    @Test
    fun `novidades overlay aparece so quando esta na pilha e onVoltar remove`() {
        val stack = mutableStateListOf<AppShellOverlay>()
        composeRule.setContent { AppShellNovidadesOverlay(overlayStack = stack, appVersion = "9.9.9") }

        composeRule.onNodeWithText("Novidades").assertDoesNotExist()

        composeRule.runOnIdle { stack.add(AppShellOverlay.Novidades) }
        composeRule.onNodeWithText("Novidades").assertExists()
        // Trava a passagem de appVersion: sem isto, fixar a versao em "0.0.0" no overlay
        // sobrevive a suite (mutante M16 do parecer de Caio, PR #1697 rodada 2).
        composeRule.onNodeWithText("v9.9.9", substring = true).assertExists()

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertFalse(AppShellOverlay.Novidades in stack) }
    }

    @Test
    fun `privacidade overlay fecha e aciona onAbrirGerenciarDados no mesmo callback`() {
        val stack = mutableStateListOf(AppShellOverlay.Privacidade)
        var gerenciarDadosAberto = false
        composeRule.setContent {
            AppShellPrivacidadeOverlay(
                overlayStack = stack,
                onAbrirGerenciarDados = { gerenciarDadosAberto = true },
            )
        }
        composeRule.onNodeWithText("Privacidade").assertExists()

        // "Gerenciar dados e privacidade" fica dentro de uma LazyColumn (PrivacidadeScreen já
        // existente, fora do escopo desta extração) -- performScrollToNode navega até um item
        // ainda não composto, diferente de performScrollTo (que só rola o já visível).
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Gerenciar dados e privacidade"))
        composeRule.onNodeWithText("Gerenciar dados e privacidade").performClick()

        composeRule.runOnIdle {
            assertFalse(AppShellOverlay.Privacidade in stack)
            assertTrue(gerenciarDadosAberto)
        }
    }

    @Test
    fun `detalhes tecnicos sem resultado compoe o estado vazio, nao um container mudo`() {
        // COMPORTAMENTO INVERTIDO PELA #1714, de propósito — mesmo motivo do diagnóstico guiado
        // acima. A versão anterior deste teste nasceu de um achado de Caio na PR #1697: asserir só
        // a ausência do texto era fachada, porque o `?.let` interno omitia o conteúdo sozinho, e o
        // `testTag` foi acrescentado para distinguir "não compôs" de "compôs vazio".
        //
        // O `testTag` continua sendo o instrumento certo; o que mudou é o veredito. "Compôs vazio"
        // deixou de ser o defeito a evitar e passou a ser o comportamento correto — só que agora o
        // vazio tem conteúdo: uma tela que explica por que o resultado sumiu.
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            AppShellDetalhesTecnicosOverlay(
                overlayStack = stack,
                resultadoSpeedtest = null,
                localizacaoServidor = null,
                localDevice = null,
            )
        }
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertExists()
        composeRule.onNodeWithText("Este resultado não está mais disponível").assertExists()
    }

    @Test
    fun `detalhes tecnicos aparece e onVoltar remove quando ha resultado`() {
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            AppShellDetalhesTecnicosOverlay(
                overlayStack = stack,
                resultadoSpeedtest = resultadoSpeedtestDeTeste(),
                localizacaoServidor = "São Paulo, SP",
                localDevice = null,
            )
        }
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertExists()
        composeRule.onNodeWithText("Detalhes da conexão").assertExists()

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertFalse(AppShellOverlay.DetalhesTecnicos in stack) }
    }

    @Test
    fun `sinal wifi overlay aparece so quando esta na pilha e onVoltar remove`() {
        val stack = mutableStateListOf<AppShellOverlay>()
        composeRule.setContent {
            AppShellSinalWifiOverlay(
                overlayStack = stack,
                temPermissaoLocalizacao = true,
                localizacaoBloqueadaPermanentemente = false,
                onSolicitarPermissaoLocalizacao = {},
            )
        }
        composeRule.onNodeWithText("Sinal WiFi").assertDoesNotExist()

        composeRule.runOnIdle { stack.add(AppShellOverlay.SinalWifi) }
        composeRule.onNodeWithText("Sinal WiFi").assertExists()

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertFalse(AppShellOverlay.SinalWifi in stack) }
    }

    @Test
    fun `ping overlay nao compoe PingScreen quando nao esta na pilha`() {
        val stack = mutableStateListOf<AppShellOverlay>()
        composeRule.setContent { AppShellPingOverlay(overlayStack = stack) }
        // PingScreen dispara IO real (PingExecutor) -- so validamos aqui o estado fechado,
        // sem instanciar a ModalBottomSheet. O caminho aberto e coberto manualmente no
        // emulador (ver relatório da issue #1695) e pelo teste de registro abaixo, que só
        // verifica o título estático (`R.string.ping_titulo`, "Teste de Latência" — renderizado
        // no primeiro frame, antes do resultado do ping real chegar) -- não espera o benchmark
        // terminar.
        composeRule.onNodeWithText("Teste de Latência", substring = false).assertDoesNotExist()
    }

    @Test
    fun `dns overlay aparece so quando esta na pilha e onVoltar remove`() {
        val stack = mutableStateListOf<AppShellOverlay>()
        composeRule.setContent {
            AppShellDnsOverlay(
                overlayStack = stack,
                snapshotDns = snapshotDnsDeTeste(),
                dnsResolverIp = null,
                snapshotRede = SnapshotRede.desconectado(0L),
                onIniciarBenchmark = {},
            )
        }
        composeRule.onNodeWithText("Comparativo de DNS").assertDoesNotExist()

        composeRule.runOnIdle { stack.add(AppShellOverlay.Dns) }
        composeRule.onNodeWithText("Comparativo de DNS").assertExists()

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertFalse(AppShellOverlay.Dns in stack) }
    }

    // ─── Por registro (a chamada dentro de AppShellOverlayRegistry existe e funciona) ────

    @Test
    fun `registry compoe overlays independentes simultaneamente sem interferencia`() {
        val stack = mutableStateListOf(AppShellOverlay.Termos, AppShellOverlay.Novidades)
        composeRule.setContent { RegistryDeTeste(stack = stack) }

        composeRule.onNodeWithText("Termos de Uso").assertExists()
        composeRule.onNodeWithText("Novidades").assertExists()

        // Remove só o Termos da pilha compartilhada e confirma que o Novidades independente
        // continua aberto -- prova que o registro não acopla estado/visibilidade entre
        // entradas (o risco de regressão citado na issue: pilha e z-index compartilhados).
        composeRule.runOnIdle { stack.remove(AppShellOverlay.Termos) }
        composeRule.onNodeWithText("Termos de Uso").assertDoesNotExist()
        composeRule.onNodeWithText("Novidades").assertExists()
    }

    @Test
    fun `registry compoe assist a partir do estado padrao SignallQ Assist`() {
        val stack = mutableStateListOf(AppShellOverlay.Assist)
        composeRule.setContent { RegistryDeTeste(stack = stack) }
        composeRule.onNodeWithText("O que está acontecendo com sua internet?").assertExists()
    }

    @Test
    fun `registry compoe privacidade e aciona onAbrirGerenciarDados`() {
        val stack = mutableStateListOf(AppShellOverlay.Privacidade)
        var gerenciarDadosAberto = false
        composeRule.setContent {
            RegistryDeTeste(stack = stack, onAbrirGerenciarDados = { gerenciarDadosAberto = true })
        }
        composeRule.onNodeWithText("Privacidade").assertExists()

        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Gerenciar dados e privacidade"))
        composeRule.onNodeWithText("Gerenciar dados e privacidade").performClick()

        composeRule.runOnIdle {
            assertFalse(AppShellOverlay.Privacidade in stack)
            assertTrue(gerenciarDadosAberto)
        }
    }

    @Test
    fun `registry compoe detalhes tecnicos quando ha resultado de speedtest`() {
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            RegistryDeTeste(stack = stack, resultadoSpeedtest = resultadoSpeedtestDeTeste())
        }
        composeRule.onNodeWithText("Detalhes da conexão").assertExists()
    }

    @Test
    fun `registry compoe sinal wifi quando esta na pilha`() {
        val stack = mutableStateListOf(AppShellOverlay.SinalWifi)
        composeRule.setContent { RegistryDeTeste(stack = stack) }
        composeRule.onNodeWithText("Sinal WiFi").assertExists()
    }

    @Test
    fun `registry compoe ping quando esta na pilha`() {
        val stack = mutableStateListOf(AppShellOverlay.Ping)
        composeRule.setContent { RegistryDeTeste(stack = stack) }
        // Mesma cautela do teste isolado de Ping: só o título estático (primeiro frame, não
        // depende do resultado do PingExecutor real) -- suficiente para travar o mutante
        // "remover a chamada de AppShellPingOverlay de dentro do registro".
        composeRule.onNodeWithText("Teste de Latência", substring = false).assertExists()
    }

    @Test
    fun `registry compoe dns quando esta na pilha`() {
        val stack = mutableStateListOf(AppShellOverlay.Dns)
        composeRule.setContent { RegistryDeTeste(stack = stack) }
        composeRule.onNodeWithText("Comparativo de DNS").assertExists()
    }
}

private val IDENTIDADE_REMOTA_DE_TESTE =
    ResolvedOperadoraIdentity(
        displayName = "Operadora remota de teste",
        monograma = "R",
        corMarca = null,
        logoRes = null,
        logoUrl = null,
        source = OperadoraSource.REMOTE,
    )

private val IDENTIDADE_DE_TESTE =
    ResolvedOperadoraIdentity(
        displayName = "Operadora local de teste",
        monograma = "T",
        corMarca = null,
        logoRes = null,
        logoUrl = null,
        source = OperadoraSource.LOCAL,
    )
