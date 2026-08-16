package io.signallq.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
    fun `detalhes tecnicos so aparece com overlay na pilha E resultado nao nulo`() {
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            AppShellDetalhesTecnicosOverlay(
                overlayStack = stack,
                resultadoSpeedtest = null,
                localizacaoServidor = null,
                localDevice = null,
            )
        }
        // Overlay na pilha mas sem resultado ainda -- nao deve desenhar tela vazia.
        composeRule.onNodeWithText("Detalhes da conexão").assertDoesNotExist()
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
        // emulador (ver relatório da issue #1695).
        composeRule.onNodeWithText("Ping", substring = false).assertDoesNotExist()
    }

    @Test
    fun `registry compoe overlays independentes simultaneamente sem interferencia`() {
        val stack = mutableStateListOf(AppShellOverlay.Termos, AppShellOverlay.Novidades)
        composeRule.setContent {
            AppShellOverlayRegistry(
                overlayStack = stack,
                onAssistObjetivo = {},
                onAssistResposta = {},
                onAssistAbandono = {},
                onPreSelecaoParaDiagnosticoGuiado = { _, _ -> },
                onSolicitarDiagnostico = { null },
                appVersion = "1.0.0",
                onAbrirGerenciarDados = {},
                resultadoSpeedtest = null,
                localizacaoServidor = null,
                localDevice = null,
                temPermissaoLocalizacao = true,
                localizacaoBloqueadaPermanentemente = false,
                onSolicitarPermissaoLocalizacao = {},
            )
        }

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
        composeRule.setContent {
            AppShellOverlayRegistry(
                overlayStack = stack,
                onAssistObjetivo = {},
                onAssistResposta = {},
                onAssistAbandono = {},
                onPreSelecaoParaDiagnosticoGuiado = { _, _ -> },
                onSolicitarDiagnostico = { null },
                appVersion = "1.0.0",
                onAbrirGerenciarDados = {},
                resultadoSpeedtest = null,
                localizacaoServidor = null,
                localDevice = null,
                temPermissaoLocalizacao = true,
                localizacaoBloqueadaPermanentemente = false,
                onSolicitarPermissaoLocalizacao = {},
            )
        }
        composeRule.onNodeWithText("O que está acontecendo com sua internet?").assertExists()
    }
}
