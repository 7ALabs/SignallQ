package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.DiagnosticStatus
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

    // -------------------------------------------------------------------
    // toDiagnosticStatus() — segundo salto MetricStatus -> DiagnosticStatus
    // (NDS-02k, issue #1759, item 5).
    // -------------------------------------------------------------------

    @Test
    fun `excelente e bom colapsam para ok`() {
        assertEquals(DiagnosticStatus.ok, MetricStatus.excelente.toDiagnosticStatus())
        assertEquals(DiagnosticStatus.ok, MetricStatus.bom.toDiagnosticStatus())
    }

    @Test
    fun `regular vira info`() {
        assertEquals(DiagnosticStatus.info, MetricStatus.regular.toDiagnosticStatus())
    }

    @Test
    fun `ruim vira attention`() {
        assertEquals(DiagnosticStatus.attention, MetricStatus.ruim.toDiagnosticStatus())
    }

    @Test
    fun `critico vira critical`() {
        assertEquals(DiagnosticStatus.critical, MetricStatus.critico.toDiagnosticStatus())
    }

    @Test
    fun `inconclusivo vira inconclusive`() {
        assertEquals(DiagnosticStatus.inconclusive, MetricStatus.inconclusivo.toDiagnosticStatus())
    }

    @Test
    fun `todos os 6 valores de MetricStatus tem mapeamento (nunca lanca excecao)`() {
        MetricStatus.entries.forEach { it.toDiagnosticStatus() }
    }
}
