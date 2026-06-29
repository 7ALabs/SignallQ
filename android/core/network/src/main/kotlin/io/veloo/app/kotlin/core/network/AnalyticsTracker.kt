package io.signallq.app.core.network

/**
 * Contrato de rastreamento de eventos de analytics.
 *
 * Implementado por FirebaseAnalyticsTracker (:app).
 * Consumido pelos modulos feature sem dependencia direta de Firebase.
 *
 * Schema de eventos GA4 — SIG-134:
 * - feature_used:     feature_id, session_id, app_version, timestamp
 * - screen_view:      screen_name, session_id, app_version
 * - app_session_start:  session_id, app_version
 * - feature_crash:    feature_id, error_type, app_version
 * - battery_snapshot: level, charging, session_id
 *
 * Sem PII — session_id e anonimo (UUID por sessao de app).
 */
interface AnalyticsTracker {
    fun registrarFeatureUsada(featureId: String)
    fun registrarScreenView(screenName: String)
    fun registrarSessionStart()
    fun registrarFeatureCrash(featureId: String, errorType: String)
    fun registrarBatterySnapshot(level: Int, charging: Boolean)
}
