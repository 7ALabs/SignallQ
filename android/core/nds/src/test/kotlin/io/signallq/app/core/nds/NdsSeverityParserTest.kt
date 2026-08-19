package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.MetricStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class NdsSeverityParserTest {

    @Test
    fun `os 6 valores conhecidos mapeiam para o MetricStatus correspondente`() {
        assertEquals(MetricStatus.excelente, parseNdsVeredicto("excelente"))
        assertEquals(MetricStatus.bom, parseNdsVeredicto("bom"))
        assertEquals(MetricStatus.regular, parseNdsVeredicto("regular"))
        assertEquals(MetricStatus.ruim, parseNdsVeredicto("ruim"))
        assertEquals(MetricStatus.critico, parseNdsVeredicto("critico"))
        assertEquals(MetricStatus.inconclusivo, parseNdsVeredicto("inconclusivo"))
    }

    @Test
    fun `veredicto fora do enum cai em inconclusivo em vez de lancar excecao`() {
        assertEquals(MetricStatus.inconclusivo, parseNdsVeredicto("fraco"))
    }

    @Test
    fun `veredicto nulo cai em inconclusivo`() {
        assertEquals(MetricStatus.inconclusivo, parseNdsVeredicto(null))
    }

    @Test
    fun `veredicto vazio cai em inconclusivo`() {
        assertEquals(MetricStatus.inconclusivo, parseNdsVeredicto(""))
    }

    @Test
    fun `veredicto com caixa diferente cai em inconclusivo (valueOf e case-sensitive)`() {
        assertEquals(MetricStatus.inconclusivo, parseNdsVeredicto("REGULAR"))
    }
}
