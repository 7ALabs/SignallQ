package io.veloo.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.veloo.app.core.database.chat.ChatMessageEntity
import io.veloo.app.core.database.chat.ChatSessionDao
import io.veloo.app.core.database.chat.ChatSessionEntity

@Database(
    entities = [
        MedicaoEntity::class,
        ApelidoDispositivoEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
abstract class SignallQDatabase : RoomDatabase() {
    abstract fun medicaoDao(): MedicaoDao
    abstract fun apelidoDispositivoDao(): ApelidoDispositivoDao
    abstract fun chatSessionDao(): ChatSessionDao
}
