package io.signallq.app.ui.relatorio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelatorioPrivacidadeTest {
    @Test
    fun `mascara ultimo octeto do ip local`() {
        assertEquals("192.168.1.*", RelatorioPrivacidade.mascararIpLocal("192.168.1.100"))
    }

    @Test
    fun `mascara dois ultimos octetos do ip publico`() {
        assertEquals("200.150.*.*", RelatorioPrivacidade.mascararIpPublico("200.150.10.20"))
    }

    @Test
    fun `mascara ssid mantendo so os 2 primeiros caracteres`() {
        assertEquals("Ca******", RelatorioPrivacidade.mascararSsid("Casa da Familia"))
    }

    @Test
    fun `ssid curto vira asteriscos`() {
        assertEquals("A***", RelatorioPrivacidade.mascararSsid("Ab"))
    }

    @Test
    fun `null ou vazio retorna null`() {
        assertNull(RelatorioPrivacidade.mascararIpLocal(null))
        assertNull(RelatorioPrivacidade.mascararIpLocal(""))
        assertNull(RelatorioPrivacidade.mascararSsid(null))
    }

    @Test
    fun `ipv6 ou formato nao ipv4 retorna sem alteracao`() {
        assertEquals("fe80::1", RelatorioPrivacidade.mascararIpLocal("fe80::1"))
    }
}
