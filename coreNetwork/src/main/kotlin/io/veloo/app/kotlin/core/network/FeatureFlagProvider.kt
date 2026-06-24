package io.veloo.app.core.network

/**
 * Contrato de acesso a feature flags remotas.
 *
 * Implementado pelo FeatureFlagManager (modulo app).
 * Consumido pelos modulos feature (ex: featureDiagnostico) sem criar
 * dependência de :app — segue a lei de dependências do projeto.
 *
 * Fallback padrao: true para qualquer flag desconhecida.
 */
interface FeatureFlagProvider {
    fun isEnabled(key: String): Boolean
    fun isAiDiagnosisEnabled(): Boolean = isEnabled("ai_diagnosis_enabled")
    fun isSpeedtestEnabled(): Boolean = isEnabled("speedtest_enabled")
    fun isFibraModuleEnabled(): Boolean = isEnabled("fibra_module_enabled")
}
