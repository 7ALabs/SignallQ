package io.signallq.app.feature.diagnostico

import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Cobre a ligacao do GH#969: [DiagnosticOrchestrator.executar] delega pro
 * [RemoteDiagnosticRepository], que resolve remoto-vs-local. Os dois caminhos
 * exigidos pelo criterio de aceite da issue: worker respondendo (remoto vence)
 * e worker fora do ar/timeout (fallback local, sem travar e sem excecao).
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
    fun `worker remoto respondendo - orquestrador usa decisao do relatorio remoto`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
        val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

        orchestrator.executar(snapshotSaudavelInput())

        val snapshot = orchestrator.snapshotFlow.value
        assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
        assertEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
        // Nunca trava a UI: fluxo sincrono ate aqui, sem excecao.
        assertNotNull(snapshot.relatorio)
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
}
