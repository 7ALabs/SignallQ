package io.signallq.app.feature.diagnostico.recommendation

import io.signallq.app.core.database.recommendation.RecommendationHistoryDao
import io.signallq.app.core.database.recommendation.RecommendationHistoryEntity

/** Fake em memoria de [RecommendationHistoryDao] -- evita depender de Room/instrumentacao nos testes JVM. */
class FakeRecommendationHistoryDao : RecommendationHistoryDao {

    private val armazenado = mutableMapOf<String, RecommendationHistoryEntity>()

    override suspend fun registrarExibicao(entrada: RecommendationHistoryEntity) {
        armazenado[entrada.id] = entrada
    }

    override suspend fun buscarRecente(desdeEpochMs: Long): List<RecommendationHistoryEntity> =
        armazenado.values
            .filter { it.shownAtEpochMs >= desdeEpochMs }
            .sortedByDescending { it.shownAtEpochMs }

    override suspend fun atualizarFeedback(id: String, feedback: String, feedbackAtEpochMs: Long) {
        armazenado[id]?.let { armazenado[id] = it.copy(feedback = feedback, feedbackAtEpochMs = feedbackAtEpochMs) }
    }

    override suspend fun buscarComFeedbackDesde(desdeEpochMs: Long): List<RecommendationHistoryEntity> =
        armazenado.values
            .filter { it.feedbackAtEpochMs != null && it.feedbackAtEpochMs!! >= desdeEpochMs }
            .sortedBy { it.feedbackAtEpochMs }
}
