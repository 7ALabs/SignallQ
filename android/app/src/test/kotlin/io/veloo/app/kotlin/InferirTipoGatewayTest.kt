package io.signallq.app

import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SegurancaWifi
import io.signallq.app.ui.ConnectionNodeType
import io.signallq.app.ui.screen.bssidCurto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InferirTipoGatewayTest {
    private fun rede(
        ssid: String?,
        bssid: String,
        rssi: Int,
    ) = RedeVizinha(
        ssid = ssid,
        bssid = bssid,
        rssiDbm = rssi,
        frequenciaMhz = 2412,
        seguranca = SegurancaWifi.wpa2,
        larguraCanalMhz = null,
    )

    // ─── Testes originais ────────────────────────────────────────────────────────

    @Test
    fun `(a) 1 BSSID SSID generico retorna wifiRouter`() {
        val redes = listOf(rede("MinhaCasa", "AA:BB:CC:DD:EE:01", -60))
        val resultado = inferirTipoGatewayPorScan("MinhaCasa", redes)
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    @Test
    fun `(b) 2 BSSIDs distintos mesmo SSID RSSI forte retorna wifiMesh`() {
        val redes =
            listOf(
                rede("MinhaCasa", "AA:BB:CC:DD:EE:01", -60),
                rede("MinhaCasa", "AA:BB:CC:DD:EE:02", -60),
            )
        val resultado = inferirTipoGatewayPorScan("MinhaCasa", redes)
        assertEquals(ConnectionNodeType.WifiMesh, resultado)
    }

    @Test
    fun `(c) 2 BSSIDs distintos mesmo SSID RSSI fraco retorna wifiRouter`() {
        val redes =
            listOf(
                rede("MinhaCasa", "AA:BB:CC:DD:EE:01", -80),
                rede("MinhaCasa", "AA:BB:CC:DD:EE:02", -80),
            )
        val resultado = inferirTipoGatewayPorScan("MinhaCasa", redes)
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    @Test
    fun `(d) SSID com DECO retorna wifiMesh pela heuristica de nome`() {
        val resultado = inferirTipoGatewayPorScan("DECO_SALA", emptyList())
        assertEquals(ConnectionNodeType.WifiMesh, resultado)
    }

    // ─── M3: threshold RSSI exatamente em -75 dBm ────────────────────────────────

    @Test
    fun `(e) 2 BSSIDs mesmo SSID RSSI exatamente -75 retorna wifiMesh (inclusivo)`() {
        val redes =
            listOf(
                rede("MinhaCasa", "AA:BB:CC:DD:EE:01", -75),
                rede("MinhaCasa", "AA:BB:CC:DD:EE:02", -75),
            )
        val resultado = inferirTipoGatewayPorScan("MinhaCasa", redes)
        assertEquals(ConnectionNodeType.WifiMesh, resultado)
    }

    // ─── M2: falsos positivos de heurística de SSID ──────────────────────────────

    @Test
    fun `(f) SSID ORANGE nao deve ser confundido com RANGE retorna wifiRouter`() {
        val resultado = inferirTipoGatewayPorScan("ORANGE", emptyList())
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    @Test
    fun `(g) SSID GRANGE nao deve ser confundido com RANGE retorna wifiRouter`() {
        val resultado = inferirTipoGatewayPorScan("GRANGE", emptyList())
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    @Test
    fun `(h) SSID WIFI-RANGE-5G com token RANGE isolado retorna wifiExtender`() {
        val resultado = inferirTipoGatewayPorScan("WIFI-RANGE-5G", emptyList())
        assertEquals(ConnectionNodeType.WifiExtender, resultado)
    }

    @Test
    fun `(i) SSID WIFI-EXT-SALA com token EXT isolado retorna wifiExtender`() {
        val resultado = inferirTipoGatewayPorScan("WIFI-EXT-SALA", emptyList())
        assertEquals(ConnectionNodeType.WifiExtender, resultado)
    }

    @Test
    fun `(j) SSID EXTERIOR nao deve ser confundido com EXT retorna wifiRouter`() {
        val resultado = inferirTipoGatewayPorScan("EXTERIOR", emptyList())
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    // ─── Edge cases de inferirTipoGatewayPorScan ────────────────────────────────

    @Test
    fun `(k) SSID vazio retorna wifiRouter sem crash`() {
        val resultado = inferirTipoGatewayPorScan("", listOf(rede("", "AA:BB:CC:DD:EE:01", -60)))
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    @Test
    fun `(l) SSID generico lista vazia retorna wifiRouter`() {
        val resultado = inferirTipoGatewayPorScan("MinhaCasa", emptyList())
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    @Test
    fun `(m) SSID com keyword de extensor lista vazia retorna wifiExtender`() {
        val resultado = inferirTipoGatewayPorScan("WIFI-EXT-SALA", emptyList())
        assertEquals(ConnectionNodeType.WifiExtender, resultado)
    }

    @Test
    fun `(n) 2 BSSIDs identicos mesma rede RSSI forte retorna wifiRouter (distinct reduz a 1)`() {
        val redes =
            listOf(
                rede("MinhaCasa", "AA:BB:CC:DD:EE:01", -60),
                rede("MinhaCasa", "AA:BB:CC:DD:EE:01", -60),
            )
        val resultado = inferirTipoGatewayPorScan("MinhaCasa", redes)
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }

    // ─── C1+C2: testes de bssidCurto ────────────────────────────────────────────

    @Test
    fun `bssidCurto BSSID valido retorna ultimos 3 octetos`() {
        val resultado = bssidCurto("AA:BB:CC:DD:EE:FF")
        assertEquals("DD:EE:FF", resultado)
    }

    @Test
    fun `bssidCurto BSSID fake 02 00 00 retorna null`() {
        val resultado = bssidCurto("02:00:00:00:00:00")
        assertNull(resultado)
    }

    @Test
    fun `bssidCurto BSSID vazio retorna null`() {
        val resultado = bssidCurto("")
        assertNull(resultado)
    }

    @Test
    fun `bssidCurto BSSID null retorna null`() {
        val resultado = bssidCurto(null)
        assertNull(resultado)
    }

    @Test
    fun `bssidCurto BSSID com separador traco retorna null`() {
        val resultado = bssidCurto("AA-BB-CC-DD-EE-FF")
        assertNull(resultado)
    }

    @Test
    fun `bssidCurto BSSID com menos de 6 pares retorna null`() {
        val resultado = bssidCurto("AA:BB:CC:DD:EE")
        assertNull(resultado)
    }
}
