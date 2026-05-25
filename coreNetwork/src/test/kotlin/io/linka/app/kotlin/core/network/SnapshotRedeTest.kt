package io.linka.app.kotlin.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotRedeTest {

    @Test
    fun `desconectado retorna snapshot com estado desconectado`() {
        val ts = 1_700_000_000_000L
        val snap = SnapshotRede.desconectado(ts)

        assertEquals(EstadoConexao.desconectado, snap.estadoConexao)
    }

    @Test
    fun `desconectado retorna conectado false`() {
        val snap = SnapshotRede.desconectado(0L)

        assertFalse(snap.conectado)
    }

    @Test
    fun `desconectado preserva timestamp informado`() {
        val ts = 9_999_999_999L
        val snap = SnapshotRede.desconectado(ts)

        assertEquals(ts, snap.timestampEpochMs)
    }

    @Test
    fun `desconectado tem wifiLinkSnapshot nulo`() {
        val snap = SnapshotRede.desconectado(0L)

        assertNull(snap.wifiLinkSnapshot)
    }

    @Test
    fun `desconectado tem privateDns inativo`() {
        val snap = SnapshotRede.desconectado(0L)

        assertFalse(snap.privateDnsAtivo)
        assertNull(snap.privateDnsHostname)
    }

    @Test
    fun `desconectado tem lista de dns vazia`() {
        val snap = SnapshotRede.desconectado(0L)

        assertTrue(snap.dnsServidores.isEmpty())
    }

    @Test
    fun `snapshot wifi conectado preserva todos os campos`() {
        val wifiSnap = WifiLinkSnapshot(
            ssid = "MinhaRede",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssiDbm = -65,
            linkSpeedMbps = 144,
            frequenciaMhz = 5180,
            padraoWifi = "802.11ac",
        )
        val snap = SnapshotRede(
            estadoConexao = EstadoConexao.wifi,
            conectado = true,
            timestampEpochMs = 1_700_000_000_000L,
            wifiLinkSnapshot = wifiSnap,
            privateDnsAtivo = true,
            privateDnsHostname = "dns.example.com",
            dnsServidores = listOf("8.8.8.8", "1.1.1.1"),
        )

        assertTrue(snap.conectado)
        assertEquals(EstadoConexao.wifi, snap.estadoConexao)
        assertEquals("MinhaRede", snap.wifiLinkSnapshot?.ssid)
        assertEquals(2, snap.dnsServidores.size)
    }

    @Test
    fun `dois snapshots identicos sao iguais por data class`() {
        val ts = 1_000L
        val a = SnapshotRede.desconectado(ts)
        val b = SnapshotRede.desconectado(ts)

        assertEquals(a, b)
    }

    @Test
    fun `locationAtivado tem default true`() {
        val snap = SnapshotRede.desconectado(0L)

        assertTrue(snap.locationAtivado)
    }
}
