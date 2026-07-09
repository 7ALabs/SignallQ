package io.signallq.app.core.recommendation

/** Feedback explicito do usuario sobre uma recomendacao ja exibida. */
enum class RecommendationFeedbackType {
    HELPFUL,
    NOT_HELPFUL,
    HIDE,
    CLICKED,
    DISMISSED,
}

/**
 * Registro historico de uma exibicao de recomendacao, usado pelo engine para aplicar
 * cooldown, limites de frequencia e nao repetir a mesma recomendacao em sequencia.
 *
 * O engine e stateless: quem chama [RecommendationEngine] e responsavel por carregar
 * o historico relevante (local ou remoto) e passa-lo em [RecommendationRequest.history].
 */
data class RecommendationHistoryEntry(
    val recommendationId: String,
    val shownAt: Long,
    val feedback: RecommendationFeedbackType? = null,
    val feedbackAt: Long? = null,
)
