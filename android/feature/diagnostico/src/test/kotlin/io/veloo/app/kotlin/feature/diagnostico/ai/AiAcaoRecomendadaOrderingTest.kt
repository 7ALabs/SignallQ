package io.signallq.app.feature.diagnostico.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `ordenadasPorPrioridade` — extraida de duplicacao entre `AnaliseDetalhadaBottomSheet.kt`
 * (fluxo "Analisar meu problema com IA") e `ResultadoVelocidadeScreen.kt` (tela 1a,
 * "Analise detalhada"). As duas telas escolhem a acao de maior prioridade pra
 * destacar num card compacto — GH#design-tobe-alinhamento, 2026-07-16.
 */
class AiAcaoRecomendadaOrderingTest {

    private fun acao(titulo: String, prioridade: String) =
        AiAcaoRecomendada(titulo = titulo, descricao = "desc $titulo", prioridade = prioridade)

    @Test
    fun `alta vem antes de media e baixa`() {
        val acoes = listOf(
            acao("baixa-1", "baixa"),
            acao("alta-1", "alta"),
            acao("media-1", "media"),
        )

        val ordenadas = acoes.ordenadasPorPrioridade()

        assertEquals(listOf("alta-1", "media-1", "baixa-1"), ordenadas.map { it.titulo })
    }

    @Test
    fun `prioridade desconhecida cai no meio, junto com media`() {
        val acoes = listOf(
            acao("desconhecida-1", "outra_coisa"),
            acao("alta-1", "alta"),
            acao("baixa-1", "baixa"),
        )

        val ordenadas = acoes.ordenadasPorPrioridade()

        assertEquals("alta-1", ordenadas.first().titulo)
        assertEquals("baixa-1", ordenadas.last().titulo)
    }

    @Test
    fun `lista vazia nao quebra`() {
        assertEquals(emptyList<AiAcaoRecomendada>(), emptyList<AiAcaoRecomendada>().ordenadasPorPrioridade())
    }
}
