package io.signallq.app.core.network

/**
 * Contrato de acesso a feature flags remotas.
 *
 * Implementado pelo FeatureFlagManager (modulo app).
 * Consumido pelos modulos feature sem criar dependência de :app.
 *
 * Fallback padrao: true para qualquer flag desconhecida.
 *
 * Flags SIG-13 (endpoint /flags): feature_speedtest, feature_wifi,
 * feature_diagnostico_ia, feature_dns, feature_fibra, feature_devices.
 *
 * Flags legadas (endpoint /feature-flags): ai_diagnosis_enabled,
 * speedtest_enabled, fibra_module_enabled — mantidas por compatibilidade.
 */
interface FeatureFlagProvider {
    fun isEnabled(key: String): Boolean

    // --- Flags SIG-13 ---
    fun isFeatureSpeedtestEnabled(): Boolean = isEnabled("feature_speedtest")
    fun isFeatureWifiEnabled(): Boolean = isEnabled("feature_wifi")
    fun isFeatureDiagnosticoIaEnabled(): Boolean = isEnabled("feature_diagnostico_ia")
    fun isFeatureDnsEnabled(): Boolean = isEnabled("feature_dns")
    fun isFeatureFibraEnabled(): Boolean = isEnabled("feature_fibra")
    fun isFeatureDevicesEnabled(): Boolean = isEnabled("feature_devices")

    // --- Flags legadas (mantidas por compatibilidade) ---
    fun isAiDiagnosisEnabled(): Boolean = isEnabled("ai_diagnosis_enabled")
    fun isSpeedtestEnabled(): Boolean = isEnabled("speedtest_enabled")
    fun isFibraModuleEnabled(): Boolean = isEnabled("fibra_module_enabled")
}
