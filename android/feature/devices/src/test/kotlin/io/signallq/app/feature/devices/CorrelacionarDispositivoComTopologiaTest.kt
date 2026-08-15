package io.signallq.app.feature.devices

import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.core.network.contracts.topologia.ClassificacaoTopologia
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.OrigemDados
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.contracts.topologia.PesoEvidencia
import io.signallq.app.core.network.contracts.topologia.TipoEvidencia
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GH#983 Fase 4 — cobre [correlacionarDispositivoComTopologia], a correlação best-effort em 4
 * níveis entre scan LAN ([DispositivoRede]), topologia Wi-Fi ([RedeVizinha] + [ClassificacaoTopologia])
 * e leitura direta do gateway ([ClientSnapshot]).
 */
class CorrelacionarDispositivoComTopologiaTest {
    private fun dispositivo(mac: String?) = DispositivoRede(
        id = "test",
        ip = "192.168.1.50",
        mac = mac,
        nomeExibicao = "Dispositivo de teste",
        fonteNome = "arp",
    )

    private fun redeClassificada(
        bssid: String,
        papel: PapelTopologia,
        confianca: NivelConfianca = NivelConfianca.ALTA,
    ): Pair<RedeVizinha, ClassificacaoTopologia> {
        val rede =
            RedeVizinha(
                ssid = "Rede de teste",
                bssid = bssid,
                rssiDbm = -50,
                frequenciaMhz = 2437,
                seguranca = SegurancaWifi.wpa2,
                larguraCanalMhz = 20,
            )
        val classificacao =
            ClassificacaoTopologia(
                papelProvavel = papel,
                confianca = confianca,
                evidencias = emptyList(),
                origemDados = OrigemDados.SCAN_WIFI_PASSIVO,
            )
        return rede to classificacao
    }

    private fun clientSnapshot(mac: String?, tipoConexaoFisica: TipoConexaoFisica? = null) = ClientSnapshot(
        mac = mac,
        ip = "192.168.1.50",
        hostname = "host-teste",
        tipoConexao = null,
        tipoConexaoFisica = tipoConexaoFisica,
    )

    // ─── Nível 1: ClientSnapshot exato ───────────────────────────────

    @Test
    fun `mac igual ao de um ClientSnapshot reclassifica com nivel CLIENT_SNAPSHOT_EXATO`() {
        val dispositivo = dispositivo(mac = "AA:BB:CC:11:22:33")
        val clientes = listOf(clientSnapshot(mac = "aa:bb:cc:11:22:33", tipoConexaoFisica = TipoConexaoFisica.ETHERNET))

        val resultado = correlacionarDispositivoComTopologia(dispositivo = dispositivo, clientesGateway = clientes)

        assertEquals(NivelCorrelacao.CLIENT_SNAPSHOT_EXATO, resultado.nivel)
        assertEquals(TipoConexaoFisica.ETHERNET, resultado.tipoConexaoFisicaConfirmada)
        assertNull(resultado.papelTopologiaHerdado)
    }

    @Test
    fun `ClientSnapshot exato que tambem bate com bssid herda o papel do no Wi-Fi`() {
        val mac = "AA:BB:CC:11:22:33"
        val dispositivo = dispositivo(mac = mac)
        val clientes = listOf(clientSnapshot(mac = mac, tipoConexaoFisica = TipoConexaoFisica.ETHERNET))
        val redes = listOf(redeClassificada(bssid = mac, papel = PapelTopologia.NO_MESH))

        val resultado =
            correlacionarDispositivoComTopologia(
                dispositivo = dispositivo,
                clientesGateway = clientes,
                redesWifiClassificadas = redes,
            )

        assertEquals(NivelCorrelacao.CLIENT_SNAPSHOT_EXATO, resultado.nivel)
        assertEquals(PapelTopologia.NO_MESH, resultado.papelTopologiaHerdado)
        assertEquals(TipoConexaoFisica.ETHERNET, resultado.tipoConexaoFisicaConfirmada)
    }

    // ─── Nível 2: MAC exato == BSSID exato ─────────────────────────────

    @Test
    fun `mac do dispositivo igual ao bssid de um no Wi-Fi classificado herda o papel`() {
        val dispositivo = dispositivo(mac = "C4:6E:1F:AA:BB:CC")
        val redes = listOf(redeClassificada(bssid = "c46e1faabbcc", papel = PapelTopologia.ROTEADOR))

        val resultado = correlacionarDispositivoComTopologia(dispositivo = dispositivo, redesWifiClassificadas = redes)

        assertEquals(NivelCorrelacao.MAC_EXATO, resultado.nivel)
        assertEquals(PapelTopologia.ROTEADOR, resultado.papelTopologiaHerdado)
        assertNull(resultado.tipoConexaoFisicaConfirmada)
    }

