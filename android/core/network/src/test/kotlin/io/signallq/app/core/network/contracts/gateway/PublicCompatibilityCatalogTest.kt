package io.signallq.app.core.network.contracts.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitarios de [PublicCompatibilityCatalog] — cobre os criterios de
 * aceite de GH#539 ("lista publica de compatibilidade nao mistura validado
 * com inferido").
 */
class PublicCompatibilityCatalogTest {

    @Test
    fun `catalogo diferencia validado, importado e inferido`() {
        val catalogo = PublicCompatibilityCatalog.montar()

        val idsValidados = catalogo.validado.map { it.driverId }
        val idsExperimentais = catalogo.experimental.map { it.driverId }

        assertTrue("nokia-g1425g-b" in idsValidados)
        assertTrue("tplink-archer-c20" in idsValidados)
        assertTrue("tplink-archer-c6" in idsValidados)
        assertTrue("tplink-stok-luci-generic" in idsExperimentais)
        assertTrue("mesh-generic" in idsExperimentais)
    }

    @Test
    fun `lista publica nao mistura validado com inferido`() {
        val catalogo = PublicCompatibilityCatalog.montar()

        val idsValidados = catalogo.validado.map { it.driverId }.toSet()
        val idsExperimentais = catalogo.experimental.map { it.driverId }.toSet()

        assertTrue(idsValidados.intersect(idsExperimentais).isEmpty())
    }

    @Test
    fun `nenhum modelo externo entra como LAB_VALIDATED sem entrada curada no catalogo`() {
        val catalogoCustom = listOf(
            DeviceDriverProfile(
                driverId = "externo-nao-testado",
                vendor = "Generico",
                modelPatterns = listOf("qualquer"),
                deviceType = DeviceType.ROUTER,
                supportLevel = SupportLevel.PARSER_IMPORTED,
            ),
        )

        val catalogo = PublicCompatibilityCatalog.montar(catalogoCustom)

        assertTrue(catalogo.validado.isEmpty())
        assertEquals(1, catalogo.experimental.size)
        assertEquals("externo-nao-testado", catalogo.experimental.first().driverId)
    }

    @Test
    fun `entrada UNKNOWN nunca aparece na lista publica, mesmo que o catalogo a contenha`() {
        val catalogoCustom = listOf(
            DeviceDriverProfile(
                driverId = "sem-driver-conhecido",
                vendor = "Desconhecido",
                deviceType = DeviceType.UNKNOWN_SUPPORTED,
                supportLevel = SupportLevel.UNKNOWN,
            ),
        )

        val catalogo = PublicCompatibilityCatalog.montar(catalogoCustom)

        assertTrue(catalogo.validado.isEmpty())
        assertTrue(catalogo.experimental.isEmpty())
    }

    @Test
    fun `entradas validadas usam nome de exibicao amigavel, nao o pattern de match cru`() {
        val catalogo = PublicCompatibilityCatalog.montar()

        val nokia = catalogo.validado.first { it.driverId == "nokia-g1425g-b" }
        assertEquals("G-1425G-B", nokia.modelo)
        assertEquals(DeviceType.ONT_GPON, nokia.deviceType)

        val c20 = catalogo.validado.first { it.driverId == "tplink-archer-c20" }
        assertEquals("Archer C20", c20.modelo)
    }
}
