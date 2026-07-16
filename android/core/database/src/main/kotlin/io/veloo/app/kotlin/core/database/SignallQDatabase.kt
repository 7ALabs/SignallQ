package io.signallq.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.signallq.app.core.database.chat.ChatMessageEntity
import io.signallq.app.core.database.chat.ChatSessionDao
import io.signallq.app.core.database.chat.ChatSessionEntity
import io.signallq.app.core.database.recommendation.RecommendationHistoryDao
import io.signallq.app.core.database.recommendation.RecommendationHistoryEntity

@Database(
    entities = [
        MedicaoEntity::class,
        ApelidoDispositivoEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        RecommendationHistoryEntity::class,
    ],
    version = 14,
    exportSchema = true,
)
abstract class SignallQDatabase : RoomDatabase() {
    abstract fun medicaoDao(): MedicaoDao
    abstract fun apelidoDispositivoDao(): ApelidoDispositivoDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun recommendationHistoryDao(): RecommendationHistoryDao
}
