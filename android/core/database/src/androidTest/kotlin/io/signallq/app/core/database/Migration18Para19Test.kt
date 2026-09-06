package io.signallq.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-18-19-test"

/**
 * GH#1707 (Task 2.0.09e, épico #1647) — migração real 18→19, rodando contra o schema 18
 * gerado (não um teste unitário da lógica). Cobre os dois requisitos do critério de aceite:
 *
 * 1. Banco existente (linhas gravadas antes da migração) sobrevive e recebe `networkId = NULL`
 *    — nunca um valor inventado.
 * 2. Escrita nova pós-migração grava e lê `networkId` normalmente.
 *
 * Plano de rollback: a migração é puramente aditiva (`ALTER TABLE ... ADD COLUMN`, nullable,
 * sem `NOT NULL DEFAULT`) — reverter é remover `MIGRATION_18_19` da lista de
 * `addMigrations()` em [CoreDatabaseModulo] e voltar `version` pra 18 em [SignallQDatabase].
 * Nenhuma linha é perdida ao reverter porque nenhuma coluna existente foi alterada; a única
 * perda é o valor de `networkId` das linhas gravadas na versão 19, que não é lido em nenhum
 * consumidor de schema 18 (coluna nova, sem consumidor anterior).
 */
@RunWith(AndroidJUnit4::class)
class Migration18Para19Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SignallQDatabase::class.java)

    @Test
    fun migracao18Para19_preservaLinhaExistenteComNetworkIdNullEAceitaEscritaNova() {
        val db = helper.createDatabase(TEST_DB, 18)
        db.execSQL(
            "INSERT INTO medicao (id, timestampEpochMs, connectionType, contaminado, status, executionId, rulesVersion) " +
                "VALUES ('medicao-legado', 1000, 'wifi', 0, 'completed', 'legacy-medicao-legado', 'legacy-unversioned')",
        )
        db.close()

        val dbMigrada = helper.runMigrationsAndValidate(TEST_DB, 19, true, CoreDatabaseModulo.MIGRATION_18_19)

        dbMigrada.query("SELECT networkId FROM medicao WHERE id = 'medicao-legado'").use { cursor ->
            cursor.moveToFirst()
            assertNull("linha gravada antes da migracao deve ter networkId NULL, nunca inventado", cursor.getString(0))
        }

        dbMigrada.execSQL(
            "INSERT INTO medicao " +
                "(id, timestampEpochMs, connectionType, contaminado, status, executionId, rulesVersion, networkId) " +
                "VALUES ('medicao-nova', 2000, 'wifi', 0, 'completed', 'exec-nova', 'legacy-unversioned', 'wifi-bssid:aa:bb:cc:dd:ee:ff')",
        )
        dbMigrada.query("SELECT networkId FROM medicao WHERE id = 'medicao-nova'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("wifi-bssid:aa:bb:cc:dd:ee:ff", cursor.getString(0))
        }
    }
}
