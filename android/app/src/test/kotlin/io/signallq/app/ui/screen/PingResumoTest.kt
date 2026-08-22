package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class PingResumoTest {
    @Test
    fun `veredito do destaque usa o classificador de latencia e prioriza perdas`() {
        assertEquals("Excelente", vereditoPing(latenciaMs = 10.0, perdaPercentual = 0.0))
        assertEquals("Com perda de dados", vereditoPing(latenciaMs = 10.0, perdaPercentual = 1.0))
        assertEquals("Ruim", vereditoPing(latenciaMs = 500.0, perdaPercentual = 0.0))
    }
}
