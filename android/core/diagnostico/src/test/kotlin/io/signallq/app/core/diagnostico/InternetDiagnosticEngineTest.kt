package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InternetDiagnosticEngineTest {
    @Test
    fun `upload zero generates critical and overrides generic upload low`() {
        val input =
            InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 0.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            )
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = true)
        assertTrue(resultados.any { it.id == "IN-NORMAL-04Z" && it.status == DiagnosticStatus.critical })
        assertTrue(resultados.none { it.id == "IN-NORMAL-04" })
    }

    @Test
    fun `packet loss recommendation mentions router on wifi`() {
        val input =
            InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 20.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 5.0,
                bufferbloatMs = 0.0,
            )
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = true, connectionType = ConnectionType.wifi)
        val achado = resultados.first { it.id == "IN-NORMAL-07" }
        assertTrue(achado.recomendacao!!.contains("roteador"))
    }

    @Test
    fun `packet loss recommendation does not mention router on mobile`() {
        val input =
            InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 20.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 5.0,
                bufferbloatMs = 0.0,
            )
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = true, connectionType = ConnectionType.mobile)
        val achado = resultados.first { it.id == "IN-NORMAL-07" }
        assertTrue(!achado.recomendacao!!.contains("roteador") && !achado.recomendacao!!.contains("modem"))
    }

    @Test
    fun `wifi not reliable converts issues to inconclusive`() {
        val input =
            InternetDiagnosticInput(
                downloadMbps = 10.0, // gera download baixo
                uploadMbps = 2.0,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            )
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = false)
        assertTrue(resultados.isNotEmpty())
        assertEquals(true, resultados.all { it.status == DiagnosticStatus.inconclusive })
    }

    // ── Testes dourados (caracterizacao) — Fase 0 da issue #1228, ver ADR-011 ──────────────
    // Congelam o comportamento atual EXATO de todas as fronteiras de threshold do engine.
    // Nao alterar producao pra fazer estes testes passarem: se um valor mudar aqui, deve ser
    // por decisao deliberada de migracao (Fase 1+), nunca por acidente. Baseline usa valores
    // "limpos" (sem gerar outros achados) pra isolar cada fronteira.

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
    fun `golden - perda de pacotes fronteira 1_0 gera atencao moderada`() {
        val abaixo = InternetDiagnosticEngine.avaliar(baseline(perda = 0.999), wifiConfiavelParaTeste = true)
        assertTrue(abaixo.none { it.id.startsWith("IN-NORMAL-07") })

        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(perda = 1.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.any { it.id == "IN-NORMAL-07b" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `golden - perda de pacotes fronteira 3_0 vira critico e substitui moderado`() {
        val abaixo = InternetDiagnosticEngine.avaliar(baseline(perda = 2.999), wifiConfiavelParaTeste = true)
        assertTrue(abaixo.any { it.id == "IN-NORMAL-07b" })
        assertTrue(abaixo.none { it.id == "IN-NORMAL-07" })

        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(perda = 3.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.any { it.id == "IN-NORMAL-07" && it.status == DiagnosticStatus.critical })
        assertTrue(naFronteira.none { it.id == "IN-NORMAL-07b" })
    }

    @Test
    fun `golden - jitter fronteira 20_0 e estritamente maior que`() {
        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(jit = 20.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.none { it.id == "IN-NORMAL-06" })

        val acima = InternetDiagnosticEngine.avaliar(baseline(jit = 20.01), wifiConfiavelParaTeste = true)
        assertTrue(acima.any { it.id == "IN-NORMAL-06" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `golden - latencia fronteira 100_0 e estritamente maior que`() {
        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(lat = 100.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.none { it.id == "IN-NORMAL-05" })

        val acima = InternetDiagnosticEngine.avaliar(baseline(lat = 100.01), wifiConfiavelParaTeste = true)
        assertTrue(acima.any { it.id == "IN-NORMAL-05" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `IN-NORMAL-05 nao cita Anatel RQUAL e mantem limiar e severidade inalterados`() {
        // GH#1502 (revisao independente da PR #1515) -- a mensagem citava "Anatel RQUAL"
        // sem fonte normativa comprovada no repositorio; a decisao aprovada na planilha
        // original foi superada por revisao de confiabilidade. Este teste trava: (1) o
        // texto antigo nao volta a aparecer, (2) o limiar (100.0, estritamente maior --
        // ja coberto pelo golden acima) e a severidade (attention) permanecem os mesmos.
        val resultados = InternetDiagnosticEngine.avaliar(baseline(lat = 150.0), wifiConfiavelParaTeste = true)
        val achado = resultados.first { it.id == "IN-NORMAL-05" }

        assertEquals(DiagnosticStatus.attention, achado.status)
        assertTrue(
            "mensagemUsuario ainda cita Anatel/RQUAL: ${achado.mensagemUsuario}",
            !achado.mensagemUsuario.contains("Anatel", ignoreCase = true) &&
                !achado.mensagemUsuario.contains("RQUAL", ignoreCase = true),
        )
        achado.recomendacao?.let {
            assertTrue(
                "recomendacao ainda cita Anatel/RQUAL: $it",
                !it.contains("Anatel", ignoreCase = true) && !it.contains("RQUAL", ignoreCase = true),
            )
        }
        // Mensagem substituta ainda corresponde ao estado tecnico real (valor medido).
        assertTrue(achado.mensagemUsuario.contains("150"))
    }

    @Test
    fun `golden - bufferbloat fronteira 30_0 nao gera achado e 30_01 gera elevado`() {
        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(bb = 30.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.none { it.id.startsWith("IN-NORMAL-09") })

        val acima = InternetDiagnosticEngine.avaliar(baseline(bb = 30.01), wifiConfiavelParaTeste = true)
        assertTrue(acima.any { it.id == "IN-NORMAL-09b" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `golden - bufferbloat fronteira 100_0 ainda e elevado e 100_01 vira critico`() {
        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(bb = 100.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.any { it.id == "IN-NORMAL-09b" && it.status == DiagnosticStatus.attention })
        assertTrue(naFronteira.none { it.id == "IN-NORMAL-09" })

        val acima = InternetDiagnosticEngine.avaliar(baseline(bb = 100.01), wifiConfiavelParaTeste = true)
        assertTrue(acima.any { it.id == "IN-NORMAL-09" && it.status == DiagnosticStatus.critical })
        assertTrue(acima.any { it.podeConcluir })
    }

    @Test
    fun `golden - upload fronteira 5_0 nao gera achado e abaixo gera upload baixo`() {
        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(ul = 5.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.none { it.id == "IN-NORMAL-04" })

        val abaixo = InternetDiagnosticEngine.avaliar(baseline(ul = 4.999), wifiConfiavelParaTeste = true)
        assertTrue(abaixo.any { it.id == "IN-NORMAL-04" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `golden - upload zerado tem prioridade sobre upload baixo generico`() {
        val resultados = InternetDiagnosticEngine.avaliar(baseline(ul = 0.0), wifiConfiavelParaTeste = true)
        assertTrue(resultados.any { it.id == "IN-NORMAL-04Z" && it.status == DiagnosticStatus.critical && it.podeConcluir })
        assertTrue(resultados.none { it.id == "IN-NORMAL-04" })
    }

    @Test
    fun `golden - download fronteira 25_0 e estritamente menor que`() {
        val naFronteira = InternetDiagnosticEngine.avaliar(baseline(dl = 25.0), wifiConfiavelParaTeste = true)
        assertTrue(naFronteira.none { it.id == "IN-NORMAL-03" })

        val abaixo = InternetDiagnosticEngine.avaliar(baseline(dl = 24.999), wifiConfiavelParaTeste = true)
        assertTrue(abaixo.any { it.id == "IN-NORMAL-03" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `golden - baseline limpa gera conexao saudavel`() {
        val resultados = InternetDiagnosticEngine.avaliar(baseline(), wifiConfiavelParaTeste = true)
        assertEquals(1, resultados.size)
        assertEquals("IN-NORMAL-02", resultados.first().id)
        assertEquals(DiagnosticStatus.ok, resultados.first().status)
        assertTrue(resultados.first().podeConcluir)
    }

    @Test
    fun `golden - download nulo gera internet indisponivel critico e conclusivo`() {
        val input = baseline().copy(downloadMbps = null)
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = true)
        assertEquals(1, resultados.size)
        assertEquals("IN-NORMAL-01", resultados.first().id)
        assertEquals(DiagnosticStatus.critical, resultados.first().status)
        assertTrue(resultados.first().podeConcluir)
    }

    @Test
    fun `golden - input nulo gera sem dados de velocidade inconclusivo`() {
        val resultados = InternetDiagnosticEngine.avaliar(null, wifiConfiavelParaTeste = true)
        assertEquals(1, resultados.size)
        assertEquals("IN-NORMAL-00", resultados.first().id)
        assertEquals(DiagnosticStatus.inconclusive, resultados.first().status)
    }
}
