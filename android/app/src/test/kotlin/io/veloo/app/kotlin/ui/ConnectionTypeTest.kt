package io.signallq.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** GH#1226 item 10/RF-J — variações de string livre que antes quebravam a comparação direta. */
class ConnectionTypeTest {
    @Test
    fun `movel (valor real produzido hoje) e MOBILE`() {
        assertEquals(ConnectionType.MOBILE, ConnectionType.parse("movel"))
    }

    @Test
    fun `variacoes mobile 4G 5G celular tambem sao MOBILE`() {
        assertEquals(ConnectionType.MOBILE, ConnectionType.parse("mobile"))
        assertEquals(ConnectionType.MOBILE, ConnectionType.parse("celular"))
        assertEquals(ConnectionType.MOBILE, ConnectionType.parse("4G"))
        assertEquals(ConnectionType.MOBILE, ConnectionType.parse("5G"))
        assertEquals(ConnectionType.MOBILE, ConnectionType.parse("LTE 4G"))
    }

    @Test
    fun `wifi e ethernet sao reconhecidos`() {
        assertEquals(ConnectionType.WIFI, ConnectionType.parse("wifi"))
        assertEquals(ConnectionType.ETHERNET, ConnectionType.parse("ethernet"))
    }

    @Test
    fun `valor nulo ou desconhecido nunca vira MOBILE por engano`() {
        assertEquals(ConnectionType.UNKNOWN, ConnectionType.parse(null))
        assertEquals(ConnectionType.UNKNOWN, ConnectionType.parse("desconhecido"))
        assertEquals(ConnectionType.UNKNOWN, ConnectionType.parse("desconectado"))
    }

    @Test
    fun `case insensitive`() {
        assertEquals(ConnectionType.MOBILE, ConnectionType.parse("MOVEL"))
        assertEquals(ConnectionType.WIFI, ConnectionType.parse("WiFi"))
    }
}
