package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.TipoTopologia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GH#1025 (3c) — cobre [resolverDispositivoParaNoTopologia], a lógica pura de decisão usada por
 * `SinalScreen` pra rotear o clique num nó da árvore de topologia entre `MeshApSheet` (dado real
 * do scan LAN) e o fallback padrão (`NetworkDetailSheet`, quando o chamador recebe null).
 */
class ResolverDispositivoParaNoTopologiaTest {
    private val rede =
        RedeVizinha(
            ssid = "CasaSilva",
            bssid = "50:C7:BF:00:00:02",
            rssiDbm = -65,
            frequenciaMhz = 2412,
            seguranca = SegurancaWifi.wpa2,
            larguraCanalMhz = 20,
        )

    private val dispositivoCorrelacionado =
        DispositivoRede(
            id = "dev-1",
            ip = "192.168.1.2",
            mac = "50:C7:BF:00:00:02",
            nomeExibicao = "Nó Mesh",
            fonteNome = "arp",
        )

    @Test
    fun `no classificado como NO_MESH com dispositivo correlacionado retorna o dispositivo`() {
        val resultado =
            resolverDispositivoParaNoTopologia(
                rede = rede,
                tipoTopologia = TipoTopologia.NO_MESH,
                dispositivosRede = listOf(dispositivoCorrelacionado),
            )

        assertEquals(dispositivoCorrelacionado, resultado)
    }

    @Test
    fun `no classificado como PONTO_DE_ACESSO com dispositivo correlacionado retorna o dispositivo`() {
        val resultado =
            resolverDispositivoParaNoTopologia(
                rede = rede,
                tipoTopologia = TipoTopologia.PONTO_DE_ACESSO,
                dispositivosRede = listOf(dispositivoCorrelacionado),
            )

        assertEquals(dispositivoCorrelacionado, resultado)
    }

    @Test
    fun `no classificado como ROTEADOR nunca abre MeshApSheet mesmo com dispositivo correlacionado`() {
        val resultado =
            resolverDispositivoParaNoTopologia(
                rede = rede,
                tipoTopologia = TipoTopologia.ROTEADOR,
                dispositivosRede = listOf(dispositivoCorrelacionado),
            )

        assertNull(resultado)
    }

    @Test
    fun `no sem classificacao nunca abre MeshApSheet`() {
        val resultado =
            resolverDispositivoParaNoTopologia(
                rede = rede,
                tipoTopologia = null,
                dispositivosRede = listOf(dispositivoCorrelacionado),
            )

        assertNull(resultado)
    }

    @Test
    fun `no AP mesh sem dispositivo correlacionado retorna null - fallback pro chamador`() {
        val resultado =
            resolverDispositivoParaNoTopologia(
                rede = rede,
                tipoTopologia = TipoTopologia.NO_MESH,
                dispositivosRede = emptyList(),
            )

        assertNull(resultado)
    }

    @Test
    fun `no REPETIDOR com dispositivo correlacionado retorna o dispositivo`() {
        val resultado =
            resolverDispositivoParaNoTopologia(
                rede = rede,
                tipoTopologia = TipoTopologia.REPETIDOR,
                dispositivosRede = listOf(dispositivoCorrelacionado),
            )

        assertEquals(dispositivoCorrelacionado, resultado)
    }
}
