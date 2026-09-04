package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DegradacaoHistoricoCalculadoraTest {

    @Test
    fun `queda relevante com testes suficientes marca degradacao detectada`() {
        val resultado =
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 240.0,
                avgDownload30d = 300.0,
                testsCount7d = 8,
                testsCount30d = 31,
            )

        assertTrue(resultado != null)
        assertEquals(true, resultado?.first)
        assertEquals(20.0, resultado?.second)
    }

    @Test
    fun `queda abaixo do limiar nao marca degradacao mas ainda reporta o percentual`() {
        val resultado =
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 285.0,
                avgDownload30d = 300.0,
                testsCount7d = 8,
                testsCount30d = 31,
            )

        assertTrue(resultado != null)
        assertEquals(false, resultado?.first)
        assertEquals(5.0, resultado?.second)
    }

    @Test
    fun `melhora produz percentual negativo e degradacao nao detectada`() {
        val resultado =
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 330.0,
                avgDownload30d = 300.0,
                testsCount7d = 8,
                testsCount30d = 31,
            )

        assertTrue(resultado != null)
        assertEquals(false, resultado?.first)
        assertEquals(-10.0, resultado?.second)
    }

    @Test
    fun `poucos testes em 7d nao calcula degradacao mesmo com medias presentes`() {
        val resultado =
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 100.0,
                avgDownload30d = 300.0,
                testsCount7d = 2,
                testsCount30d = 31,
            )

        assertNull(resultado)
    }

    @Test
    fun `poucos testes em 30d nao calcula degradacao mesmo com medias presentes`() {
        val resultado =
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 100.0,
                avgDownload30d = 300.0,
                testsCount7d = 8,
                testsCount30d = 4,
            )

        assertNull(resultado)
    }

    @Test
    fun `media ausente em qualquer janela nao calcula degradacao`() {
        assertNull(
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = null,
                avgDownload30d = 300.0,
                testsCount7d = 8,
                testsCount30d = 31,
            ),
        )
        assertNull(
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 100.0,
                avgDownload30d = null,
                testsCount7d = 8,
                testsCount30d = 31,
            ),
        )
    }

    @Test
    fun `percentual cru abaixo do limiar que arredondaria para o limiar nao marca degradacao`() {
        // percentual cru = 19.96%, que arredonda para 20.0 (1 casa decimal) — a decisao
        // de degradationDetected deve usar o valor cru, nao o arredondado, para nao
        // divergir de HistoricalDegradationEngine.severidade() (compara valor cru >= 20.0).
        val resultado =
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 240.12,
                avgDownload30d = 300.0,
                testsCount7d = 8,
                testsCount30d = 31,
            )

        assertTrue(resultado != null)
        assertEquals(false, resultado?.first)
        assertEquals(20.0, resultado?.second)
    }

    @Test
    fun `percentual cru acima do limiar que tambem arredonda para o limiar marca degradacao`() {
        // percentual cru = 20.04%, arredonda para 20.0, e deve detectar degradacao
        // pois o valor cru ja ultrapassa o limiar.
        val resultado =
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 239.88,
                avgDownload30d = 300.0,
                testsCount7d = 8,
                testsCount30d = 31,
            )

        assertTrue(resultado != null)
        assertEquals(true, resultado?.first)
        assertEquals(20.0, resultado?.second)
    }

    @Test
    fun `media de 30d zero ou negativa nao calcula degradacao`() {
        assertNull(
            DegradacaoHistoricoCalculadora.calcular(
                avgDownload7d = 100.0,
                avgDownload30d = 0.0,
                testsCount7d = 8,
                testsCount30d = 31,
            ),
        )
    }
}
