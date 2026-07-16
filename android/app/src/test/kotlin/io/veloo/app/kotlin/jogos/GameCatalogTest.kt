package io.signallq.app.jogos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCatalogTest {
    @Test
    fun `catalogo tem os 16 jogos do spec`() {
        assertEquals(16, CatalogoJogos.todos.size)
    }

    @Test
    fun `gameId sao unicos`() {
        val ids = CatalogoJogos.todos.map { it.gameId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `exclusivos PC nao aparecem em PS5 ou Xbox`() {
        val exclusivos = setOf("valorant", "league_of_legends", "counter_strike_2", "dota_2")
        CatalogoJogos.todos.filter { it.gameId in exclusivos }.forEach { jogo ->
            assertEquals(setOf(Plataforma.PC), jogo.plataformas)
        }
    }

    @Test
    fun `jogos multiplataforma cobrem PC PS5 e Xbox`() {
        val multiplataforma = CatalogoJogos.todos.filter { it.gameId == "fortnite" }.single()
        assertEquals(setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), multiplataforma.plataformas)
    }

    @Test
    fun `filtro por plataforma PS5 nao inclui exclusivos PC`() {
        val jogosPs5 = CatalogoJogos.porPlataforma(Plataforma.PS5)
        assertEquals(12, jogosPs5.size)
        assertTrue(jogosPs5.none { it.gameId == "valorant" })
    }

    @Test
    fun `estrategia declarada PROVIDER_NETWORK so nos 4 exclusivos PC`() {
        val providerNetwork = CatalogoJogos.todos.filter { it.estrategiaDeclarada == EstrategiaTeste.PROVIDER_NETWORK }
        assertEquals(4, providerNetwork.size)
        assertEquals(setOf("valorant", "league_of_legends", "counter_strike_2", "dota_2"), providerNetwork.map { it.gameId }.toSet())
    }
}
