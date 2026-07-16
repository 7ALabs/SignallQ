package io.signallq.app.ui.screen

import io.signallq.app.core.recommendation.RecommendationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#536 — orientação por tipo de rede exibida no diagnóstico detalhado deve
 * diferenciar Wi-Fi, móvel (com tecnologia) e caso não identificado.
 */
class ResultadoVelocidadeScreenTest {
    @Test
    fun `wifi menciona Wi-Fi na orientacao`() {
        val texto = orientacaoPorTipoDeRede("wifi", null)
        assertTrue(texto.contains("Wi-Fi"))
    }

    @Test
    fun `movel com 5G menciona 5G na orientacao`() {
        val texto = orientacaoPorTipoDeRede("movel", "5G NSA")
        assertTrue(texto.contains("5G"))
    }

    @Test
    fun `movel com 4G LTE menciona 4G na orientacao`() {
        val texto = orientacaoPorTipoDeRede("movel", "4G LTE")
        assertTrue(texto.contains("4G"))
    }

    @Test
    fun `movel sem tecnologia informada usa rede movel generica`() {
        val texto = orientacaoPorTipoDeRede("movel", null)
        assertTrue(texto.contains("rede móvel"))
    }

    @Test
    fun `tipo desconhecido pede para repetir o teste`() {
        val texto = orientacaoPorTipoDeRede(null, null)
        assertTrue(texto.contains("não identificado"))
    }

    // GH#813 — badge de tipo da recomendacao do Recommendation Engine (RecommendationType).
    @Test
    fun `free tip mostra rotulo dica gratuita`() {
        assertEquals("DICA GRATUITA", recommendationTypeLabel(RecommendationType.FREE_TIP))
    }

    @Test
    fun `tutorial mostra rotulo tutorial`() {
        assertEquals("TUTORIAL", recommendationTypeLabel(RecommendationType.TUTORIAL))
    }

    @Test
    fun `configuration mostra rotulo configuracao`() {
        assertEquals("CONFIGURAÇÃO", recommendationTypeLabel(RecommendationType.CONFIGURATION))
    }

    @Test
    fun `todos os tipos monetizados tem rotulo distinto e nao vazio`() {
        val monetizados =
            listOf(
                RecommendationType.AFFILIATE_PRODUCT,
                RecommendationType.PARTNER_OFFER,
                RecommendationType.OPERATOR_OFFER,
                RecommendationType.NATIVE_AD_FALLBACK,
            )
        val rotulos = monetizados.map { recommendationTypeLabel(it) }
        assertTrue(rotulos.all { it.isNotBlank() })
        assertEquals(rotulos.distinct().size, rotulos.size)
    }

    // Follow-up Lia (PR #1013) — titulo do AnalisadorEntryRow deve diferenciar o laudo
    // disparado automaticamente pela tela 1a (sem problema relatado) da analise que o
    // usuario pediu por sintoma escolhido.
    @Test
    fun `sem problema relatado mostra laudo pronto`() {
        assertEquals("Laudo pronto — toque para ver", tituloResultadoAnalisadorEntryRow(null))
    }

    @Test
    fun `com problema relatado mostra ver analise completa`() {
        assertEquals("Ver análise completa", tituloResultadoAnalisadorEntryRow("Baixa velocidade"))
    }
}
