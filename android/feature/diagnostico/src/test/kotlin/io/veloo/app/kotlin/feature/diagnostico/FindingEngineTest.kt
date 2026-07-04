package io.signallq.app.feature.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [FindingEngine]: desempate por score (severidade × confiança),
 * múltiplos achados simultâneos, hipóteses descartadas e dados ausentes.
 *
 * Os testes de regras individuais preservadas do DiagnosticDecisionEngine
 * (GW-01/02) estão em [FindingEngineGatewayTest].
 */
class FindingEngineTest {

    private fun resultado(
        id: String,
        status: DiagnosticStatus,
        categoria: String,
    ) = DiagnosticResult(
        id = id,
        titulo = id,
        status = status,
        evidencia = "test",
        mensagemUsuario = "msg $id",
        recomendacao = null,
        categoria = categoria,
    )

    // -------------------------------------------------------------------------
    // Tudo ok: nenhuma regra bate → DECISAO-04
    // -------------------------------------------------------------------------

    @Test
    fun `sem nenhum achado, principal e DECISAO-04 (tudo ok)`() {
        val r = FindingEngine.analisar(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
        )

        assertEquals("DECISAO-04", r.principal.id)
        assertEquals(DiagnosticStatus.ok, r.principal.status)
        assertTrue(r.secundarios.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Múltiplos achados simultâneos (causas independentes)
    // -------------------------------------------------------------------------

    @Test
    fun `fibra critica e DNS atencao sao causas independentes - fibra e principal, DNS e secundario`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(
                resultado("IN-CRITICO", DiagnosticStatus.critical, "internet"),
                resultado("DNS-LENTO", DiagnosticStatus.attention, "dns"),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            fibraResultados = listOf(resultado("FIB-CRITICO", DiagnosticStatus.critical, "fibra")),
        )

        assertEquals("DECISAO-00", r.principal.id)
        assertTrue(
            "DNS atencao deve aparecer como achado secundario",
            r.secundarios.any { it.id == "DECISAO-DNS-01b" },
        )
    }

    @Test
    fun `internet critica e wifi-canal atencao sem gateway - internet e principal, canal e secundario`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(
                resultado("IN-CRITICO", DiagnosticStatus.critical, "internet"),
                resultado("WCAN-01", DiagnosticStatus.attention, "wifi-canal"),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
        )

        assertEquals("DECISAO-02", r.principal.id)
        assertTrue(
            "Congestionamento de canal Wi-Fi deve aparecer como secundario (causa independente)",
            r.secundarios.any { it.id == "DECISAO-WIFI-CANAL" },
        )
    }

    // -------------------------------------------------------------------------
    // Desempate por score (severidade × confiança)
    // -------------------------------------------------------------------------

    @Test
    fun `desempate - DNS critico isolado vence sobre wifi canal atencao (maior severidade)`() {
        // DNS crítico (severidade 4 * confianca 0.85 = 3.4) vs Wi-Fi canal atencao
        // (severidade 2 * confianca 0.55 = 1.1). DNS deve vencer com folga.
        val r = FindingEngine.analisar(
            internetResultados = listOf(
                resultado("DNS-RUIM", DiagnosticStatus.critical, "dns"),
                resultado("WCAN-01", DiagnosticStatus.attention, "wifi-canal"),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
        )

        assertEquals("DECISAO-DNS-01", r.principal.id)
    }

    @Test
    fun `desempate - GW-01 (confianca 0_9) vence sobre DECISAO-02 (confianca 0_8) mesma severidade critical`() {
        // Ambas critical (severidade 4). GW-01 score = 4*0.9 = 3.6, DECISAO-02 = 4*0.8 = 3.2.
        // GW-01 deve vencer por ter confiança maior (correlaciona duas fontes).
        val r = FindingEngine.analisar(
            internetResultados = listOf(
                resultado("IN-LATENCIA", DiagnosticStatus.critical, "internet"),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 5,
            latenciaInternetMs = 250.0,
        )

        assertEquals("DECISAO-GW-01", r.principal.id)
        assertTrue(
            "DECISAO-02 deve continuar como achado secundario, nao desaparecer",
            r.secundarios.any { it.id == "DECISAO-02" },
        )
    }

    // -------------------------------------------------------------------------
    // Hipóteses descartadas (mesma causa raiz suprimida por evidência mais forte)
    // -------------------------------------------------------------------------

    @Test
    fun `DNS critico suprimido por fibra critica vira hipotese descartada, nao desaparece`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(
                resultado("IN-CRITICO", DiagnosticStatus.critical, "internet"),
                resultado("DNS-RUIM", DiagnosticStatus.critical, "dns"),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            fibraResultados = listOf(resultado("FIB-CRITICO", DiagnosticStatus.critical, "fibra")),
        )

        assertEquals("DECISAO-00", r.principal.id)
        assertTrue(
            "DNS-01 deve aparecer como hipotese descartada, nao sumir",
            r.hipotesesDescartadas.any { it.id == "DECISAO-DNS-01" },
        )
        assertTrue(
            "DNS-01 nao deve aparecer como secundario (é a mesma causa raiz suprimida)",
            r.secundarios.none { it.id == "DECISAO-DNS-01" },
        )
        val dnsDescartada = r.hipotesesDescartadas.first { it.id == "DECISAO-DNS-01" }
        assertTrue(
            "motivo do descarte deve estar na evidencia, nao pode se perder",
            dnsDescartada.evidencia?.contains("descarte:") == true,
        )
    }

    @Test
    fun `internet critica com wifi ruim - DECISAO-02 vira hipotese descartada e DECISAO-01 e o principal`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(
                resultado("IN-CRITICO", DiagnosticStatus.critical, "internet"),
            ),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = false),
        )

        assertEquals("DECISAO-01", r.principal.id)
        assertTrue(
            "DECISAO-02 deve ser hipotese descartada (Wi-Fi ruim pode ser a causa real)",
            r.hipotesesDescartadas.any { it.id == "DECISAO-02" },
        )
    }

    // -------------------------------------------------------------------------
    // Dados ausentes
    // -------------------------------------------------------------------------

    @Test
    fun `dados ausentes reporta rttGateway e fibra quando nao informados`() {
        val r = FindingEngine.analisar(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = null,
        )

        assertTrue(r.dadosAusentes.contains("rttGateway"))
        assertTrue(r.dadosAusentes.contains("fibra"))
    }

    @Test
    fun `dados ausentes nao reporta rttGateway quando informado`() {
        val r = FindingEngine.analisar(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            rttGatewayMs = 20,
        )

        assertTrue(!r.dadosAusentes.contains("rttGateway"))
    }

    // -------------------------------------------------------------------------
    // Regra DECISAO-00 (fibra crítica) preservada
    // -------------------------------------------------------------------------

    @Test
    fun `fibra critica com internet ruim continua produzindo DECISAO-00`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            fibraResultados = listOf(resultado("FIB-CRITICO", DiagnosticStatus.critical, "fibra")),
        )

        assertEquals("DECISAO-00", r.principal.id)
        assertEquals(DiagnosticStatus.critical, r.principal.status)
    }
}
