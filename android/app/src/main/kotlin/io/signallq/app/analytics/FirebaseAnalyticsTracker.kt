package io.signallq.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.app.BuildConfig
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.core.network.AssistAbandonado
import io.signallq.app.core.network.AssistObjetivoSelecionado
import io.signallq.app.core.network.AssistPerguntaRespondida
import io.signallq.app.core.network.DiagnosticoPlanoIniciado
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

        override fun registrarSessionEnd() {
            firebaseAnalytics.logEvent(
                "app_session_end",
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

        // GH#1480 (Epico #1347, F4) — feature_id e sempre um identificador curto de modulo
        // (ex.: "wifi", "dns"), nunca a chave completa do catalogo nem dado de usuario.
        override fun registrarFeatureBloqueadaRemota(featureId: String) {
            firebaseAnalytics.logEvent(
                "feature_blocked_remote",
                Bundle().apply {
                    putString("feature_id", featureId)
                    putString("session_id", sessionId)
                    putString("app_version", appVersion)
                },
            )
        }

        override fun registrarAssistObjetivo(evento: AssistObjetivoSelecionado) {
            firebaseAnalytics.logEvent(
                "diagnostico_objetivo_selecionado",
                Bundle().apply {
                    putString("objetivo", evento.objetivoId)
                    putString("origem", evento.origem.analyticsId)
                    putBoolean("retomada", evento.retomada)
                },
            )
        }

        override fun registrarAssistResposta(evento: AssistPerguntaRespondida) {
            firebaseAnalytics.logEvent(
                "diagnostico_pergunta_respondida",
                Bundle().apply {
                    putString("objetivo", evento.objetivoId)
                    putString("pergunta_id", evento.perguntaId)
                    putString("resposta_id", evento.respostaId)
                    putBoolean("retomada", evento.retomada)
                },
            )
        }

        override fun registrarAssistAbandono(evento: AssistAbandonado) {
            firebaseAnalytics.logEvent(
                "diagnostico_guiado_abandonado",
                Bundle().apply {
                    putString("etapa", evento.etapa.analyticsId)
                    evento.objetivoId?.let { putString("objetivo", it) }
                    putBoolean("retomavel", evento.retomavel)
                },
            )
        }

        override fun registrarDiagnosticoPlanoIniciado(evento: DiagnosticoPlanoIniciado) {
            firebaseAnalytics.logEvent(
                "diagnostico_plano_iniciado",
                Bundle().apply {
                    putString("analise_id", evento.analiseId)
                    putString("objetivo", evento.objetivoId)
                    putString("capacidades", evento.capacidades)
                    putLong("qtd_capacidades", evento.qtdCapacidades)
                    putBoolean("plano_adaptado", evento.planoAdaptado)
                },
            )
        }
    }
