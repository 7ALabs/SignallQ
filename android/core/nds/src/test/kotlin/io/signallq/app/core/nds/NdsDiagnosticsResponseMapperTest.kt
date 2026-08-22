package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.SpeedtestQualityInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobertura da ponte `NdsDiagnosticsResponse -> DiagnosticReport` (NDS-02k, issue
 * #1759, item 4).
 */
class NdsDiagnosticsResponseMapperTest {

    private fun responseComScoringEAi(
        veredicto: String = "regular",
        score: Int = 50,
        recommendation: NdsNextBestAction? = null,
        tituloAmigavel: String? = "Sua rede está OK",
        resumo: String? = "Score 50, regular.",
        missingInputs: List<String> = emptyList(),
    ): NdsDiagnosticsResponse {
        val aiResult = mutableMapOf<String, Any?>(
            "tokens_used" to 0,
            "ai_model_used" to "copy-catalog",
            "fallback_used" to false,
            "explanation_source" to "copy_catalog",
            "explanation_status" to "catalog_hit",
            "source_finding_ids" to emptyList<String>(),
        )
        if (tituloAmigavel != null && resumo != null) {
            aiResult["explanation"] = mapOf(
                "titulo_amigavel" to tituloAmigavel,
                "resumo_tecnico_traduzido" to resumo,
            )
        }
        return NdsDiagnosticsResponse(
            recommendation = recommendation,
            results = listOf(
                NdsModuleResult(
                    module = "scoring",
                    moduleVersion = "1.1.0",
                    requestId = "req-1",
                    warnings = emptyList(),
                    missingInputs = missingInputs,
                    result = mapOf(
                        "score" to score,
                        "veredicto" to veredicto,
                        "tipo_conexao" to "WIFI",
                        "observed_dimensions" to 1,
                        "dimensoes" to emptyList<String>(),
                    ),
                    cards = emptyList(),
                ),
                NdsModuleResult(
                    module = "ai",
                    moduleVersion = "1.5.0",
                    requestId = "req-1",
                    warnings = emptyList(),
                    missingInputs = emptyList(),
                    result = aiResult,
                    cards = emptyList(),
                ),
            ),
            traces = emptyList(),
        )
    }

    @Test
    fun `mapeia veredicto regular para status info e usa explicacao da IA`() {
        val report = responseComScoringEAi(veredicto = "regular", score = 50)
            .toDiagnosticReport(input = DiagnosticInput(executionId = "exec-1"), geradoEmMs = 123L)

        assertEquals(DiagnosticStatus.info, report.decisao.status)
        assertEquals("nds:regular", report.decisao.id)
        assertEquals("Sua rede está OK", report.decisao.titulo)
        assertEquals("Score 50, regular.", report.decisao.mensagemUsuario)
        assertEquals("nds", report.decisao.categoria)
        assertTrue(report.decisao.podeConcluir)
        assertEquals(50, report.scoreEngineResultado?.score)
        assertEquals(123L, report.geradoEmMs)
        assertEquals("exec-1", report.executionId)
    }

    @Test
    fun `veredicto critico vira status critical`() {
        val report = responseComScoringEAi(veredicto = "critico")
            .toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)

        assertEquals(DiagnosticStatus.critical, report.decisao.status)
        assertEquals("nds:critico", report.decisao.id)
    }

    @Test
    fun `sem modulo scoring, veredicto cai em inconclusivo e podeConcluir fica false`() {
        val response = NdsDiagnosticsResponse(recommendation = null, results = emptyList(), traces = emptyList())

        val report = response.toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)

        assertEquals(DiagnosticStatus.inconclusive, report.decisao.status)
        assertEquals("nds:inconclusivo", report.decisao.id)
        assertEquals(false, report.decisao.podeConcluir)
    }

    @Test
    fun `sem modulo ai, mensagemUsuario cai para recommendation e depois para texto padrao`() {
        val comRecommendation = responseComScoringEAi(
            tituloAmigavel = null,
            resumo = null,
            recommendation = NdsNextBestAction(
                id = "restart_router",
                type = "resolution",
                title = "Reinicie o roteador",
                description = "Reinicie o roteador.",
                sourceFindingIds = emptyList(),
                steps = listOf("Desligue o roteador.", "Ligue o roteador novamente."),
            ),
        )
            .toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)
        assertEquals("Reinicie o roteador.", comRecommendation.decisao.mensagemUsuario)
        assertEquals("Reinicie o roteador.", comRecommendation.decisao.recomendacao)
        assertEquals(listOf("Desligue o roteador.", "Ligue o roteador novamente."), comRecommendation.decisao.recomendacaoPassos)

        val semNada = responseComScoringEAi(tituloAmigavel = null, resumo = null, recommendation = null)
            .toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)
        assertEquals("Diagnóstico concluído.", semNada.decisao.mensagemUsuario)
    }

    @Test
    fun `as 8 listas por dominio ficam vazias -- gap documentado, NDS nao devolve granularidade por metrica`() {
        val report = responseComScoringEAi().toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)

        assertEquals(emptyList<Any>(), report.wifiResultados)
        assertEquals(emptyList<Any>(), report.internetResultados)
        assertEquals(emptyList<Any>(), report.mobileResultados)
        assertEquals(emptyList<Any>(), report.fibraResultados)
        assertEquals(emptyList<Any>(), report.dnsResultados)
        assertEquals(emptyList<Any>(), report.historicoResultados)
        assertEquals(emptyList<Any>(), report.wifiCanalResultados)
        assertEquals(emptyList<Any>(), report.redeResultados)
        assertEquals(emptyList<Any>(), report.recomendacoes)
        assertEquals(emptyList<Any>(), report.perfisUso)
        assertEquals(emptyList<Any>(), report.gameReadiness)
    }

    @Test
    fun `dadosAusentes agrega missingInputs de todos os modulos, sem duplicar`() {
        val response = responseComScoringEAi(missingInputs = listOf("fiber.rx_power_dbm", "dns.hijacked"))

        val report = response.toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)

        assertEquals(listOf("fiber.rx_power_dbm", "dns.hijacked"), report.dadosAusentes)
        assertEquals(report.dadosAusentes, report.scoreEngineResultado?.dadosAusentes)
    }

    @Test
    fun `perfisUsoSpeedtest e repassado do input, nao do NDS`() {
        val qualidade = SpeedtestQualityInput(vereditoGamer = "boa")
        val input = DiagnosticInput(
            internet = InternetDiagnosticInput(
                downloadMbps = 1.0, uploadMbps = 1.0, latencyMs = 1.0, jitterMs = 1.0, perdaPercentual = 0.0,
                qualidadeUso = qualidade,
            ),
        )

        val report = responseComScoringEAi().toDiagnosticReport(input = input, geradoEmMs = 0L)

        assertEquals(qualidade, report.perfisUsoSpeedtest)
    }

    @Test
    fun `evaluationSource default do mapper e BUNDLED_LOCAL -- quem chama sobrescreve para REMOTE`() {
        // Mesmo padrao de RemoteDiagnosticReportMapper: o mapper puro nao decide a
        // origem, quem chama (NdsDiagnosticRepository) faz .copy(evaluationSource=REMOTE).
        val report = responseComScoringEAi().toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)

        assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
    }

    @Test
    fun `modulo scoring sem campos obrigatorios (score ou veredicto) e tratado como ausente`() {
        val response = NdsDiagnosticsResponse(
            recommendation = null,
            results = listOf(
                NdsModuleResult(
                    module = "scoring",
                    moduleVersion = "1.1.0",
                    requestId = "req-1",
                    warnings = emptyList(),
                    missingInputs = emptyList(),
                    result = mapOf("veredicto" to "bom"), // falta "score"
                    cards = emptyList(),
                ),
            ),
            traces = emptyList(),
        )

        val report = response.toDiagnosticReport(input = DiagnosticInput(), geradoEmMs = 0L)

        assertNull(report.scoreEngineResultado)
        assertEquals(DiagnosticStatus.inconclusive, report.decisao.status)
    }
}
