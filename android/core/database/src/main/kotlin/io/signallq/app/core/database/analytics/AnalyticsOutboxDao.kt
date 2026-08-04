package io.signallq.app.core.database.analytics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnalyticsOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(event: AnalyticsOutboxEntity): Long

    @Query("SELECT * FROM analytics_outbox WHERE nextAttemptAtEpochMs <= :nowEpochMs ORDER BY createdAtEpochMs LIMIT :limit")
    suspend fun due(nowEpochMs: Long, limit: Int): List<AnalyticsOutboxEntity>

    @Query("DELETE FROM analytics_outbox WHERE id = :id")
    suspend fun acknowledge(id: String)

    @Query("UPDATE analytics_outbox SET attemptCount = :attemptCount, nextAttemptAtEpochMs = :nextAttemptAtEpochMs WHERE id = :id")
    suspend fun defer(id: String, attemptCount: Int, nextAttemptAtEpochMs: Long)

    @Query("DELETE FROM analytics_outbox")
    suspend fun clear()
}
