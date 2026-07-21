package io.signallq.app.feature.devices

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1217 item 5/9 — MAC localmente administrado (bit U/L) nunca gera fabricante real via OUI. */
class MacAddressUtilTest {

    @Test
    fun `MAC com bit UL setado e localmente administrado`() {
        // 0x02 = 0000 0010 -- bit U/L ligado.
        assertTrue(MacAddressUtil.ehLocalmenteAdministrado("02:00:00:00:00:01"))
        assertTrue(MacAddressUtil.ehLocalmenteAdministrado("06:AA:BB:CC:DD:EE"))
        assertTrue(MacAddressUtil.ehLocalmenteAdministrado("DA:A1:19:00:00:00"))
    }

    @Test
    fun `MAC com OUI real (bit UL desligado) nao e localmente administrado`() {
        // 00:1A:2B e um OUI real de exemplo -- primeiro byte par, bit 0x02 desligado.
        assertFalse(MacAddressUtil.ehLocalmenteAdministrado("00:1A:2B:CC:DD:EE"))
        assertFalse(MacAddressUtil.ehLocalmenteAdministrado("AC:DE:48:00:11:22"))
    }

    @Test
    fun `aceita separador hifen e case misto`() {
        assertTrue(MacAddressUtil.ehLocalmenteAdministrado("02-00-00-00-00-01"))
        assertTrue(MacAddressUtil.ehLocalmenteAdministrado("da:a1:19:00:00:00"))
    }

    @Test
    fun `mac invalido ou vazio nao lanca excecao -- retorna false`() {
        assertFalse(MacAddressUtil.ehLocalmenteAdministrado(""))
        assertFalse(MacAddressUtil.ehLocalmenteAdministrado("nao-e-um-mac"))
    }
}
