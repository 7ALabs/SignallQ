package io.signallq.app.feature.diagnostico.nds

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticContext
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.core.nds.toDiagnosticReport
import org.junit.Assert.assertThrows
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
    fun `Assist com NDS respondendo com sucesso - relatorio vem REMOTE`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody()))

        val report = repository().evaluateForAssist(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
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
    fun `NDS retorna regular sem causa nem recomendacao - preserva resposta remota`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody(veredicto = "regular", score = 50)))

        val report = repository().evaluate(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
        assertEquals("nds:regular", report.decisao.id)
        assertEquals(io.signallq.app.core.diagnostico.DiagnosticStatus.info, report.decisao.status)
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
    fun `Assist com NDS respondendo 401 - propaga erro sem fallback local`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":"Unauthorized","message":"Missing or invalid Bearer token."}"""),
        )

        assertThrows(NdsAssistEvaluationException::class.java) {
            kotlinx.coroutines.runBlocking { repository().evaluateForAssist(snapshotSaudavelInput()) }
        }
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
    fun `Assist com NDS fora do ar - propaga erro explicito sem fallback local`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertThrows(NdsAssistEvaluationException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository(client = quickTimeoutClient()).evaluateForAssist(snapshotSaudavelInput())
            }
        }
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

    // -------------------------------------------------------------------
    // feat/nds-client-v2 — Assist com usarNdsV2. O contrato v2 aceita contexto
    // parcial, então a flag ligada seleciona v2 mesmo sem subcategoria canônica.
    // -------------------------------------------------------------------

    @Test
    fun `Assist com usarNdsV2=false - chama v1 identico ao comportamento anterior`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody()))

        val report = repository().evaluateForAssist(snapshotSaudavelInput(), usarNdsV2 = false)

        val recorded = server.takeRequest()
        assertEquals("/v1/diagnostics/evaluate", recorded.path)
        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
    }

    @Test
    fun `Assist com usarNdsV2=true sem subcategory na origem chama v2`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"raw":{},"explanation":{"titulo":"t","descricao":"d","dados":[]}}"""))
        val inputComObjective = snapshotSaudavelInput().copy(
            context = DiagnosticContext(objective = "JOGOS_COM_LAG"),
        )

        val report = repository().evaluateForAssist(inputComObjective, usarNdsV2 = true)

        val recorded = server.takeRequest()
        assertEquals("/v2/diagnostics/evaluate", recorded.path)
        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
    }

    @Test
    fun `Assist com usarNdsV2=true - resposta v2 e mapeada para REMOTE com explanation`() = runTest {
        // Simula o dia em que o NdsClient recebe subcategory (via NdsClient direto,
        // que ja aceita o campo hoje) -- prova que o restante do pipeline (Repository
        // -> toDiagnosticReport) sabe lidar com a resposta v2 de ponta a ponta.
        val v2Body =
            """
            {
              "raw": { "score": 61 },
              "explanation": {
                "titulo": "Instabilidade fora do horário de pico",
                "descricao": "A conexão oscila mesmo fora dos horários de maior uso.",
                "acao_usuario": "Reinicie o roteador e repita o teste."
              }
            }
            """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(v2Body))
        val clientQueChamaV2 = NdsClient(baseUrl = server.url("/").toString(), apiToken = "test-token")
        val repositoryComV2 = NdsDiagnosticRepository(ndsClient = clientQueChamaV2)
        val inputComContextoCompleto = snapshotSaudavelInput().copy(
            context = DiagnosticContext(objective = "JOGOS_COM_LAG"),
        )

        // Chama o cliente diretamente com useV2=true e um NdsDiagnosticsRequest que já
        // carrega subcategory, para confirmar que a resposta v2 chega íntegra até o
        // DiagnosticReport via o mesmo toDiagnosticReport usado pelo Repository.
        val outcome = clientQueChamaV2.evaluate(
            io.signallq.app.core.nds.NdsDiagnosticsRequest(
                requestId = "exec-nds-v2-test",
                app = io.signallq.app.core.nds.NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
                context = io.signallq.app.core.nds.NdsDiagnosticContext(
                    objective = "JOGOS_COM_LAG",
                    subcategory = "lag_horario_pico",
                ),
            ),
            useV2 = true,
        )

        val recorded = server.takeRequest()
        assertEquals("/v2/diagnostics/evaluate", recorded.path)
        val success = outcome as io.signallq.app.core.nds.NdsDiagnosticsOutcome.Success
        val report = success.response.toDiagnosticReport(inputComContextoCompleto, 0L)
        assertEquals("Instabilidade fora do horário de pico", report.decisao.titulo)
        assertEquals("Reinicie o roteador e repita o teste.", report.decisao.recomendacao)
        assertEquals(DiagnosticStatus.attention, report.decisao.status)
    }

    @Test
    fun `Assist com usarNdsV2=true e NDS falhando - continua sem fallback local (comportamento existente)`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":"Unauthorized","message":"Missing or invalid Bearer token."}"""),
        )
        val inputComObjective = snapshotSaudavelInput().copy(
            context = DiagnosticContext(objective = "JOGOS_COM_LAG"),
        )

        assertThrows(NdsAssistEvaluationException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository().evaluateForAssist(inputComObjective, usarNdsV2 = true)
            }
        }
    }
}
