package io.signallq.app.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-19-20-test"

/**
 * GH#1787 -- migração real 19->20, corrigindo a inconsistência original da migração 17->18:
 * aquela migração já criava `index_analytics_outbox_nextAttemptAtEpochMs` via SQL bruto, mas
 * `AnalyticsOutboxEntity` nunca declarou o `@Index` correspondente. `MIGRATION_19_20` recria o
 * mesmo índice com `CREATE INDEX IF NOT EXISTS`, seguro nos dois cenários reais de produção:
 *
 * 1. Dispositivo que já migrou pela 17->18 (índice físico já existe) -- migração não pode falhar.
 * 2. Hipótese defensiva de banco em v19 sem o índice físico -- migração deve criá-lo.
 * 3. Instalação nova direto na versão mais recente (Room cria do zero a partir das entidades) --
 *    já deve sair com o índice, sem depender de nenhuma migração.
 *
 * Plano de rollback: a migração só cria um índice (`CREATE INDEX IF NOT EXISTS`), nunca altera
 * dado ou coluna. Reverter é remover `MIGRATION_19_20` de `addMigrations()` em
 * [CoreDatabaseModulo], remover `indices` de [io.signallq.app.core.database.analytics.AnalyticsOutboxEntity]
 * e voltar `version` pra 19 em [SignallQDatabase]. Nenhuma linha é perdida.
 */
@RunWith(AndroidJUnit4::class)
class Migration19Para20Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SignallQDatabase::class.java)

    @Test
    fun migracao19Para20_comIndiceFisicoJaExistente_naoFalhaEMantemDados() {
        val db = helper.createDatabase(TEST_DB, 19)
        // Simula quem já migrou pela 17->18: o índice físico já existe no SQLite antes da 19->20.
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_analytics_outbox_nextAttemptAtEpochMs` " +
                "ON `analytics_outbox` (`nextAttemptAtEpochMs`)",
        )
        db.execSQL(
            "INSERT INTO analytics_outbox (id, payloadJson, createdAtEpochMs, attemptCount, nextAttemptAtEpochMs) " +
                "VALUES ('event-com-indice', '{\"name\":\"screen_view\"}', 100, 0, 100)",
        )
        db.close()

        val dbMigrada = helper.runMigrationsAndValidate(TEST_DB, 20, true, CoreDatabaseModulo.MIGRATION_19_20)

        dbMigrada.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_analytics_outbox_nextAttemptAtEpochMs'",
        ).use { cursor ->
            assertTrue("índice deve continuar existindo após a migração", cursor.moveToFirst())
        }
        dbMigrada.query("SELECT payloadJson FROM analytics_outbox WHERE id = 'event-com-indice'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("{\"name\":\"screen_view\"}", cursor.getString(0))
        }
    }

    @Test
    fun migracao19Para20_semIndiceFisicoPrevio_criaIndiceEMantemDados() {
        val db = helper.createDatabase(TEST_DB, 19)
        // Cenário defensivo/hipotético: banco em v19 que nunca teve o índice físico.
        db.execSQL(
            "INSERT INTO analytics_outbox (id, payloadJson, createdAtEpochMs, attemptCount, nextAttemptAtEpochMs) " +
                "VALUES ('event-sem-indice', '{\"name\":\"app_open\"}', 200, 1, 300)",
        )
        db.close()

        val dbMigrada = helper.runMigrationsAndValidate(TEST_DB, 20, true, CoreDatabaseModulo.MIGRATION_19_20)

        dbMigrada.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_analytics_outbox_nextAttemptAtEpochMs'",
        ).use { cursor ->
            assertTrue("índice deve ser criado pela migração quando não existia", cursor.moveToFirst())
        }
        dbMigrada.query("SELECT payloadJson, attemptCount, nextAttemptAtEpochMs FROM analytics_outbox WHERE id = 'event-sem-indice'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("{\"name\":\"app_open\"}", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(300L, cursor.getLong(2))
        }
    }

    @Test
    fun instalacaoNova_diretoNaVersaoAtual_jaSaiComIndice() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db =
            Room.databaseBuilder(context, SignallQDatabase::class.java, "instalacao-nova-v20-test")
                .build()
        try {
            db.openHelper.writableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_analytics_outbox_nextAttemptAtEpochMs'",
            ).use { cursor ->
                assertTrue(
                    "instalação nova (Room cria direto das entidades) deve sair com o índice",
                    cursor.moveToFirst(),
                )
            }
        } finally {
            db.close()
            context.deleteDatabase("instalacao-nova-v20-test")
        }
    }
}
