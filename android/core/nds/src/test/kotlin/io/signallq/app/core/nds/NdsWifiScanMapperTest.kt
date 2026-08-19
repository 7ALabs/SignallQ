package io.signallq.app.core.nds

import io.signallq.app.core.network.contracts.wifi.channel.Band
import io.signallq.app.core.network.contracts.wifi.channel.ChannelScore
import io.signallq.app.core.network.contracts.wifi.channel.ChannelWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
