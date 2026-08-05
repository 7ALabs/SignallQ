package io.signallq.app.monitoramento

import io.signallq.app.analytics.AnalyticsOutboxFunnelTracker
import io.signallq.app.core.database.analytics.AnalyticsOutboxDao
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.ingest.analyticsPayloadFromOutboxJson
import javax.inject.Inject

/** Entrega idempotente da outbox; só remove uma linha após acknowledgement exato do Worker. */
internal class AnalyticsOutboxProcessor
    @Inject
    constructor(
        private val analyticsOutboxDao: AnalyticsOutboxDao,
        private val adminIngestRepository: AdminIngestRepository,
        private val funnelTracker: AnalyticsOutboxFunnelTracker,
    ) {
        suspend fun process(
            nowEpochMs: Long,
            limit: Int,
        ): Boolean {
            if (!adminIngestRepository.canSendTelemetry()) {
                analyticsOutboxDao.clear()
                return true
            }
            for (entry in analyticsOutboxDao.due(nowEpochMs, limit)) {
                val payload = analyticsPayloadFromOutboxJson(entry.payloadJson)
                if (payload == null || payload.id != entry.id) {
                    funnelTracker.registrar(AnalyticsOutboxFunnelTracker.Stage.LOST)
                    analyticsOutboxDao.acknowledge(entry.id)
                    continue
                }
                if (entry.attemptCount > 0) {
                    funnelTracker.registrar(AnalyticsOutboxFunnelTracker.Stage.REPROCESSED)
                }
                funnelTracker.registrar(AnalyticsOutboxFunnelTracker.Stage.SENT)
                if (adminIngestRepository.sendAnalyticsEvent(payload)) {
                    if (analyticsOutboxDao.acknowledge(entry.id) == 1) {
                        funnelTracker.registrar(AnalyticsOutboxFunnelTracker.Stage.ACCEPTED)
                    }
                    continue
                }
                funnelTracker.registrar(AnalyticsOutboxFunnelTracker.Stage.REJECTED)
                val attempt = entry.attemptCount + 1
                val delayMs = 30_000L * (1L shl (attempt - 1).coerceAtMost(6))
                analyticsOutboxDao.defer(entry.id, attempt, nowEpochMs + delayMs)
                return false
            }
            return true
        }
    }
