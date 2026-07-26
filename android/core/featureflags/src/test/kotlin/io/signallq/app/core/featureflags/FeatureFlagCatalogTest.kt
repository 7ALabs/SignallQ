package io.signallq.app.core.featureflags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeatureFlagCatalogTest {
    private val definicaoSpeedtest =
        FeatureFlagDefinition(
            key = FeatureFlagKey("consumer.speedtest.enabled"),
            module = ":featureSpeedtest",
            type = FeatureFlagType.BOOLEAN,
            defaultValue = FeatureFlagRawValue.BooleanValue(true),
            criticality = FeatureFlagCriticality.HIGH,
            owner = "product-signallq",
            description = "desc",
            disabledBehavior = FeatureFlagDisabledBehavior.HIDE_ENTRY_AND_BLOCK_ROUTE,
            disabledMessage = null,
            dependencies = emptyList(),
            androidImplemented = false,
            adminManaged = true,
            analyticsEvent = null,
        )

    @Test
    fun `defaultValueFor retorna valor do catalogo com origem DEFAULT`() {
        val catalog = FeatureFlagCatalog(listOf(definicaoSpeedtest))

        val valor = catalog.defaultValueFor(definicaoSpeedtest.key)

        assertEquals(FeatureFlagRawValue.BooleanValue(true), valor?.raw)
        assertEquals(FeatureFlagSource.DEFAULT, valor?.source)
    }

    @Test
    fun `defaultValueFor retorna null para chave fora do catalogo`() {
        val catalog = FeatureFlagCatalog(emptyList())

        assertNull(catalog.defaultValueFor(definicaoSpeedtest.key))
    }

    @Test
    fun `defaultsAsValues cobre todas as definicoes`() {
        val catalog = FeatureFlagCatalog(listOf(definicaoSpeedtest))

        val valores = catalog.defaultsAsValues()

        assertEquals(1, valores.size)
        assertEquals(FeatureFlagSource.DEFAULT, valores.getValue(definicaoSpeedtest.key).source)
    }

    @Test
    fun `toRemoteConfigDefaultsMap gera mapa nativo para setDefaultsAsync`() {
        val catalog = FeatureFlagCatalog(listOf(definicaoSpeedtest))

        val mapa = catalog.toRemoteConfigDefaultsMap()

        assertEquals(mapOf("consumer.speedtest.enabled" to true), mapa)
    }
}
