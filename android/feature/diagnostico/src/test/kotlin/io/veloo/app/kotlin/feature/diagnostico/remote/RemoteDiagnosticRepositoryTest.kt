package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.feature.diagnostico.ConnectionType
import io.signallq.app.feature.diagnostico.DiagnosticInput
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.diagnostico.InternetDiagnosticInput
import io.signallq.app.feature.diagnostico.WifiDiagnosticInput
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Cobre a estrategia local vs. remoto do GH#962: resposta normal (remoto vence,
 * mapeado corretamente) e as 2 formas de "worker indisponivel" (erro HTTP e
 * timeout) caindo pro motor local sem travar.
 */
class RemoteDiagnosticRepositoryTest {

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
            "id": "DECISAO-SAUDAVEL_MONITORAR",
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
    fun `worker responde 200 com JSON valido - evaluate usa o relatorio remoto`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

        val report = repo.evaluate(snapshotSaudavelInput())

        assertEquals("DECISAO-SAUDAVEL_MONITORAR", report.decisao.id)
        assertEquals(DiagnosticStatus.ok, report.decisao.status)
        assertEquals(95, report.scoreEngineResultado?.score)
        // perfisUso/gameReadiness sempre calculados localmente, mesmo com fonte remota.
        assertTrue(report.perfisUso.isNotEmpty())
        assertTrue(report.gameReadiness.isNotEmpty())

        val recorded = server.takeRequest()
        assertEquals("/api/diagnostic/evaluate", recorded.path)
        assertEquals("POST", recorded.method)
    }

    @Test
    fun `worker responde 500 - cai pro motor local sem travar`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

        val report = repo.evaluate(snapshotSaudavelInput())

        // Prova que caiu pro motor LOCAL de verdade (nao um relatorio remoto/vazio
        // fabricado): decisao valida e com id gerado pelo FindingEngine local,
        // nunca o placeholder "DECISAO-INCONCLUSIVO" de fallback de erro do worker.
        assertNotNull(report.decisao)
        assertTrue(report.decisao.id.isNotBlank())
        assertTrue(report.decisao.id != "DECISAO-INCONCLUSIVO")
        assertNotNull(report.scoreEngineResultado)
    }

    @Test
    fun `worker retorna corpo vazio - cai pro motor local`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

        val report = repo.evaluate(snapshotSaudavelInput())
        assertNotNull(report.decisao)
    }

    @Test
    fun `worker retorna JSON invalido - cai pro motor local`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{ nao é json valido"))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

        val report = repo.evaluate(snapshotSaudavelInput())
        assertNotNull(report.decisao)
    }

    @Test
    fun `worker nao responde (conexao cai) - nunca trava, cai pro motor local`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), client = quickTimeoutClient())

        val report = repo.evaluate(snapshotSaudavelInput())
        assertNotNull(report.decisao)
    }

    @Test
    fun `evaluateRemote isolado retorna null em qualquer falha`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
        assertNull(repo.evaluateRemote(snapshotSaudavelInput()))
    }
}
