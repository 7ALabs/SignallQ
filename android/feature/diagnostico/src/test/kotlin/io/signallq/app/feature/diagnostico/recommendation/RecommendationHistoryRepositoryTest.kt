package io.signallq.app.feature.diagnostico.recommendation

import io.signallq.app.core.database.recommendation.RecommendationHistoryEntity
import io.signallq.app.core.recommendation.DiagnosticTag
import io.signallq.app.core.recommendation.Recommendation
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationHistoryRepositoryTest {
    private val recomendacao =
        Recommendation(
            id = "free_tip_teste",
            type = RecommendationType.FREE_TIP,
            title = "Recomendacao de teste",
            tags = setOf(DiagnosticTag.WIFI_FRACO),
        )

    private fun decisao(trackingId: String) =
        RecommendationDecision(
            recommendation = recomendacao,
            matchedTags = recomendacao.tags,
            score = 100.0,
            priorityTier = 0,
            reason = "teste",
            trackingId = trackingId,
        )

    @Test
    fun `historicoRecente vazio quando nada foi persistido`() =
        runTest {
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())

            val historico = repository.historicoRecente(nowMs = 0L)

            assertTrue(historico.isEmpty())
        }

    @Test
    fun `registrarExibicao persiste e historicoRecente devolve mapeado`() =
        runTest {
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())

            repository.registrarExibicao(decisao("free_tip_teste-1000"), shownAtEpochMs = 1_000L)
            val historico = repository.historicoRecente(nowMs = 1_000L)

            assertEquals(1, historico.size)
            assertEquals("free_tip_teste", historico[0].recommendationId)
            assertEquals(1_000L, historico[0].shownAt)
            assertNull(historico[0].feedback)
        }

    @Test
    fun `historicoRecente ignora exibicoes fora da janela de 14 dias`() =
        runTest {
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())
            val quinzeDiasMs = 15 * 24 * 60 * 60 * 1000L

            repository.registrarExibicao(decisao("antiga"), shownAtEpochMs = 0L)
            val historico = repository.historicoRecente(nowMs = quinzeDiasMs)

            assertTrue(historico.isEmpty())
        }

    @Test
    fun `registrarFeedback atualiza apenas a entrada correspondente ao trackingId`() =
        runTest {
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())
            repository.registrarExibicao(decisao("rec-1"), shownAtEpochMs = 1_000L)
            repository.registrarExibicao(decisao("rec-2"), shownAtEpochMs = 2_000L)

            repository.registrarFeedback("rec-1", RecommendationFeedbackType.HELPFUL, feedbackAtEpochMs = 5_000L)

            val historico = repository.historicoRecente(nowMs = 5_000L)
            val rec1 = historico.first { it.shownAt == 1_000L }
            val rec2 = historico.first { it.shownAt == 2_000L }
            assertEquals(RecommendationFeedbackType.HELPFUL, rec1.feedback)
            assertEquals(5_000L, rec1.feedbackAt)
            assertNull(rec2.feedback)
        }

    @Test
    fun `feedback persistido com valor desconhecido nao crasha e vira null`() =
        runTest {
            val dao = FakeRecommendationHistoryDao()
            val repository = RecommendationHistoryRepository(dao)
            // Simula dado legado/futuro cujo enum nao existe mais no app instalado.
            dao.registrarExibicao(
                RecommendationHistoryEntity(
                    id = "rec-legado",
                    recommendationId = "free_tip_teste",
                    shownAtEpochMs = 1_000L,
                    feedback = "VALOR_INEXISTENTE",
                ),
            )

            val historico = repository.historicoRecente(nowMs = 1_000L)

            assertEquals(1, historico.size)
            assertNull(historico[0].feedback)
        }
}
