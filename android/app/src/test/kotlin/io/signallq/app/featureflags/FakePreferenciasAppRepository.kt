package io.signallq.app.featureflags

import io.signallq.app.core.datastore.FeatureFlagStore

class FakePreferenciasAppRepository : FeatureFlagStore {
    private var flagsMap: Map<String, Boolean> = emptyMap()

    override suspend fun salvarFeatureFlags(flags: Map<String, Boolean>) {
        flagsMap = flags
    }

    override suspend fun buscarFeatureFlags(): Map<String, Boolean> = flagsMap
}
