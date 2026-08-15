package io.signallq.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val TEST_DB = "migration-13-14-test"

/**
 * GH#1027 — migração que adiciona `bandaWifi` à tabela `medicao`.
 *
 * Valida: (1) medições já salvas na v13 sobrevivem à migração com `bandaWifi` NULL
 * (dado retroativo indisponível, decisão de produto), (2) a coluna nova aceita
 * INSERT/UPDATE de valores "ghz24"/"ghz5" após a migração.
 */
@RunWith(AndroidJUnit4::class)
class Migration13Para14Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SignallQDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migracao13Para14_preservaMedicaoExistenteComBandaWifiNull() {
        helper.createDatabase(TEST_DB, 13).use { db ->
            db.execSQL(
                "INSERT INTO medicao (id, timestampEpochMs, connectionType, connectionTypeStart, " +
                    "connectionTypeEnd, contaminado, speedtestMode, specVersion, downloadMbps, " +
                    "uploadMbps, latencyMs, jitterMs, perdaPercentual, bufferbloatMs, packetLossSource, " +
                    "vereditoStreaming, vereditoGamer, vereditoVideoChamada, gargaloPrimario, fonte, " +
                    "operadoraMovel, diagnosticoTexto, diagnosticoOrigem, diagnosticoProblemas, score, status) " +
                    "VALUES ('test-id-1', 1700000000000, 'wifi', NULL, NULL, 0, NULL, NULL, " +
                    "150.5, 20.3, 12.0, 2.5, 0.1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, " +
                    "NULL, NULL, NULL, NULL, 'completed')",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 14, true, getMigracao13Para14())

        db.query("SELECT bandaWifi, downloadMbps FROM medicao WHERE id = 'test-id-1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            val bandaWifiIdx = cursor.getColumnIndex("bandaWifi")
            assertNull("bandaWifi deve ser null para medicao existente antes da migracao", cursor.getString(bandaWifiIdx))
            val downloadIdx = cursor.getColumnIndex("downloadMbps")
            assertEquals(150.5, cursor.getDouble(downloadIdx), 0.001)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migracao13Para14_novaColunaAceitaValoresGhz24EGhz5() {
        helper.createDatabase(TEST_DB, 13).use { db ->
            db.execSQL(
                "INSERT INTO medicao (id, timestampEpochMs, connectionType, connectionTypeStart, " +
                    "connectionTypeEnd, contaminado, speedtestMode, specVersion, downloadMbps, " +
                    "uploadMbps, latencyMs, jitterMs, perdaPercentual, bufferbloatMs, packetLossSource, " +
                    "vereditoStreaming, vereditoGamer, vereditoVideoChamada, gargaloPrimario, fonte, " +
                    "operadoraMovel, diagnosticoTexto, diagnosticoOrigem, diagnosticoProblemas, score, status) " +
                    "VALUES ('test-id-2', 1700000001000, 'wifi', NULL, NULL, 0, NULL, NULL, " +
                    "100.0, 10.0, 15.0, 3.0, 0.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, " +
                    "NULL, NULL, NULL, NULL, 'completed')",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 14, true, getMigracao13Para14())

        db.execSQL("UPDATE medicao SET bandaWifi = 'ghz5' WHERE id = 'test-id-2'")
        db.execSQL(
            "INSERT INTO medicao (id, timestampEpochMs, connectionType, connectionTypeStart, " +
                "connectionTypeEnd, contaminado, speedtestMode, specVersion, downloadMbps, " +
                "uploadMbps, latencyMs, jitterMs, perdaPercentual, bufferbloatMs, packetLossSource, " +
                "vereditoStreaming, vereditoGamer, vereditoVideoChamada, gargaloPrimario, fonte, " +
                "operadoraMovel, diagnosticoTexto, diagnosticoOrigem, diagnosticoProblemas, score, status, bandaWifi) " +
                "VALUES ('test-id-3', 1700000002000, 'wifi', NULL, NULL, 0, NULL, NULL, " +
                "50.0, 5.0, 20.0, 4.0, 0.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, " +
                "NULL, NULL, NULL, NULL, 'completed', 'ghz24')",
        )

        db.query("SELECT bandaWifi FROM medicao WHERE id = 'test-id-2'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("ghz5", cursor.getString(cursor.getColumnIndex("bandaWifi")))
        }
        db.query("SELECT bandaWifi FROM medicao WHERE id = 'test-id-3'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("ghz24", cursor.getString(cursor.getColumnIndex("bandaWifi")))
        }
    }

    private fun getMigracao13Para14() =
        object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN bandaWifi TEXT")
            }
        }
}
