package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — congela, sem alterar producao, as tres divergencias ja
 * documentadas na issue #1466 entre [InternetDiagnosticEngine] (limiares de negocio
 * literais) e [MetricClassifier] (tabela "canonica" pretendida). Ver
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, Parte 2 e Parte 8
 * (P0-1), para o achado completo: as duas fontes divergem simultaneamente na MESMA tela
 * (`ResultadoVelocidadeScreen`), nao so em teoria.
 *
 * Nao "corrige" nada — so nomeia e comprova, com valor exato, cada ponto onde o card de
 * metrica (via [MetricClassifier]) e o banner de diagnostico (via
 * [InternetDiagnosticEngine]) concluiriam coisas diferentes para a mesma medicao.
 */
class InternetDiagnosticEngineVsMetricClassifierCharacterizationTest {
    private fun baseline(
        dl: Double = 100.0,
        ul: Double? = 20.0,
        lat: Double? = 20.0,
        jit: Double? = 5.0,
        perda: Double? = 0.0,
        bb: Double? = 0.0,
    ) = InternetDiagnosticInput(
        downloadMbps = dl,
        uploadMbps = ul,
        latencyMs = lat,
        jitterMs = jit,
        perdaPercentual = perda,
        bufferbloatMs = bb,
    )

    @Test
    fun `latencia de 120ms recebe classificacoes diferentes no card e no banner da mesma tela`() {
        // Card: MetricClassifier.classificarLatencia(120.0) -- "<=150" ainda e "bom".
        assertEquals(MetricStatus.bom, MetricClassifier.classificarLatencia(120.0))

        // Banner: InternetDiagnosticEngine dispara IN-NORMAL-05 (attention) para
        // qualquer latencia > 100.0ms, incluindo 120ms.
        val achados = InternetDiagnosticEngine.avaliar(baseline(lat = 120.0), wifiConfiavelParaTeste = true)
        assertTrue(achados.any { it.id == "IN-NORMAL-05" && it.status == DiagnosticStatus.attention })

        // Caracteriza a divergencia: card diz "Bom", banner diz "demorando para responder"
        // -- mesma medicao, mesma tela, dois vocabularios/reguas diferentes (issue #1466).
    }

    @Test
    fun `latencia no boundary exato de 100ms nao diverge -- fronteira coincide por acidente`() {
        // Em exatos 100.0ms nenhum dos dois dispara (classifier: <100 excelente, senao bom
        // ate 150; engine: so dispara em > 100.0, estritamente). Documenta que a divergencia
        // comeca IMEDIATAMENTE ACIMA de 100.0ms, nao em 100.0ms.
        assertEquals(MetricStatus.excelente, MetricClassifier.classificarLatencia(100.0))
        val achados = InternetDiagnosticEngine.avaliar(baseline(lat = 100.0), wifiConfiavelParaTeste = true)
        assertTrue(achados.none { it.id == "IN-NORMAL-05" })
    }

    @Test
    fun `perda de pacotes de 1,5 por cento recebe classificacoes diferentes no card e no banner`() {
        // Card: MetricClassifier.classificarPerdaPacotes(1.5) -- "<=2.0" ainda e "regular".
        assertEquals(MetricStatus.regular, MetricClassifier.classificarPerdaPacotes(1.5))

        // Banner: InternetDiagnosticEngine ja dispara IN-NORMAL-07b (attention) a partir
        // de 1.0%, um limiar de negocio diferente do da tabela canonica.
        val achados = InternetDiagnosticEngine.avaliar(baseline(perda = 1.5), wifiConfiavelParaTeste = true)
        assertTrue(achados.any { it.id == "IN-NORMAL-07b" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `upload de 4 Mbps recebe classificacoes diferentes no card e no banner`() {
        // Card: MetricClassifier.classificarUpload(4.0) -- ">=3.0" ainda e "regular" (nao "ruim").
        assertEquals(MetricStatus.regular, MetricClassifier.classificarUpload(4.0))

        // Banner: InternetDiagnosticEngine dispara IN-NORMAL-04 (attention, "velocidade de
        // envio esta baixa") para qualquer upload entre 0 e 5.0 Mbps exclusive.
        val achados = InternetDiagnosticEngine.avaliar(baseline(ul = 4.0), wifiConfiavelParaTeste = true)
        assertTrue(achados.any { it.id == "IN-NORMAL-04" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `jitter e download nao divergem -- fronteiras de negocio e canonica coincidem`() {
        // Contraste positivo: para jitter e download, InternetDiagnosticEngine ja delega a
        // MetricClassifier (Fase 1, ADR-011) porque as fronteiras de negocio coincidem
        // valor a valor. Sem divergencia aqui -- registrado para nao confundir com os
        // casos acima ao ler este arquivo isoladamente.
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarJitter(21.0))
        val achadosJitter = InternetDiagnosticEngine.avaliar(baseline(jit = 21.0), wifiConfiavelParaTeste = true)
        assertTrue(achadosJitter.any { it.id == "IN-NORMAL-06" })

        assertEquals(MetricStatus.ruim, MetricClassifier.classificarDownload(24.0))
        val achadosDownload = InternetDiagnosticEngine.avaliar(baseline(dl = 24.0), wifiConfiavelParaTeste = true)
        assertTrue(achadosDownload.any { it.id == "IN-NORMAL-03" })
    }
}
