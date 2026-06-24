package io.veloo.app.featureflags

import io.veloo.app.core.datastore.FeatureFlagStore

class FakePreferenciasAppRepository : FeatureFlagStore {
    private var flagsMap: Map<String, Boolean> = emptyMap()

    override suspend fun salvarFeatureFlags(flags: Map<String, Boolean>) {
        flagsMap = flags
    }

    override suspend fun buscarFeatureFlags(): Map<String, Boolean> = flagsMap
}
