package io.veloo.app.feature.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Testes unitários das regras de gateway (GW-01 e GW-02) no DiagnosticDecisionEngine.
 *
 * Garante:
 *  - GW-01: gateway rápido + internet lenta → culpa na operadora
 *  - GW-02: gateway lento → roteador com problema
 *  - Gateway null → regras desabilitadas, fluxo segue normalmente sem regressão
 */
class DiagnosticDecisionEngineGatewayTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun decisaoOk() = DiagnosticDecisionEngine.decidir(
        internetResultados = emptyList(),
        wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
    )

    private fun internetCriticoSemGateway() = DiagnosticDecisionEngine.decidir(
        internetResultados = listOf(
            DiagnosticResult(
                id = "IN-TEST",
                titulo = "Internet crítica",
                status = DiagnosticStatus.critical,
                evidencia = "test",
                mensagemUsuario = "Internet com problema",
                recomendacao = null,
                categoria = "internet",
            ),
        ),
        wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
    )

    // -------------------------------------------------------------------------
    // Teste GW-01: gateway rápido + internet lenta → operadora
    // -------------------------------------------------------------------------

    @Test
    fun `GW-01 ativa quando gateway rapido e latencia internet alta`() {
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = listOf(
                DiagnosticResult(
                    id = "IN-LATENCIA",
                    titulo = "Latência alta",
                    status = DiagnosticStatus.critical,
                    evidencia = "latency=250ms",
                    mensagemUsuario = "Latência alta",
                    recomendacao = null,
                    categoria = "internet",
                ),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 5,           // gateway rápido (< 10ms)
            latenciaInternetMs = 250.0, // internet lenta (> 200ms)
        )

        assertEquals("GW-01 deve ser ativada", "DECISAO-GW-01", decisao.id)
        assertEquals("Status deve ser critical", DiagnosticStatus.critical, decisao.status)
    }

    @Test
    fun `GW-01 nao ativa quando gateway rapido mas internet tambem rapida`() {
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 5,           // gateway rápido
            latenciaInternetMs = 50.0,  // internet também rápida (< 200ms)
        )

        assertNotEquals("GW-01 não deve ativar com internet rápida", "DECISAO-GW-01", decisao.id)
    }

    @Test
    fun `GW-01 nao ativa quando gateway no limiar (10ms)`() {
        // Gateway = 10ms: condição é < 10ms, então 10ms NÃO ativa
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = listOf(
                DiagnosticResult(
                    id = "IN-LATENCIA",
                    titulo = "Latência alta",
                    status = DiagnosticStatus.critical,
                    evidencia = "latency=250ms",
                    mensagemUsuario = "Latência alta",
                    recomendacao = null,
                    categoria = "internet",
                ),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 10,          // limiar: não ativa GW-01
            latenciaInternetMs = 250.0,
        )

        assertNotEquals("GW-01 não deve ativar com gateway=10ms (limiar)", "DECISAO-GW-01", decisao.id)
    }

    @Test
    fun `GW-01 nao ativa quando wifi ruim (resultado pode ser impreciso)`() {
        // Com Wi-Fi ruim, o diagnóstico não conclui sobre ISP
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = listOf(
                DiagnosticResult(
                    id = "IN-LATENCIA",
                    titulo = "Latência alta",
                    status = DiagnosticStatus.critical,
                    evidencia = "latency=250ms",
                    mensagemUsuario = "Latência alta",
                    recomendacao = null,
                    categoria = "internet",
                ),
            ),
            wifiQuality = WifiQualityResult(
                resultados = emptyList(),
                confiavelParaTeste = false, // Wi-Fi ruim
            ),
            rttGatewayMs = 5,
            latenciaInternetMs = 250.0,
        )

        assertNotEquals("GW-01 não deve ativar com Wi-Fi ruim", "DECISAO-GW-01", decisao.id)
    }

    // -------------------------------------------------------------------------
    // Teste GW-02: gateway lento → roteador
    // -------------------------------------------------------------------------

    @Test
    fun `GW-02 ativa quando gateway lento (acima de 50ms)`() {
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 80, // gateway lento (> 50ms)
        )

        assertEquals("GW-02 deve ser ativada", "DECISAO-GW-02", decisao.id)
        assertEquals("Status deve ser attention", DiagnosticStatus.attention, decisao.status)
    }

    @Test
    fun `GW-02 nao ativa quando gateway normal (abaixo de 50ms)`() {
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 30, // gateway normal (< 50ms)
        )

        assertNotEquals("GW-02 não deve ativar com gateway=30ms", "DECISAO-GW-02", decisao.id)
    }

    @Test
    fun `GW-02 nao ativa quando gateway no limiar (50ms)`() {
        // Exatamente 50ms: condição é > 50ms, então 50ms NÃO ativa
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 50, // limiar: não ativa GW-02
        )

        assertNotEquals("GW-02 não deve ativar com gateway=50ms (limiar)", "DECISAO-GW-02", decisao.id)
    }

    // -------------------------------------------------------------------------
    // Teste: Gateway null → regras desabilitadas, sem regressão
    // -------------------------------------------------------------------------

    @Test
    fun `gateway null desabilita GW-01 e GW-02`() {
        val decisaoSemGateway = DiagnosticDecisionEngine.decidir(
            internetResultados = listOf(
                DiagnosticResult(
                    id = "IN-LATENCIA",
                    titulo = "Latência alta",
                    status = DiagnosticStatus.critical,
                    evidencia = "latency=250ms",
                    mensagemUsuario = "Latência alta",
                    recomendacao = null,
                    categoria = "internet",
                ),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = null,        // sem gateway
            latenciaInternetMs = 250.0,
        )

        assertNotEquals("GW-01 não deve ativar sem gateway", "DECISAO-GW-01", decisaoSemGateway.id)
        assertNotEquals("GW-02 não deve ativar sem gateway", "DECISAO-GW-02", decisaoSemGateway.id)
    }

    @Test
    fun `gateway null nao regride decisao DECISAO-02 (internet critica sem wifi ruim)`() {
        // Comportamento pré-existente: internet crítica + Wi-Fi ok → DECISAO-02
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = listOf(
                DiagnosticResult(
                    id = "IN-CRITICO",
                    titulo = "Internet crítica",
                    status = DiagnosticStatus.critical,
                    evidencia = "test",
                    mensagemUsuario = "Internet com problema",
                    recomendacao = null,
                    categoria = "internet",
                ),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = null,
        )

        assertEquals("Sem gateway, DECISAO-02 deve funcionar normalmente", "DECISAO-02", decisao.id)
    }

    @Test
    fun `gateway null nao regride decisao DECISAO-04 (tudo ok)`() {
        val decisao = DiagnosticDecisionEngine.decidir(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = null,
        )

        assertEquals("Sem problemas, deve retornar DECISAO-04", "DECISAO-04", decisao.id)
        assertEquals("Status deve ser ok", DiagnosticStatus.ok, decisao.status)
    }

    // -------------------------------------------------------------------------
    // Teste via DiagnosticRunner (integração ponta-a-ponta)
    // -------------------------------------------------------------------------

    @Test
    fun `runner repassa rttGatewayMs do input para o engine e ativa GW-01`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 50.0,
                uploadMbps = 10.0,
                latencyMs = 250.0,  // internet lenta
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                rttGatewayMs = 5,   // gateway rápido
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -55,      // Wi-Fi bom
                linkSpeedMbps = 100,
                frequenciaMhz = 5180,
            ),
        )

        val report = DiagnosticRunner.run(input)
        assertEquals("Runner deve propagar rttGatewayMs e ativar GW-01", "DECISAO-GW-01", report.decisao.id)
    }

    @Test
    fun `runner com rttGatewayMs null nao ativa regras GW`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 50.0,
                uploadMbps = 10.0,
                latencyMs = 250.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                rttGatewayMs = null, // sem gateway
            ),
            wifi = WifiDiagnosticInput(
                rssiDbm = -55,
                linkSpeedMbps = 100,
                frequenciaMhz = 5180,
            ),
        )

        val report = DiagnosticRunner.run(input)
        assertNotEquals("Sem gateway, GW-01 não deve ativar", "DECISAO-GW-01", report.decisao.id)
        assertNotEquals("Sem gateway, GW-02 não deve ativar", "DECISAO-GW-02", report.decisao.id)
    }
}
