package io.signallq.app.ui

import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GH#531 — cobre o resumo de bandas Wi-Fi exibido no subtítulo do gateway
 *  em Ajustes ("Roteador e rede") e Dispositivos (GatewayItem). */
class GatewayResumoWifiTest {
    private fun rede(
        ssid: String?,
        frequenciaMhz: Int,
        bssid: String = "AA:BB:CC:DD:EE:${frequenciaMhz % 100}",
    ) = RedeVizinha(
        ssid = ssid,
        bssid = bssid,
        rssiDbm = -50,
        frequenciaMhz = frequenciaMhz,
        seguranca = SegurancaWifi.wpa2,
        larguraCanalMhz = 20,
    )

    @Test
    fun `sem ssid conectado retorna null`() {
        val redes = listOf(rede("Casa", 2437))
        assertNull(resumoBandasWifi(redes, ssidConectado = null))
    }

    @Test
    fun `sem redes com o ssid conectado retorna null`() {
        val redes = listOf(rede("Vizinho", 2437))
        assertNull(resumoBandasWifi(redes, ssidConectado = "Casa"))
    }

    @Test
    fun `roteador single-band 2,4GHz retorna so 2,4G`() {
        val redes = listOf(rede("Casa", 2437))
        assertEquals("2,4G", resumoBandasWifi(redes, ssidConectado = "Casa"))
    }

    @Test
    fun `roteador dual-band mesmo ssid retorna 2,4G mais 5G em ordem fixa`() {
        val redes =
            listOf(
                rede("Casa", 5180), // 5GHz cadastrada primeiro no scan
                rede("Casa", 2437), // 2.4GHz cadastrada depois
            )
        assertEquals("2,4G + 5G", resumoBandasWifi(redes, ssidConectado = "Casa"))
    }

    @Test
    fun `ignora redes vizinhas com ssid diferente`() {
        val redes =
            listOf(
                rede("Casa", 2437),
                rede("Vizinho", 5180),
            )
        assertEquals("2,4G", resumoBandasWifi(redes, ssidConectado = "Casa"))
    }

    @Test
    fun `tri-band inclui 6G`() {
        val redes =
            listOf(
                rede("Casa", 2437),
                rede("Casa", 5180),
                rede("Casa", 6115),
            )
        assertEquals("2,4G + 5G + 6G", resumoBandasWifi(redes, ssidConectado = "Casa"))
    }
}
