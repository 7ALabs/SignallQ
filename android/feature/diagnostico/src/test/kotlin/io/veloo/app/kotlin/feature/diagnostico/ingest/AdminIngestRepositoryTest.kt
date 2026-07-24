package io.signallq.app.feature.diagnostico.ingest

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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
        server.enqueue(MockResponse().setResponseCode(201))
        server.enqueue(MockResponse().setResponseCode(201))

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
        server.enqueue(MockResponse().setResponseCode(201))

        repository.sendAiUsage(aiUsagePayload(id = "ai-3", sessionId = null))

        assertEquals(1, server.requestCount)
        assertEquals("/ingest/ai-usage", server.takeRequest().path)
    }
}
