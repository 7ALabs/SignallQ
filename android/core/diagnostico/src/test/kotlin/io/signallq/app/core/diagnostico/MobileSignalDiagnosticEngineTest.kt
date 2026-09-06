package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de caracterizacao do [MobileSignalDiagnosticEngine] apos a migracao para
 * [MetricClassifier] (issue #998) — RSRP/RSRQ/SINR agora sao classificados pela
 * mesma fonte de verdade usada pelo Wi-Fi, em vez de um enum proprio de 4 niveis.
 */
class MobileSignalDiagnosticEngineTest {
    private fun avaliar(input: MobileDiagnosticInput) =
        MobileSignalDiagnosticEngine.avaliar(ConnectionType.mobile, input)

    @Test
    fun `conexao nao movel nao gera resultado`() {
        val r =
            MobileSignalDiagnosticEngine.avaliar(
                ConnectionType.wifi,
                MobileDiagnosticInput(rsrpDbm = -120),
            )
        assertTrue(r.isEmpty())
    }

    @Test
    fun `input nulo gera resultado inconclusivo`() {
        val r = MobileSignalDiagnosticEngine.avaliar(ConnectionType.mobile, null)
        assertEquals(1, r.size)
        assertEquals("MOB-INC-00", r.first().id)
    }

    @Test
    fun `4G rsrp excelente nao gera alerta de metricas`() {
        val r = avaliar(MobileDiagnosticInput(mobileTechnology = "4G", rsrpDbm = -79))
        assertNull(r.find { it.id == "MOB-02" || it.id == "MOB-02b" })
    }

    @Test
    fun `4G rsrp regular gera MOB-02b (atencao)`() {
        val r = avaliar(MobileDiagnosticInput(mobileTechnology = "4G", rsrpDbm = -95))
        val achado = r.find { it.id == "MOB-02b" }
        assertEquals("MOB-02b", achado?.id)
        assertEquals(DiagnosticStatus.attention, achado?.status)
    }

    @Test
    fun `4G rsrp ruim gera MOB-02 (critico)`() {
        val r = avaliar(MobileDiagnosticInput(mobileTechnology = "4G", rsrpDbm = -105))
        val achado = r.find { it.id == "MOB-02" }
        assertEquals("MOB-02", achado?.id)
        assertEquals(DiagnosticStatus.critical, achado?.status)
    }

    @Test
    fun `5G e mais permissivo que 4G na mesma faixa de rsrp`() {
        // -92 dBm: 4G ja cai em "regular" (abaixo de -90), 5G ainda fica em "bom" (acima de -95)
        val r4g = avaliar(MobileDiagnosticInput(mobileTechnology = "4G", rsrpDbm = -92))
        val r5g = avaliar(MobileDiagnosticInput(mobileTechnology = "5G", rsrpDbm = -92))
        assertEquals("MOB-02b", r4g.find { it.id == "MOB-02b" }?.id)
        assertNull(r5g.find { it.id == "MOB-02" || it.id == "MOB-02b" })
    }

    @Test
    fun `pior metrica entre rsrp-rsrq-sinr define o resultado`() {
        // rsrp excelente, mas sinr ruim -> deve prevalecer o pior (MOB-02)
        val r = avaliar(MobileDiagnosticInput(mobileTechnology = "4G", rsrpDbm = -79, sinrDb = -5))
        assertEquals("MOB-02", r.find { it.id == "MOB-02" }?.id)
    }

    @Test
    fun `sinal movel muito ruim gera MOB-01 baseado em signalQualityPercent`() {
        val r = avaliar(MobileDiagnosticInput(mobileTechnology = "4G", signalQualityPercent = 10))
        assertEquals("MOB-01", r.find { it.id == "MOB-01" }?.id)
    }
}
