package io.signallq.app.feature.diagnostico.recommendation

import io.signallq.app.core.database.recommendation.RecommendationHistoryDao
import io.signallq.app.core.database.recommendation.RecommendationHistoryEntity
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationHistoryEntry
import javax.inject.Inject

/**
 * Ponte entre o Room (`coreDatabase`) e o `RecommendationEngine` (`coreRecommendation`,
 * issue #790) -- issue #812. O engine e stateless por desenho; esta classe carrega o
 * historico local e persiste exibicao/feedback do usuario.
 */
class RecommendationHistoryRepository
    @Inject
    constructor(
        private val dao: RecommendationHistoryDao,
    ) {
        /**
         * Historico recente o suficiente para o engine aplicar cooldown/maxPerDay/maxPerWeek
         * do catalogo atual (maior janela hoje: 7 dias, `operator_offer_upgrade_plano`).
         * Janela de leitura com margem de 2x para nao exigir alterar este numero se o
         * catalogo remoto aumentar essa janela no futuro.
         */
        suspend fun historicoRecente(nowMs: Long = System.currentTimeMillis()): List<RecommendationHistoryEntry> =
            dao.buscarRecente(desdeEpochMs = nowMs - JANELA_HISTORICO_MS).map { it.paraDominio() }

        /**
         * Persiste a exibicao de uma [RecommendationDecision]. Usa `decision.trackingId` como
         * chave primaria -- ele ja identifica de forma unica recomendacao + instante de
         * exibicao, entao o feedback do usuario ([registrarFeedback]) sempre atualiza a
         * entrada correta, sem precisar de uma segunda leitura para descobrir o id.
         */
        suspend fun registrarExibicao(
            decision: RecommendationDecision,
            shownAtEpochMs: Long,
        ) {
            dao.registrarExibicao(
                RecommendationHistoryEntity(
                    id = decision.trackingId,
                    recommendationId = decision.recommendation.id,
                    shownAtEpochMs = shownAtEpochMs,
                ),
            )
        }

        suspend fun registrarFeedback(
            trackingId: String,
            feedback: RecommendationFeedbackType,
            feedbackAtEpochMs: Long = System.currentTimeMillis(),
        ) {
            dao.atualizarFeedback(id = trackingId, feedback = feedback.name, feedbackAtEpochMs = feedbackAtEpochMs)
        }

        private fun RecommendationHistoryEntity.paraDominio() =
            RecommendationHistoryEntry(
                recommendationId = recommendationId,
                shownAt = shownAtEpochMs,
                // find (nao valueOf): tolera valor persistido de uma versao antiga/futura do
                // app cujo enum nao bate mais -- trata como "sem feedback" em vez de crashar.
                feedback = feedback?.let { valor -> RecommendationFeedbackType.entries.find { it.name == valor } },
                feedbackAt = feedbackAtEpochMs,
            )

        private companion object {
            const val JANELA_HISTORICO_MS = 14 * 24 * 60 * 60 * 1000L
        }
    }
