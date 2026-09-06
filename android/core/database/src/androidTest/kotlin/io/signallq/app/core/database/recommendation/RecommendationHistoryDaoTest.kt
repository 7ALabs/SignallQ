package io.signallq.app.core.database.recommendation

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.signallq.app.core.database.SignallQDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecommendationHistoryDaoTest {
    private lateinit var db: SignallQDatabase
    private lateinit var dao: RecommendationHistoryDao

    @Before
    fun criarBanco() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    SignallQDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = db.recommendationHistoryDao()
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    private fun entrada(
        id: String,
        recommendationId: String = "free_tip_reposicionar_roteador",
        shownAtEpochMs: Long,
        feedback: String? = null,
        feedbackAtEpochMs: Long? = null,
    ) = RecommendationHistoryEntity(
        id = id,
        recommendationId = recommendationId,
        shownAtEpochMs = shownAtEpochMs,
        feedback = feedback,
        feedbackAtEpochMs = feedbackAtEpochMs,
    )

    @Test
    fun registrarExibicao_leDevolve() =
        runTest {
            dao.registrarExibicao(entrada(id = "rec-1", shownAtEpochMs = 1_000L))

            val historico = dao.buscarRecente(0L)
            assertEquals(1, historico.size)
            assertEquals("rec-1", historico[0].id)
            assertNull(historico[0].feedback)
        }

    @Test
    fun buscarRecente_ignoraEntradasForaDaJanela() =
        runTest {
            dao.registrarExibicao(entrada(id = "antiga", shownAtEpochMs = 1_000L))
            dao.registrarExibicao(entrada(id = "recente", shownAtEpochMs = 9_000L))

            val historico = dao.buscarRecente(desdeEpochMs = 5_000L)

            assertEquals(1, historico.size)
            assertEquals("recente", historico[0].id)
        }

    @Test
    fun buscarRecente_ordenadoPorShownAtDesc() =
        runTest {
            dao.registrarExibicao(entrada(id = "a", shownAtEpochMs = 1_000L))
            dao.registrarExibicao(entrada(id = "b", shownAtEpochMs = 3_000L))
            dao.registrarExibicao(entrada(id = "c", shownAtEpochMs = 2_000L))

            val historico = dao.buscarRecente(0L)

            assertEquals(listOf("b", "c", "a"), historico.map { it.id })
        }

    @Test
    fun atualizarFeedback_atualizaApenasAEntradaCorrespondente() =
        runTest {
            dao.registrarExibicao(entrada(id = "rec-1", shownAtEpochMs = 1_000L))
            dao.registrarExibicao(entrada(id = "rec-2", shownAtEpochMs = 2_000L))

            dao.atualizarFeedback(id = "rec-1", feedback = "HELPFUL", feedbackAtEpochMs = 5_000L)

            val historico = dao.buscarRecente(0L).associateBy { it.id }
            assertEquals("HELPFUL", historico["rec-1"]!!.feedback)
            assertEquals(5_000L, historico["rec-1"]!!.feedbackAtEpochMs)
            assertNull(historico["rec-2"]!!.feedback)
        }

    @Test
    fun registrarExibicao_comMesmoId_substituiEntradaAnterior() =
        runTest {
            dao.registrarExibicao(entrada(id = "rec-1", shownAtEpochMs = 1_000L))
            dao.registrarExibicao(entrada(id = "rec-1", shownAtEpochMs = 1_000L, feedback = "HIDE"))

            val historico = dao.buscarRecente(0L)

            assertEquals(1, historico.size)
            assertTrue(historico[0].feedback == "HIDE")
        }
}
