package io.veloo.app.feature.diagnostico

import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticRunnerIntegrationTest {

    @Test
    fun `mobile connection does not apply wifi diagnostics`() {
        val input =
            DiagnosticInput(
                connectionType = ConnectionType.mobile,
                internet =
                    InternetDiagnosticInput(
                        downloadMbps = 10.0,
                        uploadMbps = 2.0,
                        latencyMs = 50.0,
                        jitterMs = 10.0,
                        perdaPercentual = 0.0,
                        bufferbloatMs = 0.0,
                    ),
                wifi =
                    WifiDiagnosticInput(
                        rssiDbm = -90,
                        linkSpeedMbps = 10,
                        frequenciaMhz = 2412,
                    ),
                mobile =
                    MobileDiagnosticInput(
                        carrierName = "operadora",
                        mobileTechnology = "4G",
                        signalQualityPercent = 20,
                    ),
            )

        val r = DiagnosticRunner.run(input)
        assertTrue(r.wifiResultados.isEmpty()) // sem diagnostico wifi em rede movel
        assertTrue(r.mobileResultados.isNotEmpty())
    }

    @Test
    fun `wifi poor plus internet issues does not blame isp directly`() {
        val input =
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet =
                    InternetDiagnosticInput(
                        downloadMbps = 10.0,
                        uploadMbps = 2.0,
                        latencyMs = 200.0,
                        jitterMs = 10.0,
                        perdaPercentual = 0.0,
                        bufferbloatMs = 0.0,
                    ),
                wifi =
                    WifiDiagnosticInput(
                        rssiDbm = -90, // wifi ruim => nao confiavel
                        linkSpeedMbps = 10,
                        frequenciaMhz = 2412,
                    ),
            )

        val r = DiagnosticRunner.run(input)
        // O ponto do teste: com Wi-Fi nao confiavel, o motor nao deve concluir "Problema na Internet/ISP".
        assertTrue(r.decisao.id != "DECISAO-02")
    }

    @Test
    fun `speedtest usage profiles appear in report`() {
        val perfis =
            SpeedtestQualityInput(
                vereditoStreaming = "good",
                vereditoGamer = "poor",
                vereditoVideochamada = "acceptable",
                gargaloPrimario = "upload",
                severidadeBufferbloat = "mild",
            )
        val input =
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet =
                    InternetDiagnosticInput(
                        downloadMbps = 100.0,
                        uploadMbps = 10.0,
                        latencyMs = 20.0,
                        jitterMs = 5.0,
                        perdaPercentual = 0.0,
                        bufferbloatMs = 0.0,
                        qualidadeUso = perfis,
                    ),
                wifi =
                    WifiDiagnosticInput(
                        rssiDbm = -55,
                        linkSpeedMbps = 300,
                        frequenciaMhz = 5200,
                    ),
            )

        val r = DiagnosticRunner.run(input)
        assertTrue(r.perfisUsoSpeedtest == perfis)
    }
}
