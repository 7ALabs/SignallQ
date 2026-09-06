package io.signallq.app.core.featureflags

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Cobertura de [DisabledFeatureFlagProvider] (NDS-02k, issue #1759) — default seguro
 * para pontos de instanciação manual fora do grafo Hilt: toda flag sempre `false`.
 */
class DisabledFeatureFlagProviderTest {
    @Test
    fun `isEnabled e sempre false, para qualquer chave`() {
        assertFalse(DisabledFeatureFlagProvider.isEnabled(FeatureFlagKeys.CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED))
        assertFalse(DisabledFeatureFlagProvider.isEnabled(FeatureFlagKeys.CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED))
        assertFalse(DisabledFeatureFlagProvider.isEnabled(FeatureFlagKey("chave.inexistente")))
    }

    @Test
    fun `observe emite BooleanValue false com source DEFAULT`() =
        runTest {
            val value = DisabledFeatureFlagProvider.observe(FeatureFlagKeys.CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED).first()

            assertEquals(FeatureFlagRawValue.BooleanValue(false), value.raw)
            assertEquals(FeatureFlagSource.DEFAULT, value.source)
        }

    @Test
    fun `refresh nunca busca rede, devolve Success com activated false`() =
        runTest {
            val result = DisabledFeatureFlagProvider.refresh() as FeatureFlagRefreshResult.Success

            assertFalse(result.activated)
        }
}
