package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regressão GH#515 — "5G NSA"/"5G SA" (jargão de operadora) não deve aparecer em texto
 * voltado ao usuário leigo fora de telas de detalhe técnico.
 */
class HomeScreenHelpersTest {
    @Test
    fun `5G NSA simplifica para 5G`() {
        assertEquals("5G", tecnologiaSimplificada("5G NSA"))
    }

    @Test
    fun `5G SA simplifica para 5G`() {
        assertEquals("5G", tecnologiaSimplificada("5G SA"))
    }

    @Test
    fun `4G sem sufixo permanece 4G`() {
        assertEquals("4G", tecnologiaSimplificada("4G"))
    }

    @Test
    fun `nulo ou vazio retorna null`() {
        assertNull(tecnologiaSimplificada(null))
        assertNull(tecnologiaSimplificada(""))
    }
}
