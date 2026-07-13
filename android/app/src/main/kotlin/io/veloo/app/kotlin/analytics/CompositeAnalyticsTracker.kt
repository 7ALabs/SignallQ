package io.signallq.app.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.app.BuildConfig
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.di.ApplicationScope
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.ingest.AnalyticsEventIngestPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AnalyticsTracker] composto (GH#759): registra cada evento no Firebase Analytics
 * (GA4 — como sempre foi) E envia o mesmo evento ao signallq-admin-worker
 * (POST /ingest/analytics), para alimentar as telas "Uso do App" e
 * "Problemas & Incidentes" do painel admin.
 *
 * O envio ao admin-worker e fire-and-forget via [AdminIngestRepository] (mesmo
 * padrao/gate de consentimento LGPD de sendDiagnostic/sendAiUsage) — nunca
 * bloqueia nem falha a chamada do site de chamada original.
 *
 * session_id aqui e independente do session_id interno do FirebaseAnalyticsTracker:
 * sao dois sistemas de analytics distintos, sem necessidade de correlacao cruzada.
 */
@Singleton
class CompositeAnalyticsTracker
    @Inject
    constructor(
        private val firebaseTracker: FirebaseAnalyticsTracker,
        private val adminIngestRepository: AdminIngestRepository,
        private val preferenciasAppRepository: PreferenciasAppRepository,
        @ApplicationContext private val context: Context,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : AnalyticsTracker {
        private val sessionId: String = UUID.randomUUID().toString()

        override fun registrarFeatureUsada(
            featureId: String,
            sessionIdOverride: String?,
        ) {
            firebaseTracker.registrarFeatureUsada(featureId)
            enviarEvento(name = "feature_used", featureId = featureId, sessionIdOverride = sessionIdOverride)
        }

        override fun registrarScreenView(screenName: String) {
            firebaseTracker.registrarScreenView(screenName)
            enviarEvento(name = "screen_view", screenName = screenName)
        }

        override fun registrarSessionStart() {
            firebaseTracker.registrarSessionStart()
            enviarEvento(name = "session_start")
        }

        override fun registrarFeatureCrash(
            featureId: String,
            errorType: String,
        ) {
            firebaseTracker.registrarFeatureCrash(featureId, errorType)
            enviarEvento(name = "feature_crash", featureId = featureId, errorType = errorType)
        }

        override fun registrarBatterySnapshot(
            level: Int,
            charging: Boolean,
        ) {
            firebaseTracker.registrarBatterySnapshot(level, charging)
            enviarEvento(name = "battery_snapshot", batteryLevel = level, batteryCharging = charging)
        }

        private fun enviarEvento(
            name: String,
            featureId: String? = null,
            screenName: String? = null,
            errorType: String? = null,
            batteryLevel: Int? = null,
            batteryCharging: Boolean? = null,
            // GH#919 — quando presente, correlaciona o evento a diagnostic_sessions.id
            // (mesmo id gravado em ai_usage.session_id) em vez do UUID de instancia.
            sessionIdOverride: String? = null,
        ) {
            applicationScope.launch {
                val distChannel = distributionChannel(context)
                val deviceId =
                    runCatching {
                        preferenciasAppRepository.buscarOuGerarAnonDeviceId()
                    }.getOrDefault("unknown")
                adminIngestRepository.sendAnalyticsEvent(
                    AnalyticsEventIngestPayload(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        sessionId = sessionIdOverride ?: sessionId,
                        appVersion = BuildConfig.VERSION_NAME,
                        featureId = featureId,
                        screenName = screenName,
                        errorType = errorType,
                        batteryLevel = batteryLevel,
                        batteryCharging = batteryCharging,
                        environment = environmentFor(distChannel),
                        distChannel = distChannel,
                        buildType = BuildConfig.BUILD_TYPE,
                        versionCode = BuildConfig.VERSION_CODE,
                        deviceId = deviceId,
                    ),
                )
            }
        }
    }
