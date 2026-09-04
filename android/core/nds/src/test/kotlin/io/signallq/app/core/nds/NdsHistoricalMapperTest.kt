package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.HistoricalDiagnosticInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobertura da ponte `HistoricalDiagnosticInput -> NdsHistoricalInfo`
 * (NDS-Snapshot-06, issue #1838, ADR-018 seção 13).
 */
class NdsHistoricalMapperTest {

    @Test
    fun `input nulo produz bloco ausente`() {
        assertNull((null as HistoricalDiagnosticInput?).toNdsHistoricalInfo())
    }

    @Test
    fun `usuario novo sem nenhum teste produz bloco ausente, nunca zeros`() {
        val input = HistoricalDiagnosticInput(testsCount7d = 0, testsCount30d = 0)

        assertNull(input.toNdsHistoricalInfo())
    }

    @Test
    fun `historico completo com degradacao ja calculada chega integralmente ao bloco`() {
        val input =
            HistoricalDiagnosticInput(
                avgDownload7d = 287.4,
                avgUpload7d = 141.2,
                avgPing7d = 19.2,
                testsCount7d = 8,
                avgDownload30d = 301.8,
                avgUpload30d = 149.1,
                avgPing30d = 17.5,
                testsCount30d = 31,
                degradationDetected = true,
                degradationPercent = 18.3,
            )

        val historical = input.toNdsHistoricalInfo()

        assertTrue(historical != null)
        assertEquals(8, historical?.testsCount7d)
        assertEquals(287.4, historical?.avgDownload7d)
        assertEquals(141.2, historical?.avgUpload7d)
        assertEquals(19.2, historical?.avgPing7d)
        assertEquals(31, historical?.testsCount30d)
        assertEquals(301.8, historical?.avgDownload30d)
        assertEquals(149.1, historical?.avgUpload30d)
        assertEquals(17.5, historical?.avgPing30d)
        assertEquals(true, historical?.degradationDetected)
        assertEquals(18.3, historical?.degradationPercent)
    }

    @Test
    fun `historico parcial -- so 30d com dado suficiente, 7d sem teste algum -- ainda preenche o bloco`() {
        // Cenario realista: usuario testou bastante ha 2-3 semanas mas nao testou nos ultimos 7
        // dias -- a janela de 30d contem dados, a de 7d fica vazia (nunca inventamos media/zero).
        val input =
            HistoricalDiagnosticInput(
                avgDownload7d = null,
                testsCount7d = 0,
                avgDownload30d = 250.0,
                testsCount30d = 12,
            )

        val historical = input.toNdsHistoricalInfo()

        assertTrue(historical != null)
        assertEquals(0, historical?.testsCount7d)
        assertNull(historical?.avgDownload7d)
        assertEquals(12, historical?.testsCount30d)
        assertEquals(250.0, historical?.avgDownload30d)
        assertNull("degradacao nao pode ser inventada sem dado de 7d", historical?.degradationDetected)
    }

    @Test
    fun `campos sem fonte hoje -- dns e janelas de horario -- ficam nulos sem quebrar o restante`() {
        val input = HistoricalDiagnosticInput(avgDownload7d = 100.0, testsCount7d = 5)

        val historical = input.toNdsHistoricalInfo()

        assertNull(historical?.avgDns7d)
        assertNull(historical?.avgDns30d)
        assertNull(historical?.worstTimeWindow)
        assertNull(historical?.bestTimeWindow)
    }
}
