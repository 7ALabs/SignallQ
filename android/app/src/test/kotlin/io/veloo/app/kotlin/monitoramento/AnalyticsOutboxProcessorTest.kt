package io.signallq.app.monitoramento

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import io.signallq.app.analytics.AnalyticsOutboxFunnelTracker
import io.signallq.app.core.database.analytics.AnalyticsOutboxDao
import io.signallq.app.core.database.analytics.AnalyticsOutboxEntity
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalyticsOutboxProcessorTest {
    private lateinit var dao: AnalyticsOutboxDao
    private lateinit var repository: AdminIngestRepository
    private lateinit var funnel: AnalyticsOutboxFunnelTracker
    private lateinit var processor: AnalyticsOutboxProcessor

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        funnel = mockk(relaxed = true)
        processor = AnalyticsOutboxProcessor(dao, repository, funnel)
        coEvery { repository.canSendTelemetry() } returns true
    }

    @Test
    fun `acknowledgement valido remove evento e registra enviado e aceito`() = runBlocking {
        val entry = entry("event-1")
        coEvery { dao.due(1_000, 50) } returns listOf(entry)
        coEvery { repository.sendAnalyticsEvent(any()) } returns true
        coEvery { dao.acknowledge("event-1") } returns 1

        assertTrue(processor.process(1_000, 50))
        coVerify { dao.acknowledge("event-1") }
        verify { funnel.registrar(AnalyticsOutboxFunnelTracker.Stage.SENT) }
        verify { funnel.registrar(AnalyticsOutboxFunnelTracker.Stage.ACCEPTED) }
    }

    @Test
    fun `acknowledgement ausente ou incorreto mantem evento e aplica backoff`() = runBlocking {
        val entry = entry("event-2", attemptCount = 1)
        coEvery { dao.due(1_000, 50) } returns listOf(entry)
        coEvery { repository.sendAnalyticsEvent(any()) } returns false

        assertFalse(processor.process(1_000, 50))
        coVerify { dao.defer("event-2", attemptCount = 2, nextAttemptAtEpochMs = 61_000) }
        coVerify(exactly = 0) { dao.acknowledge("event-2") }
        verify { funnel.registrar(AnalyticsOutboxFunnelTracker.Stage.REPROCESSED) }
        verify { funnel.registrar(AnalyticsOutboxFunnelTracker.Stage.REJECTED) }
    }

    @Test
    fun `revogacao de consentimento limpa fila sem enviar`() = runBlocking {
        coEvery { repository.canSendTelemetry() } returns false

        assertTrue(processor.process(1_000, 50))
        coVerify { dao.clear() }
        coVerify(exactly = 0) { repository.sendAnalyticsEvent(any()) }
    }

    @Test
    fun `payload corrompido e perdido sem tentar rede`() = runBlocking {
        coEvery { dao.due(1_000, 50) } returns listOf(AnalyticsOutboxEntity("event-3", "{}", 1))

        assertTrue(processor.process(1_000, 50))
        coVerify { dao.acknowledge("event-3") }
        coVerify(exactly = 0) { repository.sendAnalyticsEvent(any()) }
        verify { funnel.registrar(AnalyticsOutboxFunnelTracker.Stage.LOST) }
    }

    private fun entry(id: String, attemptCount: Int = 0): AnalyticsOutboxEntity =
        AnalyticsOutboxEntity(id, "{\"id\":\"$id\",\"name\":\"screen_view\",\"timestamp\":1}", 1, attemptCount)
}
