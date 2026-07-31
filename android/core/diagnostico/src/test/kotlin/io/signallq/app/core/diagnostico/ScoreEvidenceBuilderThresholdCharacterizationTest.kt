package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — congela que [ScoreEvidenceBuilder] reimplementa thresholds
 * de upload em vez de chamar [MetricClassifier], apesar do proprio kdoc do arquivo
 * afirmar "Nao reclassifica nada do zero — reaproveita o MetricClassifier". Ver
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, Parte 8, P1-6.
 *
 * `ScoreEvidenceBuilder.velocidade()` (privada) usa um corte de upload de dois niveis
 * (`ul<=0.0 -> 15`, `ul<5.0 -> 55`, `else -> 100`) que bate com o limiar de negocio de
 * [InternetDiagnosticEngine] (cliff em 5.0 Mbps), NAO com a tabela de 4 niveis de
 * [MetricClassifier.classificarUpload] (1/3/10/20 Mbps). Este teste exercita a funcao
 * publica [ScoreEvidenceBuilder.construir] (unico ponto de entrada acessivel de fora do
 * arquivo) e compara a nota de "velocidade" resultante com a nota que
 * [MetricClassifier.classificarUpload] + [ScoreEngine.notaParaStatus] produziriam para o
 * MESMO valor de upload.
 */
class ScoreEvidenceBuilderThresholdCharacterizationTest {

    private fun notaVelocidade(uploadMbps: Double): Int? {
        val input = DiagnosticInput(
            internet = InternetDiagnosticInput(
                downloadMbps = 100.0, // fixo, so upload varia nesta caracterizacao
                uploadMbps = uploadMbps,
                latencyMs = 20.0,
                jitterMs = 5.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 0.0,
            ),
        )
        val decisaoNeutra = DiagnosticResult(
            id = "IN-NORMAL-02",
            titulo = "Sua conexão está boa",
            status = DiagnosticStatus.ok,
            evidencia = null,
            mensagemUsuario = "Sua conexão está funcionando bem.",
            recomendacao = null,
            categoria = "internet",
        )
        val report = DiagnosticReport(
            wifiResultados = emptyList(),
            internetResultados = emptyList(),
            fibraResultados = emptyList(),
            decisao = decisaoNeutra,
        )
        return ScoreEvidenceBuilder.construir(input, report)
            .first { it.dimensao == "velocidade" }
            .nota
    }

    @Test
    fun `upload de 4 Mbps -- ScoreEvidenceBuilder e MetricClassifier calculam notas diferentes`() {
        // ScoreEvidenceBuilder.velocidade(): ul=4.0 cai em "ul < 5.0" -> nota de upload = 55
        // (limitada pelo minOf com a nota de download, que aqui e 100 -- entao a nota final
        // e 55, vinda so do upload).
        assertEquals(55, notaVelocidade(4.0))

        // MetricClassifier.classificarUpload(4.0) + ScoreEngine.notaParaStatus: 4.0 Mbps
        // cai em "regular" (>=3.0), que ScoreEngine.notaParaStatus mapeia para 60 -- um
        // valor DIFERENTE do 55 que ScoreEvidenceBuilder realmente usa.
        val statusCanonico = MetricClassifier.classificarUpload(4.0)
        assertEquals(MetricStatus.regular, statusCanonico)
        val notaCanonica = ScoreEngine.notaParaStatus(statusCanonico)
        assertEquals(60, notaCanonica)

        // Caracteriza a divergencia: 55 (o que o Score realmente usa) != 60 (o que o
        // Score usaria se de fato reaproveitasse MetricClassifier, como o kdoc do arquivo
        // afirma).
        assertNotEquals(notaCanonica, notaVelocidade(4.0))
    }

    @Test
    fun `upload de 15 Mbps -- as duas contas coincidem por acidente de calibracao`() {
        // Acima de 5.0 Mbps, ScoreEvidenceBuilder.velocidade() sempre devolve 100 pro
        // upload (sem mais niveis) -- coincide com MetricClassifier soh quando o status
        // canonico tambem for "excelente" (>=20.0) ou nas faixas que dao nota alta o
        // bastante para nao mudar o minOf com o download. Em 15.0 Mbps o classifier
        // devolve "bom" (nota 80), MAS o minOf com download=100 nao muda o resultado
        // porque ScoreEvidenceBuilder.velocidade() ja fixa 100 (nao 80) para qualquer
        // ul >= 5.0 -- OUTRA forma da mesma divergencia, so que "escondida" porque o
        // minOf com download (100) nao deixa a diferenca aparecer quando download tambem
        // e alto. Aqui explicitamos numericamente o descolamento.
        assertEquals(100, notaVelocidade(15.0))
        val notaCanonica15 = ScoreEngine.notaParaStatus(MetricClassifier.classificarUpload(15.0))
        assertEquals(80, notaCanonica15)
        assertNotEquals(notaCanonica15, notaVelocidade(15.0))
    }
}
