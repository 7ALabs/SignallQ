package io.signallq.pro.core.database

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val TEST_DB = "pro-migration-1-2-test"

/**
 * Issue #1166 -- gap real do MVP0: entidade `local` nao existia, `visita` nao tinha
 * `localId`. Valida que a migracao v1 -> v2 (1) cria a tabela `local` e backfilla um local
 * "Principal" por cliente ja existente, (2) preenche `visita.localId` a partir do local do
 * mesmo cliente sem perder nenhuma visita/cliente ja persistido.
 */
@RunWith(AndroidJUnit4::class)
class Migration1Para2Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SignallQProDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migracao1Para2_backfillaLocalPrincipalParaClienteExistente() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO cliente (id, nome, telefone, criadoEmEpochMs) " +
                    "VALUES ('cliente-1', 'Maria Cliente', '11999999999', 1700000000000)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, getMigracaoLocal1Para2())

        db.query("SELECT id, clienteId, nome, endereco FROM local WHERE clienteId = 'cliente-1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("cliente-1-local", cursor.getString(cursor.getColumnIndex("id")))
            assertEquals("cliente-1", cursor.getString(cursor.getColumnIndex("clienteId")))
            assertEquals("Principal", cursor.getString(cursor.getColumnIndex("nome")))
            assertEquals("", cursor.getString(cursor.getColumnIndex("endereco")))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migracao1Para2_preencheLocalIdDaVisitaExistenteSemPerderDado() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO cliente (id, nome, telefone, criadoEmEpochMs) " +
                    "VALUES ('cliente-2', 'Joao Cliente', NULL, 1700000000000)",
            )
            db.execSQL(
                "INSERT INTO visita (id, clienteId, tipo, status, etapaAtual, modoRapido, " +
                    "iniciadaEmEpochMs, atualizadaEmEpochMs) VALUES ('visita-1', 'cliente-2', " +
                    "'INSTALACAO', 'CONCLUIDA', 'CONCLUSAO', 0, 1700000000000, 1700000001000)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, getMigracaoLocal1Para2())

        db.query("SELECT id, clienteId, localId, status FROM visita WHERE id = 'visita-1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("cliente-2", cursor.getString(cursor.getColumnIndex("clienteId")))
            assertEquals("CONCLUIDA", cursor.getString(cursor.getColumnIndex("status")))
            val localId = cursor.getString(cursor.getColumnIndex("localId"))
            assertNotNull("visita migrada precisa ter localId preenchido", localId)
            assertEquals("cliente-2-local", localId)
        }
    }

    /**
     * Reproducao fiel de `migracaoLocal1Para2` (`ProDatabaseModule.kt`, privada ao modulo de
     * producao) -- mesmo padrao de `Migration13Para14Test` no `:core:database` do consumidor
     * (androidTest nao enxerga membros privados de outro source set).
     */
    private fun getMigracaoLocal1Para2() =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `local` (`id` TEXT NOT NULL, `clienteId` TEXT NOT NULL, " +
                        "`nome` TEXT NOT NULL, `endereco` TEXT NOT NULL, `criadoEmEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT INTO local (id, clienteId, nome, endereco, criadoEmEpochMs) " +
                        "SELECT id || '-local', id, 'Principal', '', criadoEmEpochMs FROM cliente",
                )
                db.execSQL("ALTER TABLE visita ADD COLUMN localId TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "UPDATE visita SET localId = (SELECT id FROM local WHERE local.clienteId = visita.clienteId LIMIT 1) " +
                        "WHERE localId = ''",
                )
            }
        }
}
