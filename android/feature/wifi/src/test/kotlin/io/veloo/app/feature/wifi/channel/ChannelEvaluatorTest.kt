package io.veloo.app.feature.wifi.channel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelEvaluatorTest {

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun neighbor24(
        bssid: String,
        channel: Int,
        rssiDbm: Int = -60,
        width: ChannelWidth = ChannelWidth.W20,
    ): Neighbor {
        val centerFreq = channelToCenterFreq(Band.GHZ_24, channel, width)
        return Neighbor(bssid, Band.GHZ_24, centerFreq, null, width, rssiDbm)
    }

    private fun neighbor5(
        bssid: String,
        channel: Int,
        rssiDbm: Int = -60,
        width: ChannelWidth = ChannelWidth.W80,
    ): Neighbor {
        val centerFreq = channelToCenterFreq(Band.GHZ_5, channel, width)
        return Neighbor(bssid, Band.GHZ_5, centerFreq, null, width, rssiDbm)
    }

    // ── Critério 1 — 2.4 GHz não-sobreposto ─────────────────────────────────────

    @Test
    fun `criterio1 tres APs fortes no canal 6 recomenda 1 ou 11 nunca 6`() {
        val aps = List(3) { i ->
            neighbor24(bssid = "AA:BB:CC:00:00:0$i", channel = 6, rssiDbm = -50)
        }
        val scores = evaluateChannels(aps)[Band.GHZ_24]!!
        val rec = scores.first { it.recommended }

        assertNotEquals("Canal 6 congestionado não deve ser recomendado", 6, rec.channel)
        assertTrue("Recomendação deve ser 1 ou 11", rec.channel == 1 || rec.channel == 11)
    }

    // ── Critério 2 — conversão linear (mW, não dBm) ──────────────────────────────

    @Test
    fun `criterio2 dois APs a -40dBm pesam mais que dez APs a -85dBm`() {
        // 2 APs muito fortes no canal 6; 10 APs fracos no canal 1.
        // Canais 1 e 6 não se sobrepõem (Δ≥5 canais), então cada um só é penalizado pelos seus próprios APs.
        val apsFortes = List(2) { i ->
            neighbor24(bssid = "AA:AA:AA:00:00:0$i", channel = 6, rssiDbm = -40)
        }
        val apsFracos = List(10) { i ->
            neighbor24(bssid = "BB:BB:BB:00:00:0$i", channel = 1, rssiDbm = -85)
        }

        val scores = evaluateChannels(apsFortes + apsFracos)[Band.GHZ_24]!!
            .associateBy { it.channel }

        val score6 = scores[6]!!.score
        val score1 = scores[1]!!.score

        // Com mW: score6 = 2×10^(-4) ≈ 0.0002; score1 = 10×10^(-8.5) ≈ 3.16e-8
        // Confirma acumulação linear — se somasse dBm a ordem seria invertida
        assertTrue(
            "score ch6 ($score6) deve ser muito maior que score ch1 ($score1)",
            score6 > score1 * 100,
        )
    }

    // ── Critério 3 — bonding 5 GHz ────────────────────────────────────────────────

    @Test
    fun `criterio3 AP em W80 canal 36 penaliza candidatos 36 40 44 48 nao so 36`() {
        // AP com W80 centrado no grupo {36-48} (centerFreqMhz = 5210, span [5170, 5250])
        val apW80 = Neighbor(
            bssid = "CC:CC:CC:00:00:01",
            band = Band.GHZ_5,
            centerFreqMhz = 5210, // centro do grupo W80 dos canais 36-48
            centerFreq1Mhz = null,
            width = ChannelWidth.W80,
            rssiDbm = -55,
        )

        // Avalia com W20 para ver todos os candidatos individualmente
        val config = EvalConfig(targetWidth5 = ChannelWidth.W20, avoidDfs = false)
        val scores = evaluateChannels(listOf(apW80), config)[Band.GHZ_5]!!
            .associateBy { it.channel }

        // Todos os canais dentro do span [5170, 5250] devem ser penalizados
        listOf(36, 40, 44, 48).forEach { ch ->
            assertTrue("Canal $ch deve ter score > 0", scores[ch]!!.score > 0.0)
        }

        // Canal 52 está fora do span (5250-5270) — sem sobreposição com [5170, 5250]
        assertEquals("Canal 52 deve ter score 0", 0.0, scores[52]!!.score, 1e-15)
    }

    // ── Critério 4 — DFS ──────────────────────────────────────────────────────────

    @Test
    fun `criterio4 avoidDfs true nenhum canal 52 ate 144 recomendado`() {
        val config = EvalConfig(targetWidth5 = ChannelWidth.W20, avoidDfs = true)
        val scores = evaluateChannels(emptyList(), config)[Band.GHZ_5]!!

        val dfsChs = scores.filter { it.channel in 52..144 }
        assertTrue("Com avoidDfs=true, nenhum canal DFS deve aparecer no ranking", dfsChs.isEmpty())

        val rec = scores.first { it.recommended }
        assertFalse("Canal recomendado não deve ser DFS", rec.isDfs)
    }

    @Test
    fun `criterio4 avoidDfs false canais DFS aparecem com score penalizado`() {
        // AP forte em canal não-DFS 36 para que algum DFS "vença" na disputa
        val apNaoDfs = neighbor5(bssid = "DD:DD:DD:00:00:01", channel = 36, rssiDbm = -30)
        val config = EvalConfig(targetWidth5 = ChannelWidth.W20, avoidDfs = false, dfsPenalty = 1.3)
        val scores = evaluateChannels(listOf(apNaoDfs), config)[Band.GHZ_5]!!

        val ch36Score = scores.first { it.channel == 36 }.score
        val ch52Score = scores.first { it.channel == 52 }.score

        // Canal 52 tem score 0 (sem AP) mas isDfs=true → finalScore = 0 * 1.3 = 0 ainda (sem neighbor)
        // O teste real: um canal DFS COM vizinho deve receber a penalidade
        val apDfs = Neighbor("EE:EE:EE:00:00:01", Band.GHZ_5, 5260, null, ChannelWidth.W20, -55)
        val scoresComDfs = evaluateChannels(listOf(apDfs), config)[Band.GHZ_5]!!
        val ch52ScoreComAp = scoresComDfs.first { it.channel == 52 }.score
        val ch52ScoreSemPenalidade = dbmToMw(-55) // score sem penalidade

        assertEquals(
            "Score DFS deve ser score_base × dfsPenalty",
            ch52ScoreSemPenalidade * 1.3,
            ch52ScoreComAp,
            1e-15,
        )
        assertTrue("Canal 52 deve ser marcado como DFS", scoresComDfs.first { it.channel == 52 }.isDfs)
    }

    // ── Critério 5 — PSC 6 GHz ────────────────────────────────────────────────────

    @Test
    fun `criterio5 preferPsc true apenas canais PSC no ranking`() {
        val pscSet = setOf(5, 21, 37, 53, 69, 85, 101, 117, 133, 149, 165, 181, 197, 213, 229)
        val config = EvalConfig(preferPsc = true)
        val scores = evaluateChannels(emptyList(), config)[Band.GHZ_6]!!

        assertTrue("Com preferPsc=true deve haver canais no ranking", scores.isNotEmpty())
        scores.forEach { s ->
            assertTrue("Canal ${s.channel} não é PSC", s.channel in pscSet)
            assertTrue("Canal PSC deve ter isPsc=true", s.isPsc)
        }
    }

    @Test
    fun `criterio5 preferPsc false inclui canais nao-PSC`() {
        val pscSet = setOf(5, 21, 37, 53, 69, 85, 101, 117, 133, 149, 165, 181, 197, 213, 229)
        val config = EvalConfig(preferPsc = false)
        val scores = evaluateChannels(emptyList(), config)[Band.GHZ_6]!!

        val nonPsc = scores.filter { it.channel !in pscSet }
        assertTrue("Com preferPsc=false deve haver canais não-PSC no ranking", nonPsc.isNotEmpty())
    }

    // ── Critério 6 — excludeBssids ────────────────────────────────────────────────

    @Test
    fun `criterio6 bssid em excludeBssids nao contribui para score`() {
        val bssidExcluido = "FF:FF:FF:00:00:01"
        val ap = neighbor24(bssid = bssidExcluido, channel = 6, rssiDbm = -40)
        val config = EvalConfig(excludeBssids = setOf(bssidExcluido))

        val scores = evaluateChannels(listOf(ap), config)[Band.GHZ_24]!!
        // Canal 6 deve ter score 0 porque o único AP foi excluído
        val score6 = scores.first { it.channel == 6 }.score
        assertEquals("AP excluído não deve contribuir para o score", 0.0, score6, 1e-15)
    }

    // ── Critério 7 — entrada vazia ────────────────────────────────────────────────

    @Test
    fun `criterio7 lista vazia retorna rankings com score zero sem excecao`() {
        val result = evaluateChannels(emptyList())

        Band.entries.forEach { band ->
            val scores = result[band]!!
            assertTrue("Banda $band deve ter candidatos mesmo sem vizinhos", scores.isNotEmpty())
            scores.forEach { s ->
                assertEquals("Score deve ser 0.0 sem vizinhos (banda=$band ch=${s.channel})", 0.0, s.score, 1e-15)
            }
        }
    }

    @Test
    fun `criterio7 ranked tem exatamente um recomendado por banda`() {
        val result = evaluateChannels(emptyList())
        Band.entries.forEach { band ->
            val recommended = result[band]!!.filter { it.recommended }
            assertEquals("Deve haver exatamente 1 recomendado na banda $band", 1, recommended.size)
        }
    }

    // ── Critério 8 — desempate por overlappingAps ─────────────────────────────────

    @Test
    fun `criterio8 score identico desempata por numero de APs sobrepostos`() {
        // Canal 1: 1 AP com overlap 100% → score = dbmToMw(-50) × 1.0
        // Canal 11: 2 APs com overlap 50% cada → score = dbmToMw(-50) × 0.5 × 2 = dbmToMw(-50)
        // Score idêntico, mas ch 1 tem 1 AP e ch 11 tem 2 → ch 1 vence
        //
        // Para ch 11 (center 2462, span [2452, 2472]):
        //   AP A no ch 9 (center 2452, span [2442, 2462]): overlap = 10 MHz → fraction 0.5
        //   AP B no ch 13 (center 2472, span [2462, 2482]): overlap = 10 MHz → fraction 0.5
        //
        // Para ch 1 (center 2412, span [2402, 2422]):
        //   AP C no ch 1: overlap = 20 MHz → fraction 1.0
        //   APs A e B não atingem ch 1 (Δ≥5 canais).

        val apA = neighbor24("AA:00:00:00:00:01", channel = 9, rssiDbm = -50)
        val apB = neighbor24("AA:00:00:00:00:02", channel = 13, rssiDbm = -50)
        val apC = neighbor24("AA:00:00:00:00:03", channel = 1, rssiDbm = -50)

        val config = EvalConfig(allow24Overlapping = true)
        val scores = evaluateChannels(listOf(apA, apB, apC), config)[Band.GHZ_24]!!
            .associateBy { it.channel }

        val s1 = scores[1]!!
        val s11 = scores[11]!!

        assertEquals("Scores devem ser iguais", s1.score, s11.score, 1e-12)
        assertEquals("Ch 1 deve ter 1 AP sobreposto", 1, s1.overlappingAps)
        assertEquals("Ch 11 deve ter 2 APs sobrepostos", 2, s11.overlappingAps)

        // No ranking, ch 1 deve aparecer antes de ch 11
        val rank1 = scores.values.sortedWith(
            compareBy({ it.score }, { it.overlappingAps }, { it.channel }),
        ).indexOfFirst { it.channel == 1 }
        val rank11 = scores.values.sortedWith(
            compareBy({ it.score }, { it.overlappingAps }, { it.channel }),
        ).indexOfFirst { it.channel == 11 }

        assertTrue("Ch 1 (rank=$rank1) deve vencer ch 11 (rank=$rank11) no desempate", rank1 < rank11)
    }
}
