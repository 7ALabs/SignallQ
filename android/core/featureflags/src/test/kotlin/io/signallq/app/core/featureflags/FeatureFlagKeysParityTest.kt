package io.signallq.app.core.featureflags

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garante que [FeatureFlagKeys] (constantes tipadas usadas em codigo) e
 * `consumer-catalog.json` (fonte da verdade dos metadados) nunca divergem --
 * critério de aceite da issue #1477 ("catalogo tipado sem strings de chave
 * soltas"). Toda flag nova adicionada ao catalogo em F4 precisa de uma
 * constante correspondente aqui, e vice-versa, ou este teste quebra.
 */
class FeatureFlagKeysParityTest {
    @Test
    fun `toda constante de FeatureFlagKeys existe no catalogo canonico`() {
        val catalog = FeatureFlagCatalogLoader.loadConsumerCatalog()

        val ausentes = FeatureFlagKeys.ALL.filter { catalog.definitionFor(it) == null }

        assertTrue("Constantes sem entrada no catalogo: $ausentes", ausentes.isEmpty())
    }

    @Test
    fun `toda entrada do catalogo canonico tem constante em FeatureFlagKeys`() {
        val catalog = FeatureFlagCatalogLoader.loadConsumerCatalog()

        val semConstante = catalog.definitions.map { it.key }.filter { it !in FeatureFlagKeys.ALL }

        assertTrue("Entradas do catalogo sem constante em FeatureFlagKeys: $semConstante", semConstante.isEmpty())
    }
}
