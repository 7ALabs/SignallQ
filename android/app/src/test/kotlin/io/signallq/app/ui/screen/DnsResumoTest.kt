package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsResumoTest {
    @Test
    fun `titulo so afirma resposta apos existir comparacao`() {
        assertEquals("Compare servidores DNS", tituloResumoDns(false))
        assertEquals("Seu DNS responde bem.", tituloResumoDns(true))
    }
}
