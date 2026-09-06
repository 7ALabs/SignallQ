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

private const val TEST_DB = "migration-14-15-test"

/**
 * GH#1512 — migração que cria `connectivity_diagnosis_history` (diagnóstico local de
 * Wi-Fi conectado sem internet).
 *
 * Valida: (1) a tabela nova é criada sem afetar tabelas existentes da v14, (2) aceita
 * INSERT com os campos sanitizados esperados (sem SSID/BSSID/IP/DNS).
 */
@RunWith(AndroidJUnit4::class)
class Migration14Para15Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SignallQDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migracao14Para15_criaTabelaEAceitaInsertSanitizado() {
        helper.createDatabase(TEST_DB, 14).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 15, true, getMigracao14Para15())

        db.execSQL(
            "INSERT INTO connectivity_diagnosis_history (id, startedAtEpochMs, finishedAtEpochMs, " +
                "transport, status, confidence, wifiConnected, localAddressAvailable, gatewayConfigured, " +
                "gatewayReachableOutcome, dnsConfigured, dnsReachableOutcome, externalIpReachableOutcome, " +
                "hostnameReachableOutcome, androidInternetCapability, androidValidated, " +
                "captivePortalDetected, mobileFallbackAvailable, incompleteReason) VALUES (" +
                "'diag-1', 1700000000000, 1700000001000, 'wifi', 'WIFI_WITHOUT_INTERNET', 'MEDIA', " +
                "1, 1, 1, 'Success', 1, 'Success', 'Failure', 'Failure', 1, 0, 0, 1, NULL)",
        )

        db.query("SELECT status, incompleteReason FROM connectivity_diagnosis_history WHERE id = 'diag-1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("WIFI_WITHOUT_INTERNET", cursor.getString(cursor.getColumnIndex("status")))
            assertNull(cursor.getString(cursor.getColumnIndex("incompleteReason")))
        }
    }

    private fun getMigracao14Para15() =
        object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `connectivity_diagnosis_history` (" +
                        "`id` TEXT NOT NULL, " +
                        "`startedAtEpochMs` INTEGER NOT NULL, " +
                        "`finishedAtEpochMs` INTEGER NOT NULL, " +
                        "`transport` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`confidence` TEXT NOT NULL, " +
                        "`wifiConnected` INTEGER NOT NULL, " +
                        "`localAddressAvailable` INTEGER NOT NULL, " +
                        "`gatewayConfigured` INTEGER NOT NULL, " +
                        "`gatewayReachableOutcome` TEXT NOT NULL, " +
                        "`dnsConfigured` INTEGER NOT NULL, " +
                        "`dnsReachableOutcome` TEXT NOT NULL, " +
                        "`externalIpReachableOutcome` TEXT NOT NULL, " +
                        "`hostnameReachableOutcome` TEXT NOT NULL, " +
                        "`androidInternetCapability` INTEGER NOT NULL, " +
                        "`androidValidated` INTEGER NOT NULL, " +
                        "`captivePortalDetected` INTEGER NOT NULL, " +
                        "`mobileFallbackAvailable` INTEGER NOT NULL, " +
                        "`incompleteReason` TEXT, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_connectivity_diagnosis_history_startedAtEpochMs` " +
                        "ON `connectivity_diagnosis_history` (`startedAtEpochMs`)",
                )
            }
        }
}
