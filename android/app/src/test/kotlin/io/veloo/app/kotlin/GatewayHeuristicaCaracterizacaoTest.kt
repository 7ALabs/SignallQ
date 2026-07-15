package io.signallq.app

import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SegurancaWifi
import io.signallq.app.ui.ConnectionNodeType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de CARACTERIZAÇÃO de [inferirTipoGatewayPorScan] (`GatewayHeuristica.kt`) — issue #976
 * (fase Preparação do épico #975).
 *
 * `GatewayHeuristica` só enxerga SSID + contagem de BSSIDs/RSSI — não tem noção de OUI nem de
 * banda (2.4/5/6GHz). Isso significa que, hoje, ela **não consegue distinguir** um roteador
 * dual-band (2 BSSIDs, mesma rede física) nem um extensor por OUI diferente (2 BSSIDs, mesmo
 * SSID) de um mesh real — todos os três caem na mesma regra "2+ BSSIDs, mesmo SSID, RSSI forte
 * = mesh". `TopologiaWifiEngine` (que tem OUI e banda) diferencia esses casos corretamente.
 * Essa divergência é exatamente o motivo do épico #975 existir — os testes abaixo documentam
 * o comportamento ATUAL como baseline, não corrigem nada aqui.
 *
 * Cenários cobertos (ver PLANO_UNIFICACAO_TOPOLOGIA_WIFI_2026-07-15.md, seção "Preparação"):
 * 1. Mesh real — múltiplos nós, mesmo SSID, RSSI forte
 * 2. Roteador único dual-band — FALSO POSITIVO conhecido: vira "mesh" hoje
 * 3. Extensor por OUI diferente — FALSO POSITIVO conhecido: vira "mesh" hoje (heurística só vê SSID)
 * 4. Múltiplos APs cabeados com SSIDs diferentes — não é mesh (SSID conectado filtra os outros)
 */
class GatewayHeuristicaCaracterizacaoTest {
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

    // ─── 1. Mesh real: múltiplos nós, mesmo SSID, RSSI forte ───────────────────────

    @Test
    fun `mesh real 3 nos mesmo SSID RSSI forte retorna wifiMesh`() {
        val redes =
            listOf(
                rede("CasaSilva", "50:C7:BF:00:00:01", -50),
                rede("CasaSilva", "50:C7:BF:00:00:02", -65),
                rede("CasaSilva", "50:C7:BF:00:00:03", -72),
            )
        val resultado = inferirTipoGatewayPorScan("CasaSilva", redes)
        assertEquals(ConnectionNodeType.WifiMesh, resultado)
    }

    // ─── 2. Roteador único dual-band — FALSO POSITIVO conhecido ────────────────────

    @Test
    fun `FALSO POSITIVO CONHECIDO - roteador dual-band 2 BSSIDs mesmo SSID RSSI forte tambem retorna wifiMesh`() {
        // Fisicamente é UM roteador (2.4GHz + 5GHz = 2 BSSIDs), não um sistema mesh.
        // GatewayHeuristica não tem como saber disso (não vê banda/frequência nem OUI) —
        // usa só "2+ BSSIDs distintos, mesmo SSID, RSSI forte" como proxy de mesh, que aqui
        // dá falso positivo. TopologiaWifiEngine (que vê banda) acerta esse caso — ver
        // TopologiaCaracterizacaoTest. Baseline atual, não corrigir nesta task.
        val redes =
            listOf(
                rede("CasaDual", "11:22:AA:00:00:01", -55), // 2.4GHz
                rede("CasaDual", "11:22:AA:00:00:02", -58), // 5GHz, BSSID/RSSI distintos, banda não modelada aqui
            )
        val resultado = inferirTipoGatewayPorScan("CasaDual", redes)
        assertEquals(ConnectionNodeType.WifiMesh, resultado)
    }

    // ─── 3. Extensor por OUI diferente — FALSO POSITIVO conhecido ──────────────────

    @Test
    fun `FALSO POSITIVO CONHECIDO - extensor com OUI diferente do roteador tambem retorna wifiMesh (heuristica so ve SSID)`() {
        // Mesmo SSID, BSSIDs de OUI totalmente diferentes (roteador vs. extensor de outro
        // fabricante) — GatewayHeuristica não consegue diferenciar de mesh porque não olha
        // OUI, só conta BSSIDs distintos com o mesmo SSID e RSSI acima do threshold.
        val redes =
            listOf(
                rede("CasaSilva", "11:22:AA:00:00:01", -50), // roteador
                rede("CasaSilva", "33:44:55:00:00:01", -70), // extensor, OUI diferente
            )
        val resultado = inferirTipoGatewayPorScan("CasaSilva", redes)
        assertEquals(ConnectionNodeType.WifiMesh, resultado)
    }

    // ─── 4. Múltiplos APs cabeados, SSIDs diferentes — não é mesh ──────────────────

    @Test
    fun `multiplos APs cabeados com SSIDs diferentes nao geram falso positivo de mesh - so o SSID conectado conta`() {
        // Rede corporativa com 3 APs cabeados, cada um com seu próprio SSID. Ao consultar
        // pelo SSID ao qual o device está de fato conectado, os outros SSIDs não entram na
        // contagem de bssidsComMesmoSsid — resultado correto (WifiRouter), sem falso positivo.
        val redesVisiveis =
            listOf(
                rede("Escritorio-A", "AA:AA:AA:00:00:01", -50),
                rede("Escritorio-B", "BB:BB:BB:00:00:01", -55),
                rede("Escritorio-C", "CC:CC:CC:00:00:01", -60),
            )
        val resultado = inferirTipoGatewayPorScan("Escritorio-A", redesVisiveis)
        assertEquals(ConnectionNodeType.WifiRouter, resultado)
    }
}
