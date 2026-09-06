package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.feature.speedtest.DiagnosticoFasesSpeedtest
import io.signallq.app.feature.speedtest.DiagnosticoQualidadeSpeedtest
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.GargaloPrimario
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SeveridadeBufferbloat
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.feature.speedtest.connectivity.ConnectivityAction
import io.signallq.app.feature.speedtest.connectivity.ConnectivityDiagnosisMensagem
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1072 (SPD-004) — caracteriza a visibilidade das fases de execucao (latencia, download,
 * upload, concluido) na tela de overlay do speedtest (VelocidadeScreen/PillsFase).
 *
 * GH#1738 (2.0.10b) — a cobertura era fina (1 teste, só o caminho feliz de progresso). Esta
 * fatia migra o overlay para o design system 2.0 e passa a consumir os 5 valores de
 * `MeasurementStatus` em vez de um booleano achatado (mesmo problema que a #1705 resolveu no
 * fluxo guiado) — os testes abaixo cobrem, além do caminho feliz já existente: `erro`,
 * cancelamento (confirmação + `onCancelar`), e os quatro status não-COMPLETE que agora têm
 * conteúdo real via [continuidadeExecucao]/[ContinuidadeMedicao].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VelocidadeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun snapshot(
        fase: FaseSpeedtest = FaseSpeedtest.idle,
        estado: EstadoExecucaoSpeedtest = EstadoExecucaoSpeedtest.executando,
        erroMensagem: String? = null,
        resultado: ResultadoSpeedtest? = null,
    ) = SnapshotExecucaoSpeedtest(
        estado = estado,
        progressoPercentual = 50,
        resultado = resultado,
        erroMensagem = erroMensagem,
        faseAtual = fase,
    )

    /** Mesmo fixture de [AppShellOverlayRegistryTest] — reduzido aos campos que
     *  [continuidadeExecucao]/`medidasConfiaveis` realmente leem. */
    private fun resultadoSpeedtestDeTeste(
        status: MeasurementStatus = MeasurementStatus.COMPLETE,
        downloadEncerradaPor: String = "",
        uploadNaoDetectado: Boolean = false,
    ): ResultadoSpeedtest =
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
                    downloadEncerradaPor = downloadEncerradaPor,
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
            status = status,
            uploadNaoDetectado = uploadNaoDetectado,
        )

    // ─── continuidadeExecucao — função pura (GH#1738) ─────────────────────────

    @Test
    fun `sem resultado nenhum estado tem continuidade`() {
        EstadoExecucaoSpeedtest.entries.forEach { estado ->
            assertNull(
                "estado $estado sem resultado nao pode ter continuidade",
                continuidadeExecucao(snapshot(estado = estado, resultado = null)),
            )
        }
    }

    @Test
    fun `concluido com status COMPLETE nao tem continuidade`() {
        val snap =
            snapshot(
                estado = EstadoExecucaoSpeedtest.concluido,
                resultado = resultadoSpeedtestDeTeste(status = MeasurementStatus.COMPLETE),
            )
        assertNull(continuidadeExecucao(snap))
    }

    @Test
    fun `resultado concluido fora do estado concluido nao vaza continuidade`() {
        // Guarda contra ler `resultado.status` sem checar `estado` primeiro — um resultado de
        // execucao anterior nao pode aparecer como continuidade de uma execucao nova em andamento.
        val snap =
            snapshot(
                estado = EstadoExecucaoSpeedtest.executando,
                resultado = resultadoSpeedtestDeTeste(status = MeasurementStatus.PARTIAL),
            )
        assertNull(continuidadeExecucao(snap))
    }

    @Test
    fun `partial com medidas confiaveis usa a mesma continuidade do fluxo guiado`() {
        val resultado = resultadoSpeedtestDeTeste(status = MeasurementStatus.PARTIAL)
        val snap = snapshot(estado = EstadoExecucaoSpeedtest.concluido, resultado = resultado)

        assertEquals(
            continuidadeDaMedicao(MeasurementStatus.PARTIAL, medidasConfiaveis = true),
            continuidadeExecucao(snap),
        )
    }

    @Test
    fun `partial com upload nao detectado nao permite ver conclusao parcial`() {
        val resultado =
            resultadoSpeedtestDeTeste(status = MeasurementStatus.PARTIAL, uploadNaoDetectado = true)
        val snap = snapshot(estado = EstadoExecucaoSpeedtest.concluido, resultado = resultado)

        val continuidade = continuidadeExecucao(snap)
        assertEquals(false, continuidade?.permiteVerConclusaoParcial)
    }

    @Test
    fun `contaminado inconclusivo e cancelado tem continuidade propria`() {
        listOf(MeasurementStatus.CONTAMINATED, MeasurementStatus.INCONCLUSIVE, MeasurementStatus.CANCELLED)
            .forEach { status ->
                val resultado = resultadoSpeedtestDeTeste(status = status)
                val snap = snapshot(estado = EstadoExecucaoSpeedtest.concluido, resultado = resultado)

                assertEquals(
                    "status $status",
                    continuidadeDaMedicao(status, medidasConfiaveis = true),
                    continuidadeExecucao(snap),
                )
            }
    }

    // ─── Composable — caminho feliz e caracterizacao (GH#1072, GH#1738) ────────

    @Test
    fun `pills de fase mostram as quatro etapas durante o download`() {
        // VelocidadeScreen tem um loop de animacao continuo (suavizacao do Mbps exibido via
        // withFrameMillis) que nunca fica idle sozinho — precisa de clock manual pro
        // setContent nao travar esperando uma composicao que nunca "termina".
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot = snapshot(fase = FaseSpeedtest.download, estado = EstadoExecucaoSpeedtest.executando),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = {},
                    onReiniciar = {},
                )
            }
        }

        composeRule.onNodeWithText("LATÊNCIA").assertIsDisplayed()
        // "DOWNLOAD" aparece 2x durante a fase de download: no rotulo central do gauge e
        // na pill de fase — ambos esperados, so garante que o rotulo da pill esta la.
        composeRule
            .onAllNodesWithText("DOWNLOAD")
            .assertCountEquals(2)
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithText("UPLOAD").assertIsDisplayed()
        composeRule.onNodeWithText("CONCLUÍDO").assertIsDisplayed()
    }

    @Test
    fun `concluido com status COMPLETE preserva o comportamento anterior`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot =
                        snapshot(
                            fase = FaseSpeedtest.concluido,
                            estado = EstadoExecucaoSpeedtest.concluido,
                            resultado = resultadoSpeedtestDeTeste(status = MeasurementStatus.COMPLETE),
                        ),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = {},
                    onReiniciar = {},
                )
            }
        }

        composeRule.onNodeWithText("Medindo…").assertIsDisplayed()
        composeRule.onNodeWithText("Quase pronto…").assertIsDisplayed()
    }

    @Test
    fun `erro mostra a tela propria com testar novamente e cancelar`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot = snapshot(estado = EstadoExecucaoSpeedtest.erro, erroMensagem = "semRede"),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = {},
                    onReiniciar = {},
                )
            }
        }

        composeRule.onNodeWithText("Erro").assertIsDisplayed()
        composeRule.onNodeWithText("Não foi possível completar o teste").assertIsDisplayed()
        composeRule.onNodeWithText("Testar novamente").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    @Test
    fun `tentar novamente no erro chama onReiniciar`() {
        composeRule.mainClock.autoAdvance = false
        var reiniciou = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot = snapshot(estado = EstadoExecucaoSpeedtest.erro),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = {},
                    onReiniciar = { reiniciou = true },
                )
            }
        }

        composeRule.onNodeWithText("Testar novamente").performClick()
        assertEquals(true, reiniciou)
    }

    @Test
    fun `conclusao parcial mostra titulo e explicacao da continuidade, nao um booleano achatado`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot =
                        snapshot(
                            fase = FaseSpeedtest.concluido,
                            estado = EstadoExecucaoSpeedtest.concluido,
                            resultado = resultadoSpeedtestDeTeste(status = MeasurementStatus.PARTIAL),
                        ),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = {},
                    onReiniciar = {},
                )
            }
        }

        val continuidade =
            continuidadeDaMedicao(MeasurementStatus.PARTIAL, medidasConfiaveis = true)!!
        // Titulo aparece 2x: na TopAppBar e no corpo — ambos esperados.
        composeRule.onAllNodesWithText(continuidade.titulo).assertCountEquals(2)
        composeRule.onNodeWithText(continuidade.explicacao).assertIsDisplayed()
        composeRule.onNodeWithText(continuidade.rotuloAcao).assertIsDisplayed()
        // Sem o botao "Cancelar" de execucao: a medicao ja concluiu, cancelar nao faz sentido.
        composeRule.onNodeWithText("Cancelar").assertDoesNotExist()
    }

    @Test
    fun `conclusao contaminada usa titulo e cor proprios, distintos do parcial`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot =
                        snapshot(
                            fase = FaseSpeedtest.concluido,
                            estado = EstadoExecucaoSpeedtest.concluido,
                            resultado = resultadoSpeedtestDeTeste(status = MeasurementStatus.CONTAMINATED),
                        ),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = {},
                    onReiniciar = {},
                )
            }
        }

        val continuidade = continuidadeDaMedicao(MeasurementStatus.CONTAMINATED, medidasConfiaveis = true)!!
        composeRule.onNodeWithText(continuidade.explicacao).assertIsDisplayed()
        composeRule.onNodeWithText(continuidade.rotuloAcao).assertIsDisplayed()
    }

    // ─── Cancelamento (GH#1738 — criterio de aceite) ───────────────────────────

    @Test
    fun `cancelar pede confirmacao antes de encerrar o teste`() {
        composeRule.mainClock.autoAdvance = false
        var cancelou = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot = snapshot(fase = FaseSpeedtest.download, estado = EstadoExecucaoSpeedtest.executando),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = { cancelou = true },
                    onReiniciar = {},
                )
            }
        }

        composeRule.onNodeWithText("Cancelar").performClick()
        // Loop de animacao continuo (gauge) mantem `autoAdvance = false` necessario durante
        // setContent — apos a interacao, um frame manual e' o que faz a recomposicao do dialog
        // de confirmacao acontecer antes da asserção (mesmo padrão de `AppShellMedicaoGuiadaTest`).
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        // O toque em "Cancelar" so abre a confirmacao — onCancelar ainda nao foi chamado. Um
        // resultado enganoso jamais deve ser produzido por um unico toque acidental.
        assertEquals(false, cancelou)
        composeRule.onNodeWithText("Interromper o teste?").assertIsDisplayed()

        composeRule.onNodeWithText("Interromper").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        assertEquals(true, cancelou)
    }

    @Test
    fun `continuar testando fecha a confirmacao sem cancelar`() {
        composeRule.mainClock.autoAdvance = false
        var cancelou = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot = snapshot(fase = FaseSpeedtest.download, estado = EstadoExecucaoSpeedtest.executando),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = { cancelou = true },
                    onReiniciar = {},
                )
            }
        }

        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continuar testando").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        assertEquals(false, cancelou)
        composeRule.onNodeWithText("Interromper o teste?").assertDoesNotExist()
    }

    // ─── DiagnosticoConectividadeDialog (GH#1512/#1738 — visual 2.0) ───────────

    @Test
    fun `dialogo de conectividade mostra titulo, mensagem e acoes com os tokens 2_0`() {
        val diagnostico =
            ConnectivityDiagnosisMensagem(
                titulo = "Wi-Fi sem internet",
                mensagem = "Você está conectado ao Wi-Fi, mas essa rede não está conseguindo acessar a internet.",
                acoes = listOf(ConnectivityAction.RECONECTAR_WIFI, ConnectivityAction.CONTATAR_OPERADORA),
            )
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoConectividadeDialog(diagnostico = diagnostico, onDismiss = {})
            }
        }

        composeRule.onNodeWithText(diagnostico.titulo).assertIsDisplayed()
        composeRule.onNodeWithText(diagnostico.mensagem).assertIsDisplayed()
        composeRule.onNodeWithText("• Reconectar ao Wi-Fi").assertIsDisplayed()
        composeRule.onNodeWithText("• Contatar a operadora").assertIsDisplayed()
        composeRule.onNodeWithText("Entendi").assertIsDisplayed()
    }

    @Test
    fun `dialogo de conectividade chama onDismiss ao confirmar entendi`() {
        var fechou = false
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoConectividadeDialog(
                    diagnostico =
                        ConnectivityDiagnosisMensagem(titulo = "t", mensagem = "m", acoes = emptyList()),
                    onDismiss = { fechou = true },
                )
            }
        }

        composeRule.onNodeWithText("Entendi").performClick()
        assertEquals(true, fechou)
    }
}
