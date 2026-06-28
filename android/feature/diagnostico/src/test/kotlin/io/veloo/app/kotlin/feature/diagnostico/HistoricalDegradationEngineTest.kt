package io.signallq.app.feature.diagnostico

import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalDegradationEngineTest {

    @Test
    fun `insufficient history does not declare degradation`() {
        val input =
            HistoricalDiagnosticInput(
                avgDownload7d = 50.0,
                avgDownload30d = 80.0,
                testsCount7d = 2,
                testsCount30d = 3,
            )
        val r = HistoricalDegradationEngine.avaliar(input)
        assertTrue(r.size == 1)
        assertTrue(r.first().status == DiagnosticStatus.inconclusive)
    }

    @Test
    fun `sufficient history with relevant drop declares degradation`() {
        val input =
            HistoricalDiagnosticInput(
                avgDownload7d = 40.0,
                avgDownload30d = 100.0, // 60% drop
                avgUpload7d = 5.0,
                avgUpload30d = 10.0,
                avgPing7d = 40.0,
                avgPing30d = 30.0,
                testsCount7d = 8,
                testsCount30d = 20,
                worstTimeWindow = "20-23",
                bestTimeWindow = "02-05",
            )
        val r = HistoricalDegradationEngine.avaliar(input)
        assertTrue(r.any { it.status == DiagnosticStatus.critical || it.status == DiagnosticStatus.attention })
    }
}

