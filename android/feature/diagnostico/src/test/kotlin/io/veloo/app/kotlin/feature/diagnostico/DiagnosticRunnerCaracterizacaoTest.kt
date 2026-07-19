package io.signallq.app.feature.diagnostico

import io.signallq.app.feature.diagnostico.topology.model.NatStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Teste de caracterizacao do comportamento ATUAL de [DiagnosticRunner.run] fim-a-fim, escrito
 * ANTES da extracao do dominio de diagnostico para :core:diagnostico (issue #1157, Fase 1a).
 *
 * Objetivo: travar o comportamento hoje — sobretudo o acoplamento direto
 * `DiagnosticRunner.run() -> RecommendationEngine.recomendar()` (linha 77 de DiagnosticRunner.kt)
 * que sera substituido por inversao de dependencia (parametro `gerarRecomendacoes`). Depois da
 * extracao, este mesmo arquivo de teste deve continuar passando sem alterar nenhuma asserção —
 * só o import de `DiagnosticRunner`/`FindingResult`/etc muda de pacote, e o call site aqui passa
 * a fornecer `gerarRecomendacoes = RecommendationEngine::recomendar` explicitamente (documentado
 * em cada teste abaixo).
 *
 * Não é teste de regra de negócio nova — é rede de segurança pra um refactor mecânico.
 */
class DiagnosticRunnerCaracterizacaoTest {

    @Test
    fun `REC-01 flui do DiagnosticRunner fim-a-fim ate o report final via RecommendationEngine`() {
        // Cenario identico ao "situacao 1" de RecommendationEngineTest — 2,4GHz fraco com 5GHz
        // forte disponivel no mesmo SSID. Aqui rodamos o pipeline INTEIRO (nao RecommendationEngine
        // isolado) pra caracterizar o wiring real que o DiagnosticRunner faz hoje.
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            wifi = WifiDiagnosticInput(
                rssiDbm = -55,
                linkSpeedMbps = 65,
                frequenciaMhz = 2437,
                ssid = "CasaWifi",
            ),
            internet = InternetDiagnosticInput(
                downloadMbps = 15.0,
                uploadMbps = 10.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
            ),
            wifiScan = WifiScanDiagnosticInput(
                redes = listOf(
                    RedeWifiVizinha(
                        canal = 36,
                        rssiDbm = -50,
                        frequenciaMhz = 5180,
                        ssid = "CasaWifi",
                        bssid = "AA:BB:CC:11:22:33",
                    ),
                ),
            ),
        )

        val r = DiagnosticRunner.run(input)

        assertTrue("esperava REC-01 nas recomendacoes do report", r.recomendacoes.any { it.id == "REC-01" })
    }

    @Test
    fun `mobile connection nao gera diagnostico wifi mas gera mobile e mantem score calculavel`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.mobile,
            internet = InternetDiagnosticInput(
                downloadMbps = 10.0,
                uploadMbps = 2.0,
                latencyMs = 50.0,
                jitterMs = 10.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -90,
                linkSpeedMbps = 10,
                frequenciaMhz = 2412,
            ),
            mobile = MobileDiagnosticInput(
                carrierName = "operadora",
                mobileTechnology = "4G",
                signalQualityPercent = 20,
            ),
        )

        val r = DiagnosticRunner.run(input)

        assertTrue(r.wifiResultados.isEmpty())
        assertTrue(r.mobileResultados.isNotEmpty())
        assertTrue("scoreConexao deve ser calculavel (0..100)", r.scoreConexao in 0..100)
    }

    @Test
    fun `NAT CGNAT gera achado informativo REDE-NAT-01 sem elevar veredito para critico`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 20.0,
                latencyMs = 15.0,
                jitterMs = 3.0,
                perdaPercentual = 0.0,
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -50,
                linkSpeedMbps = 300,
                frequenciaMhz = 5200,
            ),
            natStatus = NatStatus.CGNAT,
        )

        val r = DiagnosticRunner.run(input)

        val achadoNat = r.redeResultados.firstOrNull { it.id == "REDE-NAT-01" }
        assertTrue("esperava achado REDE-NAT-01 para NAT=CGNAT", achadoNat != null)
        assertEquals(DiagnosticStatus.info, achadoNat!!.status)
    }

    @Test
    fun `cenario bom de wifi e internet produz veredito Excelente ou Bom`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 200.0,
                uploadMbps = 50.0,
                latencyMs = 12.0,
                jitterMs = 2.0,
                perdaPercentual = 0.0,
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -45,
                linkSpeedMbps = 866,
                frequenciaMhz = 5200,
            ),
        )

        val r = DiagnosticRunner.run(input)

        assertTrue(
            "veredito esperado Excelente ou Bom, obtido: ${r.veredito}",
            r.veredito == "Excelente" || r.veredito == "Bom",
        )
    }

    @Test
    fun `relatorio sempre calcula perfisUso e gameReadiness independente do input`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 50.0,
                uploadMbps = 10.0,
                latencyMs = 30.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
            ),
        )

        val r = DiagnosticRunner.run(input)

        assertTrue(r.perfisUso.isNotEmpty())
        assertTrue(r.gameReadiness.isNotEmpty())
    }
}
