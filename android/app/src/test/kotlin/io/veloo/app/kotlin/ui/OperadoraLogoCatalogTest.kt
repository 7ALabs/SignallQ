package io.signallq.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SIG-292 — catalogo local de identidade visual de operadora.
 *
 * Reaproveita o catalogo/matcher de [BancoOperadoras] (SIG-293 / GH#411): estes testes
 * cobrem apenas o mapeamento `id -> identidade visual`, sem duplicar os testes de deteccao
 * ja cobertos em [BancoOperadorasTest].
 */
class OperadoraLogoCatalogTest {
    @Test
    fun `toda operadora do catalogo possui monograma nao vazio`() {
        BancoOperadoras.lista.forEach { operadora ->
            val identidade = OperadoraLogoCatalog.identidadePara(operadora)
            assertTrue(
                "Operadora ${operadora.id} sem monograma",
                identidade.monograma.isNotBlank(),
            )
        }
    }

    @Test
    fun `operadoras distintas do catalogo tendem a ter cores de marca distintas`() {
        val cores = BancoOperadoras.lista.map { OperadoraLogoCatalog.identidadePara(it).corMarca }
        // Vivo e Vero dividem monograma "V", mas as cores de marca continuam distintas.
        assertNotEquals(cores[0], cores[1])
    }

    @Test
    fun `operadora nao cadastrada no catalogo cai no fallback com monograma da primeira letra`() {
        val operadoraDesconhecida =
            ContatoOperadora(
                id = "xyz_nao_existe",
                nome = "Zeta Telecom",
                grupo = "Zeta",
                detectarPor = listOf("zeta"),
                sac = "0000",
                whatsapp = null,
                site = "https://www.zeta.com.br",
            )
        val identidade = OperadoraLogoCatalog.identidadePara(operadoraDesconhecida)
        assertEquals("Z", identidade.monograma)
    }

    @Test
    fun `vivo resolve para identidade visual com monograma V`() {
        val vivo = BancoOperadoras.lista.first { it.id == "vivo_fibra" }
        val identidade = OperadoraLogoCatalog.identidadePara(vivo)
        assertEquals("V", identidade.monograma)
    }

    // --- SIG-292 fase 2: logo oficial real (docs/brand-assets/operators-sources.md) ---

    @Test
    fun `operadoras com logo oficial disponivel expoem logoRes nao nulo`() {
        val comLogoOficial =
            setOf(
                "vivo_fibra",
                "claro_net",
                "tim_live",
                "nio",
                "algar",
                "unifique",
                "brisanet",
                "desktop",
                "ligga",
                "vero",
                "giga_mais",
                "topfibra",
                "coopertec_speed",
                "viamar",
                "west_telecom",
                "west_fibra",
                "fhp_fibra",
                "amigo",
                "wlinks",
            )
        comLogoOficial.forEach { id ->
            val operadora = BancoOperadoras.lista.first { it.id == id }
            val identidade = OperadoraLogoCatalog.identidadePara(operadora)
            assertTrue("Operadora $id deveria ter logoRes", identidade.logoRes != null)
        }
    }
}
