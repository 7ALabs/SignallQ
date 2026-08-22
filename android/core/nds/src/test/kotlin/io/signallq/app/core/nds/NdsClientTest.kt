package io.signallq.app.core.nds

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Cobertura da fatia NDS-01 (#1744, ADR-017): mock do HTTP client (nunca bate
 * no NDS real), cobrindo sucesso com os 3 modulos do exemplo do ADR, erro
 * 401/429 (shape confirmado) e modulo ausente/inesperado em `results[]`.
 */
class NdsClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: NdsClient

    /** Corpo de sucesso exato do exemplo do ADR-017 (3 modulos: diagnostics.wifi, scoring, ai). */
    private val successBody =
        """
        {
          "recommendation": null,
          "results": [
            { "module": "diagnostics.wifi", "module_version": "1.0.0", "request_id": "req-12345", "warnings": [], "missing_inputs": [], "result": { "matched_rules": [] }, "cards": [] },
            { "module": "scoring", "module_version": "1.1.0", "request_id": "req-12345", "warnings": [], "missing_inputs": [], "result": { "score": 50, "veredicto": "regular", "tipo_conexao": "WIFI", "observed_dimensions": 1, "dimensoes": [] } },
            { "module": "ai", "module_version": "1.5.0", "request_id": "req-12345", "warnings": [], "missing_inputs": [], "result": { "tokens_used": 0, "ai_model_used": "copy-catalog", "fallback_used": false, "explanation_source": "copy_catalog", "explanation_status": "catalog_hit", "explanation": { "titulo_amigavel": "Sua rede esta OK", "resumo_tecnico_traduzido": "Score 50, regular." }, "source_finding_ids": [] } }
          ],
          "traces": [
            { "module": "wifi", "duration_ms": 2, "status": "ok" },
            { "module": "ai", "duration_ms": 15, "status": "ok", "source": "copy_catalog" }
          ]
        }
        """.trimIndent()

    private val sampleRequest =
        NdsDiagnosticsRequest(
            requestId = "req-12345",
            app = NdsAppInfo(id = "io.signallq.app", version = "2.4.0"),
            profile = "gamer",
            capabilities = listOf("scoring", "ai", "wifi", "fiber"),
            connection = NdsConnectionInfo(type = "WIFI", ssid = "MinhaRede_5G", bssid = "00:14:22:01:23:45"),
            wifi = NdsWifiInfo(rssi = -65, band = "5GHz", channel = 36, linkSpeed = 433, standard = "802.11ac"),
        )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            NdsClient(
                baseUrl = server.url("/").toString(),
                apiToken = "test-token",
                client = OkHttpClient(),
            )
    }

    @After
    fun tearDown() {
        // Um teste (servidor fora do ar) ja chama shutdown() propositalmente —
        // shutdown duplo do MockWebServer lanca excecao, entao ignoramos aqui.
        try {
            server.shutdown()
        } catch (_: Exception) {
            // já encerrado pelo próprio teste — nada a fazer.
        }
    }

    // -------------------------------------------------------------------
    // Request — path, header, body.
    // -------------------------------------------------------------------

    @Test
    fun `evaluate envia POST no path correto com Bearer token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody))

        client.evaluate(sampleRequest)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/diagnostics/evaluate", recorded.path)
        assertEquals("Bearer test-token", recorded.getHeader("Authorization"))

        val sentBody = JSONObject(recorded.body.readUtf8())
        assertEquals("req-12345", sentBody.getString("request_id"))
        assertEquals("io.signallq.app", sentBody.getJSONObject("app").getString("id"))
        assertEquals("gamer", sentBody.getString("profile"))
        assertEquals("WIFI", sentBody.getJSONObject("connection").getString("type"))
        assertEquals(-65, sentBody.getJSONObject("wifi").getInt("rssi"))
    }

    // -------------------------------------------------------------------
    // Sucesso — os 3 modulos do exemplo do ADR-017.
    // -------------------------------------------------------------------

    @Test
    fun `evaluate parseia sucesso com os 3 modulos confirmados do ADR-017`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody))

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.Success)
        val response = (outcome as NdsDiagnosticsOutcome.Success).response
        assertNull(response.recommendation)
        assertEquals(3, response.results.size)
        assertEquals(2, response.traces.size)

        val wifiDiagnostics = response.resultFor("diagnostics.wifi")?.asWifiDiagnostics()
        assertEquals(emptyList<String>(), wifiDiagnostics?.matchedRules)

        val scoring = response.resultFor("scoring")?.asScoring()
        assertEquals(50, scoring?.score)
        assertEquals("regular", scoring?.veredicto)
        assertEquals("WIFI", scoring?.tipoConexao)
        assertEquals(1, scoring?.observedDimensions)

        val ai = response.resultFor("ai")?.asAi()
        assertEquals("copy-catalog", ai?.aiModelUsed)
        assertFalse(ai?.fallbackUsed ?: true)
        assertEquals("catalog_hit", ai?.explanationStatus)
        assertEquals("Sua rede esta OK", ai?.explanation?.tituloAmigavel)

        val aiTrace = response.traces.first { it.module == "ai" }
        assertEquals(15L, aiTrace.durationMs)
        assertEquals("copy_catalog", aiTrace.source)
    }

    @Test
    fun `evaluate parseia recommendation como objeto tipado com steps`() = runBlocking {
        val body = successBody.replace(
            "\"recommendation\": null",
            "\"recommendation\": {\"id\":\"retest_near_router\",\"type\":\"diagnostic\",\"title\":\"Teste perto do roteador\",\"description\":\"Faça uma nova medição.\",\"source_finding_ids\":[\"wifi_signal_critical\"],\"steps\":[\"Aproxime-se do roteador.\",\"Repita a medição.\"]}",
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val response = (client.evaluate(sampleRequest) as NdsDiagnosticsOutcome.Success).response

        assertEquals("retest_near_router", response.recommendation?.id)
        assertEquals("Faça uma nova medição.", response.recommendation?.description)
        assertEquals(listOf("Aproxime-se do roteador.", "Repita a medição."), response.recommendation?.steps)
    }

    @Test
    fun `resultFor busca por module e nao por posicao`() = runBlocking {
        // capabilities pedidas foram scoring+ai, mas o servidor devolveu wifi tambem
        // e em ordem diferente da capabilities[] — o cliente nao pode assumir posicao.
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody))

        val outcome = client.evaluate(sampleRequest) as NdsDiagnosticsOutcome.Success

        assertEquals("scoring", outcome.response.resultFor("scoring")?.module)
        assertEquals("ai", outcome.response.resultFor("ai")?.module)
        assertEquals("diagnostics.wifi", outcome.response.resultFor("diagnostics.wifi")?.module)
    }

    // -------------------------------------------------------------------
    // Modulo ausente / inesperado.
    // -------------------------------------------------------------------

    @Test
    fun `modulo pedido mas ausente na resposta devolve null via resultFor`() = runBlocking {
        val bodySemAi =
            """
            {
              "recommendation": null,
              "results": [
                { "module": "scoring", "module_version": "1.1.0", "request_id": "req-1", "warnings": [], "missing_inputs": [], "result": { "score": 80, "veredicto": "boa", "tipo_conexao": "WIFI", "observed_dimensions": 1, "dimensoes": [] } }
              ],
              "traces": []
            }
            """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodySemAi))

        val outcome = client.evaluate(sampleRequest) as NdsDiagnosticsOutcome.Success

        assertNull(outcome.response.resultFor("ai"))
        assertNull(outcome.response.resultFor("ai")?.asAi())
    }

    @Test
    fun `modulo desconhecido e futuro fica acessivel via result bruto sem decoder tipado`() = runBlocking {
        val bodyComModuloNovo =
            """
            {
              "recommendation": null,
              "results": [
                { "module": "diagnostics.fiber", "module_version": "0.1.0", "request_id": "req-1", "warnings": ["low_confidence"], "missing_inputs": ["fiber.temperature_c"], "result": { "estado": "atencao", "detalhe_futuro": { "nivel": 2 } }, "cards": [ { "titulo": "Card novo" } ] }
              ],
              "traces": []
            }
            """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodyComModuloNovo))

        val outcome = client.evaluate(sampleRequest) as NdsDiagnosticsOutcome.Success
        val moduloNovo = outcome.response.resultFor("diagnostics.fiber")

        assertEquals("diagnostics.fiber", moduloNovo?.module)
        assertEquals(listOf("low_confidence"), moduloNovo?.warnings)
        assertEquals(listOf("fiber.temperature_c"), moduloNovo?.missingInputs)
        assertEquals("atencao", moduloNovo?.result?.get("estado"))
        @Suppress("UNCHECKED_CAST")
        val detalhe = moduloNovo?.result?.get("detalhe_futuro") as? Map<String, Any?>
        assertEquals(2, (detalhe?.get("nivel") as? Number)?.toInt())
        assertEquals(1, moduloNovo?.cards?.size)
        // Sem decoder tipado para este modulo ainda — os 3 decoders confirmados
        // devolvem null porque o nome do modulo nao bate.
        assertNull(moduloNovo?.asScoring())
        assertNull(moduloNovo?.asAi())
        assertNull(moduloNovo?.asWifiDiagnostics())
    }

    // -------------------------------------------------------------------
    // Erros com shape confirmado: 401 e 429.
    // -------------------------------------------------------------------

    @Test
    fun `evaluate mapeia 401 para KnownError com shape confirmado`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":"Unauthorized","message":"Missing or invalid Bearer token."}"""),
        )

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.KnownError)
        val error = outcome as NdsDiagnosticsOutcome.KnownError
        assertEquals(401, error.statusCode)
        assertEquals("Unauthorized", error.error)
        assertEquals("Missing or invalid Bearer token.", error.message)
    }

    @Test
    fun `evaluate mapeia 429 para KnownError com shape confirmado`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(429)
                .setBody("""{"error":"Rate limit exceeded","message":"Too many requests. Try again shortly."}"""),
        )

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.KnownError)
        val error = outcome as NdsDiagnosticsOutcome.KnownError
        assertEquals(429, error.statusCode)
        assertEquals("Rate limit exceeded", error.error)
        assertEquals("Too many requests. Try again shortly.", error.message)
        assertNull(error.code)
        assertNull(error.retryable)
        assertNull(error.requestId)
    }

    // -------------------------------------------------------------------
    // Envelope canonico do PR#12 do NDS (ADR-017, ainda em draft) —
    // NDS-02k, issue #1759, item 9: tentado primeiro, cai para o shape
    // antigo se nao bater.
    // -------------------------------------------------------------------

    @Test
    fun `evaluate mapeia envelope canonico 504 NDS_TIMEOUT para KnownError com code e retryable`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(504)
                .setBody(
                    """{"error":{"code":"NDS_TIMEOUT","message":"Evaluation timed out.","retryable":true},"request_id":"req-99"}""",
                ),
        )

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.KnownError)
        val error = outcome as NdsDiagnosticsOutcome.KnownError
        assertEquals(504, error.statusCode)
        assertEquals("NDS_TIMEOUT", error.error)
        assertEquals("NDS_TIMEOUT", error.code)
        assertEquals("Evaluation timed out.", error.message)
        assertEquals(true, error.retryable)
        assertEquals("req-99", error.requestId)
    }

    @Test
    fun `evaluate mapeia envelope canonico com retryable false`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody(
                    """{"error":{"code":"INVALID_INPUT","message":"profile invalido.","retryable":false},"request_id":"req-1"}""",
                ),
        )

        val outcome = client.evaluate(sampleRequest) as NdsDiagnosticsOutcome.KnownError

        assertEquals("INVALID_INPUT", outcome.code)
        assertEquals(false, outcome.retryable)
    }

    @Test
    fun `evaluate mapeia envelope canonico sem retryable presente para retryable nulo`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"error":{"code":"INTERNAL_ERROR","message":"boom"},"request_id":"req-2"}"""),
        )

        val outcome = client.evaluate(sampleRequest) as NdsDiagnosticsOutcome.KnownError

        assertEquals("INTERNAL_ERROR", outcome.code)
        assertNull(outcome.retryable)
    }

    @Test
    fun `evaluate ainda parseia shape antigo flat quando error e string, nao objeto`() = runBlocking {
        // Mesmo corpo do teste de 401 acima, repetido aqui de proposito para deixar
        // explicito que a tentativa do envelope canonico (que exige "error" como
        // objeto JSON) falha e cai para o parser antigo sem lancar excecao.
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":"Unauthorized","message":"Missing or invalid Bearer token."}"""),
        )

        val outcome = client.evaluate(sampleRequest) as NdsDiagnosticsOutcome.KnownError

        assertEquals("Unauthorized", outcome.error)
        assertNull(outcome.code)
    }

    @Test
    fun `evaluate mapeia erro com error objeto malformado (sem code) para UnknownError`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"error":{"message":"sem code"},"request_id":"req-3"}"""),
        )

        val outcome = client.evaluate(sampleRequest)

        // "error" e objeto (nao bate no shape antigo, que espera String) e falta
        // "code" (nao bate no envelope canonico) -- nenhum dos dois formatos bate,
        // cai para UnknownError sem lancar excecao.
        assertTrue(outcome is NdsDiagnosticsOutcome.UnknownError)
    }

    // -------------------------------------------------------------------
    // Erros sem shape confirmado — tratamento defensivo (5xx, corpo vazio,
    // corpo nao-JSON). Nunca lanca excecao para o chamador.
    // -------------------------------------------------------------------

    @Test
    fun `evaluate mapeia 500 com corpo nao-JSON para UnknownError sem lancar excecao`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(500).setBody("Internal Server Error (texto puro, nao-JSON)"),
        )

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.UnknownError)
        val error = outcome as NdsDiagnosticsOutcome.UnknownError
        assertEquals(500, error.statusCode)
        assertEquals("Internal Server Error (texto puro, nao-JSON)", error.rawBody)
    }

    @Test
    fun `evaluate mapeia corpo vazio em 503 para UnknownError`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setBody(""))

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.UnknownError)
        assertEquals(503, (outcome as NdsDiagnosticsOutcome.UnknownError).statusCode)
    }

    @Test
    fun `evaluate mapeia 200 com corpo malformado para UnknownError`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not-valid-json"))

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.UnknownError)
        val error = outcome as NdsDiagnosticsOutcome.UnknownError
        assertEquals(200, error.statusCode)
        assertTrue(error.cause != null)
    }

    @Test
    fun `evaluate sem servidor no ar devolve UnknownError sem statusCode`() = runBlocking {
        server.shutdown()

        val outcome = client.evaluate(sampleRequest)

        assertTrue(outcome is NdsDiagnosticsOutcome.UnknownError)
        val error = outcome as NdsDiagnosticsOutcome.UnknownError
        assertNull(error.statusCode)
        assertNull(error.rawBody)
        assertTrue(error.cause != null)
    }
}
