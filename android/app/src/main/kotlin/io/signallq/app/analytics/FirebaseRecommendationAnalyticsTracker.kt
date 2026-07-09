package io.signallq.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.signallq.app.core.recommendation.analytics.RecommendationAnalyticsPayload
import io.signallq.app.core.recommendation.analytics.RecommendationAnalyticsTracker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacao de [RecommendationAnalyticsTracker] (`coreRecommendation`, issue #790)
 * usando Firebase Analytics -- issue #813.
 *
 * Nome do evento = [RecommendationAnalyticsPayload.eventName] (ja no formato
 * `recommendation_*` esperado pela #790). Sem PII -- so ids/metricas do catalogo e do
 * diagnostico, mesma politica das demais implementacoes de analytics do app.
 */
@Singleton
class FirebaseRecommendationAnalyticsTracker
    @Inject
    constructor(
        private val firebaseAnalytics: FirebaseAnalytics,
    ) : RecommendationAnalyticsTracker {
        override fun track(payload: RecommendationAnalyticsPayload) {
            firebaseAnalytics.logEvent(
                payload.eventName.eventName,
                Bundle().apply {
                    putString("recommendation_id", payload.recommendationId)
                    putString("type", payload.type.name)
                    putString("matched_tags", payload.matchedTags.joinToString(",") { it.id })
                    putDouble("score", payload.score)
                    payload.diagnosticId?.let { putString("diagnostic_id", it) }
                    putBoolean("monetized", payload.monetized)
                    putString("rule_origin", payload.ruleOrigin)
                    payload.feedback?.let { putString("feedback", it.name) }
                },
            )
        }
    }
