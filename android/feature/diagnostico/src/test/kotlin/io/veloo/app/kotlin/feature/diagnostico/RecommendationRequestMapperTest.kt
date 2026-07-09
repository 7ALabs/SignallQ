package io.signallq.app.feature.diagnostico

import io.signallq.app.core.recommendation.DiagnosticTag
import io.signallq.app.core.recommendation.NetworkContextType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationRequestMapperTest {

    @Test
    fun `wifi fraco e muitos dispositivos geram tags e contexto wifi`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 80.0,
                uploadMbps = 20.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -80,
                linkSpeedMbps = 100,
                frequenciaMhz = 2412,
                dispositivosNaRede = 25,
            ),
        )
        val report = DiagnosticRunner.run(input)

        val request = RecommendationRequestMapper.map(report, input)

        assertEquals(NetworkContextType.WIFI, request.network)
        assertTrue(DiagnosticTag.WIFI_FRACO in request.tags)
        assertTrue(DiagnosticTag.MUITOS_DISPOSITIVOS in request.tags)
    }

    @Test
    fun `perda de pacotes alta e bufferbloat geram tags e contexto movel`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.mobile,
            internet = InternetDiagnosticInput(
                downloadMbps = 50.0,
                uploadMbps = 10.0,
                latencyMs = 30.0,
                jitterMs = 5.0,
                perdaPercentual = 5.0,
                bufferbloatMs = 150.0,
            ),
            mobile = MobileDiagnosticInput(
                carrierName = "operadora",
                mobileTechnology = "4G",
                signalQualityPercent = 80,
            ),
        )
        val report = DiagnosticRunner.run(input)

        val request = RecommendationRequestMapper.map(report, input)

        assertEquals(NetworkContextType.MOVEL, request.network)
        assertTrue(DiagnosticTag.PERDA_PACOTES_ALTA in request.tags)
        assertTrue(DiagnosticTag.BUFFERBLOAT_ALTO in request.tags)
        assertEquals(50.0, request.metrics.downloadMbps)
        assertEquals(5.0, request.metrics.packetLossPercent)
    }

    @Test
    fun `dns lento gera tag dns_lento`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 20.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -55,
                linkSpeedMbps = 300,
                frequenciaMhz = 5180,
            ),
            dns = DnsDiagnosticInput(
                currentDnsIp = "10.0.0.1",
                currentDnsName = "operadora",
                currentDnsLatencyMs = 320,
            ),
        )
        val report = DiagnosticRunner.run(input)

        val request = RecommendationRequestMapper.map(report, input)

        assertTrue(DiagnosticTag.DNS_LENTO in request.tags)
        assertTrue(DiagnosticTag.WIFI_FRACO !in request.tags)
    }

    @Test
    fun `velocidade abaixo do contratado e calculada direto das metricas`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.ethernet,
            internet = InternetDiagnosticInput(
                downloadMbps = 40.0,
                uploadMbps = 20.0,
                latencyMs = 15.0,
                jitterMs = 2.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            ),
            velocidadeContratadaMbps = 100,
        )
        val report = DiagnosticRunner.run(input)

        val request = RecommendationRequestMapper.map(report, input)

        assertEquals(NetworkContextType.ETHERNET, request.network)
        assertTrue(DiagnosticTag.VELOCIDADE_ABAIXO_DO_CONTRATADO in request.tags)
    }

    @Test
    fun `conexao saudavel nao gera nenhuma tag de problema`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 200.0,
                uploadMbps = 50.0,
                latencyMs = 15.0,
                jitterMs = 2.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -50,
                linkSpeedMbps = 866,
                frequenciaMhz = 5180,
            ),
        )
        val report = DiagnosticRunner.run(input)

        val request = RecommendationRequestMapper.map(report, input)

        assertTrue(request.tags.isEmpty())
        assertEquals(emptyList<Any>(), request.history)
    }
}
