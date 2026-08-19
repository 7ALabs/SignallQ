package io.signallq.app.feature.diagnostico.nds

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.core.nds.NdsClient
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
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
 * Cobertura de [NdsDiagnosticRepository] (NDS-02k, issue #1759) — estratégia
 * remoto-primeiro/fallback-total (estilo `RemoteDiagnosticRepository.evaluate`,
 * NÃO `evaluateShadow`): sucesso do NDS vira `DiagnosticReport` REMOTE; qualquer
 * falha cai para o `DiagnosticRunner` local (BUNDLED_LOCAL), sem exceção.
 */
class NdsDiagnosticRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        try {
            server.shutdown()
        } catch (_: Exception) {
            // já encerrado pelo próprio teste — nada a fazer.
        }
    }

    private fun quickTimeoutClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .writeTimeout(300, TimeUnit.MILLISECONDS)
            .build()

    private fun repository(analyticsHelper: AnalyticsHelper = NoOpAnalyticsHelper, client: OkHttpClient = OkHttpClient()) =
        NdsDiagnosticRepository(
            ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "test-token", client = client),
            analyticsHelper = analyticsHelper,
        )

    private fun snapshotSaudavelInput() = DiagnosticInput(
        connectionType = ConnectionType.wifi,
        wifi = WifiDiagnosticInput(rssiDbm = -55, linkSpeedMbps = 400, frequenciaMhz = 5180),
        internet = InternetDiagnosticInput(
            downloadMbps = 200.0, uploadMbps = 50.0, latencyMs = 12.0, jitterMs = 2.0, perdaPercentual = 0.0,
        ),
        executionId = "exec-nds-test",
    )

    private fun successBody(veredicto: String = "excelente", score: Int = 92): String =
        """
        {
          "recommendation": null,
          "results": [
            { "module": "scoring", "module_version": "1.1.0", "request_id": "req-1", "warnings": [], "missing_inputs": [], "result": { "score": $score, "veredicto": "$veredicto", "tipo_conexao": "WIFI", "observed_dimensions": 1, "dimensoes": [] } },
            { "module": "ai", "module_version": "1.5.0", "request_id": "req-1", "warnings": [], "missing_inputs": [], "result": { "tokens_used": 0, "ai_model_used": "copy-catalog", "fallback_used": false, "explanation_source": "copy_catalog", "explanation_status": "catalog_hit", "explanation": { "titulo_amigavel": "Tudo certo", "resumo_tecnico_traduzido": "Conexao excelente." }, "source_finding_ids": [] } }
          ],
          "traces": []
        }
        """.trimIndent()

    @Test
    fun `NDS respondendo com sucesso - relatorio vem REMOTE com o veredicto do NDS`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody(veredicto = "excelente", score = 92)))

        val report = repository().evaluate(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
        assertEquals("nds:excelente", report.decisao.id)
        assertEquals(92, report.scoreEngineResultado?.score)
        assertEquals("exec-nds-test", report.executionId)
    }

    @Test
    fun `NDS respondendo com sucesso - dispara telemetria de outcome success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody()))
        var outcomeRegistrado: String? = null
        var fallbackRegistrado: Boolean? = null
        val analytics = object : AnalyticsHelper by NoOpAnalyticsHelper {
            override fun registrarDiagNdsOutcome(outcome: String, fallbackLocalUsado: Boolean, latenciaMs: Long, errorCode: String?) {
                outcomeRegistrado = outcome
                fallbackRegistrado = fallbackLocalUsado
            }
        }

        repository(analyticsHelper = analytics).evaluate(snapshotSaudavelInput())

        assertEquals("success", outcomeRegistrado)
        assertEquals(false, fallbackRegistrado)
    }

    @Test
    fun `NDS respondendo 401 (KnownError) - cai para motor local sem excecao`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":"Unauthorized","message":"Missing or invalid Bearer token."}"""),
        )

        val report = repository().evaluate(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
        assertNotEquals("nds:excelente", report.decisao.id)
        assertTrue(report.decisao.id.isNotBlank())
    }

    @Test
    fun `NDS respondendo 401 - dispara telemetria known_error com fallback usado`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":"Unauthorized","message":"Missing or invalid Bearer token."}"""),
        )
        var outcomeRegistrado: String? = null
        var errorCodeRegistrado: String? = null
        var fallbackRegistrado: Boolean? = null
        val analytics = object : AnalyticsHelper by NoOpAnalyticsHelper {
            override fun registrarDiagNdsOutcome(outcome: String, fallbackLocalUsado: Boolean, latenciaMs: Long, errorCode: String?) {
                outcomeRegistrado = outcome
                errorCodeRegistrado = errorCode
                fallbackRegistrado = fallbackLocalUsado
            }
        }

        repository(analyticsHelper = analytics).evaluate(snapshotSaudavelInput())

        assertEquals("known_error", outcomeRegistrado)
        assertEquals("Unauthorized", errorCodeRegistrado)
        assertEquals(true, fallbackRegistrado)
    }

    @Test
    fun `NDS fora do ar (timeout) - cai para motor local sem travar`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val report = repository(client = quickTimeoutClient()).evaluate(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
        assertNotNull(report.decisao)
    }

    @Test
    fun `NDS respondendo 500 com corpo nao-JSON (UnknownError) - cai para motor local`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val report = repository().evaluate(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
    }

    @Test
    fun `NDS respondendo 200 com corpo malformado - NdsClient ja devolve UnknownError, repository cai para motor local`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not-valid-json"))

        val report = repository().evaluate(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
    }

    // issue #1762 (achado do Caio na PR #1760) — evaluate() nunca repassava perfilGamer pro
    // mapper; o request que saia sempre tinha profile omitido, mesmo dentro do Modo Gamer.
    @Test
    fun `perfilGamer true - request enviado ao NDS carrega profile gamer`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody()))

        repository().evaluate(snapshotSaudavelInput(), perfilGamer = true)

        val corpoEnviado = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("gamer", corpoEnviado.getString("profile"))
    }

    @Test
    fun `perfilGamer default false - request enviado ao NDS nao carrega profile`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody()))

        repository().evaluate(snapshotSaudavelInput())

        val corpoEnviado = JSONObject(server.takeRequest().body.readUtf8())
        assertFalse("profile nao deveria estar presente no JSON quando perfilGamer=false", corpoEnviado.has("profile"))
    }
}
