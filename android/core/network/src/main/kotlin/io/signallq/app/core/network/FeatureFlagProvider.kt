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
 *
 * Flag GH#1444 (parte de #952): feature_diagnostic_shadow_mode — kill switch do
 * shadow mode de diagnostico (comparacao local-vs-remoto, nunca altera o que a
 * UI mostra). Reusa este mecanismo em vez de duplicar um novo enquanto #1347
 * (Firebase Remote Config) nao esta pronto para governa-lo — ver kdoc de
 * `DiagnosticDivergenceReporter` em `:featureDiagnostico`.
 *
 * Flag GH#1464 (parte de #951): feature_provider_directory_enabled — kill switch do
 * diretorio remoto de operadoras (`ProviderDirectoryRepository`/`OperadoraDirectoryResolver`,
 * GH#965). Desligada, o resolver pula direto do catalogo local pro fallback generico, sem
 * tentar rede — nivel 1 (catalogo local) nunca e afetado.
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

    // --- Flag GH#1444 (shadow mode, #952) ---
    fun isDiagnosticShadowModeEnabled(): Boolean = isEnabled("feature_diagnostic_shadow_mode")

    // --- Flag GH#1464 (diretorio remoto de operadoras, #951) ---
    fun isProviderDirectoryEnabled(): Boolean = isEnabled("feature_provider_directory_enabled")

    // --- Flags legadas (mantidas por compatibilidade) ---
    fun isAiDiagnosisEnabled(): Boolean = isEnabled("ai_diagnosis_enabled")

    fun isSpeedtestEnabled(): Boolean = isEnabled("speedtest_enabled")

    fun isFibraModuleEnabled(): Boolean = isEnabled("fibra_module_enabled")
}
