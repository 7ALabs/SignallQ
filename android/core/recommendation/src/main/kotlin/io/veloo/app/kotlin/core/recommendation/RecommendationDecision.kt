package io.signallq.app.core.recommendation

/**
 * Saida do Recommendation Engine para uma recomendacao elegivel: a recomendacao do
 * catalogo mais o resultado do calculo de score/prioridade feito pelo engine.
 */
data class RecommendationDecision(
    val recommendation: Recommendation,
    val matchedTags: Set<DiagnosticTag>,
    val score: Double,
    val priorityTier: Int,
    val reason: String,
    val trackingId: String,
) {
    val type: RecommendationType get() = recommendation.type
    val monetized: Boolean get() = recommendation.monetized
    val ruleOrigin: String get() = recommendation.ruleOrigin
}
