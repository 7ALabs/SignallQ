package io.signallq.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Métrica operacional agregada da entrega da outbox. Não carrega ID, payload,
 * identificador de dispositivo ou qualquer dado de evento.
 */
@Singleton
class AnalyticsOutboxFunnelTracker
    @Inject
    constructor(
        private val firebaseAnalytics: FirebaseAnalytics,
    ) {
        fun registrar(stage: Stage) {
            firebaseAnalytics.logEvent(
                "analytics_outbox_delivery",
                Bundle().apply { putString("stage", stage.value) },
            )
        }

        enum class Stage(val value: String) {
            CREATED("created"),
            SENT("sent"),
            ACCEPTED("accepted"),
            REJECTED("rejected"),
            REPROCESSED("reprocessed"),
            LOST("lost"),
        }
    }
