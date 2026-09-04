package io.signallq.app.core.nds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NdsProfileCapabilitiesMapperTest {

    @Test
    fun `profile e gamer dentro do fluxo Modo Gamer`() {
        assertEquals("gamer", ndsProfile(dentroDoModoGamer = true))
    }

    @Test
    fun `profile e omitido (null) fora do fluxo Modo Gamer, nunca general`() {
        assertNull(ndsProfile(dentroDoModoGamer = false))
    }

    @Test
    fun `capabilities sempre inclui scoring e ai, mesmo sem wifi nem fiber`() {
        val capabilities = ndsCapabilities(incluiWifi = false, incluiFiber = false)

        assertEquals(listOf("scoring", "ai"), capabilities)
    }

    @Test
    fun `capabilities inclui wifi quando o bloco wifi existe no request`() {
        val capabilities = ndsCapabilities(incluiWifi = true, incluiFiber = false)

        assertEquals(listOf("scoring", "ai", "wifi"), capabilities)
    }

    @Test
    fun `capabilities inclui fiber quando o bloco fiber existe no request`() {
        val capabilities = ndsCapabilities(incluiWifi = false, incluiFiber = true)

        assertEquals(listOf("scoring", "ai", "fiber"), capabilities)
    }

    @Test
    fun `capabilities inclui wifi e fiber juntos quando ambos os blocos existem`() {
        val capabilities = ndsCapabilities(incluiWifi = true, incluiFiber = true)

        assertEquals(listOf("scoring", "ai", "wifi", "fiber"), capabilities)
    }

    @Test
    fun `sobrecarga com blocos reais deriva incluiWifi e incluiFiber de nulidade, nao de conteudo`() {
        val comWifiSemFiber = ndsCapabilities(wifi = NdsWifiInfo(rssi = -60), fiber = null)
        val semWifiComFiber = ndsCapabilities(wifi = null, fiber = NdsFiberInfo(rxPowerDbm = -20.0))
        val semNenhum = ndsCapabilities(wifi = null, fiber = null)

        assertEquals(listOf("scoring", "ai", "wifi"), comWifiSemFiber)
        assertEquals(listOf("scoring", "ai", "fiber"), semWifiComFiber)
        assertEquals(listOf("scoring", "ai"), semNenhum)
    }

    @Test
    fun `capabilities inclui wifi_scan quando o bloco wifiScan existe no request (ADR-018)`() {
        val capabilities = ndsCapabilities(incluiWifi = false, incluiFiber = false, incluiWifiScan = true)

        assertEquals(listOf("scoring", "ai", "wifi_scan"), capabilities)
    }

    @Test
    fun `sobrecarga com bloco wifiScan real deriva incluiWifiScan de nulidade`() {
        val comWifiScan = ndsCapabilities(wifi = null, fiber = null, wifiScan = NdsWifiScanInfo(bestChannel = 149))
        val semWifiScan = ndsCapabilities(wifi = null, fiber = null, wifiScan = null)

        assertEquals(listOf("scoring", "ai", "wifi_scan"), comWifiScan)
        assertEquals(listOf("scoring", "ai"), semWifiScan)
    }

    @Test
    fun `capabilities inclui historical quando o bloco historical existe no request (ADR-018, NDS-Snapshot-06)`() {
        val capabilities = ndsCapabilities(incluiWifi = false, incluiFiber = false, incluiHistorical = true)

        assertEquals(listOf("scoring", "ai", "historical"), capabilities)
    }

    @Test
    fun `sobrecarga com bloco historical real deriva incluiHistorical de nulidade`() {
        val comHistorico = ndsCapabilities(wifi = null, fiber = null, historical = NdsHistoricalInfo(testsCount30d = 12))
        val semHistorico = ndsCapabilities(wifi = null, fiber = null, historical = null)

        assertEquals(listOf("scoring", "ai", "historical"), comHistorico)
        assertEquals(listOf("scoring", "ai"), semHistorico)
    }
}
