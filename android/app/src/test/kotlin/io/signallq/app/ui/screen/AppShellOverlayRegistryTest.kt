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
     * Wiring padrão do [AppShellOverlayRegistry] para os testes desta classe — os parâmetros que
     * um teste precisa customizar (pilha, resultado de speedtest, callback de gerenciar dados)
     * ficam explícitos; o resto é o mínimo neutro para compor sem crash.
     */
    @Composable
    private fun RegistryDeTeste(
        stack: MutableList<AppShellOverlay>,
        resultadoSpeedtest: ResultadoSpeedtest? = null,
        onAbrirGerenciarDados: () -> Unit = {},
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
        )
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
    fun `detalhes tecnicos nao compoe o container quando resultado e nulo mesmo com overlay na pilha`() {
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            AppShellDetalhesTecnicosOverlay(
                overlayStack = stack,
                resultadoSpeedtest = null,
                localizacaoServidor = null,
                localDevice = null,
            )
        }
        // Achado do parecer da PR #1697: asserir só a ausência do texto "Detalhes da conexão"
        // era fachada -- o `?.let` interno já omite o texto sozinho, então a asserção passava
        // igual com a guarda `&& resultadoSpeedtest != null` do `visible` removida (o container
        // `AnimatedVisibility` compunha vazio, mas sem nenhum node de texto para achar). O
        // `testTag` no modifier do container (ver AppShellDetalhesTecnicosOverlay.kt) existe só
        // para este teste distinguir "container não compôs" (guarda presente, comportamento
        // real) de "container compôs vazio" (guarda removida, mutante).
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertDoesNotExist()
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
