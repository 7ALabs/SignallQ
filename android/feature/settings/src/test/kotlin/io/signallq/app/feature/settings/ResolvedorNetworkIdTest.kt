package io.signallq.app.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GH#1227 item 3/RF-A — networkId nunca deve colidir entre redes diferentes nem cair pra
 *  um valor global/genérico quando há sinal de rede real disponível. */
class ResolvedorNetworkIdTest {

    @Test
    fun `bssid valido gera id estavel e normalizado em minusculas`() {
        val id1 = ResolvedorNetworkId.paraWifi(ssid = "MinhaRede", bssid = "AA:BB:CC:DD:EE:FF")
        val id2 = ResolvedorNetworkId.paraWifi(ssid = "MinhaRede", bssid = "aa:bb:cc:dd:ee:ff")
        assertEquals(id1, id2)
    }

    @Test
    fun `redes com SSID igual mas BSSID diferente geram ids diferentes`() {
        val id1 = ResolvedorNetworkId.paraWifi(ssid = "Wifi Publico", bssid = "AA:BB:CC:00:00:01")
        val id2 = ResolvedorNetworkId.paraWifi(ssid = "Wifi Publico", bssid = "AA:BB:CC:00:00:02")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `sem bssid disponivel cai para ssid, ainda vinculado a rede`() {
        val id = ResolvedorNetworkId.paraWifi(ssid = "MinhaRede", bssid = null)
        assertEquals("wifi-ssid:MinhaRede", id)
    }

    @Test
    fun `bssid placeholder do Android sem permissao de localizacao e ignorado`() {
        val id = ResolvedorNetworkId.paraWifi(ssid = "MinhaRede", bssid = "02:00:00:00:00:00")
        assertEquals("wifi-ssid:MinhaRede", id)
    }

    @Test
    fun `sem ssid nem bssid retorna null`() {
        assertNull(ResolvedorNetworkId.paraWifi(ssid = null, bssid = null))
    }

    @Test
    fun `rede movel usa identificador de operadora, nunca vazio`() {
        val id = ResolvedorNetworkId.paraRedeMovel("Vivo")
        assertEquals("movel:Vivo", id)
        assertNull(ResolvedorNetworkId.paraRedeMovel(null))
        assertNull(ResolvedorNetworkId.paraRedeMovel("  "))
    }
}
