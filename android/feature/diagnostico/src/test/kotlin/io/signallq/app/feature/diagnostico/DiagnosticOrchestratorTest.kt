package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Cobre a ligacao do GH#1444 (shadow mode, parte de #952): desde essa issue,
 * [DiagnosticOrchestrator.executar] delega pro
 * [RemoteDiagnosticRepository.evaluateShadow], NAO mais [RemoteDiagnosticRepository.evaluate]
 * (GH#969, remoto-primeiro) — motor LOCAL sempre autoritativo, worker remoto
 * saudavel ou fora do ar NUNCA muda o que a UI mostra. Ver kdoc de
 * [RemoteDiagnosticRepository] para a distincao completa entre os dois metodos.
 */
class DiagnosticOrchestratorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun quickTimeoutClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .writeTimeout(300, TimeUnit.MILLISECONDS)
            .build()

    private fun snapshotSaudavelInput() = DiagnosticInput(
        connectionType = ConnectionType.wifi,
        wifi = WifiDiagnosticInput(rssiDbm = -55, linkSpeedMbps = 400, frequenciaMhz = 5180),
        internet = InternetDiagnosticInput(
            downloadMbps = 200.0, uploadMbps = 50.0, latencyMs = 12.0, jitterMs = 2.0, perdaPercentual = 0.0,
        ),
    )

    private fun remoteReportJson(): String = """
        {
          "evaluationSource": "REMOTE",
          "wifiResultados": [],
          "internetResultados": [],
          "mobileResultados": [],
          "fibraResultados": [],
          "dnsResultados": [],
          "historicoResultados": [],
          "wifiCanalResultados": [],
          "redeResultados": [],
          "decisao": {
            "id": "DECISAO-REMOTA-TESTE",
            "titulo": "Conexao saudavel no momento",
            "status": "ok",
            "evidencia": null,
            "mensagemUsuario": "Tudo certo por aqui.",
            "recomendacao": null,
            "categoria": "decisao",
            "podeConcluir": true,
            "categoriaOrigem": null
          },
          "achadosSecundarios": [],
          "hipotesesDescartadas": [],
          "dadosAusentes": [],
          "limitacoesEquipamentoLocal": [],
          "recomendacoes": [],
          "scoreEngineResultado": { "score": 95, "veredictoHumano": "excelente", "dimensoes": [{"id":"wifi","score":95}] },
          "perfisUso": [],
          "gameReadiness": [],
          "geradoEmMs": 1700000000000
        }
    """.trimIndent()

    @Test
    fun `worker remoto saudavel respondendo - orquestrador ainda assim usa decisao do motor LOCAL`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
        val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

        orchestrator.executar(snapshotSaudavelInput())

        val snapshot = orchestrator.snapshotFlow.value
        assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
        assertNotNull(snapshot.relatorio)
        // GH#1444: shadow mode -- mesmo com o worker saudavel e respondendo rapido,
        // o resultado exibido e sempre o do motor local, nunca o id fabricado do
        // fixture remoto usado neste teste.
        assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
        assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, snapshot.relatorio?.evaluationSource)
    }

    @Test
    fun `worker indisponivel (timeout) - orquestrador cai pro motor local sem travar`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), client = quickTimeoutClient())
        val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

        orchestrator.executar(snapshotSaudavelInput())

        val snapshot = orchestrator.snapshotFlow.value
        assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
        assertNotNull(snapshot.relatorio)
        // Prova que caiu pro motor LOCAL de verdade, nunca o id fabricado do teste remoto.
        assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
        assertTrue(snapshot.relatorio!!.decisao.id.isNotBlank())
    }

    @Test
    fun `worker respondendo 500 - orquestrador cai pro motor local sem excecao`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
        val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

        orchestrator.executar(snapshotSaudavelInput())

        val snapshot = orchestrator.snapshotFlow.value
        assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
        assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
    }

    @Test
    fun `geracao avanca mesmo quando o veredito final se repete`() = runTest {
        val orchestrator = DiagnosticOrchestrator()

        orchestrator.executar(snapshotSaudavelInput())
        val primeiro = orchestrator.snapshotFlow.value
        orchestrator.executar(snapshotSaudavelInput())
        val segundo = orchestrator.snapshotFlow.value

        assertEquals(primeiro.relatorio?.veredito, segundo.relatorio?.veredito)
        assertEquals(EstadoDiagnostico.concluido, segundo.estado)
        assertEquals(primeiro.geracao + 1L, segundo.geracao)
    }

    @Test
    fun `falha precoce e cancelamento finalizam a geracao canonica`() = runTest {
        val orchestrator = DiagnosticOrchestrator()

        orchestrator.executarPreparando { error("falha antes do input") }
        assertEquals(EstadoDiagnostico.erro, orchestrator.snapshotFlow.value.estado)
        assertEquals(1L, orchestrator.snapshotFlow.value.geracao)

        var cancelamentoPropagado = false
        try {
            orchestrator.executarPreparando { throw CancellationException("cancelado") }
        } catch (_: CancellationException) {
            cancelamentoPropagado = true
        }
        assertTrue(cancelamentoPropagado)
        assertEquals(EstadoDiagnostico.cancelado, orchestrator.snapshotFlow.value.estado)
        assertEquals(2L, orchestrator.snapshotFlow.value.geracao)
    }

    @Test
    fun `solicitacao concorrente e rejeitada sem nova geracao`() = runTest {
        val liberar = CompletableDeferred<Unit>()
        var diagnosticosIniciados = 0
        val analytics =
            object : AnalyticsHelper by NoOpAnalyticsHelper {
                override fun registrarDiagIniciado(
                    tipoConexao: String,
                    areasHabilitadas: String?,
                    temSpeedtest: Boolean,
                ) {
                    diagnosticosIniciados++
                }
            }
        val orchestrator = DiagnosticOrchestrator(analyticsHelper = analytics)
        val primeira =
            async {
                orchestrator.executarPreparando {
                    liberar.await()
                    snapshotSaudavelInput()
                }
            }

        yield()
        assertEquals(EstadoDiagnostico.executando, orchestrator.snapshotFlow.value.estado)
        assertFalse(orchestrator.executarPreparando { snapshotSaudavelInput() })
        assertEquals(1L, orchestrator.snapshotFlow.value.geracao)
        liberar.complete(Unit)
        primeira.await()
        assertEquals(1, diagnosticosIniciados)
    }
}
