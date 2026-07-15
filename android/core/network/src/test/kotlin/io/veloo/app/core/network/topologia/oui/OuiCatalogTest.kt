package io.signallq.app.core.network.topologia.oui

import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de [OuiCatalog] — Fase 1 do plano de unificação de topologia (issue #975/#978).
 *
 * Cobre especificamente o critério de aceite "conflito Intelbras resolvido por schema, não por
 * regra ad-hoc" e o caso "mesmo fabricante do gateway mas não é o gateway".
 */
class OuiCatalogTest {

    // ─── Conflito Intelbras: um registro só, papéis múltiplos declarados ───────────

    @Test
    fun `OUI Intelbras C46E1F retorna um unico registro com papeis ROTEADOR e NO_MESH`() {
        val entry = OuiCatalog.lookup("C4:6E:1F:00:00:01")

        requireNotNull(entry)
        assertEquals("C46E1F", entry.prefixo)
        assertEquals(setOf(PapelTopologia.ROTEADOR, PapelTopologia.NO_MESH), entry.papeisPossiveis)
    }

    @Test
    fun `OUI Intelbras 6C5AB0 tambem retorna um unico registro com papeis ROTEADOR e NO_MESH`() {
        val entry = OuiCatalog.lookup("6C5AB0")

        requireNotNull(entry)
        assertEquals(setOf(PapelTopologia.ROTEADOR, PapelTopologia.NO_MESH), entry.papeisPossiveis)
    }

    @Test
    fun `papeis possiveis do conflito Intelbras nao dependem da ordem de consulta - resultado e sempre o mesmo conjunto`() {
        // Consultar 6C5AB0 antes de C46E1F (ou vice-versa) não muda o conteúdo do conjunto
        // retornado para nenhum dos dois — o catálogo é um Map por prefixo, não duas listas
        // sequenciais como antes (MESH_NO_OUIS consultado antes de GATEWAY_ISP_OUIS mudava o
        // resultado do `if` em cascata do TopologiaWifiEngine antigo).
        OuiCatalog.lookup("6C5AB0")
        val depoisDeConsultarOOutroOui = OuiCatalog.lookup("C46E1F")?.papeisPossiveis

        OuiCatalog.lookup("C46E1F")
        val depoisDeConsultarSiMesmoAntes = OuiCatalog.lookup("C46E1F")?.papeisPossiveis

        assertEquals(depoisDeConsultarSiMesmoAntes, depoisDeConsultarOOutroOui)
        assertEquals(setOf(PapelTopologia.ROTEADOR, PapelTopologia.NO_MESH), depoisDeConsultarOOutroOui)
    }

    // ─── Mesmo fabricante do gateway, mas não é o gateway ───────────────────────────

    @Test
    fun `OUI Intelbras de dispositivo cliente comum 00E09F tem fabricante Intelbras mas papeis possiveis vazio`() {
        // 00E09F é um bloco Intelbras genérico (equipamento de rede da marca que NÃO é o
        // roteador/ACMesh — ex. câmera IP, switch, outro produto da linha). Diferente de
        // C46E1F/6C5AB0 (linha de roteador/mesh), esse prefixo não deve carregar nenhum
        // papel de topologia — evita o falso positivo "mesmo fabricante = é o roteador".
        val entry = OuiCatalog.lookup("00:E0:9F:AA:BB:CC")

        requireNotNull(entry)
        assertEquals("Intelbras", entry.fabricante)
        assertTrue(entry.papeisPossiveis.isEmpty())
    }

    @Test
    fun `OUI generico sem nenhum registro retorna null`() {
        assertEquals(null, OuiCatalog.lookup("FF:FF:FF:AA:BB:CC"))
    }
}
