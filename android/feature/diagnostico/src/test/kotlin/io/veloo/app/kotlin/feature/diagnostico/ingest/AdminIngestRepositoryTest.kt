package io.signallq.app.feature.diagnostico.ingest

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * GH#1332 — `ai_usage.session_id` tem FOREIGN KEY REFERENCES diagnostic_sessions(id)
 * no signallq-admin-worker. sendAiUsage() so pode chegar no servidor DEPOIS que
 * sendDiagnostic() da mesma sessao tiver sido confirmado — senao o D1 rejeita o
 * insert com "FOREIGN KEY constraint failed" (achado real em producao, 16
 * ocorrencias entre 2026-07-09 e 2026-07-22, `system_errors.id=6f9c784a`).
 *
 * Estes testes cobrem a correlacao por sessionId que substitui a antiga corrida
 * entre dois `scope.launch` independentes.
 */
class AdminIngestRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AdminIngestRepository
    private val requestPathsOrder = mutableListOf<String>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = AdminIngestRepository(
            baseUrl = server.url("/").toString(),
            ingestKey = "test-key",
            client = OkHttpClient(),
            consentimentoProvider = { true },
        )
        requestPathsOrder.clear()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun diagnosticPayload(id: String) = DiagnosticIngestPayload(id = id)

    private fun aiUsagePayload(id: String, sessionId: String?) = AiUsageIngestPayload(
        id = id,
        model = "test-model",
        sessionId = sessionId,
    )

    @Test
    fun `sendAiUsage disparado antes de sendDiagnostic espera e envia so depois`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{\"ok\":true,\"id\":\"sessao-1\"}"))
        server.enqueue(MockResponse().setResponseCode(201).setBody("{\"ok\":true,\"id\":\"ai-1\"}"))

        // sendAiUsage() e chamado primeiro (mesma ordem do bug real: dispararIngestAiUsage
        // roda dentro de callAi(), que termina ANTES de dispararIngestDiagnostico ser
        // chamado no SignallQOrchestrator) — sem a correlacao, isso perderia a corrida.
        val aiUsageJob = async {
            repository.sendAiUsage(aiUsagePayload(id = "ai-1", sessionId = "sessao-1"))
        }

        // Simula o atraso real de rede do diagnostico chegando depois.
        delay(50)
        repository.sendDiagnostic(diagnosticPayload(id = "sessao-1"))

        aiUsageJob.await()

        assertEquals(2, server.requestCount)
        val diagnosticRequest = server.takeRequest()
        val aiUsageRequest = server.takeRequest()
        assertEquals("/ingest/diagnostic", diagnosticRequest.path)
        assertEquals("/ingest/ai-usage", aiUsageRequest.path)
    }

    @Test
    fun `sendAiUsage nao envia quando sendDiagnostic da mesma sessao falha`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val aiUsageJob = async {
            repository.sendAiUsage(aiUsagePayload(id = "ai-2", sessionId = "sessao-2"))
        }
        delay(50)
        repository.sendDiagnostic(diagnosticPayload(id = "sessao-2"))
        aiUsageJob.await()

        // So a chamada de sendDiagnostic (que falhou) chegou ao servidor —
        // sendAiUsage desistiu sem tentar, evitando a FK que falharia mesmo assim.
        assertEquals(1, server.requestCount)
        assertEquals("/ingest/diagnostic", server.takeRequest().path)
    }

    @Test
    fun `sendAiUsage sem sessionId nao espera nenhuma correlacao`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{\"ok\":true,\"id\":\"ai-3\"}"))

        repository.sendAiUsage(aiUsagePayload(id = "ai-3", sessionId = null))

        assertEquals(1, server.requestCount)
        assertEquals("/ingest/ai-usage", server.takeRequest().path)
    }

    @Test
    fun `sendDiagnostic não confirma resposta que pertence a outro id`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{\"ok\":true,\"id\":\"outro-id\"}"))

        val confirmado = repository.sendDiagnostic(diagnosticPayload(id = "sessao-4"))

        assertFalse(confirmado)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `sendAnalyticsEvent rejeita acknowledgement ausente ou de outro evento`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{\"acceptedIds\":[\"outro-id\"]}"))

        val confirmado = repository.sendAnalyticsEvent(AnalyticsEventIngestPayload(id = "event-4", name = "screen_view"))

        assertFalse(confirmado)
        assertEquals("/ingest/analytics", server.takeRequest().path)
    }

    @Test
    fun `sendAnalyticsEvent nao faz rede sem consentimento`() = runBlocking {
        val semConsentimento = AdminIngestRepository(server.url("/").toString(), "test-key", OkHttpClient()) { false }

        assertFalse(semConsentimento.sendAnalyticsEvent(AnalyticsEventIngestPayload(id = "event-5", name = "screen_view")))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `outbox round trip preserva evento quando todos opcionais sao nulos`() {
        val original = AnalyticsEventIngestPayload(id = "event-null", name = "screen_view")

        val restored = analyticsPayloadFromOutboxJson(original.toOutboxJson())

        requireNotNull(restored)
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertNull(restored.sessionId)
        assertNull(restored.appVersion)
        assertNull(restored.featureId)
        assertNull(restored.screenName)
        assertNull(restored.errorType)
        assertNull(restored.batteryLevel)
        assertNull(restored.batteryCharging)
        assertNull(restored.environment)
        assertNull(restored.distChannel)
        assertNull(restored.buildType)
        assertNull(restored.versionCode)
        assertNull(restored.deviceId)
        assertNull(restored.durationMs)
    }
}
