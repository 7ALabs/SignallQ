package io.signallq.app.feature.diagnostico.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicQuestionEngineTest {

    private val engine = DynamicQuestionEngine()

    // ---- getInitialChips ----

    @Test
    fun `getInitialChips without report returns 6 chips`() {
        val chips = engine.getInitialChips(null)
        assertEquals(6, chips.size)
    }

    @Test
    fun `getInitialChips covers all expected topics`() {
        val chips = engine.getInitialChips(null)
        val ids = chips.map { it.id }.toSet()
        assertTrue(ids.contains("internet_lenta"))
        assertTrue(ids.contains("jogos_travando"))
        assertTrue(ids.contains("streaming_ruim"))
        assertTrue(ids.contains("wifi_oscilando"))
        assertTrue(ids.contains("chamadas_ruins"))
        assertTrue(ids.contains("nao_sei_explicar"))
    }

    // ---- getNextQuestion depth 0 ----

    @Test
    fun `jogos_travando first question returned on empty history`() {
        val q = engine.getNextQuestion("jogos_travando", emptyList())
        assertNotNull(q)
        assertEquals("jogos_q1", q!!.id)
        assertEquals(3, q.opcoes.size)
    }

    @Test
    fun `internet_lenta first question returned on empty history`() {
        val q = engine.getNextQuestion("internet_lenta", emptyList())
        assertNotNull(q)
        assertEquals("internet_lenta_q1", q!!.id)
    }

    @Test
    fun `wifi_oscilando first question returned on empty history`() {
        val q = engine.getNextQuestion("wifi_oscilando", emptyList())
        assertNotNull(q)
        assertEquals("wifi_q1", q!!.id)
    }

    @Test
    fun `unknown chip returns null`() {
        assertNull(engine.getNextQuestion("desconhecido", emptyList()))
    }

    // ---- qual_jogo_device (SIG-290) ----

    @Test
    fun `qual_jogo_device first question returned on empty history`() {
        val q = engine.getNextQuestion("qual_jogo_device", emptyList())
        assertNotNull(q)
        assertEquals("qual_jogo_device_q1", q!!.id)
        assertEquals(5, q.opcoes.size)
    }

    @Test
    fun `qual_jogo_device covers the 5 device presets`() {
        val q = engine.getNextQuestion("qual_jogo_device", emptyList())
        val ids = q!!.opcoes.map { it.id }.toSet()
        assertEquals(setOf("playstation", "xbox", "pc", "switch", "mobile"), ids)
    }

    @Test
    fun `qual_jogo_device xbox is leaf at root level`() {
        assertTrue(engine.isLeafAnswer("qual_jogo_device", "xbox", emptyList()))
    }

    @Test
    fun `qual_jogo_device returns null after answering once`() {
        val history = listOf(
            QuestionAnswer("qual_jogo_device_q1", "Qual jogo ou console?", "xbox", "Xbox", "Xbox."),
        )
        assertNull(engine.getNextQuestion("qual_jogo_device", history))
    }

    // ---- getNextQuestion depth 1 ----

    @Test
    fun `jogos console follow-up question is connection type`() {
        val history = listOf(
            QuestionAnswer("jogos_q1", "Qual dispositivo?", "console", "Console", "Jogo em console."),
        )
        val q = engine.getNextQuestion("jogos_travando", history)
        assertNotNull(q)
        assertEquals("jogos_q2_console", q!!.id)
        assertEquals(2, q.opcoes.size)
    }

    @Test
    fun `internet_lenta sempre follow-up question asks about situation`() {
        val history = listOf(
            QuestionAnswer("internet_lenta_q1", "Quando ocorre?", "sempre", "Sempre", "Lentidão constante."),
        )
        val q = engine.getNextQuestion("internet_lenta", history)
        assertNotNull(q)
        assertEquals("internet_lenta_q2a", q!!.id)
    }

    @Test
    fun `wifi proximo follow-up question asks when oscillation occurs`() {
        val history = listOf(
            QuestionAnswer("wifi_q1", "Em qual área?", "proximo", "Perto do roteador", "Oscilação perto do roteador."),
        )
        val q = engine.getNextQuestion("wifi_oscilando", history)
        assertNotNull(q)
        assertEquals("wifi_q2_proximo", q!!.id)
    }

    // ---- getNextQuestion depth 2 (leaf) ----

    @Test
    fun `jogos console cabo returns null after two answers`() {
        val history = listOf(
            QuestionAnswer("jogos_q1", "Qual dispositivo?", "console", "Console", "Console."),
            QuestionAnswer("jogos_q2_console", "Conexão?", "cabo", "Cabo Ethernet", "Cabo."),
        )
        val q = engine.getNextQuestion("jogos_travando", history)
        assertNull(q)
    }

    @Test
    fun `internet sempre web returns null after two answers`() {
        val history = listOf(
            QuestionAnswer("internet_lenta_q1", "Quando?", "sempre", "Sempre", "Constante."),
            QuestionAnswer("internet_lenta_q2a", "Situação?", "web", "Navegar na web", "Web."),
        )
        val q = engine.getNextQuestion("internet_lenta", history)
        assertNull(q)
    }

    // ---- isLeafAnswer ----

    @Test
    fun `jogos console cabo is leaf after first question`() {
        val history = listOf(
            QuestionAnswer("jogos_q1", "Qual dispositivo?", "console", "Console", "Console."),
        )
        assertTrue(engine.isLeafAnswer("jogos_travando", "cabo", history))
    }

    @Test
    fun `jogos console is NOT leaf at root level`() {
        assertFalse(engine.isLeafAnswer("jogos_travando", "console", emptyList()))
    }

    @Test
    fun `streaming outro_str is leaf at root level`() {
        assertTrue(engine.isLeafAnswer("streaming_ruim", "outro_str", emptyList()))
    }

    @Test
    fun `wifi toda_casa is leaf at root level`() {
        assertTrue(engine.isLeafAnswer("wifi_oscilando", "toda_casa", emptyList()))
    }

    @Test
    fun `chamadas celular_op is leaf at root level`() {
        assertTrue(engine.isLeafAnswer("chamadas_ruins", "celular_op", emptyList()))
    }

    // ---- buildContextContribution ----

    @Test
    fun `buildContextContribution contains question and answer texts`() {
        val q = QuestionNode("q1", "Qual dispositivo?", emptyList())
        val a = OpcaoResposta("console", "Console", "Jogo em console doméstico.")
        val ctx = engine.buildContextContribution(q, a)
        assertTrue(ctx.contains("Qual dispositivo?"))
        assertTrue(ctx.contains("Console"))
        assertTrue(ctx.contains("Jogo em console doméstico."))
    }

    // ---- RotatingMessageProvider ----

    @Test
    fun `RotatingMessageProvider has messages for all PulseStates`() {
        PulseState.entries.forEach { state ->
            val first = RotatingMessageProvider.first(state)
            assertTrue("Estado $state não tem mensagem", first.isNotBlank())
        }
    }

    @Test
    fun `RotatingMessageProvider next cycles through messages`() {
        val msgs = RotatingMessageProvider.all(PulseState.Collecting)
        assertTrue(msgs.size >= 2)
        val first = msgs[0]
        val second = RotatingMessageProvider.next(PulseState.Collecting, first)
        assertEquals(msgs[1], second)
    }

    @Test
    fun `RotatingMessageProvider next wraps around`() {
        val msgs = RotatingMessageProvider.all(PulseState.Collecting)
        val last = msgs.last()
        val next = RotatingMessageProvider.next(PulseState.Collecting, last)
        assertEquals(msgs[0], next)
    }

    // ---- ContextAccumulator ----

    @Test
    fun `ContextAccumulator buildInitial contains speedtest data`() {
        val ctx = ContextAccumulator.buildInitial(
            downloadMbps = 50.0, uploadMbps = 10.0, latencyMs = 25.0,
            jitterMs = 3.0, lossPercent = 0.5, stabilityScore = 85.0,
            wifiSsid = "MinhaRede", wifiRssiDbm = -60, wifiFrequencyMhz = 5200,
            report = null,
        )
        assertTrue(ctx.contains("50"))
        assertTrue(ctx.contains("10"))
        assertTrue(ctx.contains("MinhaRede"))
        assertTrue(ctx.contains("5 GHz"))
    }

    @Test
    fun `ContextAccumulator appendChip adds chip info`() {
        val base = "contexto base"
        val chip = OpcaoResposta("jogos_travando", "Jogos travando", "Travamento em jogos.")
        val result = ContextAccumulator.appendChip(base, chip)
        assertTrue(result.contains("contexto base"))
        assertTrue(result.contains("Jogos travando"))
        assertTrue(result.contains("Travamento em jogos."))
    }

    @Test
    fun `ContextAccumulator appendAnswer adds question and answer`() {
        val base = "contexto"
        val q = QuestionNode("q1", "Qual dispositivo?", emptyList())
        val a = OpcaoResposta("console", "Console", "Console doméstico.")
        val result = ContextAccumulator.appendAnswer(base, q, a)
        assertTrue(result.contains("Qual dispositivo?"))
        assertTrue(result.contains("Console"))
    }
}
