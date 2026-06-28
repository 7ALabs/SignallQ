package io.signallq.app.feature.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes unitários para [OuiDatabase].
 * Verifica:
 *  - Lookups corretos de fabricantes conhecidos.
 *  - Resolução de colisões corrigidas (2C56DC → TP-Link; 002275 → Netgear).
 *  - Insensibilidade a case e formato do MAC.
 *  - Retorno null para OUIs desconhecidos.
 */
class OuiDatabaseTest {

    @Test
    fun `lookup Apple por MAC completo retorna Apple`() {
        val resultado = OuiDatabase.lookupFabricante("00:03:93:AA:BB:CC")
        assertEquals("Apple", resultado)
    }

    @Test
    fun `lookup Samsung por MAC com hifen retorna Samsung`() {
        val resultado = OuiDatabase.lookupFabricante("00-12-47-AA-BB-CC")
        assertEquals("Samsung", resultado)
    }

    @Test
    fun `lookup TP-Link no OUI 2C56DC retorna TP-Link (colisao corrigida)`() {
        // 2C56DC pertence à TP-Link (IEEE OUI registry).
        // Antes da correção, ASUS sobrescrevia silenciosamente esse entry.
        val resultado = OuiDatabase.lookupFabricante("2C:56:DC:AA:BB:CC")
        assertEquals("TP-Link", resultado)
    }

    @Test
    fun `lookup Netgear no OUI 002275 retorna Netgear (colisao corrigida)`() {
        // 002275 pertence à Netgear (IEEE OUI registry).
        // Antes da correção, Qualcomm sobrescrevia silenciosamente esse entry.
        val resultado = OuiDatabase.lookupFabricante("00:22:75:AA:BB:CC")
        assertEquals("Netgear", resultado)
    }

    @Test
    fun `lookup ASUS por OUI que nao e 2C56DC retorna ASUS`() {
        // 001A92 é legitimamente ASUS
        val resultado = OuiDatabase.lookupFabricante("00:1A:92:AA:BB:CC")
        assertEquals("ASUS", resultado)
    }

    @Test
    fun `lookup Qualcomm por OUI que nao e 002275 retorna Qualcomm`() {
        // 08865D é legitimamente Qualcomm
        val resultado = OuiDatabase.lookupFabricante("08:86:5D:AA:BB:CC")
        assertEquals("Qualcomm", resultado)
    }

    @Test
    fun `lookup Google retorna Google`() {
        val resultado = OuiDatabase.lookupFabricante("60:A4:D0:AA:BB:CC")
        assertEquals("Google", resultado)
    }

    @Test
    fun `lookup Intelbras retorna Intelbras`() {
        val resultado = OuiDatabase.lookupFabricante("00:E0:9F:AA:BB:CC")
        assertEquals("Intelbras", resultado)
    }

    @Test
    fun `lookup OUI desconhecido retorna null`() {
        val resultado = OuiDatabase.lookupFabricante("FF:FF:FF:AA:BB:CC")
        assertNull(resultado)
    }

    @Test
    fun `lookup com mac null retorna null`() {
        val resultado = OuiDatabase.lookupFabricante(null)
        assertNull(resultado)
    }

    @Test
    fun `lookup com mac vazio retorna null`() {
        val resultado = OuiDatabase.lookupFabricante("")
        assertNull(resultado)
    }

    @Test
    fun `lookup case insensitive minusculas retorna fabricante`() {
        val resultado = OuiDatabase.lookupFabricante("2c:56:dc:aa:bb:cc")
        assertEquals("TP-Link", resultado)
    }

    @Test
    fun `lookup case insensitive maiusculas retorna fabricante`() {
        val resultado = OuiDatabase.lookupFabricante("2C:56:DC:AA:BB:CC")
        assertEquals("TP-Link", resultado)
    }

    @Test
    fun `lookup Ubiquiti retorna Ubiquiti`() {
        val resultado = OuiDatabase.lookupFabricante("00:15:6D:AA:BB:CC")
        assertEquals("Ubiquiti", resultado)
    }

    @Test
    fun `lookup ZTE retorna ZTE`() {
        val resultado = OuiDatabase.lookupFabricante("28:D8:55:AA:BB:CC")
        assertEquals("ZTE", resultado)
    }
}
