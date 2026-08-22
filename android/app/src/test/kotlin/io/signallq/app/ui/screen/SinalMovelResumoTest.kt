package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class SinalMovelResumoTest {
    @Test
    fun `destaque visual traduz a classificacao real sem promover qualidade desconhecida`() {
        assertEquals("Forte", destaqueSinalMovel("Excelente"))
        assertEquals("Regular", destaqueSinalMovel("Regular"))
        assertEquals("Fraco", destaqueSinalMovel("Ruim"))
        assertEquals("Indisponível", destaqueSinalMovel("Indisponível"))
    }
}
