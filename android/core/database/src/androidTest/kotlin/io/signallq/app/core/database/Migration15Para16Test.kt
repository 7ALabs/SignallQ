package io.signallq.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val TEST_DB = "migration-15-16-test"

/**
 * GH#1228 (Fase 3) — migração que adiciona `executionId`/`rulesVersion` à tabela `medicao`
 * (requisito não-negociável da issue #1228: "mudança futura de regra não reescreve
 * silenciosamente o significado de resultados antigos").
 *
 * Valida:
 * 1. medições já salvas na v15 sobrevivem à migração — nenhuma outra coluna muda de valor;
 * 2. cada linha existente recebe `executionId = 'legacy-' || id` (nunca vazio, nunca
 *    reaproveitado entre linhas — cada `id` de origem já é único);
 * 3. cada linha existente recebe `rulesVersion = 'legacy-unversioned'` (nunca inventamos qual
 *    conjunto de regras classificou dados antigos);
 * 4. a coluna aceita escrita explícita de um `executionId`/`rulesVersion` real para linhas
 *    novas, pós-migração.
 */
@RunWith(AndroidJUnit4::class)
class Migration15Para16Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SignallQDatabase::class.java,
        )

    private fun inserirMedicaoV15(
        db: SupportSQLiteDatabase,
        id: String,
        downloadMbps: Double,
    ) {
        db.execSQL(
            "INSERT INTO medicao (id, timestampEpochMs, connectionType, connectionTypeStart, " +
                "connectionTypeEnd, contaminado, speedtestMode, specVersion, downloadMbps, " +
                "uploadMbps, latencyMs, jitterMs, perdaPercentual, bufferbloatMs, packetLossSource, " +
                "vereditoStreaming, vereditoGamer, vereditoVideoChamada, gargaloPrimario, fonte, " +
                "operadoraMovel, bandaWifi, diagnosticoTexto, diagnosticoOrigem, diagnosticoProblemas, " +
                "score, status) " +
                "VALUES ('$id', 1700000000000, 'wifi', NULL, NULL, 0, 'fast', '3', " +
                "$downloadMbps, 20.0, 15.0, 2.0, 0.1, 5.0, NULL, 'good', 'poor', 'acceptable', " +
                "'upload', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'completed')",
        )
    }

    @Test
    @Throws(IOException::class)
    fun migracao15Para16_preservaMedicaoExistenteEAdicionaExecutionIdLegado() {
        helper.createDatabase(TEST_DB, 15).use { db ->
            inserirMedicaoV15(db, id = "test-id-legado-1", downloadMbps = 150.5)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 16, true, getMigracao15Para16())

        db
            .query(
                "SELECT executionId, rulesVersion, downloadMbps, uploadMbps, latencyMs, status " +
                    "FROM medicao WHERE id = 'test-id-legado-1'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                // Nenhuma outra coluna/valor foi alterado pela migracao.
                assertEquals(150.5, cursor.getDouble(cursor.getColumnIndex("downloadMbps")), 0.001)
                assertEquals(20.0, cursor.getDouble(cursor.getColumnIndex("uploadMbps")), 0.001)
                assertEquals(15.0, cursor.getDouble(cursor.getColumnIndex("latencyMs")), 0.001)
                assertEquals("completed", cursor.getString(cursor.getColumnIndex("status")))
                // executionId legado usa o proprio PK da linha, nunca vazio.
                assertEquals("legacy-test-id-legado-1", cursor.getString(cursor.getColumnIndex("executionId")))
                // rulesVersion legado -- nunca inventamos qual regra classificou dados antigos.
                assertEquals("legacy-unversioned", cursor.getString(cursor.getColumnIndex("rulesVersion")))
            }
    }

    @Test
    @Throws(IOException::class)
    fun migracao15Para16_duasLinhasLegadasRecebemExecutionIdDiferentesEDerivadosDoProprioId() {
        helper.createDatabase(TEST_DB, 15).use { db ->
            inserirMedicaoV15(db, id = "legado-A", downloadMbps = 100.0)
            inserirMedicaoV15(db, id = "legado-B", downloadMbps = 200.0)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 16, true, getMigracao15Para16())

        db.query("SELECT id, executionId FROM medicao ORDER BY id").use { cursor ->
            assertEquals(2, cursor.count)
            val idsPorExecutionId = mutableMapOf<String, String>()
            while (cursor.moveToNext()) {
                idsPorExecutionId[cursor.getString(cursor.getColumnIndex("id"))] =
                    cursor.getString(cursor.getColumnIndex("executionId"))
            }
            assertEquals("legacy-legado-A", idsPorExecutionId["legado-A"])
            assertEquals("legacy-legado-B", idsPorExecutionId["legado-B"])
            // Nunca reaproveitado entre linhas.
            assertFalse(idsPorExecutionId["legado-A"] == idsPorExecutionId["legado-B"])
        }
    }

    @Test
    @Throws(IOException::class)
    fun migracao15Para16_novaColunaAceitaEscritaExplicitaDeExecutionIdRealPosMigracao() {
        helper.createDatabase(TEST_DB, 15).use { db ->
            inserirMedicaoV15(db, id = "pre-existente", downloadMbps = 50.0)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 16, true, getMigracao15Para16())

        db.execSQL(
            "INSERT INTO medicao (id, timestampEpochMs, connectionType, connectionTypeStart, " +
                "connectionTypeEnd, contaminado, speedtestMode, specVersion, downloadMbps, " +
                "uploadMbps, latencyMs, jitterMs, perdaPercentual, bufferbloatMs, packetLossSource, " +
                "vereditoStreaming, vereditoGamer, vereditoVideoChamada, gargaloPrimario, fonte, " +
                "operadoraMovel, bandaWifi, diagnosticoTexto, diagnosticoOrigem, diagnosticoProblemas, " +
                "score, status, executionId, rulesVersion) " +
                "VALUES ('pos-migracao', 1700000002000, 'wifi', NULL, NULL, 0, 'fast', '3', " +
                "300.0, 30.0, 10.0, 1.0, 0.0, 3.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, " +
                "NULL, NULL, NULL, NULL, 'completed', 'exec-real-uuid-001', 'diagnostic-rules-v1')",
        )

        db.query("SELECT executionId, rulesVersion FROM medicao WHERE id = 'pos-migracao'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("exec-real-uuid-001", cursor.getString(cursor.getColumnIndex("executionId")))
            assertEquals("diagnostic-rules-v1", cursor.getString(cursor.getColumnIndex("rulesVersion")))
        }
    }

    private fun getMigracao15Para16() =
        object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicao ADD COLUMN executionId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicao ADD COLUMN rulesVersion TEXT NOT NULL DEFAULT 'legacy-unversioned'")
                db.execSQL("UPDATE medicao SET executionId = 'legacy-' || id")
            }
        }
}
