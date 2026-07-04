package io.signallq.app.feature.diagnostico

import io.signallq.app.feature.diagnostico.MetricClassifier.RadioTech
import io.signallq.app.feature.diagnostico.MetricClassifier.WifiBand
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricClassifierTest {

    // ── RSSI Wi-Fi 2.4GHz ────────────────────────────────────────────────────

    @Test
    fun `rssi 2_4ghz excelente acima de -50`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarRssiWifi(-49, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz limite -50 e bom (nao excelente)`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarRssiWifi(-50, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz bom entre -50 e -60`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarRssiWifi(-55, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz limite -60 e aceitavel (nao bom)`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarRssiWifi(-60, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz aceitavel entre -60 e -70`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarRssiWifi(-65, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz limite -70 e ruim (nao aceitavel)`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarRssiWifi(-70, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz ruim entre -70 e -80`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarRssiWifi(-75, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz limite -80 e inutilizavel (critico)`() {
        assertEquals(MetricStatus.critico, MetricClassifier.classificarRssiWifi(-80, WifiBand.GHZ_2_4))
    }

    @Test
    fun `rssi 2_4ghz critico abaixo de -80`() {
        assertEquals(MetricStatus.critico, MetricClassifier.classificarRssiWifi(-90, WifiBand.GHZ_2_4))
    }

    // ── RSSI Wi-Fi 5GHz ──────────────────────────────────────────────────────

    @Test
    fun `rssi 5ghz excelente acima de -55`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarRssiWifi(-54, WifiBand.GHZ_5))
    }

    @Test
    fun `rssi 5ghz limite -55 e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarRssiWifi(-55, WifiBand.GHZ_5))
    }

    @Test
    fun `rssi 5ghz limite -65 e aceitavel`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarRssiWifi(-65, WifiBand.GHZ_5))
    }

    @Test
    fun `rssi 5ghz limite -75 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarRssiWifi(-75, WifiBand.GHZ_5))
    }

    @Test
    fun `rssi 5ghz limite -82 e critico`() {
        assertEquals(MetricStatus.critico, MetricClassifier.classificarRssiWifi(-82, WifiBand.GHZ_5))
    }

    @Test
    fun `rssi 5ghz critico abaixo de -82`() {
        assertEquals(MetricStatus.critico, MetricClassifier.classificarRssiWifi(-95, WifiBand.GHZ_5))
    }

    @Test
    fun `rssi 6ghz reaproveita regua de 5ghz`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarRssiWifi(-54, WifiBand.GHZ_6))
        assertEquals(MetricStatus.critico, MetricClassifier.classificarRssiWifi(-90, WifiBand.GHZ_6))
    }

    // ── Latencia ─────────────────────────────────────────────────────────────

    @Test
    fun `latencia excelente abaixo de 100ms`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarLatencia(99.0))
    }

    @Test
    fun `latencia 100ms e bom (nao excelente)`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarLatencia(100.0))
    }

    @Test
    fun `latencia 150ms e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarLatencia(150.0))
    }

    @Test
    fun `latencia 150_01ms e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarLatencia(150.01))
    }

    @Test
    fun `latencia 200ms e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarLatencia(200.0))
    }

    @Test
    fun `latencia acima de 200ms e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarLatencia(201.0))
    }

    // ── Jitter ───────────────────────────────────────────────────────────────

    @Test
    fun `jitter excelente abaixo de 5ms`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarJitter(4.9))
    }

    @Test
    fun `jitter 5ms e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarJitter(5.0))
    }

    @Test
    fun `jitter 10ms e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarJitter(10.0))
    }

    @Test
    fun `jitter 10_01ms e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarJitter(10.01))
    }

    @Test
    fun `jitter 20ms e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarJitter(20.0))
    }

    @Test
    fun `jitter acima de 20ms e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarJitter(21.0))
    }

    // ── Perda de pacotes ─────────────────────────────────────────────────────

    @Test
    fun `perda 0 por cento e excelente`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarPerdaPacotes(0.0))
    }

    @Test
    fun `perda abaixo de 0_5 por cento e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarPerdaPacotes(0.4))
    }

    @Test
    fun `perda 0_5 por cento e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarPerdaPacotes(0.5))
    }

    @Test
    fun `perda 2 por cento e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarPerdaPacotes(2.0))
    }

    @Test
    fun `perda acima de 2 por cento e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarPerdaPacotes(2.1))
    }

    // ── RSRP 4G LTE ──────────────────────────────────────────────────────────

    @Test
    fun `rsrp 4g excelente acima de -80`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarRsrp(-79, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrp 4g limite -80 e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarRsrp(-80, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrp 4g limite -90 e aceitavel-regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarRsrp(-90, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrp 4g limite -100 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarRsrp(-100, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrp 4g abaixo de -100 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarRsrp(-120, RadioTech.LTE_4G))
    }

    // ── RSRP 5G NR ───────────────────────────────────────────────────────────

    @Test
    fun `rsrp 5g excelente acima de -80`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarRsrp(-79, RadioTech.NR_5G))
    }

    @Test
    fun `rsrp 5g limite -95 e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarRsrp(-95, RadioTech.NR_5G))
    }

    @Test
    fun `rsrp 5g limite -110 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarRsrp(-110, RadioTech.NR_5G))
    }

    @Test
    fun `rsrp 5g e mais permissivo que 4g na faixa bom`() {
        // -92 dBm: 4G ja seria "regular" (abaixo de -90), 5G ainda e "bom" (acima de -95)
        assertEquals(MetricStatus.regular, MetricClassifier.classificarRsrp(-92, RadioTech.LTE_4G))
        assertEquals(MetricStatus.bom, MetricClassifier.classificarRsrp(-92, RadioTech.NR_5G))
    }

    // ── RSRQ (4G e 5G usam a mesma tabela) ───────────────────────────────────

    @Test
    fun `rsrq excelente acima de -10`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarRsrq(-9, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrq limite -10 e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarRsrq(-10, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrq limite -15 e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarRsrq(-15, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrq limite -20 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarRsrq(-20, RadioTech.LTE_4G))
    }

    @Test
    fun `rsrq 5g usa mesma tabela do 4g`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarRsrq(-12, RadioTech.NR_5G))
    }

    // ── SINR 4G LTE ──────────────────────────────────────────────────────────

    @Test
    fun `sinr 4g excelente acima de 20`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarSinr(21, RadioTech.LTE_4G))
    }

    @Test
    fun `sinr 4g limite 20 e bom`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarSinr(20, RadioTech.LTE_4G))
    }

    @Test
    fun `sinr 4g limite 13 e aceitavel-regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarSinr(13, RadioTech.LTE_4G))
    }

    @Test
    fun `sinr 4g limite 0 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarSinr(0, RadioTech.LTE_4G))
    }

    @Test
    fun `sinr 4g abaixo de 0 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarSinr(-5, RadioTech.LTE_4G))
    }

    // ── SINR 5G NR ───────────────────────────────────────────────────────────

    @Test
    fun `sinr 5g excelente acima de 20`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarSinr(21, RadioTech.NR_5G))
    }

    @Test
    fun `sinr 5g limite 10 e regular`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarSinr(10, RadioTech.NR_5G))
    }

    @Test
    fun `sinr 5g limite 0 e ruim`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarSinr(0, RadioTech.NR_5G))
    }

    @Test
    fun `sinr 5g e mais rigoroso que 4g na faixa bom`() {
        // sinr=12: 4G ainda e "regular" (abaixo de 13), 5G ja e "bom" (acima de 10)
        assertEquals(MetricStatus.regular, MetricClassifier.classificarSinr(12, RadioTech.LTE_4G))
        assertEquals(MetricStatus.bom, MetricClassifier.classificarSinr(12, RadioTech.NR_5G))
    }

    // ── DNS (latencia) ───────────────────────────────────────────────────────

    @Test
    fun `dns latencia excelente ate 50ms`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarLatenciaDns(50))
    }

    @Test
    fun `dns latencia bom ate 150ms`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarLatenciaDns(150))
    }

    @Test
    fun `dns latencia regular ate 300ms`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarLatenciaDns(300))
    }

    @Test
    fun `dns latencia ruim acima de 300ms`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarLatenciaDns(301))
    }

    // ── Bufferbloat ──────────────────────────────────────────────────────────

    @Test
    fun `bufferbloat excelente abaixo de 5ms`() {
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarBufferbloat(4.9))
    }

    @Test
    fun `bufferbloat bom entre 5 e 30ms`() {
        assertEquals(MetricStatus.bom, MetricClassifier.classificarBufferbloat(5.0))
        assertEquals(MetricStatus.bom, MetricClassifier.classificarBufferbloat(30.0))
    }

    @Test
    fun `bufferbloat regular entre 30 e 100ms`() {
        assertEquals(MetricStatus.regular, MetricClassifier.classificarBufferbloat(30.01))
        assertEquals(MetricStatus.regular, MetricClassifier.classificarBufferbloat(100.0))
    }

    @Test
    fun `bufferbloat ruim acima de 100ms`() {
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarBufferbloat(100.01))
    }
}
