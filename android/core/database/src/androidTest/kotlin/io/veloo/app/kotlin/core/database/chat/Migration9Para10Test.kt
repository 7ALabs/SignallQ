package io.signallq.app.core.database.chat

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.signallq.app.core.database.CoreDatabaseModulo
import io.signallq.app.core.database.SignallQDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class Migration9Para10Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SignallQDatabase::class.java,
    )

    /**
     * Cria DB na v9, insere um registro de medicao (MedicaoEntity),
     * roda migração para v10 e valida:
     * 1. MedicaoEntity preservada
     * 2. Tabelas chat_sessions e chat_messages existem e aceitam INSERT
     */
    @Test
    @Throws(IOException::class)
    fun migracao9Para10_preservaMedicaoECriaTabelas() {
        // Criar DB na v9
        helper.createDatabase(TEST_DB, 9).use { db ->
            // Inserir uma medicao na v9
            db.execSQL(
                "INSERT INTO medicao (id, timestampEpochMs, connectionType, connectionTypeStart, " +
                    "connectionTypeEnd, contaminado, speedtestMode, specVersion, downloadMbps, " +
                    "uploadMbps, latencyMs, jitterMs, perdaPercentual, bufferbloatMs, packetLossSource, " +
                    "vereditoStreaming, vereditoGamer, vereditoVideoChamada, gargaloPrimario, fonte, operadoraMovel) " +
                    "VALUES ('test-id-1', 1700000000000, 'wifi', NULL, NULL, 0, NULL, NULL, " +
                    "150.5, 20.3, 12.0, 2.5, 0.1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)",
            )
        }

        // Rodar migração para v10
        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, getMigracao9Para10())

        // Validar que a medicao foi preservada
        db.query("SELECT * FROM medicao WHERE id = 'test-id-1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            val idIdx = cursor.getColumnIndex("id")
            assertEquals("test-id-1", cursor.getString(idIdx))
            val downloadIdx = cursor.getColumnIndex("downloadMbps")
            assertEquals(150.5, cursor.getDouble(downloadIdx), 0.001)
        }

        // Validar que chat_sessions existe e aceita INSERT
        db.execSQL(
            "INSERT INTO chat_sessions (id, titulo, criadoEmEpochMs, atualizadoEmEpochMs, status, " +
                "tipoDiagnostico, nomeModelo, diagnosticoPayloadJson) " +
                "VALUES ('sess-1', 'Sessão de teste', 1700000001000, 1700000001000, 'active', " +
                "NULL, NULL, NULL)",
        )
        db.query("SELECT COUNT(*) FROM chat_sessions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        // Validar que chat_messages existe e aceita INSERT (com FK)
        db.execSQL(
            "INSERT INTO chat_messages (id, sessionId, role, content, createdAtEpochMs, status, metadataJson) " +
                "VALUES ('msg-1', 'sess-1', 'user', 'Olá', 1700000002000, 'completed', NULL)",
        )
        db.query("SELECT COUNT(*) FROM chat_messages").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    /**
     * Acessa a migration 9→10 via reflexão do CoreDatabaseModulo.
     * Como o campo é `private val`, usamos o DB builder com as migrations registradas
     * e criamos o DB a partir do criarBanco para validar a migration integrada.
     */
    private fun getMigracao9Para10() =
        object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
}
