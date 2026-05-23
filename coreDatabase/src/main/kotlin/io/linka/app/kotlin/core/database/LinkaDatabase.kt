package io.linka.app.kotlin.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MedicaoEntity::class,
        ApelidoDispositivoEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class LinkaDatabase : RoomDatabase() {
    abstract fun medicaoDao(): MedicaoDao
    abstract fun apelidoDispositivoDao(): ApelidoDispositivoDao
}
