package io.veloo.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.veloo.app.BuildConfig
import io.veloo.app.core.network.AnalyticsTracker
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacao de [AnalyticsTracker] usando Firebase Analytics.
 *
 * session_id: UUID anonimo gerado uma vez por instancia de processo.
 * Nao persiste entre sessoes — identificacao anonima por sessao de app.
 *
 * Sem PII: nenhum dado de usuario, MAC, IMEI ou localizacao e enviado.
 */
@Singleton
class FirebaseAnalyticsTracker
    @Inject
    constructor(
        private val firebaseAnalytics: FirebaseAnalytics,
    ) : AnalyticsTracker {
        private val sessionId: String = UUID.randomUUID().toString()
        private val appVersion: String = BuildConfig.VERSION_NAME

        override fun registrarFeatureUsada(featureId: String) {
            firebaseAnalytics.logEvent("feature_used", Bundle().apply {
                putString("feature_id", featureId)
                putString("session_id", sessionId)
                putString("app_version", appVersion)
                putLong("timestamp", System.currentTimeMillis())
            })
        }

        override fun registrarScreenView(screenName: String) {
            firebaseAnalytics.logEvent("screen_view", Bundle().apply {
                putString("screen_name", screenName)
                putString("session_id", sessionId)
                putString("app_version", appVersion)
            })
        }

        override fun registrarSessionStart() {
            firebaseAnalytics.logEvent("session_start", Bundle().apply {
                putString("session_id", sessionId)
                putString("app_version", appVersion)
            })
        }

        override fun registrarFeatureCrash(featureId: String, errorType: String) {
            firebaseAnalytics.logEvent("feature_crash", Bundle().apply {
                putString("feature_id", featureId)
                putString("error_type", errorType)
                putString("app_version", appVersion)
            })
        }

        override fun registrarBatterySnapshot(level: Int, charging: Boolean) {
            firebaseAnalytics.logEvent("battery_snapshot", Bundle().apply {
                putInt("level", level)
                putBoolean("charging", charging)
                putString("session_id", sessionId)
            })
        }
    }
