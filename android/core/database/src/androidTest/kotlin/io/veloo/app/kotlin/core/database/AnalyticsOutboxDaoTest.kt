package io.signallq.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.signallq.app.core.database.analytics.AnalyticsOutboxEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalyticsOutboxDaoTest {
    private lateinit var database: SignallQDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SignallQDatabase::class.java).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun due_defer_acknowledge_e_clear_persistem_o_ciclo_da_outbox() = runBlocking {
        val dao = database.analyticsOutboxDao()
        assertEquals(1L, dao.enqueue(event("primeiro", createdAt = 100)))
        assertEquals(1L, dao.enqueue(event("segundo", createdAt = 200)))
        assertEquals(-1L, dao.enqueue(event("primeiro", createdAt = 300)))

        assertEquals(listOf("primeiro"), dao.due(nowEpochMs = 150, limit = 10).map { it.id })
        assertEquals(1, dao.defer("primeiro", attemptCount = 1, nextAttemptAtEpochMs = 500))
        assertEquals(listOf("segundo"), dao.due(nowEpochMs = 250, limit = 10).map { it.id })
        assertEquals(1, dao.acknowledge("segundo"))
        assertTrue(dao.due(nowEpochMs = 250, limit = 10).isEmpty())
        assertEquals(listOf("primeiro"), dao.due(nowEpochMs = 500, limit = 10).map { it.id })

        dao.clear()
        assertTrue(dao.due(nowEpochMs = Long.MAX_VALUE, limit = 10).isEmpty())
    }

    private fun event(id: String, createdAt: Long) = AnalyticsOutboxEntity(id, "{\"id\":\"$id\"}", createdAt)
}
