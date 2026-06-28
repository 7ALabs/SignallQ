package io.signallq.app.feature.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopologiaWifiEngineTest {

    // ----------------------------------------------------------------
    // Helpers para construir RedeVizinha sem boilerplate
    // ----------------------------------------------------------------

    private fun rede(
        ssid: String? = "MinhaRede",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        rssiDbm: Int = -60,
        oui: String = "",
    ) = RedeVizinha(
        ssid = ssid,
        bssid = bssid,
        rssiDbm = rssiDbm,
        frequenciaMhz = 2412,
        seguranca = SegurancaWifi.wpa2,
        larguraCanalMhz = 20,
        oui = oui,
    )

    // ----------------------------------------------------------------
    // Teste 1 — rede única sem OUI mesh → ROTEADOR
    // ----------------------------------------------------------------

    @Test
    fun `classificar rede unica sem OUI mesh retorna ROTEADOR`() {
        // OUI genérico, fora de qualquer banco conhecido
        val redes = listOf(
            rede(ssid = "CasaDoZe", bssid = "AA:BB:CC:11:22:33", oui = "AABBCC"),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(1, resultado.size)
        assertEquals(TipoTopologia.ROTEADOR, resultado.first().tipo)
    }

    // ----------------------------------------------------------------
    // Teste 2 — OUI de mesh node (Google Nest) → ROTEADOR_MESH quando é o conectado
    // ----------------------------------------------------------------

    @Test
    fun `classificar OUI Google Nest WiFi conectado retorna ROTEADOR_MESH`() {
        // F4F5D8 é Google Nest WiFi — consta em MESH_NO_OUIS
        val bssidConectado = "F4:F5:D8:AA:BB:CC"
        val redes = listOf(
            rede(ssid = "GoogleHome", bssid = bssidConectado, oui = "F4F5D8"),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = bssidConectado)

        assertEquals(1, resultado.size)
        assertEquals(TipoTopologia.ROTEADOR_MESH, resultado.first().tipo)
        assertEquals(ConfiancaTopologia.ALTA, resultado.first().confianca)
    }

    // ----------------------------------------------------------------
    // Teste 3 — OUI de mesh node não conectado → NO_MESH
    // ----------------------------------------------------------------

    @Test
    fun `classificar OUI TP-Link Deco nao conectado retorna NO_MESH`() {
        // 50C7BF é TP-Link Deco — consta em MESH_NO_OUIS
        val bssidConectado = "AA:BB:CC:00:00:01"
        val redes = listOf(
            // nó mesh, mas NÃO é o bssid conectado
            rede(ssid = "DecoRede", bssid = "50:C7:BF:AA:BB:CC", oui = "50C7BF"),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = bssidConectado)

        assertEquals(1, resultado.size)
        assertEquals(TipoTopologia.NO_MESH, resultado.first().tipo)
        assertEquals(ConfiancaTopologia.ALTA, resultado.first().confianca)
    }

    // ----------------------------------------------------------------
    // Teste 4 — classificar lista vazia → retorna lista vazia sem exceção
    // ----------------------------------------------------------------

    @Test
    fun `classificar lista vazia retorna lista vazia sem lancar excecao`() {
        val resultado = TopologiaWifiEngine.classificar(redes = emptyList(), connectedBssid = null)
        assertTrue(resultado.isEmpty())
    }

    // ----------------------------------------------------------------
    // Teste 5 — agrupar() agrupa por SSID e ordena por RSSI decrescente
    // ----------------------------------------------------------------

    @Test
    fun `agrupar agrupa por SSID e ordena nos por RSSI decrescente`() {
        val redes = listOf(
            RedeClassificada(rede("MeshCasa", "AA:AA:AA:AA:AA:01", rssiDbm = -75, oui = "F4F5D8"), TipoTopologia.NO_MESH, ConfiancaTopologia.ALTA, ""),
            RedeClassificada(rede("MeshCasa", "AA:AA:AA:AA:AA:02", rssiDbm = -55, oui = "F4F5D8"), TipoTopologia.ROTEADOR_MESH, ConfiancaTopologia.ALTA, ""),
            RedeClassificada(rede("MeshCasa", "AA:AA:AA:AA:AA:03", rssiDbm = -65, oui = "F4F5D8"), TipoTopologia.NO_MESH, ConfiancaTopologia.ALTA, ""),
            RedeClassificada(rede("RedeVizinha", "BB:BB:BB:BB:BB:01", rssiDbm = -80, oui = "AABBCC"), TipoTopologia.ROTEADOR, ConfiancaTopologia.BAIXA, ""),
        )

        val grupos = TopologiaWifiEngine.agrupar(redes)

        // Deve haver 2 grupos
        assertEquals(2, grupos.size)

        // Grupo MeshCasa deve vir primeiro (RSSI mais forte = -55)
        val grupoCasa = grupos.first()
        assertEquals("MeshCasa", grupoCasa.ssid)

        // Dentro do grupo, nós ordenados por RSSI decrescente: -55, -65, -75
        val rssis = grupoCasa.redes.map { it.rede.rssiDbm }
        assertEquals(listOf(-55, -65, -75), rssis)
    }
}