    @Test
    fun `comparacao de mac e bssid ignora maiusculas e separadores`() {
        val dispositivo = dispositivo(mac = "aa-bb-cc-dd-ee-ff")
        val redes = listOf(redeClassificada(bssid = "AA:BB:CC:DD:EE:FF", papel = PapelTopologia.NO_MESH))

        val resultado = correlacionarDispositivoComTopologia(dispositivo = dispositivo, redesWifiClassificadas = redes)

        assertEquals(NivelCorrelacao.MAC_EXATO, resultado.nivel)
        assertEquals(PapelTopologia.NO_MESH, resultado.papelTopologiaHerdado)
    }

    // ─── Nível 3: mesmo prefixo OUI apenas (fraco/auxiliar) ────────────

    @Test
    fun `mesmo prefixo OUI sem mac exato retorna nivel OUI_FRACO com evidencia auxiliar`() {
        // Mesmo prefixo Intelbras (C46E1F) do roteador, mas o restante do MAC diverge —
        // não é o mesmo dispositivo físico, só o mesmo fabricante.
        val dispositivo = dispositivo(mac = "C4:6E:1F:99:88:77")
        val redes = listOf(redeClassificada(bssid = "C4:6E:1F:AA:BB:CC", papel = PapelTopologia.ROTEADOR))

        val resultado = correlacionarDispositivoComTopologia(dispositivo = dispositivo, redesWifiClassificadas = redes)

        assertEquals(NivelCorrelacao.OUI_FRACO, resultado.nivel)
        requireNotNull(resultado.evidenciaAuxiliar)
        assertEquals(TipoEvidencia.OUI, resultado.evidenciaAuxiliar.tipo)
        assertEquals("C46E1F", resultado.evidenciaAuxiliar.valorBruto)
        assertEquals(PesoEvidencia.FRACO, resultado.evidenciaAuxiliar.peso)
    }

    @Test
    fun `smartphone com mesmo OUI do gateway nao reclassifica sozinho como roteador`() {
        // Caso negativo explícito do critério de aceite #983: um smartphone (Intelbras é
        // hipotético aqui, mas o ponto vale pra qualquer fabricante que também apareça em
        // gatewayIspOuis/meshNoOuis) com o mesmo prefixo de fabricante do roteador da casa
        // NUNCA deve virar "possível roteador" só por causa do fabricante compartilhado.
        val smartphone =
            dispositivo(mac = "C4:6E:1F:12:34:56").copy(
                nomeExibicao = "iPhone de Ana",
                tipoDispositivo = TipoDispositivo.smartphone,
            )
        val redeDoRoteador = listOf(redeClassificada(bssid = "C4:6E:1F:AA:BB:CC", papel = PapelTopologia.ROTEADOR))

        val resultado =
            correlacionarDispositivoComTopologia(
                dispositivo = smartphone,
                redesWifiClassificadas = redeDoRoteador,
            )

        assertEquals(NivelCorrelacao.OUI_FRACO, resultado.nivel)
        assertNull(
            "correlação fraca por OUI nunca pode preencher papelTopologiaHerdado sozinha",
            resultado.papelTopologiaHerdado,
        )
        assertEquals(smartphone, resultado.dispositivo)
    }

    // ─── Nível 4: sem match — comportamento atual preservado ───────────

    @Test
    fun `sem nenhum sinal disponivel retorna SEM_MATCH e o dispositivo inalterado`() {
        val dispositivo = dispositivo(mac = "11:22:33:44:55:66")

        val resultado = correlacionarDispositivoComTopologia(dispositivo = dispositivo)

        assertEquals(NivelCorrelacao.SEM_MATCH, resultado.nivel)
        assertEquals(dispositivo, resultado.dispositivo)
        assertNull(resultado.papelTopologiaHerdado)
        assertNull(resultado.evidenciaAuxiliar)
        assertNull(resultado.tipoConexaoFisicaConfirmada)
    }

    @Test
    fun `oui totalmente desconhecido entre as redes disponiveis tambem retorna SEM_MATCH`() {
        val dispositivo = dispositivo(mac = "FF:FF:FF:00:00:01")
        val redes = listOf(redeClassificada(bssid = "C4:6E:1F:AA:BB:CC", papel = PapelTopologia.ROTEADOR))

        val resultado = correlacionarDispositivoComTopologia(dispositivo = dispositivo, redesWifiClassificadas = redes)

        assertEquals(NivelCorrelacao.SEM_MATCH, resultado.nivel)
    }

    @Test
    fun `dispositivo sem mac nunca lanca excecao e cai em SEM_MATCH`() {
        val dispositivo = dispositivo(mac = null)
        val redes = listOf(redeClassificada(bssid = "C4:6E:1F:AA:BB:CC", papel = PapelTopologia.ROTEADOR))
        val clientes = listOf(clientSnapshot(mac = null))

        val resultado =
            correlacionarDispositivoComTopologia(
                dispositivo = dispositivo,
                clientesGateway = clientes,
                redesWifiClassificadas = redes,
            )

        assertEquals(NivelCorrelacao.SEM_MATCH, resultado.nivel)
    }
}
