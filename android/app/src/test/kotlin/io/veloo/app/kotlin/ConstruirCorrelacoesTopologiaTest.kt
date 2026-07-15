package io.signallq.app

import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.core.network.contracts.topologia.ClassificacaoTopologia
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.OrigemDados
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.NivelCorrelacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa isoladamente [construirCorrelacoesTopologia], extraída de `MainViewModel` (que tem
 * dependências demais — Hilt/Android — para instanciar em teste unitário puro) para alimentar
 * `MainViewModel.correlacoesTopologia` — wiring de #983 (Fase 4) na Dispositivos, ainda sem
 * consumidor real até este ponto.
 */
class ConstruirCorrelacoesTopologiaTest {
    private fun dispositivo(
        id: String,
        mac: String?,
    ) = DispositivoRede(
        id = id,
        ip = "192.168.1.50",
        mac = mac,
        nomeExibicao = "Dispositivo de teste",
        fonteNome = "arp",
    )

    private fun redeClassificada(
        bssid: String,
        papel: PapelTopologia,
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
                confianca = NivelConfianca.ALTA,
                evidencias = emptyList(),
                origemDados = OrigemDados.SCAN_WIFI_PASSIVO,
            )
        return rede to classificacao
    }

    @Test
    fun `sem scan Wi-Fi ou credencial de gateway, todos os dispositivos caem em SEM_MATCH`() {
        val dispositivos = listOf(dispositivo(id = "d1", mac = "AA:BB:CC:11:22:33"), dispositivo(id = "d2", mac = null))

        val resultado =
            construirCorrelacoesTopologia(
                dispositivos = dispositivos,
                redesWifiClassificadas = emptyList(),
                clientesGateway = emptyList(),
            )

        assertEquals(setOf("d1", "d2"), resultado.keys)
        assertTrue(resultado.values.all { it.nivel == NivelCorrelacao.SEM_MATCH })
    }

    @Test
    fun `lista de dispositivos vazia retorna mapa vazio`() {
        val resultado =
            construirCorrelacoesTopologia(
                dispositivos = emptyList(),
                redesWifiClassificadas = emptyList(),
                clientesGateway = emptyList(),
            )

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `dispositivo com ClientSnapshot exato aparece no mapa com nivel e conexao confirmados`() {
        val mac = "C4:6E:1F:AA:BB:CC"
        val dispositivos = listOf(dispositivo(id = "gateway-1", mac = mac))
        val clientes =
            listOf(
                ClientSnapshot(
                    mac = mac,
                    ip = "192.168.1.50",
                    hostname = "host-teste",
                    tipoConexao = null,
                    tipoConexaoFisica = TipoConexaoFisica.ETHERNET,
                ),
            )

        val resultado =
            construirCorrelacoesTopologia(
                dispositivos = dispositivos,
                redesWifiClassificadas = emptyList(),
                clientesGateway = clientes,
            )

        val correlacao = resultado.getValue("gateway-1")
        assertEquals(NivelCorrelacao.CLIENT_SNAPSHOT_EXATO, correlacao.nivel)
        assertEquals(TipoConexaoFisica.ETHERNET, correlacao.tipoConexaoFisicaConfirmada)
    }

    @Test
    fun `dispositivo com mac igual ao bssid de um no Wi-Fi classificado herda o papel`() {
        val mac = "C4:6E:1F:AA:BB:CC"
        val dispositivos = listOf(dispositivo(id = "no-mesh-1", mac = mac))
        val redes = listOf(redeClassificada(bssid = mac, papel = PapelTopologia.NO_MESH))

        val resultado =
            construirCorrelacoesTopologia(
                dispositivos = dispositivos,
                redesWifiClassificadas = redes,
                clientesGateway = emptyList(),
            )

        assertEquals(NivelCorrelacao.MAC_EXATO, resultado.getValue("no-mesh-1").nivel)
        assertEquals(PapelTopologia.NO_MESH, resultado.getValue("no-mesh-1").papelTopologiaHerdado)
    }

    @Test
    fun `cada dispositivo e correlacionado independentemente, chaveado pelo proprio id`() {
        val redeDoRoteador = "C4:6E:1F:AA:BB:CC"
        val roteador = dispositivo(id = "roteador", mac = redeDoRoteador)
        val semRelacao = dispositivo(id = "notebook", mac = "11:22:33:44:55:66")
        val redes = listOf(redeClassificada(bssid = redeDoRoteador, papel = PapelTopologia.ROTEADOR))

        val resultado =
            construirCorrelacoesTopologia(
                dispositivos = listOf(roteador, semRelacao),
                redesWifiClassificadas = redes,
                clientesGateway = emptyList(),
            )

        assertEquals(PapelTopologia.ROTEADOR, resultado.getValue("roteador").papelTopologiaHerdado)
        assertEquals(NivelCorrelacao.SEM_MATCH, resultado.getValue("notebook").nivel)
    }
}
