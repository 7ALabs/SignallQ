package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Test

class RotuloConfiancaTest {
    @Test
    fun `0,30 (inconclusivo) e baixa`() {
        assertEquals(RotuloConfianca.BAIXA, RotuloConfianca.de(0.30))
    }

    @Test
    fun `0,65 (conclusivo fraco) e media`() {
        assertEquals(RotuloConfianca.MEDIA, RotuloConfianca.de(0.65))
    }

    @Test
    fun `0,88 e alta`() {
        assertEquals(RotuloConfianca.ALTA, RotuloConfianca.de(0.88))
    }

    @Test
    fun `0,90 (ok) e alta`() {
        assertEquals(RotuloConfianca.ALTA, RotuloConfianca.de(0.90))
    }

    @Test
    fun `limiares exatos ficam do lado alto`() {
        assertEquals(RotuloConfianca.MEDIA, RotuloConfianca.de(0.5))
        assertEquals(RotuloConfianca.ALTA, RotuloConfianca.de(0.8))
    }

    @Test
    fun `wireValue reusa vocabulario ja aprovado da telemetria`() {
        assertEquals("baixa", RotuloConfianca.BAIXA.wireValue)
        assertEquals("media", RotuloConfianca.MEDIA.wireValue)
        assertEquals("alta", RotuloConfianca.ALTA.wireValue)
    }
}
