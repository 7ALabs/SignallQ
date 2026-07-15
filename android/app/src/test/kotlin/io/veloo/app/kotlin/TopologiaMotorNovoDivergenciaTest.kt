package io.signallq.app

import io.signallq.app.core.network.topologia.engine.TopologiaRedeEngine
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SegurancaWifi
import io.signallq.app.feature.wifi.TipoTopologia
import io.signallq.app.feature.wifi.TopologiaWifiEngine
import io.signallq.app.ui.ConnectionNodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comparação lado a lado — Fase 2A do plano de unificação de topologia (issue #975/#979).
 *
 * Roda os três motores antigos (`inferirTipoGatewayPorScan`, `TopologiaWifiEngine`) e o motor
 * novo (`TopologiaRedeEngine`) sobre os mesmos fixtures de caracterização, e registra (via
 * `println`, pra inspeção manual) e trava em assert (pra não regredir silenciosamente) as
 * divergências esperadas entre eles. Nenhuma tela consome o motor novo ainda (isso é Fase 2B) —
 * este teste é só o comparador exigido pela Fase 2A antes de migrar qualquer consumidor.
 */
class TopologiaMotorNovoDivergenciaTest {
    private fun rede(
        ssid: String? = "MinhaRede",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        rssiDbm: Int = -60,
        oui: String = "",
        frequenciaMhz: Int = 2412,
    ) = RedeVizinha(
        ssid = ssid,
        bssid = bssid,
        rssiDbm = rssiDbm,
        frequenciaMhz = frequenciaMhz,
        seguranca = SegurancaWifi.wpa2,
        larguraCanalMhz = 20,
        oui = oui,
    )

    private fun logDivergencia(
        cenario: String,
        ssid: String,
        redes: List<RedeVizinha>,
    ) {
        val gateway = inferirTipoGatewayPorScan(ssid, redes)
        val topologiaAntiga = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)
        val topologiaNova = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)
        println(
            "[divergencia-fase2a] $cenario | GatewayHeuristica=$gateway | " +
                "TopologiaWifiEngine=${topologiaAntiga.map { it.tipo }} | " +
                "TopologiaRedeEngine=${topologiaNova.map { it.second.papelProvavel }}",
        )
    }

    // ─── Mesh real: os três motores concordam que é mesh (de formas diferentes) ────────

    @Test
    fun `mesh real 3 nos - os tres motores indicam mesh, motor novo sem afirmar qual no e central`() {
        val ssid = "CasaSilva"
        val redes =
            listOf(
                rede(ssid = ssid, bssid = "50:C7:BF:00:00:01", oui = "50C7BF", rssiDbm = -50),
                rede(ssid = ssid, bssid = "50:C7:BF:00:00:02", oui = "50C7BF", rssiDbm = -65),
                rede(ssid = ssid, bssid = "50:C7:BF:00:00:03", oui = "50C7BF", rssiDbm = -72),
            )
        logDivergencia("mesh real 3 nos", ssid, redes)

        assertEquals(ConnectionNodeType.WifiMesh, inferirTipoGatewayPorScan(ssid, redes))
        val topologiaNova = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)
        assertEquals(3, topologiaNova.size)
        // Motor novo nunca afirma um nó "roteador central" sem 2ª rota IP — divergência
        // intencional em relação ao motor antigo (que escolhe o conectado como ROTEADOR_MESH).
        assertTrue(topologiaNova.none { it.second.papelProvavel.name == "ROTEADOR" })
    }

    // ─── Roteador dual-band: motor novo corrige o falso positivo dos dois motores antigos ─

    @Test
    fun `roteador dual-band - motores antigos de SSID dao falso positivo de mesh, motor novo corrige via banda`() {
        val ssid = "CasaDual"
        val redes =
            listOf(
                rede(ssid = ssid, bssid = "11:22:AA:00:00:01", oui = "1122AA", rssiDbm = -55, frequenciaMhz = 2412),
                rede(ssid = ssid, bssid = "11:22:AA:00:00:02", oui = "1122AA", rssiDbm = -58, frequenciaMhz = 5180),
            )
        logDivergencia("roteador dual-band", ssid, redes)

        // FALSO POSITIVO CONHECIDO do motor de SSID (ver GatewayHeuristicaCaracterizacaoTest).
        assertEquals(ConnectionNodeType.WifiMesh, inferirTipoGatewayPorScan(ssid, redes))

        val topologiaNova = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)
        // Motor novo, que vê banda, corrige: os dois BSSIDs viram ROTEADOR, nenhum mesh.
        assertEquals(2, topologiaNova.size)
        assertEquals(2, topologiaNova.count { it.second.papelProvavel.name == "ROTEADOR" })
    }

    // ─── Conflito Intelbras isolado: motor antigo confiante e errado, motor novo honesto ──

    @Test
    fun `Intelbras isolado - TopologiaWifiEngine antigo afirma ROTEADOR_MESH ALTA, motor novo reconhece a ambiguidade`() {
        val ssid = "MinhaCasaIntelbras"
        val redes = listOf(rede(ssid = ssid, bssid = "C4:6E:1F:00:00:01", oui = "C46E1F"))
        logDivergencia("Intelbras isolado", ssid, redes)

        val antigo = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null).first()
        assertEquals(TipoTopologia.ROTEADOR_MESH, antigo.tipo) // bug conhecido, baseline preservado

        val novo = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null).first().second
        assertEquals("DESCONHECIDO", novo.papelProvavel.name)
        assertEquals(1, novo.conflitos.size)
    }
}
