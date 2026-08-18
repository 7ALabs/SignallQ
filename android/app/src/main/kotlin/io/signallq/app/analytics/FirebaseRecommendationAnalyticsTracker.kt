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
                    // GH#1717 — `matched_tags` NAO e mais enviado. Levava o mesmo vocabulario de
                    // `DiagnosticTag` que acabou de sair do AdMob (`wifi_fraco`,
                    // `velocidade_abaixo_do_contratado`), isto e, a CONCLUSAO do diagnostico da
                    // pessoa indo para o Google por uma segunda porta -- e a politica de
                    // privacidade descreve o Firebase Analytics como "eventos anonimos de uso
                    // (telas visitadas, acoes realizadas)". Conclusao de diagnostico nao e nem uma
                    // coisa nem outra.
                    //
                    // Achado do bloqueio B9 de Caio na PR #1717, decidido por Luiz em 2026-08-17:
                    // tirar o rotulo em vez de alargar o texto. `recommendation_id`, `type`,
                    // `rule_origin` e `diagnostic_id` continuam permitindo medir o funil de
                    // recomendacao -- o que se perde e conseguir quebrar a metrica POR TIPO DE
                    // PROBLEMA da pessoa, que era justamente o dado sensivel.
                    putDouble("score", payload.score)
                    payload.diagnosticId?.let { putString("diagnostic_id", it) }
                    putBoolean("monetized", payload.monetized)
                    putString("rule_origin", payload.ruleOrigin)
                    payload.feedback?.let { putString("feedback", it.name) }
                },
            )
        }
    }
