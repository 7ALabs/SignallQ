package io.signallq.app.feature.wifi

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de [TopologiaWifiEngine] — Fase 1 do plano de unificação de topologia (issue #975/#978).
 *
 * O catálogo unificado ([io.signallq.app.core.network.topologia.oui.OuiCatalog]) resolveu o
 * conflito Intelbras (`C46E1F`/`6C5AB0`) num registro só com `papeisPossiveis = {ROTEADOR,
 * NO_MESH}` — mas o catálogo, por design, **nunca decide sozinho** qual papel vale. Estes
 * testes confirmam que é o *contexto* (rede isolada vs. agrupada com outro nó de OUI diferente
 * no mesmo SSID) que muda o papel efetivo escolhido pelo motor, não a ordem em que o OUI foi
 * declarado no catálogo (que nem existe mais como conceito — é um único `Map` por prefixo).
 */
class TopologiaConflitoIntelbrasFase1Test {

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

    @Test
    fun `roteador Intelbras isolado - SSID unico - vira ROTEADOR_MESH`() {
        val redes = listOf(
            rede(ssid = "CasaIntelbras", bssid = "C4:6E:1F:00:00:02", oui = "C46E1F"),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(1, resultado.size)
        assertEquals(TipoTopologia.ROTEADOR_MESH, resultado.first().tipo)
    }

    @Test
    fun `mesmo OUI Intelbras agrupado com no de outro fabricante no mesmo SSID vira NO_MESH - contexto muda o papel, nao o catalogo`() {
        val ouiIntelbras = "C46E1F"
        val ouiOutroFabricante = "334455"
        val bssidIntelbras = "C4:6E:1F:00:00:01"
        val bssidOutroFabricante = "33:44:55:00:00:01"
        val redes = listOf(
            rede(ssid = "CasaMista", bssid = bssidIntelbras, oui = ouiIntelbras, rssiDbm = -50),
            rede(ssid = "CasaMista", bssid = bssidOutroFabricante, oui = ouiOutroFabricante, rssiDbm = -70),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(2, resultado.size)
        val no1 = resultado.first { it.rede.bssid == bssidIntelbras }
        // Mesmo OUI, mesmo registro de catálogo (papeisPossiveis = {ROTEADOR, NO_MESH}) do
        // teste anterior — mas agora o resultado é NO_MESH, não ROTEADOR_MESH, porque o grupo
        // de redes com o mesmo SSID tem um nó de OUI diferente (contexto de agrupamento).
        assertEquals(TipoTopologia.NO_MESH, no1.tipo)
    }
}
