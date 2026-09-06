package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Casos dourados dos 6 valores de [DiagnosticDivergenceClassifier.Classification]
 * fixados por #952/#1444. Fixtures montam apenas o suficiente de [DiagnosticReport]
 * para o classificador (decisao.status/categoriaOrigem + scoreConexao via
 * `scoreEngineResultado = null`, que cai na tabela legada por status).
 */
class DiagnosticDivergenceClassifierTest {
    private fun reportDe(
        status: DiagnosticStatus,
        categoriaOrigem: String? = "wifi",
        podeConcluir: Boolean = true,
    ): DiagnosticReport =
        DiagnosticReport(
            wifiResultados = emptyList(),
            internetResultados = emptyList(),
            fibraResultados = emptyList(),
            decisao =
                DiagnosticResult(
                    id = "DECISAO-TESTE",
                    titulo = "Teste",
                    status = status,
                    evidencia = null,
                    mensagemUsuario = "Mensagem de teste.",
                    recomendacao = null,
                    categoria = "decisao",
                    podeConcluir = podeConcluir,
                    categoriaOrigem = categoriaOrigem,
                ),
            geradoEmMs = 0L,
        )

    @Test
    fun `EXACT_MATCH quando status, origem e score coincidem`() {
        val local = reportDe(DiagnosticStatus.ok, categoriaOrigem = "wifi")
        val remote = reportDe(DiagnosticStatus.ok, categoriaOrigem = "wifi")

        val comparison = DiagnosticDivergenceClassifier.classify(local, remote)

        assertEquals(DiagnosticDivergenceClassifier.Classification.EXACT_MATCH, comparison.classification)
        assertEquals(DiagnosticStatus.ok, comparison.localStatus)
        assertEquals(DiagnosticStatus.ok, comparison.remoteStatus)
    }

    @Test
    fun `EQUIVALENT_RESULT quando os dois estao saudaveis mas com status ou origem textual diferente`() {
        // ok (score 90) x info (score 75) -- mesmo grupo "saudavel", delta de score 15 (dentro da tolerancia)
        val local = reportDe(DiagnosticStatus.ok, categoriaOrigem = "wifi")
        val remote = reportDe(DiagnosticStatus.info, categoriaOrigem = "dns")

        val comparison = DiagnosticDivergenceClassifier.classify(local, remote)

        assertEquals(DiagnosticDivergenceClassifier.Classification.EQUIVALENT_RESULT, comparison.classification)
    }

    @Test
    fun `MINOR_DIVERGENCE quando os grupos de severidade sao adjacentes`() {
        val local = reportDe(DiagnosticStatus.ok, categoriaOrigem = "wifi")
        val remote = reportDe(DiagnosticStatus.attention, categoriaOrigem = "wifi")

        val comparison = DiagnosticDivergenceClassifier.classify(local, remote)

        assertEquals(DiagnosticDivergenceClassifier.Classification.MINOR_DIVERGENCE, comparison.classification)
    }

    @Test
    fun `MAJOR_DIVERGENCE quando um lado diz saudavel e o outro critico`() {
        val local = reportDe(DiagnosticStatus.ok, categoriaOrigem = "wifi")
        val remote = reportDe(DiagnosticStatus.critical, categoriaOrigem = "isp", podeConcluir = true)

        val comparison = DiagnosticDivergenceClassifier.classify(local, remote)

        assertEquals(DiagnosticDivergenceClassifier.Classification.MAJOR_DIVERGENCE, comparison.classification)
    }

    @Test
    fun `REMOTE_ERROR quando a avaliacao remota nao produziu relatorio`() {
        val local = reportDe(DiagnosticStatus.ok)

        val comparison = DiagnosticDivergenceClassifier.classify(local, null)

        assertEquals(DiagnosticDivergenceClassifier.Classification.REMOTE_ERROR, comparison.classification)
        assertEquals(DiagnosticStatus.ok, comparison.localStatus)
        assertNull(comparison.remoteStatus)
    }

    @Test
    fun `LOCAL_ERROR quando o motor local nao produziu relatorio`() {
        val remote = reportDe(DiagnosticStatus.critical)

        val comparison = DiagnosticDivergenceClassifier.classify(null, remote)

        assertEquals(DiagnosticDivergenceClassifier.Classification.LOCAL_ERROR, comparison.classification)
        assertNull(comparison.localStatus)
        assertEquals(DiagnosticStatus.critical, comparison.remoteStatus)
    }

    @Test
    fun `score fora da tolerancia de EXACT_MATCH mesmo com status e origem iguais cai para EQUIVALENT_RESULT`() {
        // ok x ok normalmente teria mesmo score (90), entao forcamos via scoreEngineResultado
        // diferente para exercitar a fronteira de tolerancia isoladamente.
        val local =
            reportDe(DiagnosticStatus.attention, categoriaOrigem = "wifi", podeConcluir = true)
                .copy(scoreEngineResultado = ScoreResult(score = 65, dimensoesUsadas = emptyList(), dadosAusentes = emptyList()))
        val remote =
            reportDe(DiagnosticStatus.attention, categoriaOrigem = "wifi", podeConcluir = true)
                .copy(scoreEngineResultado = ScoreResult(score = 50, dimensoesUsadas = emptyList(), dadosAusentes = emptyList()))

        val comparison = DiagnosticDivergenceClassifier.classify(local, remote)

        assertEquals(DiagnosticDivergenceClassifier.Classification.EQUIVALENT_RESULT, comparison.classification)
    }

    @Test
    fun `inconclusivo de um lado nunca e EQUIVALENT_RESULT mesmo com o outro lado ok`() {
        val local = reportDe(DiagnosticStatus.ok, categoriaOrigem = "wifi")
        val remote = reportDe(DiagnosticStatus.inconclusive, categoriaOrigem = null, podeConcluir = false)

        val comparison = DiagnosticDivergenceClassifier.classify(local, remote)

        assertEquals(DiagnosticDivergenceClassifier.Classification.MINOR_DIVERGENCE, comparison.classification)
    }
}
