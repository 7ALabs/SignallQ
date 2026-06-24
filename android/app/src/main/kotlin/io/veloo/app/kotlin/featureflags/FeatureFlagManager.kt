package io.veloo.app.featureflags

import io.veloo.app.core.network.FeatureFlagProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de feature flags remoto. Singleton injetado via Hilt.
 *
 * Inicializado no startup via [inicializar]. Flags ficam disponíveis
 * imediatamente via StateFlow — o fallback (todos true) garante que
 * o app funcione normalmente enquanto o fetch não completa.
 *
 * Implementa [FeatureFlagProvider] para injeção nos módulos feature
 * sem criar dependência direta em :app.
 *
 * Uso:
 *   if (featureFlagManager.isAiDiagnosisEnabled()) { ... }
 */
@Singleton
class FeatureFlagManager
    @Inject
    constructor(
        private val repository: FeatureFlagRepository,
    ) : FeatureFlagProvider {
        private val _flags = MutableStateFlow<Map<String, Boolean>>(emptyMap())
        val flags: StateFlow<Map<String, Boolean>> = _flags

        /**
         * Chamado no startup (SignallQApplication). Carrega cache local
         * e dispara fetch em background — nunca bloqueia a UI.
         */
        fun inicializar(scope: CoroutineScope) {
            scope.launch {
                // Carrega do DataStore primeiro (resposta imediata)
                _flags.value = repository.lerFlags()
                // Busca do worker e atualiza
                repository.sincronizarFlags()
                _flags.value = repository.lerFlags()
            }
        }

        override fun isEnabled(key: String): Boolean = _flags.value[key] ?: true
    }
