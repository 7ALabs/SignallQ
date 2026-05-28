package io.linka.app.kotlin.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.linka.app.kotlin.core.database.chat.ChatMessageEntity
import io.linka.app.kotlin.core.database.chat.ChatSessionDao
import io.linka.app.kotlin.core.database.chat.ChatSessionEntity

@Database(
    entities = [
        MedicaoEntity::class,
        ApelidoDispositivoEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class LinkaDatabase : RoomDatabase() {
    abstract fun medicaoDao(): MedicaoDao
    abstract fun apelidoDispositivoDao(): ApelidoDispositivoDao
    abstract fun chatSessionDao(): ChatSessionDao
}
