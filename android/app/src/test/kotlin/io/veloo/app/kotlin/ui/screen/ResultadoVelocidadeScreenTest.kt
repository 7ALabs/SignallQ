package io.signallq.app.ui.screen

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
}
