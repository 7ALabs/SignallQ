package io.signallq.app.core.database.analytics

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Fila local mínima para analytics: só payload já sanitizado e metadados de retry.
 *
 * GH#1787 -- `MIGRATION_17_18` (v17->18, já publicada) sempre criou o índice
 * `index_analytics_outbox_nextAttemptAtEpochMs` via SQL bruto, mas a entidade nunca declarou o
 * `@Index` correspondente. `MIGRATION_19_20` recria o mesmo índice de forma idempotente
 * (`CREATE INDEX IF NOT EXISTS`) para instalações novas que nunca passaram pela 17->18. */
@Entity(
    tableName = "analytics_outbox",
    indices = [Index(value = ["nextAttemptAtEpochMs"])],
)
data class AnalyticsOutboxEntity(
    @PrimaryKey val id: String,
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMs: Long = createdAtEpochMs,
)
