package io.signallq.app.feature.diagnostico.topology

import io.signallq.app.feature.diagnostico.topology.lan.OuiVendorLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class OuiVendorLookupTest {

    private val ouiData = """
        50C7BF	TP-Link
        000000	Xerox
        001882	Huawei
        AC84C6	TP-Link
    """.trimIndent()

    private fun createLookup(): OuiVendorLookup =
        OuiVendorLookup { ByteArrayInputStream(ouiData.toByteArray(Charsets.UTF_8)) }

    @Test
    fun `lookup com MAC separado por dois-pontos retorna fabricante correto`() {
        val lookup = createLookup()
        assertEquals("TP-Link", lookup.lookup("50:C7:BF:11:22:33"))
    }

    @Test
    fun `lookup com MAC de OUI existente retorna valor nao nulo`() {
        val lookup = createLookup()
        assertNotNull(lookup.lookup("00:00:00:AA:BB:CC"))
    }

    @Test
    fun `lookup com MAC desconhecido retorna null`() {
        val lookup = createLookup()
        assertNull(lookup.lookup("FF:FF:FF:11:22:33"))
    }

    @Test
    fun `lookup com MAC sem separadores funciona corretamente`() {
        val lookup = createLookup()
        assertEquals("TP-Link", lookup.lookup("50C7BF112233"))
    }

    @Test
    fun `lookup com MAC separado por traco retorna fabricante correto`() {
        val lookup = createLookup()
        assertEquals("Huawei", lookup.lookup("00-18-82-AA-BB-CC"))
    }

    @Test
    fun `lookup e case insensitive para o MAC`() {
        val lookup = createLookup()
        assertEquals("TP-Link", lookup.lookup("50c7bf112233"))
    }
}
