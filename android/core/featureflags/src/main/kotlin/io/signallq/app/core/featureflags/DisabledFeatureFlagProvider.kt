package io.signallq.app.core.featureflags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Implementacao no-op de [FeatureFlagProvider] — toda flag sempre desligada
 * ([isEnabled] = `false`), sem fetch remoto/dependencia de Firebase nenhuma.
 *
 * Default seguro para pontos de instanciacao manual fora do grafo Hilt (ex.:
 * construtor default de
 * [io.signallq.app.feature.diagnostico.DiagnosticOrchestrator], NDS-02k, issue
 * #1759 — testes que nao precisam configurar comportamento de flag continuam
 * batendo em `false`, igual ao default do catalogo para toda flag nova) — mesmo
 * espirito de `io.signallq.app.core.network.NoOpAnalyticsHelper`. O grafo Hilt
 * de producao sempre injeta [RemoteConfigFeatureFlagProvider].
 */
object DisabledFeatureFlagProvider : FeatureFlagProvider {
    override fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue> =
        flowOf(
            FeatureFlagValue(
                key = key,
                raw = FeatureFlagRawValue.BooleanValue(false),
                source = FeatureFlagSource.DEFAULT,
            ),
        )

    override fun isEnabled(key: FeatureFlagKey): Boolean = false

    override suspend fun refresh(force: Boolean): FeatureFlagRefreshResult =
        FeatureFlagRefreshResult.Success(activated = false, fetchTimeMillis = null)
}
