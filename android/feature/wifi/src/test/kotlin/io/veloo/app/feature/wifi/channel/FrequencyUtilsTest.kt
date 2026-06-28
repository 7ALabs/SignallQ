package io.signallq.app.feature.wifi.channel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyUtilsTest {

    // ── freqToChannel ───────────────────────────────────────────────────────────

    @Test
    fun `freqToChannel canal 1 em 2_4GHz`() {
        val (band, ch) = freqToChannel(2412)!!
        assertEquals(Band.GHZ_24, band)
        assertEquals(1, ch)
    }

    @Test
    fun `freqToChannel canal 6 em 2_4GHz`() {
        val (band, ch) = freqToChannel(2437)!!
        assertEquals(Band.GHZ_24, band)
        assertEquals(6, ch)
    }

    @Test
    fun `freqToChannel canal 11 em 2_4GHz`() {
        val (band, ch) = freqToChannel(2462)!!
        assertEquals(Band.GHZ_24, band)
        assertEquals(11, ch)
    }

    @Test
    fun `freqToChannel canal 36 em 5GHz`() {
        val (band, ch) = freqToChannel(5180)!!
        assertEquals(Band.GHZ_5, band)
        assertEquals(36, ch)
    }

    @Test
    fun `freqToChannel canal 149 em 5GHz`() {
        val (band, ch) = freqToChannel(5745)!!
        assertEquals(Band.GHZ_5, band)
        assertEquals(149, ch)
    }

    @Test
    fun `freqToChannel canal PSC 5 em 6GHz`() {
        val (band, ch) = freqToChannel(5975)!!
        assertEquals(Band.GHZ_6, band)
        assertEquals(5, ch)
    }

    @Test
    fun `freqToChannel canal PSC 21 em 6GHz`() {
        val (band, ch) = freqToChannel(6055)!!
        assertEquals(Band.GHZ_6, band)
        assertEquals(21, ch)
    }

    @Test
    fun `freqToChannel frequencia invalida retorna null`() {
        assertNull(freqToChannel(3000))
        assertNull(freqToChannel(0))
    }

    // ── channelToCenterFreq ─────────────────────────────────────────────────────

    @Test
    fun `channelToCenterFreq 2_4GHz canais padrao`() {
        assertEquals(2412, channelToCenterFreq(Band.GHZ_24, 1, ChannelWidth.W20))
        assertEquals(2437, channelToCenterFreq(Band.GHZ_24, 6, ChannelWidth.W20))
        assertEquals(2462, channelToCenterFreq(Band.GHZ_24, 11, ChannelWidth.W20))
    }

    @Test
    fun `channelToCenterFreq 5GHz W20 canal 36`() {
        assertEquals(5180, channelToCenterFreq(Band.GHZ_5, 36, ChannelWidth.W20))
    }

    @Test
    fun `channelToCenterFreq 5GHz W40 canal 36 e 40 mesmo centro`() {
        val center36 = channelToCenterFreq(Band.GHZ_5, 36, ChannelWidth.W40)
        val center40 = channelToCenterFreq(Band.GHZ_5, 40, ChannelWidth.W40)
        assertEquals(5190, center36) // grupo (36,40): centro = 5170+20 = 5190
        assertEquals(center36, center40)
    }

    @Test
    fun `channelToCenterFreq 5GHz W80 canais 36 ate 48 mesmo centro`() {
        val expected = 5210 // grupo (36-48): base 5170 + 0*80 + 40
        listOf(36, 40, 44, 48).forEach { ch ->
            assertEquals("ch=$ch", expected, channelToCenterFreq(Band.GHZ_5, ch, ChannelWidth.W80))
        }
    }

    @Test
    fun `channelToCenterFreq 5GHz W80 canal 52 proximo grupo`() {
        assertEquals(5290, channelToCenterFreq(Band.GHZ_5, 52, ChannelWidth.W80))
    }

    @Test
    fun `channelToCenterFreq 6GHz W80 canal PSC 5`() {
        // ch 5 = 5975 MHz; grupo W80 [5955, 6035]; centro = 5995
        assertEquals(5995, channelToCenterFreq(Band.GHZ_6, 5, ChannelWidth.W80))
    }

    @Test
    fun `channelToCenterFreq 6GHz W80 canal PSC 21`() {
        // ch 21 = 6055 MHz; grupo W80 [6035, 6115]; centro = 6075
        assertEquals(6075, channelToCenterFreq(Band.GHZ_6, 21, ChannelWidth.W80))
    }

    // ── dbmToMw ─────────────────────────────────────────────────────────────────

    @Test
    fun `dbmToMw -30dBm da 0_001mW`() {
        assertEquals(0.001, dbmToMw(-30), 1e-10)
    }

    @Test
    fun `dbmToMw -40dBm muito maior que -85dBm`() {
        // Valida que escala é logarítmica: -40 dBm é 10^(45/10) ≈ 31623x mais forte que -85 dBm
        val forte = dbmToMw(-40)
        val fraco = dbmToMw(-85)
        assertTrue("$forte deve ser muito maior que $fraco", forte > fraco * 10_000)
    }

    // ── overlapMhz ──────────────────────────────────────────────────────────────

    @Test
    fun `overlapMhz sobreposicao total`() {
        assertEquals(20, overlapMhz(2427, 2447, 2427, 2447))
    }

    @Test
    fun `overlapMhz sem sobreposicao canais 1 e 6`() {
        // ch 1 W20: [2402, 2422]; ch 6 W20: [2427, 2447] — separação ≥5 canais = sem overlap
        assertEquals(0, overlapMhz(2402, 2422, 2427, 2447))
    }

    @Test
    fun `overlapMhz parcial canal 6 e canal 7`() {
        // ch 6 W20: [2427, 2447]; ch 7 W20: [2432, 2452]
        assertEquals(15, overlapMhz(2427, 2447, 2432, 2452))
    }

    // ── candidateSpan ────────────────────────────────────────────────────────────

    @Test
    fun `candidateSpan 5GHz W80 canal 36 cobre 36 ate 48`() {
        val (lo, hi) = candidateSpan(Band.GHZ_5, 36, ChannelWidth.W80)
        assertEquals(5170, lo)
        assertEquals(5250, hi)
    }
}
