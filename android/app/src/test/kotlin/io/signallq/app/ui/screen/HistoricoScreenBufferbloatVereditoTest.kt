package io.signallq.app.ui.screen

import io.signallq.app.ui.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Teste de caracterização — issue #1749 (NDS-02b), escrito ANTES de `bufferbloatVeredito` trocar
 * `MetricClassifier.classificarBufferbloat` direto por [classificarBufferbloatLocal]
 * (`ClassificacaoMetricaLocal.kt`). Prova que o rótulo/cor mostrados no histórico continuam
 * idênticos — ressalva de dado histórico confirmada em #1746: medição já persistida nunca é
 * reclassificada via NDS, então este ponto continua 100% local, de propósito.
 */
class HistoricoScreenBufferbloatVereditoTest {
    private val c = lightTokens()

    @Test
    fun `bufferbloat abaixo de 5ms e Baixo (excelente)`() {
        assertEquals("Baixo" to c.success, bufferbloatVeredito(0.0, c))
        assertEquals("Baixo" to c.success, bufferbloatVeredito(4.9, c))
    }

    @Test
    fun `bufferbloat entre 5 e 30ms e Baixo (bom)`() {
        assertEquals("Baixo" to c.success, bufferbloatVeredito(5.0, c))
        assertEquals("Baixo" to c.success, bufferbloatVeredito(30.0, c))
    }

    @Test
    fun `bufferbloat entre 30 e 100ms e Moderado`() {
        assertEquals("Moderado" to c.warning, bufferbloatVeredito(30.1, c))
        assertEquals("Moderado" to c.warning, bufferbloatVeredito(100.0, c))
    }

    @Test
    fun `bufferbloat acima de 100ms e Alto`() {
        assertEquals("Alto" to c.error, bufferbloatVeredito(100.1, c))
        assertEquals("Alto" to c.error, bufferbloatVeredito(500.0, c))
    }
}
