package io.signallq.app.core.database.analytics

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fila local mínima para analytics: só payload já sanitizado e metadados de retry. */
@Entity(tableName = "analytics_outbox")
data class AnalyticsOutboxEntity(
    @PrimaryKey val id: String,
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMs: Long = createdAtEpochMs,
)
