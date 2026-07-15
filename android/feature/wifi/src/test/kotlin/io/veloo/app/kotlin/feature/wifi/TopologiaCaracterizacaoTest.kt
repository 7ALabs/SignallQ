package io.signallq.app.feature.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de CARACTERIZAÇÃO de [TopologiaWifiEngine] — issue #976 (fase Preparação do épico #975).
 *
 * Travam o comportamento ATUAL do motor como baseline, antes de qualquer refactor de
 * unificação de topologia. Não corrigem nada — inclusive documentam um bug de curadoria
 * conhecido (conflito Intelbras) exatamente como ele se comporta hoje. Se algum destes
 * testes quebrar numa fase futura do épico #975, é sinal de mudança de comportamento que
 * precisa ser avaliada explicitamente (regressão real ou correção intencional).
 *
 * Cenários cobertos (ver PLANO_UNIFICACAO_TOPOLOGIA_WIFI_2026-07-15.md, seção "Preparação"):
 * 1. Mesh real — múltiplos nós, mesmo OUI, banda repetida entre nós
 * 2. Roteador único dual-band — mesmo OUI, banda NÃO repetida
 * 3. Extensor — mesmo SSID, OUI diferente do roteador principal
 * 4. Múltiplos APs cabeados no mesmo switch, SSIDs diferentes — não é mesh Wi-Fi
 * 5. Conflito de curadoria Intelbras — OUI cadastrado como mesh E gateway ISP
 */
class TopologiaCaracterizacaoTest {

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

    // ─── 1. Mesh real: múltiplos nós, mesmo OUI (TP-Link Deco), banda repetida ──────

    @Test
    fun `mesh real 3 nos mesmo OUI Deco banda repetida - conectado vira ROTEADOR_MESH e os outros NO_MESH`() {
        val ouiDeco = "50C7BF" // TP-Link Deco, consta em MESH_NO_OUIS (não em GATEWAY_ISP_OUIS)
        val bssidPrincipal = "50:C7:BF:00:00:01"
        val redes = listOf(
            rede(ssid = "CasaSilva", bssid = bssidPrincipal, oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -50),
            rede(ssid = "CasaSilva", bssid = "50:C7:BF:00:00:02", oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -65),
            rede(ssid = "CasaSilva", bssid = "50:C7:BF:00:00:03", oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -72),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = bssidPrincipal)

        assertEquals(3, resultado.size)
        val principal = resultado.first { it.rede.bssid == bssidPrincipal }
        assertEquals(TipoTopologia.ROTEADOR_MESH, principal.tipo)
        assertEquals(ConfiancaTopologia.ALTA, principal.confianca)
        val secundarios = resultado.filter { it.rede.bssid != bssidPrincipal }
        assertTrue(secundarios.all { it.tipo == TipoTopologia.NO_MESH })
    }

    // ─── 2. Roteador único dual-band: mesmo OUI, banda NÃO repetida ────────────────

