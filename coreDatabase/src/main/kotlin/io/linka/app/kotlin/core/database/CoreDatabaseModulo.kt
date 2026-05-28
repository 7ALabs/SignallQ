package io.linka.app.kotlin.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
object CoreDatabaseModulo {
    private val migracao1para2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN jitterMs REAL")
                db.execSQL("ALTER TABLE medicao ADD COLUMN perdaPercentual REAL")
                db.execSQL("ALTER TABLE medicao ADD COLUMN bufferbloatMs REAL")
            }
        }

    private val migracao2para3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN speedtestMode TEXT")
                db.execSQL("ALTER TABLE medicao ADD COLUMN specVersion TEXT")
                db.execSQL("ALTER TABLE medicao ADD COLUMN packetLossSource TEXT")
            }
        }

    private val migracao3para4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN connectionTypeStart TEXT")
                db.execSQL("ALTER TABLE medicao ADD COLUMN connectionTypeEnd TEXT")
                db.execSQL("ALTER TABLE medicao ADD COLUMN contaminado INTEGER NOT NULL DEFAULT 0")
            }
        }

    private val migracao4para5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN vereditoStreaming TEXT")
                db.execSQL("ALTER TABLE medicao ADD COLUMN vereditoGamer TEXT")
                db.execSQL("ALTER TABLE medicao ADD COLUMN vereditoVideoChamada TEXT")
                db.execSQL("ALTER TABLE medicao ADD COLUMN gargaloPrimario TEXT")
            }
        }

    private val migracao5para6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS apelido_dispositivo " +
                        "(mac TEXT NOT NULL PRIMARY KEY, apelido TEXT NOT NULL)",
                )
            }
        }

    private val migracao6para7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN fonte TEXT")
            }
        }

    /**
     * Torna apelido_dispositivo.apelido nullable.
     * SQLite nao suporta DROP CONSTRAINT — recria a tabela via rename/create/copy/drop.
     */
    private val migracao7para8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS apelido_dispositivo_new " +
                        "(mac TEXT NOT NULL PRIMARY KEY, apelido TEXT)",
                )
                db.execSQL(
                    "INSERT INTO apelido_dispositivo_new (mac, apelido) " +
                        "SELECT mac, apelido FROM apelido_dispositivo",
                )
                db.execSQL("DROP TABLE apelido_dispositivo")
                db.execSQL("ALTER TABLE apelido_dispositivo_new RENAME TO apelido_dispositivo")
            }
        }

    private val migracao8para9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN operadoraMovel TEXT")
            }
        }

    private val migracao9para10 =
        object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_sessions` (" +
                        "`id` TEXT NOT NULL, " +
                        "`titulo` TEXT NOT NULL, " +
                        "`criadoEmEpochMs` INTEGER NOT NULL, " +
                        "`atualizadoEmEpochMs` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`tipoDiagnostico` TEXT, " +
                        "`nomeModelo` TEXT, " +
                        "`diagnosticoPayloadJson` TEXT, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_sessions_atualizadoEmEpochMs` " +
                        "ON `chat_sessions` (`atualizadoEmEpochMs`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                        "`id` TEXT NOT NULL, " +
                        "`sessionId` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`createdAtEpochMs` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`metadataJson` TEXT, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` " +
                        "ON `chat_messages` (`sessionId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId_createdAtEpochMs` " +
                        "ON `chat_messages` (`sessionId`, `createdAtEpochMs`)",
                )
            }
        }

    fun criarBanco(context: Context): LinkaDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            LinkaDatabase::class.java,
            "linkaKotlin.db",
        ).addMigrations(migracao1para2)
            .addMigrations(migracao2para3)
            .addMigrations(migracao3para4)
            .addMigrations(migracao4para5)
            .addMigrations(migracao5para6)
            .addMigrations(migracao6para7)
            .addMigrations(migracao7para8)
            .addMigrations(migracao8para9)
            .addMigrations(migracao9para10)
            .build()
    }

}
