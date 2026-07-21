package io.signallq.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GH#1226 item D/5 — única função de normalização de WhatsApp local, usada tanto por
 * [ResolvedOperadoraContact.whatsappUrl] quanto pela linha de "outras operadoras" (antes
 * cada um fazia essa conta de um jeito diferente).
 */
class NormalizarWhatsappLocalTest {
    @Test
    fun `numero local simples ganha prefixo 55`() {
        assertEquals("https://wa.me/5511999151515", normalizarWhatsappLocal("11999151515"))
    }

    @Test
    fun `numero ja com 55 nao duplica o codigo do pais`() {
        assertEquals("https://wa.me/5511999151515", normalizarWhatsappLocal("5511999151515"))
    }

    @Test
    fun `numero com mais (+55) e normalizado sem duplicar`() {
        assertEquals("https://wa.me/5511999151515", normalizarWhatsappLocal("+5511999151515"))
    }

    @Test
    fun `numero com espacos hifens e parenteses e normalizado`() {
        assertEquals("https://wa.me/5511999151515", normalizarWhatsappLocal("(11) 99915-1515"))
    }

    @Test
    fun `numero nulo ou em branco retorna null`() {
        assertNull(normalizarWhatsappLocal(null))
        assertNull(normalizarWhatsappLocal(""))
        assertNull(normalizarWhatsappLocal("   "))
    }
}
