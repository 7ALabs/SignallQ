package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de caracterizacao do [WifiSignalQualityEngine] apos a migracao para
 * [MetricClassifier] (issue #998) — protegem a classificacao de RSSI diferenciada
 * por banda (2.4GHz vs 5/6GHz), que antes era ignorada.
 */
class WifiSignalQualityEngineTest {
    private val idsRssi = setOf("WIFI-01", "WIFI-02", "WIFI-03", "WIFI-04")

    private fun rssiResultado(
        rssiDbm: Int,
        frequenciaMhz: Int?,
    ) =
        WifiSignalQualityEngine
            .avaliar(
                WifiDiagnosticInput(rssiDbm = rssiDbm, linkSpeedMbps = null, frequenciaMhz = frequenciaMhz),
            ).resultados
            .first { it.id in idsRssi }

    @Test
    fun `2_4GHz -45dBm e excelente (WIFI-01)`() {
        val r = rssiResultado(-45, 2412)
        assertEquals("WIFI-01", r.id)
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    @Test
    fun `2_4GHz -55dBm e bom (WIFI-02)`() {
        val r = rssiResultado(-55, 2412)
        assertEquals("WIFI-02", r.id)
    }

    @Test
    fun `2_4GHz -65dBm e fraco-regular (WIFI-03)`() {
        val r = rssiResultado(-65, 2412)
        assertEquals("WIFI-03", r.id)
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    @Test
    fun `2_4GHz -85dBm e muito fraco (WIFI-04)`() {
        val r = rssiResultado(-85, 2412)
        assertEquals("WIFI-04", r.id)
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    @Test
    fun `5GHz -50dBm e excelente (regua diferente de 2_4GHz)`() {
        val r = rssiResultado(-50, 5180)
        assertEquals("WIFI-01", r.id)
    }

    @Test
    fun `5GHz -70dBm e fraco-regular`() {
        val r = rssiResultado(-70, 5180)
        assertEquals("WIFI-03", r.id)
    }

    @Test
    fun `mesmo rssi recebe classificacao diferente em 2_4GHz e 5GHz`() {
        // -63 dBm: em 2.4GHz cai em "regular" (WIFI-03), em 5GHz cai em "bom" (WIFI-02) —
        // exatamente a inconsistencia apontada na issue #998, agora corrigida por banda.
        val em24 = rssiResultado(-63, 2412)
        val em5 = rssiResultado(-63, 5180)
        assertEquals("WIFI-03", em24.id)
        assertEquals("WIFI-02", em5.id)
    }

    @Test
    fun `banda desconhecida usa regua conservadora 2_4GHz`() {
        val r = rssiResultado(-63, null)
        assertEquals("WIFI-03", r.id)
    }

    @Test
    fun `sem rssi nao gera resultado de sinal`() {
        val resultado =
            WifiSignalQualityEngine.avaliar(
                WifiDiagnosticInput(rssiDbm = null, linkSpeedMbps = null, frequenciaMhz = 2412),
            )
        assertTrue(resultado.resultados.none { it.id in idsRssi })
    }

    @Test
    fun `input nulo retorna lista vazia e confiavel`() {
        val resultado = WifiSignalQualityEngine.avaliar(null)
        assertTrue(resultado.resultados.isEmpty())
        assertTrue(resultado.confiavelParaTeste)
    }

    @Test
    fun `sinal regular ainda e confiavel para teste`() {
        val resultado =
            WifiSignalQualityEngine.avaliar(
                WifiDiagnosticInput(rssiDbm = -65, linkSpeedMbps = null, frequenciaMhz = 2412),
            )
        assertTrue(resultado.confiavelParaTeste)
    }

    @Test
    fun `sinal muito fraco nao e confiavel para teste`() {
        val resultado =
            WifiSignalQualityEngine.avaliar(
                WifiDiagnosticInput(rssiDbm = -85, linkSpeedMbps = null, frequenciaMhz = 2412),
            )
        assertFalse(resultado.confiavelParaTeste)
    }
}
