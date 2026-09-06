package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class EquipamentoResumoTest {
    @Test
    fun `titulo do resumo preserva o papel real do equipamento`() {
        assertEquals("Roteador principal", tituloResumoEquipamento("roteador principal"))
        assertEquals("Ponto de acesso", tituloResumoEquipamento("ponto de acesso"))
    }
}
