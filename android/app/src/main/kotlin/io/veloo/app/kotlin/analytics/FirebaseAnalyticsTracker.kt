package io.signallq.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.app.BuildConfig
import io.signallq.app.core.network.AnalyticsTracker
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
        @ApplicationContext private val context: Context,
    ) : AnalyticsTracker {
        private val sessionId: String = UUID.randomUUID().toString()
        private val appVersion: String = BuildConfig.VERSION_NAME

        /**
         * GH#1360 — user properties do GA4 (valem pra sessao inteira, nao por evento
         * isolado): `environment`/`dist_channel` reaproveitam [distributionChannel] e
         * [environmentFor] (DistributionChannel.kt, ja usados pelo CompositeAnalyticsTracker
         * no envio a /ingest/analytics — GH#759) para que o mesmo criterio de ambiente
         * classifique tambem os eventos do Firebase. `build_type` vem direto de
         * [BuildConfig.BUILD_TYPE]. Os dois sistemas de analytics continuam paralelos —
         * isto so alinha a fonte de ambiente/canal entre eles.
         *
         * Chamado uma vez por [registrarSessionStart] (inicio de sessao, hoje disparado
         * uma vez em MainActivity.onCreate) — setUserProperty e idempotente, entao uma
         * eventual segunda chamada na mesma sessao nao tem efeito colateral.
         */
        private fun registrarUserPropertiesDeAmbiente() {
            val distChannel = distributionChannel(context)
            firebaseAnalytics.setUserProperty("environment", environmentFor(distChannel))
            firebaseAnalytics.setUserProperty("dist_channel", distChannel)
            firebaseAnalytics.setUserProperty("build_type", BuildConfig.BUILD_TYPE)
        }

        // GH#919 — sessionIdOverride nao se aplica ao Firebase/GA4: o SDK nativo
        // ja tem seu proprio conceito de sessao, independente do session_id
        // usado no schema SIG-134 enviado ao admin-worker (ver classe doc acima).
        override fun registrarFeatureUsada(
            featureId: String,
            sessionIdOverride: String?,
        ) {
            firebaseAnalytics.logEvent(
                "feature_used",
                Bundle().apply {
                    putString("feature_id", featureId)
                    putString("session_id", sessionId)
                    putString("app_version", appVersion)
                    putLong("timestamp", System.currentTimeMillis())
                },
            )
        }

        override fun registrarScreenView(screenName: String) {
            firebaseAnalytics.logEvent(
                "screen_view",
                Bundle().apply {
                    putString("screen_name", screenName)
                    putString("session_id", sessionId)
                    putString("app_version", appVersion)
                },
            )
        }

        override fun registrarSessionStart() {
            registrarUserPropertiesDeAmbiente()
            firebaseAnalytics.logEvent(
                "app_session_start",
                Bundle().apply {
                    putString("session_id", sessionId)
                    putString("app_version", appVersion)
                },
            )
        }

        override fun registrarFeatureCrash(
            featureId: String,
            errorType: String,
        ) {
            firebaseAnalytics.logEvent(
                "feature_crash",
                Bundle().apply {
                    putString("feature_id", featureId)
                    putString("error_type", errorType)
                    putString("app_version", appVersion)
                },
            )
        }

        override fun registrarBatterySnapshot(
            level: Int,
            charging: Boolean,
        ) {
            firebaseAnalytics.logEvent(
                "battery_snapshot",
                Bundle().apply {
                    putInt("level", level)
                    putBoolean("charging", charging)
                    putString("session_id", sessionId)
                },
            )
        }
    }
