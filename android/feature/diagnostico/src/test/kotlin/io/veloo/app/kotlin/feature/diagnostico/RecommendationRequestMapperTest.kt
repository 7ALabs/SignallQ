package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.DnsDiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.MobileDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.recommendation.DiagnosticTag
import io.signallq.app.core.recommendation.NetworkContextType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #1528: desde que [RecommendationRequestMapper.mapTags] passou a ler
 * `report.recomendacoes` (saida real do [RecomendacaoPraticaEngine]) para as tags com regra
 * REC equivalente, todo teste aqui precisa passar `gerarRecomendacoes =
 * RecomendacaoPraticaEngine::recomendar` para [DiagnosticRunner.run] -- sem isso
 * `report.recomendacoes` fica vazio e as tags correspondentes nunca aparecem.
 */
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
            // rssi=-75dBm (2.4GHz) + linkSpeed=40Mbps dispara REC-02 (Aproxime-se do
            // roteador) -- rssi mais fraco que -80 bloquearia REC-01/02 (ver
            // recomendarWifi5Ghz/recomendarDistanciaRoteador), e linkSpeed >= 54 tambem
            // bloquearia REC-02.
            wifi = WifiDiagnosticInput(
                rssiDbm = -75,
                linkSpeedMbps = 40,
                frequenciaMhz = 2412,
                dispositivosNaRede = 25,
            ),
        )
        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

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
        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

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
            // REC-06 exige uma alternativa de DNS com margem segura de 5ms (ver
            // recomendarDnsLento) -- so a latencia atual alta (DNS-01/02 bruto) nao basta.
            dns = DnsDiagnosticInput(
                currentDnsIp = "10.0.0.1",
                currentDnsName = "operadora",
                currentDnsLatencyMs = 320,
                bestDnsNameFromComparison = "Cloudflare",
                bestDnsLatencyMsFromComparison = 30,
                dnsComparisonAvailable = true,
            ),
        )
        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

        val request = RecommendationRequestMapper.map(report, input)

        assertTrue(DiagnosticTag.DNS_LENTO in request.tags)
        assertTrue(DiagnosticTag.WIFI_FRACO !in request.tags)
    }

    @Test
    fun `velocidade abaixo do contratado reflete a comparacao REC-04 com o linkSpeed`() {
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
            // REC-04 (recomendarRoteadorLimitado) exige input.wifi != null e compara
            // velocidadeContratadaMbps contra linkSpeedMbps (nao contra download) -- rssi
            // bom + linkSpeed < 144 + plano (100) > linkSpeed (80) dispara a regra.
            wifi = WifiDiagnosticInput(
                rssiDbm = -55,
                linkSpeedMbps = 80,
                frequenciaMhz = 5180,
            ),
            velocidadeContratadaMbps = 100,
        )
        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

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
        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

        val request = RecommendationRequestMapper.map(report, input)

        assertTrue(request.tags.isEmpty())
        assertEquals(emptyList<Any>(), request.history)
    }
}
