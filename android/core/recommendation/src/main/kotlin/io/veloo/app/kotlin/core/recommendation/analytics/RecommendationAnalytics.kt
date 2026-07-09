package io.signallq.app.core.recommendation.analytics

import io.signallq.app.core.recommendation.DiagnosticTag
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationType

/** Nome dos eventos de analytics gerados pelo Recommendation Engine, conforme issue #790. */
enum class RecommendationAnalyticsEventName(val eventName: String) {
    ELIGIBLE("recommendation_eligible"),
    SHOWN("recommendation_shown"),
    CLICKED("recommendation_clicked"),
    DISMISSED("recommendation_dismissed"),
    FEEDBACK("recommendation_feedback"),
    FALLBACK_AD_SHOWN("recommendation_fallback_ad_shown"),
}

/** Payload de analytics gerado a partir de uma [RecommendationDecision]. */
data class RecommendationAnalyticsPayload(
    val eventName: RecommendationAnalyticsEventName,
    val recommendationId: String,
    val type: RecommendationType,
    val matchedTags: Set<DiagnosticTag>,
    val score: Double,
    val diagnosticId: String?,
    val monetized: Boolean,
    val ruleOrigin: String,
    val feedback: RecommendationFeedbackType? = null,
)

/** Implementado por quem consome o engine (ex: featureDiagnostico) para enviar ao Firebase Analytics. */
fun interface RecommendationAnalyticsTracker {
    fun track(payload: RecommendationAnalyticsPayload)
}

/** Monta o payload de analytics para uma decisao do engine, sem acoplar o engine a nenhum SDK. */
fun RecommendationDecision.toAnalyticsPayload(
    eventName: RecommendationAnalyticsEventName,
    diagnosticId: String?,
    feedback: RecommendationFeedbackType? = null,
): RecommendationAnalyticsPayload = RecommendationAnalyticsPayload(
    eventName = eventName,
    recommendationId = recommendation.id,
    type = type,
    matchedTags = matchedTags,
    score = score,
    diagnosticId = diagnosticId,
    monetized = monetized,
    ruleOrigin = ruleOrigin,
    feedback = feedback,
)
