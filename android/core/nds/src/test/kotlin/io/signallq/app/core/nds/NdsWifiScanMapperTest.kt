package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.RedeWifiVizinha
import io.signallq.app.core.diagnostico.WifiScanDiagnosticInput
import io.signallq.app.core.network.contracts.wifi.channel.Band
import io.signallq.app.core.network.contracts.wifi.channel.ChannelScore
import io.signallq.app.core.network.contracts.wifi.channel.ChannelWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NdsWifiScanMapperTest {
    private fun score(
        channel: Int,
        score: Double,
        recommended: Boolean = false,
    ) = ChannelScore(
        band = Band.GHZ_5,
        channel = channel,
        width = ChannelWidth.W80,
        score = score,
        overlappingAps = 0,
        strongestNeighborDbm = null,
        isDfs = false,
        isPsc = false,
        recommended = recommended,
    )

    @Test
    fun `canal livre (score zero) mapeia para channelCongestion zero`() {
        val bandScores = listOf(score(channel = 36, score = 0.0, recommended = true))

        val resultado = mapWifiScanToNds(bandScores, canalConectado = 36)

        assertEquals(0, resultado.channelCongestion)
        assertEquals(36, resultado.bestChannel)
    }

    @Test
    fun `canal muito congestionado satura em 100, nunca estoura o range`() {
        val bandScores = listOf(score(channel = 36, score = 1.0e9, recommended = false))

        val resultado = mapWifiScanToNds(bandScores, canalConectado = 36)

        assertEquals(100, resultado.channelCongestion)
    }

    @Test
    fun `bestChannel usa o candidato marcado recommended, nao o primeiro da lista`() {
        val bandScores =
            listOf(
                score(channel = 36, score = 500.0, recommended = false),
                score(channel = 149, score = 10.0, recommended = true),
            )

        val resultado = mapWifiScanToNds(bandScores, canalConectado = 36)

        assertEquals(149, resultado.bestChannel)
    }

    @Test
    fun `sem candidato recommended, bestChannel fica nulo`() {
        val bandScores = listOf(score(channel = 36, score = 500.0, recommended = false))

        val resultado = mapWifiScanToNds(bandScores, canalConectado = 36)

        assertNull(resultado.bestChannel)
    }

    @Test
    fun `sem canal conectado (null), channelCongestion fica nulo mesmo com scores disponiveis`() {
        val bandScores = listOf(score(channel = 36, score = 500.0, recommended = true))

        val resultado = mapWifiScanToNds(bandScores, canalConectado = null)

        assertNull(resultado.channelCongestion)
    }

    @Test
    fun `canal conectado fora da lista de scores, channelCongestion fica nulo`() {
        val bandScores = listOf(score(channel = 36, score = 500.0, recommended = true))

        val resultado = mapWifiScanToNds(bandScores, canalConectado = 149)

        assertNull(resultado.channelCongestion)
    }

    @Test
    fun `lista de scores vazia produz os dois campos nulos, sem excecao`() {
        val resultado = mapWifiScanToNds(bandScores = emptyList(), canalConectado = 36)

        assertNull(resultado.bestChannel)
        assertNull(resultado.channelCongestion)
    }

    @Test
    fun `redes vizinhas viram evidencia sem depender de estarem no bandScores`() {
        val bandScores = listOf(score(channel = 36, score = 0.0, recommended = true))
        val redes =
            listOf(
                RedeWifiVizinha(canal = 40, rssiDbm = -55, frequenciaMhz = 5200, larguraCanalMhz = 80),
                RedeWifiVizinha(canal = 44, rssiDbm = -70, frequenciaMhz = 5220),
            )

        val resultado = mapWifiScanToNds(bandScores, canalConectado = 36, redesVizinhas = redes)

        assertEquals(2, resultado.neighborCount)
        assertEquals(2, resultado.neighbors.size)
        assertEquals(40, resultado.neighbors[0].channel)
        assertEquals(5200, resultado.neighbors[0].frequencyMhz)
        assertEquals(-55, resultado.neighbors[0].rssiDbm)
        assertEquals(80, resultado.neighbors[0].widthMhz)
        // Sem largura reportada pelo scan -- nao inventa 20 MHz, fica nula.
        assertNull(resultado.neighbors[1].widthMhz)
    }

    @Test
    fun `algorithmVersion presente quando ha score calculado, nulo quando nao ha`() {
        val comScore = mapWifiScanToNds(listOf(score(channel = 36, score = 0.0, recommended = true)), canalConectado = 36)
        val semScore = mapWifiScanToNds(emptyList(), canalConectado = null)

        assertEquals("channel-evaluator@1", comScore.algorithmVersion)
        assertNull(semScore.algorithmVersion)
    }

    @Test
    fun `zero redes vizinhas com canal conectado produz neighborCount zero, nao nulo`() {
        val resultado = mapWifiScanToNds(emptyList(), canalConectado = 36, redesVizinhas = emptyList())

        assertEquals(0, resultado.neighborCount)
        assertTrue(resultado.neighbors.isEmpty())
    }

    // ── toNdsWifiScanInfo (ponte WifiScanDiagnosticInput -> NdsWifiScanInfo) ────

    @Test
    fun `scan nulo produz bloco ausente`() {
        assertNull(null.toNdsWifiScanInfo(BandaWifi.ghz5))
    }

    @Test
    fun `scan sem nenhuma evidencia (sem permissao de scan) produz bloco ausente`() {
        val scan = WifiScanDiagnosticInput()

        assertNull(scan.toNdsWifiScanInfo(BandaWifi.desconhecida))
    }

    @Test
    fun `sem conexao wifi ativa (banda desconhecida) ainda localiza banda pelo canal conectado`() {
        val scan =
            WifiScanDiagnosticInput(
                conectadoCanal = 36,
                redes =
                    listOf(
                        RedeWifiVizinha(canal = 36, rssiDbm = -60, frequenciaMhz = 5180, bssid = "AA:AA:AA:00:00:01"),
                        RedeWifiVizinha(canal = 40, rssiDbm = -50, frequenciaMhz = 5200, bssid = "AA:AA:AA:00:00:02"),
                    ),
            )

        val resultado = scan.toNdsWifiScanInfo(bandaConectada = BandaWifi.desconhecida)

        assertEquals(36, resultado?.connectedChannel)
        assertEquals(2, resultado?.neighborCount)
    }

    @Test
    fun `scan completo com banda conectada calcula congestionamento e melhor canal`() {
        val scan =
            WifiScanDiagnosticInput(
                conectadoCanal = 36,
                conectadoBanda = BandaWifi.ghz5,
                redes =
                    listOf(
                        RedeWifiVizinha(canal = 36, rssiDbm = -40, frequenciaMhz = 5180, bssid = "AA:AA:AA:00:00:01"),
                        RedeWifiVizinha(canal = 149, rssiDbm = -80, frequenciaMhz = 5745, bssid = "AA:AA:AA:00:00:02"),
                    ),
            )

        val resultado = scan.toNdsWifiScanInfo(bandaConectada = BandaWifi.ghz5)

        assertEquals(36, resultado?.connectedChannel)
        assertTrue("canal 36 tem vizinha forte no mesmo canal -- deve estar mais congestionado que 0", (resultado?.channelCongestion ?: 0) > 0)
        assertEquals("channel-evaluator@1", resultado?.algorithmVersion)
    }

    @Test
    fun `zero redes vizinhas com canal conectado presente produz canal livre, nao ausencia`() {
        val scan = WifiScanDiagnosticInput(conectadoCanal = 36, conectadoBanda = BandaWifi.ghz5, redes = emptyList())

        val resultado = scan.toNdsWifiScanInfo(bandaConectada = BandaWifi.ghz5)

        // Scan rodou e nao achou vizinha nenhuma -- isso e uma medicao real de
        // "canal livre" (congestion 0), nao a ausencia de avaliacao (null).
        assertEquals(0, resultado?.neighborCount)
        assertEquals(0, resultado?.channelCongestion)
        assertEquals("channel-evaluator@1", resultado?.algorithmVersion)
    }

    @Test
    fun `redes com dados invalidos (sem frequencia ou rssi) nao quebram e viram evidencia parcial`() {
        val scan =
            WifiScanDiagnosticInput(
                conectadoCanal = 36,
                conectadoBanda = BandaWifi.ghz5,
                redes =
                    listOf(
                        RedeWifiVizinha(canal = null, rssiDbm = null, frequenciaMhz = null),
                        RedeWifiVizinha(canal = 40, rssiDbm = -60, frequenciaMhz = null),
                    ),
            )

        val resultado = scan.toNdsWifiScanInfo(bandaConectada = BandaWifi.ghz5)

        assertEquals(2, resultado?.neighborCount)
        assertEquals(2, resultado?.neighbors?.size)
        assertNull(resultado?.neighbors?.get(0)?.channel)
        assertEquals(40, resultado?.neighbors?.get(1)?.channel)
        // Nenhuma das duas e utilizavel pelo ChannelEvaluator (falta frequencia) --
        // sem excecao, so sem contribuicao pro calculo de congestionamento (mesmo
        // resultado de zero vizinhas utilizaveis: canal avaliado como livre).
        assertEquals(0, resultado?.channelCongestion)
    }
}
