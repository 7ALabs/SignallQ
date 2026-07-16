package io.signallq.app.core.network.topologia.engine

import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.contracts.topologia.TipoEvidencia
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de [TopologiaRedeEngine] — Fase 2A do plano de unificação de topologia Wi-Fi
 * (issue #975/#979).
 *
 * Cobre os cenários de caracterização da Preparação (issue #976) reaplicados ao motor novo, mais
 * os comportamentos específicos da Fase 2A: SISTEMA_MESH_PROVAVEL sem 2ª rota IP, resolução do
 * conflito Intelbras via [io.signallq.app.core.network.topologia.oui.OuiCatalog], e conflito
 * SSID-vs-OUI/banda com prioridade de hardware.
 */
class TopologiaRedeEngineTest {
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

    // ─── 1. Mesh real, mesmo OUI, banda repetida, SEM confirmação de roteador central ──

    @Test
    fun `mesh real 3 nos mesmo OUI Deco banda repetida sem confirmacao de rota - todos viram SISTEMA_MESH_PROVAVEL`() {
        val ouiDeco = "50C7BF"
        val bssidPrincipal = "50:C7:BF:00:00:01"
        val redes =
            listOf(
                rede(ssid = "CasaSilva", bssid = bssidPrincipal, oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -50),
                rede(ssid = "CasaSilva", bssid = "50:C7:BF:00:00:02", oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -65),
                rede(ssid = "CasaSilva", bssid = "50:C7:BF:00:00:03", oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -72),
            )

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = bssidPrincipal)

        assertEquals(3, resultado.size)
        assertTrue(resultado.all { (_, c) -> c.papelProvavel == PapelTopologia.SISTEMA_MESH_PROVAVEL })
        assertTrue(resultado.all { (_, c) -> c.confianca == NivelConfianca.MEDIA })
        val (_, classificacaoPrincipal) = resultado.first { it.first.bssid == bssidPrincipal }
        assertTrue(classificacaoPrincipal.evidencias.any { it.tipo == TipoEvidencia.OUI })
        assertTrue(classificacaoPrincipal.evidencias.any { it.tipo == TipoEvidencia.BANDA })
    }

    // ─── 2. Mesmo cenário, COM confirmação de roteador central (2ª rota IP) ─────────────

    @Test
    fun `mesh real com confirmacao de rota central - conectado vira ROTEADOR e os outros NO_MESH`() {
        val ouiDeco = "50C7BF"
        val bssidPrincipal = "50:C7:BF:00:00:01"
        val redes =
            listOf(
                rede(ssid = "CasaSilva", bssid = bssidPrincipal, oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -50),
                rede(ssid = "CasaSilva", bssid = "50:C7:BF:00:00:02", oui = ouiDeco, frequenciaMhz = 2412, rssiDbm = -65),
            )

        val resultado =
            TopologiaRedeEngine.classificar(
                redes = redes,
                connectedBssid = bssidPrincipal,
                temConfirmacaoRoteadorCentral = true,
            )

        val (_, principal) = resultado.first { it.first.bssid == bssidPrincipal }
        val (_, secundario) = resultado.first { it.first.bssid != bssidPrincipal }
        assertEquals(PapelTopologia.ROTEADOR, principal.papelProvavel)
        assertEquals(NivelConfianca.ALTA, principal.confianca)
        assertEquals(PapelTopologia.NO_MESH, secundario.papelProvavel)
        assertEquals(NivelConfianca.ALTA, secundario.confianca)
    }

    // ─── 3. Roteador único dual-band: mesmo OUI, banda NÃO repetida — nunca mesh ────────

    @Test
    fun `roteador dual-band mesmo OUI banda nao repetida - ROTEADOR para as duas bandas, nunca SISTEMA_MESH_PROVAVEL`() {
        val ouiGenerico = "1122AA"
        val redes =
            listOf(
                rede(ssid = "CasaDual", bssid = "11:22:AA:00:00:01", oui = ouiGenerico, frequenciaMhz = 2412),
                rede(ssid = "CasaDual", bssid = "11:22:AA:00:00:02", oui = ouiGenerico, frequenciaMhz = 5180),
            )

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(2, resultado.size)
        assertTrue(resultado.all { (_, c) -> c.papelProvavel == PapelTopologia.ROTEADOR })
        assertTrue(resultado.all { (_, c) -> c.confianca == NivelConfianca.MEDIA })
    }

    // ─── 4. Extensor: mesmo SSID, OUI diferente do principal ────────────────────────────

    @Test
    fun `extensor mesmo SSID OUI diferente do principal - sinal mais forte vira ROTEADOR e o outro REPETIDOR`() {
        val bssidRoteador = "11:22:AA:00:00:01"
        val bssidExtensor = "33:44:55:00:00:01"
        val redes =
            listOf(
                rede(ssid = "CasaSilva", bssid = bssidRoteador, oui = "1122AA", rssiDbm = -50),
                rede(ssid = "CasaSilva", bssid = bssidExtensor, oui = "334455", rssiDbm = -70),
            )

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        val (_, principal) = resultado.first { it.first.bssid == bssidRoteador }
        val (_, extensor) = resultado.first { it.first.bssid == bssidExtensor }
        assertEquals(PapelTopologia.ROTEADOR, principal.papelProvavel)
        assertEquals(PapelTopologia.REPETIDOR, extensor.papelProvavel)
        assertEquals(NivelConfianca.MEDIA, principal.confianca)
        assertEquals(NivelConfianca.MEDIA, extensor.confianca)
    }

    // ─── 5. Múltiplos APs cabeados, SSIDs diferentes — nunca confundido com mesh ────────

    @Test
    fun `multiplos APs cabeados com SSIDs diferentes nao geram falso positivo de mesh`() {
        val redes =
            listOf(
                rede(ssid = "Escritorio-A", bssid = "AA:AA:AA:00:00:01", oui = "AAAAAA"),
                rede(ssid = "Escritorio-B", bssid = "BB:BB:BB:00:00:01", oui = "BBBBBB"),
                rede(ssid = "Escritorio-C", bssid = "CC:CC:CC:00:00:01", oui = "CCCCCC"),
            )

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(3, resultado.size)
        assertTrue(resultado.none { (_, c) -> c.papelProvavel == PapelTopologia.NO_MESH || c.papelProvavel == PapelTopologia.SISTEMA_MESH_PROVAVEL })
        assertTrue(resultado.all { (_, c) -> c.papelProvavel == PapelTopologia.ROTEADOR && c.confianca == NivelConfianca.BAIXA })
    }

    // ─── 6. Conflito Intelbras isolado — a CORREÇÃO em relação ao motor antigo ──────────

    @Test
    fun `roteador Intelbras isolado C46E1F sem grupo - vira DESCONHECIDO com conflito registrado, nao mais ROTEADOR_MESH afirmativo`() {
        // TopologiaWifiEngine (antigo, ver TopologiaCaracterizacaoTest) retorna ROTEADOR_MESH/ALTA
        // aqui — bug de curadoria documentado como baseline. O motor novo, com o catálogo único
        // (OuiCatalog: papeisPossiveis = {ROTEADOR, NO_MESH}) e sem nenhum outro nó no grupo pra
        // desempatar, não finge certeza: registra o conflito e retorna DESCONHECIDO/BAIXA.
        val redes = listOf(rede(ssid = "MinhaCasaIntelbras", bssid = "C4:6E:1F:00:00:01", oui = "C46E1F"))

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        assertEquals(1, resultado.size)
        val (_, classificacao) = resultado.first()
        assertEquals(PapelTopologia.DESCONHECIDO, classificacao.papelProvavel)
        assertEquals(NivelConfianca.BAIXA, classificacao.confianca)
        assertEquals(1, classificacao.conflitos.size)
    }

    @Test
    fun `roteador Intelbras 6C5AB0 isolado tambem vira DESCONHECIDO com conflito`() {
        val redes = listOf(rede(ssid = "OutraCasaIntelbras", bssid = "6C:5A:B0:00:00:01", oui = "6C5AB0"))

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        val (_, classificacao) = resultado.first()
        assertEquals(PapelTopologia.DESCONHECIDO, classificacao.papelProvavel)
        assertEquals(1, classificacao.conflitos.size)
    }

    // ─── 7. Intelbras agrupado com OUI diferente no mesmo SSID — contexto desempata ─────

    @Test
    fun `Intelbras agrupado com no de outro fabricante no mesmo SSID - contexto de grupo misto decide, nao a ambiguidade do OUI isolado`() {
        val bssidIntelbras = "C4:6E:1F:00:00:01"
        val bssidOutroFabricante = "33:44:55:00:00:01"
        val redes =
            listOf(
                rede(ssid = "CasaMista", bssid = bssidIntelbras, oui = "C46E1F", rssiDbm = -50),
                rede(ssid = "CasaMista", bssid = bssidOutroFabricante, oui = "334455", rssiDbm = -70),
            )

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        val (_, classificacaoIntelbras) = resultado.first { it.first.bssid == bssidIntelbras }
        // Sinal mais forte do grupo misto -> ROTEADOR (não mais DESCONHECIDO nem ROTEADOR_MESH).
        assertEquals(PapelTopologia.ROTEADOR, classificacaoIntelbras.papelProvavel)
        assertTrue(classificacaoIntelbras.conflitos.isEmpty())
    }

    // ─── 8. Múltiplos nós Intelbras (mesmo OUI, banda repetida) — evidência real de mesh ─

    @Test
    fun `multiplos nos Intelbras mesmo OUI banda repetida - evidencia real de mesh sobrepoe ambiguidade do OUI isolado`() {
        val redes =
            listOf(
                rede(ssid = "CasaIntelbrasMesh", bssid = "C4:6E:1F:00:00:01", oui = "C46E1F", frequenciaMhz = 2412, rssiDbm = -50),
                rede(ssid = "CasaIntelbrasMesh", bssid = "C4:6E:1F:00:00:02", oui = "C46E1F", frequenciaMhz = 2412, rssiDbm = -68),
            )

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        assertTrue(resultado.all { (_, c) -> c.papelProvavel == PapelTopologia.SISTEMA_MESH_PROVAVEL })
    }

    // ─── 9. SSID keyword concorda com OUI/banda — confiança sobe ────────────────────────

    @Test
    fun `SSID com palavra-chave mesh concorda com evidencia de banda - confianca sobe de MEDIA para ALTA`() {
        val redes =
            listOf(
                rede(ssid = "Casa_Deco_Mesh", bssid = "50:C7:BF:00:00:01", oui = "50C7BF", frequenciaMhz = 2412, rssiDbm = -50),
                rede(ssid = "Casa_Deco_Mesh", bssid = "50:C7:BF:00:00:02", oui = "50C7BF", frequenciaMhz = 2412, rssiDbm = -68),
            )

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        assertTrue(resultado.all { (_, c) -> c.papelProvavel == PapelTopologia.SISTEMA_MESH_PROVAVEL })
        assertTrue(resultado.all { (_, c) -> c.confianca == NivelConfianca.ALTA })
        assertTrue(resultado.all { (_, c) -> c.evidencias.any { it.tipo == TipoEvidencia.SSID } })
    }

    // ─── 10. SSID keyword diverge do OUI/banda — conflito registrado, OUI prioridado ────

    @Test
    fun `SSID com palavra-chave extensor diverge de OUI de gateway ISP conhecido - conflito registrado mas OUI decide`() {
        val redes = listOf(rede(ssid = "Casa_Extensor_Sala", bssid = "C4:6E:1F:00:00:01", oui = "B4A9FC"))

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        val (_, classificacao) = resultado.first()
        // B4A9FC é OUI de gateway ISP puro (sem ambiguidade mesh) -> ROTEADOR decidido pelo OUI,
        // apesar do SSID sugerir "extensor".
        assertEquals(PapelTopologia.ROTEADOR, classificacao.papelProvavel)
        assertEquals(1, classificacao.conflitos.size)
    }

    // ─── 11. Rede vazia — sem exceção ────────────────────────────────────────────────────

    @Test
    fun `lista vazia retorna lista vazia sem excecao`() {
        val resultado = TopologiaRedeEngine.classificar(redes = emptyList(), connectedBssid = null)
        assertTrue(resultado.isEmpty())
    }

    // ─── 12. Mesmo fabricante do gateway, mas não é o gateway (evidência fraca isolada) ──

    @Test
    fun `dispositivo com mesmo OUI generico do fabricante do gateway mas sem papel de topologia - nao vira roteador so por isso`() {
        // 00E09F é Intelbras genérico (câmera, switch etc.) sem papeisPossiveis no catálogo —
        // não deve ser confundido com o gateway Intelbras da casa (C46E1F/6C5AB0) só por
        // compartilhar fabricante.
        val redes = listOf(rede(ssid = "DispositivoQualquer", bssid = "00:E0:9F:00:00:01", oui = "00E09F"))

        val resultado = TopologiaRedeEngine.classificar(redes = redes, connectedBssid = null)

        val (_, classificacao) = resultado.first()
        assertEquals(PapelTopologia.ROTEADOR, classificacao.papelProvavel)
        assertEquals(NivelConfianca.BAIXA, classificacao.confianca)
        assertTrue(classificacao.conflitos.isEmpty())
    }
}
