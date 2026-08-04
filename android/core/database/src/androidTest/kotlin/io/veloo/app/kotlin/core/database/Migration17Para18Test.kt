package io.signallq.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-17-18-test"

@RunWith(AndroidJUnit4::class)
class Migration17Para18Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SignallQDatabase::class.java)

    @Test
    fun migracao17Para18_criaOutboxComRetryEAceitaDadosSanitizados() {
        helper.createDatabase(TEST_DB, 17).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 18, true, migration())
        db.execSQL(
            "INSERT INTO analytics_outbox (id, payloadJson, createdAtEpochMs, attemptCount, nextAttemptAtEpochMs) " +
                "VALUES ('event-1', '{\"id\":\"event-1\",\"name\":\"screen_view\"}', 100, 2, 500)",
        )
        db.query("SELECT payloadJson, createdAtEpochMs, attemptCount, nextAttemptAtEpochMs FROM analytics_outbox WHERE id = 'event-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("{\"id\":\"event-1\",\"name\":\"screen_view\"}", cursor.getString(0))
            assertEquals(100L, cursor.getLong(1))
            assertEquals(2, cursor.getInt(2))
            assertEquals(500L, cursor.getLong(3))
        }
    }

    private fun migration() = object : androidx.room.migration.Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `analytics_outbox` (`id` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `attemptCount` INTEGER NOT NULL, `nextAttemptAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_analytics_outbox_nextAttemptAtEpochMs` ON `analytics_outbox` (`nextAttemptAtEpochMs`)")
        }
    }
}
