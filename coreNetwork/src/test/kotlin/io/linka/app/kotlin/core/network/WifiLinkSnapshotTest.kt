package io.linka.app.kotlin.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WifiLinkSnapshotTest {

    @Test
    fun `rssi negativo e preservado corretamente`() {
        // RSSI válido em Wi-Fi é sempre negativo (dBm)
        val snap = WifiLinkSnapshot(
            ssid = "RedeTest",
            bssid = "00:11:22:33:44:55",
            rssiDbm = -72,
            linkSpeedMbps = 300,
            frequenciaMhz = 2412,
            padraoWifi = "802.11n",
        )

        assertEquals(-72, snap.rssiDbm)
    }

    @Test
    fun `frequencia 5ghz e preservada`() {
        val snap = WifiLinkSnapshot(
            ssid = "Rede5G",
            bssid = null,
            rssiDbm = -55,
            linkSpeedMbps = 867,
            frequenciaMhz = 5745,
            padraoWifi = "802.11ax",
        )

        assertEquals(5745, snap.frequenciaMhz)
        assertEquals(867, snap.linkSpeedMbps)
    }

    @Test
    fun `snapshots com ssids diferentes nao sao iguais`() {
        val a = WifiLinkSnapshot("RedeA", null, -60, null, null, null)
        val b = WifiLinkSnapshot("RedeB", null, -60, null, null, null)

        assertNotEquals(a, b)
    }

    @Test
    fun `copy preserva campos nao alterados`() {
        val original = WifiLinkSnapshot(
            ssid = "Original",
            bssid = "11:22:33:44:55:66",
            rssiDbm = -80,
            linkSpeedMbps = 54,
            frequenciaMhz = 2437,
            padraoWifi = "802.11g",
        )
        val copia = original.copy(rssiDbm = -50)

        assertEquals("Original", copia.ssid)
        assertEquals(-50, copia.rssiDbm)
        assertEquals(original.bssid, copia.bssid)
    }
}
