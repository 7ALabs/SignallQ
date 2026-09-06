package io.signallq.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val TEST_DB = "migration-16-17-test"

/**
 * GH#1462 (parte de #951) — migracao aditiva que cria a tabela
 * `provider_directory_cache` (cache local por resultado individual do diretorio remoto
 * de provedores). Nenhuma tabela existente e alterada.
 *
 * Valida: a tabela nova aceita escrita/leitura pos-migracao, com todas as colunas
 * (incluindo as nullable `logoUrl`/`sacPhone`/.../`cacheVersion`/`cacheExpiresAtMs`).
 */
@RunWith(AndroidJUnit4::class)
class Migration16Para17Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SignallQDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migracao16Para17_criaTabelaProviderDirectoryCacheEAceitaEscritaELeitura() {
        helper.createDatabase(TEST_DB, 16).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 17, true, getMigracao16Para17())

        db.execSQL(
            "INSERT INTO provider_directory_cache (cacheKey, providerId, displayName, logoUrl, " +
                "sacPhone, technicalSupportPhone, whatsappUrl, websiteUrl, customerAreaUrl, " +
                "ombudsmanPhone, status, cacheVersion, cacheExpiresAtMs, fetchedAtEpochMs) " +
                "VALUES ('id:regional_teste', 'regional_teste', 'Regional Teste', NULL, " +
                "'10000', NULL, 'https://wa.me/5511999999999', 'https://regional.example.com', " +
                "NULL, NULL, 'VERIFIED', 3, 1800000000000, 1700000000000)",
        )

        db
            .query(
                "SELECT providerId, displayName, sacPhone, status, cacheVersion, cacheExpiresAtMs " +
                    "FROM provider_directory_cache WHERE cacheKey = 'id:regional_teste'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("regional_teste", cursor.getString(cursor.getColumnIndex("providerId")))
                assertEquals("Regional Teste", cursor.getString(cursor.getColumnIndex("displayName")))
                assertEquals("10000", cursor.getString(cursor.getColumnIndex("sacPhone")))
                assertEquals("VERIFIED", cursor.getString(cursor.getColumnIndex("status")))
                assertEquals(3, cursor.getInt(cursor.getColumnIndex("cacheVersion")))
                assertEquals(1800000000000L, cursor.getLong(cursor.getColumnIndex("cacheExpiresAtMs")))
            }
    }

    private fun getMigracao16Para17() =
        object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `provider_directory_cache` (" +
                        "`cacheKey` TEXT NOT NULL, " +
                        "`providerId` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, " +
                        "`logoUrl` TEXT, " +
                        "`sacPhone` TEXT, " +
                        "`technicalSupportPhone` TEXT, " +
                        "`whatsappUrl` TEXT, " +
                        "`websiteUrl` TEXT, " +
                        "`customerAreaUrl` TEXT, " +
                        "`ombudsmanPhone` TEXT, " +
                        "`status` TEXT, " +
                        "`cacheVersion` INTEGER, " +
                        "`cacheExpiresAtMs` INTEGER, " +
                        "`fetchedAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`cacheKey`))",
                )
            }
        }
}
