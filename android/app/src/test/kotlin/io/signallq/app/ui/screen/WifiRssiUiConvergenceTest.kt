package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.ui.component.signalColor
import io.signallq.app.ui.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GH#1228 Fatia 5 — corrige o P0-2 documentado em
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`: ate esta fatia,
 * [signalColor] (`SignalBars.kt`) e [signalQuality] (`SinalTopologiaHelpers.kt`) tinham regua
 * propria (`>=` inclusivo) divergente do canonico [MetricClassifier.classificarRssiWifi]
 * (`>` exclusivo), usado pelo motor de diagnostico via `WifiSignalQualityEngine`. Ambos agora
 * delegam ao classifier canonico — este teste substitui o antigo
 * `WifiRssiUiVsMetricClassifierCharacterizationTest` (que documentava a divergencia) por uma
 * prova de convergencia nos mesmos boundaries.
 */
class WifiRssiUiConvergenceTest {
    private val tokens = lightTokens()

    @Test
    fun `rssi de -65dBm em 5GHz e Regular tanto na tela Sinal quanto no motor de diagnostico`() {
        assertEquals("Regular", signalQuality(rssiDbm = -65, banda = BandaWifi.ghz5))
        assertEquals(
            MetricStatus.regular,
            MetricClassifier.classificarRssiWifi(rssiDbm = -65, band = MetricClassifier.WifiBand.GHZ_5),
        )
        assertEquals(tokens.warning, signalColor(rssiDbm = -65, banda = BandaWifi.ghz5, c = tokens))
    }

    @Test
    fun `rssi de -60dBm em 2,4GHz e Regular tanto na tela Sinal quanto no motor de diagnostico`() {
        assertEquals("Regular", signalQuality(rssiDbm = -60, banda = BandaWifi.ghz24))
        assertEquals(
            MetricStatus.regular,
            MetricClassifier.classificarRssiWifi(rssiDbm = -60, band = MetricClassifier.WifiBand.GHZ_2_4),
        )
        assertEquals(tokens.warning, signalColor(rssiDbm = -60, banda = BandaWifi.ghz24, c = tokens))
    }

    @Test
    fun `rssi de -70dBm em 5GHz -- tela Sinal e motor concordam em Regular (fronteira ja concordava antes)`() {
        assertEquals("Regular", signalQuality(rssiDbm = -70, banda = BandaWifi.ghz5))
        assertEquals(
            MetricStatus.regular,
            MetricClassifier.classificarRssiWifi(rssiDbm = -70, band = MetricClassifier.WifiBand.GHZ_5),
        )
    }

    @Test
    fun `rssi de -64dBm em 5GHz e Bom nos dois -- um dBm acima do boundary critico`() {
        assertEquals("Bom", signalQuality(rssiDbm = -64, banda = BandaWifi.ghz5))
        assertEquals(
            MetricStatus.bom,
            MetricClassifier.classificarRssiWifi(rssiDbm = -64, band = MetricClassifier.WifiBand.GHZ_5),
        )
        assertEquals(tokens.success, signalColor(rssiDbm = -64, banda = BandaWifi.ghz5, c = tokens))
    }

    @Test
    fun `banda desconhecida usa a regua 2,4GHz nas 3 implementacoes`() {
        assertEquals("Regular", signalQuality(rssiDbm = -60, banda = BandaWifi.desconhecida))
        assertEquals(
            MetricStatus.regular,
            MetricClassifier.classificarRssiWifi(rssiDbm = -60, banda = BandaWifi.desconhecida),
        )
        assertEquals(tokens.warning, signalColor(rssiDbm = -60, banda = BandaWifi.desconhecida, c = tokens))
    }

    @Test
    fun `sinal critico vira Fraco no rotulo e erro na cor`() {
        assertEquals("Fraco", signalQuality(rssiDbm = -90, banda = BandaWifi.ghz5))
        assertEquals(tokens.error, signalColor(rssiDbm = -90, banda = BandaWifi.ghz5, c = tokens))
    }
}
