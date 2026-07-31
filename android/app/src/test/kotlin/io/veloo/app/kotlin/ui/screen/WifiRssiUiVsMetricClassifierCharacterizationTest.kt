package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — congela a divergencia entre a classificacao de RSSI Wi-Fi
 * feita na UI ([signalQuality], `SinalTopologiaHelpers.kt`) e a classificacao "canonica"
 * do motor de diagnostico ([MetricClassifier.classificarRssiWifi], usada por
 * `WifiSignalQualityEngine`). O proprio kdoc de [MetricClassifier] admite que a tela
 * Sinal "ainda NAO foi migrado" -- este teste torna esse gap concreto e mensuravel.
 *
 * Deliberadamente testado como funcao pura (sem Compose): [signalQuality] nao depende de
 * nenhum tipo de UI, entao nao ha motivo para depender de infra de teste de Compose aqui
 * (ver instrucao da auditoria: "Evite depender de Compose quando a regra puder ser
 * testada antes da UI").
 *
 * Ver `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, Parte 8, P0-2.
 */
class WifiRssiUiVsMetricClassifierCharacterizationTest {

    @Test
    fun `rssi de -65dBm em 5GHz e Bom na tela Sinal mas regular no motor de diagnostico`() {
        // Tela Sinal (SinalTopologiaHelpers.signalQuality): limite >= -65 (inclusivo) e "Bom".
        assertEquals("Bom", signalQuality(rssiDbm = -65, banda = BandaWifi.ghz5))

        // Motor de diagnostico (MetricClassifier.classificarRssiWifi): limite > -65
        // (exclusivo) -- -65 exatos cai em "regular", nao em "bom".
        assertEquals(
            MetricStatus.regular,
            MetricClassifier.classificarRssiWifi(rssiDbm = -65, band = MetricClassifier.WifiBand.GHZ_5),
        )

        // Caracteriza a divergencia: mesmo valor de RSSI, "Bom" (verde) na tela Sinal e
        // "regular" (que WifiSignalQualityEngine pode reportar como achado attention) no
        // motor que alimenta o banner de diagnostico.
    }

    @Test
    fun `rssi de -60dBm em 2,4GHz e Bom na tela Sinal mas regular no motor de diagnostico`() {
        // Mesmo padrao de divergencia na banda 2.4GHz, no seu proprio boundary (-60 vs -60).
        assertEquals("Bom", signalQuality(rssiDbm = -60, banda = BandaWifi.ghz24))
        assertEquals(
            MetricStatus.regular,
            MetricClassifier.classificarRssiWifi(rssiDbm = -60, band = MetricClassifier.WifiBand.GHZ_2_4),
        )
    }

    @Test
    fun `rssi de -70dBm em 5GHz -- tela Sinal e motor concordam em Regular`() {
        // Contraste: mais abaixo da fronteira de "Bom" as duas reguas concordam -- a
        // divergencia so aparece exatamente nos boundaries testados acima.
        assertEquals("Regular", signalQuality(rssiDbm = -70, banda = BandaWifi.ghz5))
        assertEquals(
            MetricStatus.regular,
            MetricClassifier.classificarRssiWifi(rssiDbm = -70, band = MetricClassifier.WifiBand.GHZ_5),
        )
    }
}
