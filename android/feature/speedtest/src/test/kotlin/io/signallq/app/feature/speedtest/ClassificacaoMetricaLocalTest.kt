package io.signallq.app.feature.speedtest

import io.signallq.app.core.diagnostico.MetricClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Teste de caracterização — NDS-02k (issue #1746/#1759), escrito ANTES de
 * [SpeedtestQualityClassifier] trocar a chamada direta a `MetricClassifier.classificarBufferbloat`
 * pelo seam `classificarBufferbloatLocal`. Prova que a nova função produz exatamente o mesmo
 * resultado que `MetricClassifier` produzia antes da migração — comportamento idêntico, só a
 * fonte da chamada muda de lugar (ver KDoc de `ClassificacaoMetricaLocal.kt` para o racional
 * completo, incluindo por que este ponto continua 100% local em vez de ler um veredicto do NDS).
 */
class ClassificacaoMetricaLocalTest {
    @Test
    fun `classificarBufferbloatLocal converge com MetricClassifier em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(0.0, 4.9, 5.0, 5.1, 29.9, 30.0, 30.1, 99.9, 100.0, 100.1, 500.0)
        fronteiras.forEach { deltaMs ->
            assertEquals(
                "divergencia em deltaMs=$deltaMs",
                MetricClassifier.classificarBufferbloat(deltaMs),
                classificarBufferbloatLocal(deltaMs),
            )
        }
    }
}
