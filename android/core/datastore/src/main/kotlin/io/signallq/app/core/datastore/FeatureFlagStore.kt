package io.signallq.app.core.datastore

/**
 * Contrato minimo para persistencia de feature flags remotas.
 * Implementado por PreferenciasAppRepository.
 * Isolado para permitir fake em testes sem instanciar Context/DataStore.
 */
interface FeatureFlagStore {
    suspend fun salvarFeatureFlags(flags: Map<String, Boolean>)
    suspend fun buscarFeatureFlags(): Map<String, Boolean>
}
