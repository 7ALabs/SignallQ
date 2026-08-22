package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class DispositivosResumoTest {
    @Test
    fun `resumo usa singular e plural de acordo com a quantidade observada`() {
        assertEquals("1 aparelho nesta rede", tituloResumoDispositivos(1))
        assertEquals("5 aparelhos nesta rede", tituloResumoDispositivos(5))
    }
}