    @Test
    fun `roteador dual-band 2_4 e 5GHz mesmo OUI banda nao repetida retorna ROTEADOR para as duas bandas`() {
        val ouiGenerico = "1122AA" // fora de MESH_NO_OUIS e de GATEWAY_ISP_OUIS
        val redes = listOf(
            rede(ssid = "CasaDual", bssid = "11:22:AA:00:00:01", oui = ouiGenerico, frequenciaMhz = 2412),
            rede(ssid = "CasaDual", bssid = "11:22:AA:00:00:02", oui = ouiGenerico, frequenciaMhz = 5180),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(2, resultado.size)
        assertTrue(resultado.all { it.tipo == TipoTopologia.ROTEADOR })
        assertTrue(resultado.all { it.confianca == ConfiancaTopologia.MEDIA })
    }

    // ─── 3. Extensor: mesmo SSID, OUI diferente do roteador principal ──────────────

    @Test
    fun `extensor mesmo SSID OUI diferente do principal - sinal mais forte vira ROTEADOR e o outro REPETIDOR`() {
        val ouiRoteador = "1122AA"
        val ouiExtensor = "334455"
        val bssidRoteador = "11:22:AA:00:00:01"
        val bssidExtensor = "33:44:55:00:00:01"
        val redes = listOf(
            rede(ssid = "CasaSilva", bssid = bssidRoteador, oui = ouiRoteador, rssiDbm = -50),
            rede(ssid = "CasaSilva", bssid = bssidExtensor, oui = ouiExtensor, rssiDbm = -70),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(2, resultado.size)
        val principal = resultado.first { it.rede.bssid == bssidRoteador }
        val extensor = resultado.first { it.rede.bssid == bssidExtensor }
        assertEquals(TipoTopologia.ROTEADOR, principal.tipo)
        assertEquals(TipoTopologia.REPETIDOR, extensor.tipo)
        assertEquals(ConfiancaTopologia.MEDIA, principal.confianca)
        assertEquals(ConfiancaTopologia.MEDIA, extensor.confianca)
    }

    // ─── 4. Múltiplos APs cabeados no mesmo switch, SSIDs diferentes — não é mesh ──

    @Test
    fun `multiplos APs cabeados com SSIDs diferentes nao sao confundidos com mesh - cada um vira ROTEADOR isolado`() {
        val redes = listOf(
            rede(ssid = "Escritorio-A", bssid = "AA:AA:AA:00:00:01", oui = "AAAAAA"),
            rede(ssid = "Escritorio-B", bssid = "BB:BB:BB:00:00:01", oui = "BBBBBB"),
            rede(ssid = "Escritorio-C", bssid = "CC:CC:CC:00:00:01", oui = "CCCCCC"),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(3, resultado.size)
        // Nenhum é agrupado com outro (SSIDs diferentes) → cada um cai no caso "rede única"
        assertTrue(resultado.none { it.tipo == TipoTopologia.NO_MESH || it.tipo == TipoTopologia.ROTEADOR_MESH })
        assertTrue(resultado.all { it.tipo == TipoTopologia.ROTEADOR && it.confianca == ConfiancaTopologia.BAIXA })
    }

    // ─── 5. Conflito de curadoria Intelbras — OUI em MESH_NO_OUIS E GATEWAY_ISP_OUIS ─

    @Test
    fun `BUG CONHECIDO - roteador Intelbras isolado C46E1F e classificado como ROTEADOR_MESH por causa do conflito de curadoria`() {
        // C46E1F está cadastrado em MESH_NO_OUIS *e* GATEWAY_ISP_OUIS ao mesmo tempo
        // (MeshOuiDatabase.kt). Isso faz um roteador Intelbras comum, standalone, sem
        // nenhum outro nó na rede, cair na branch de mesh (Caso 1) e ainda ser tratado
        // como "nó principal provável" só porque isGatewayIsp(oui) também é true.
        // Resultado incorreto do ponto de vista do usuário (não é mesh), mas É o
        // comportamento atual — baseline, não corrigir aqui (fica para a Fase 1 do #975).
        val redes = listOf(
            rede(ssid = "MinhaCasaIntelbras", bssid = "C4:6E:1F:00:00:01", oui = "C46E1F"),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(1, resultado.size)
        assertEquals(TipoTopologia.ROTEADOR_MESH, resultado.first().tipo)
        assertEquals(ConfiancaTopologia.ALTA, resultado.first().confianca)
    }

    @Test
    fun `BUG CONHECIDO - roteador Intelbras isolado 6C5AB0 tambem cai no mesmo conflito de curadoria`() {
        val redes = listOf(
            rede(ssid = "OutraCasaIntelbras", bssid = "6C:5A:B0:00:00:01", oui = "6C5AB0"),
        )

        val resultado = TopologiaWifiEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(1, resultado.size)
        assertEquals(TipoTopologia.ROTEADOR_MESH, resultado.first().tipo)
        assertEquals(ConfiancaTopologia.ALTA, resultado.first().confianca)
    }
}
