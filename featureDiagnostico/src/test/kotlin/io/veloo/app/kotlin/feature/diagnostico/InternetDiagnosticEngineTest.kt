package io.veloo.app.feature.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InternetDiagnosticEngineTest {

    @Test
    fun `upload zero generates critical and overrides generic upload low`() {
        val input =
            InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 0.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            )
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = true)
        assertTrue(resultados.any { it.id == "IN-NORMAL-04Z" && it.status == DiagnosticStatus.critical })
        assertTrue(resultados.none { it.id == "IN-NORMAL-04" })
    }

    @Test
    fun `wifi not reliable converts issues to inconclusive`() {
        val input =
            InternetDiagnosticInput(
                downloadMbps = 10.0, // gera download baixo
                uploadMbps = 2.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            )
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = false)
        assertTrue(resultados.isNotEmpty())
        assertEquals(true, resultados.all { it.status == DiagnosticStatus.inconclusive })
    }
}

