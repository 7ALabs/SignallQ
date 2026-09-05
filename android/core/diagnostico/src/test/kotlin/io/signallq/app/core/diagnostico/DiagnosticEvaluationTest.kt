package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes do tipo novo [DiagnosticEvaluation] (issue #1443, ADR-011 Decisao 3.2).
 *
 * Este tipo ainda nao tem mapper de JSON (isso e escopo de #1444) -- por isso o
 * teste de "serializacao/desserializacao" cobre o que ja existe hoje: o round
 * trip `wireValue -> fromWire` de cada enum que vai alimentar esse mapper
 * futuro, e a integridade estrutural do envelope completo via `data class`
 * (equals/copy), sem inventar um codec so para o teste.
 */
class DiagnosticEvaluationTest {
    @Test
    fun `todo wireValue de DiagnosticEvaluationSource volta ao enum original`() {
        DiagnosticEvaluationSource.entries.forEach { valor ->
            assertEquals(valor, DiagnosticEvaluationSource.fromWire(valor.wireValue))
        }
    }

    @Test
    fun `todo wireValue de DiagnosticEvaluationStatus volta ao enum original`() {
        DiagnosticEvaluationStatus.entries.forEach { valor ->
            assertEquals(valor, DiagnosticEvaluationStatus.fromWire(valor.wireValue))
        }
    }

    @Test
    fun `todo wireValue de DiagnosticEvaluationConfidence volta ao enum original`() {
        DiagnosticEvaluationConfidence.entries.forEach { valor ->
            assertEquals(valor, DiagnosticEvaluationConfidence.fromWire(valor.wireValue))
        }
    }

    @Test
    fun `todo wireValue de DiagnosticEvaluationSeverity volta ao enum original`() {
        DiagnosticEvaluationSeverity.entries.forEach { valor ->
            assertEquals(valor, DiagnosticEvaluationSeverity.fromWire(valor.wireValue))
        }
    }

    @Test
    fun `todo wireValue de DiagnosticEvaluationFindingCategory volta ao enum original, incluindo wifi-canal com hifen`() {
        DiagnosticEvaluationFindingCategory.entries.forEach { valor ->
            assertEquals(valor, DiagnosticEvaluationFindingCategory.fromWire(valor.wireValue))
        }
        assertEquals(
            DiagnosticEvaluationFindingCategory.WIFI_CANAL,
            DiagnosticEvaluationFindingCategory.fromWire("wifi-canal"),
        )
    }

    @Test
    fun `todo wireValue de DiagnosticEvaluationFlow volta ao enum original`() {
        DiagnosticEvaluationFlow.entries.forEach { valor ->
            assertEquals(valor, DiagnosticEvaluationFlow.fromWire(valor.wireValue))
        }
    }

    @Test
    fun `wireValue desconhecido nunca inventa um enum -- retorna null`() {
        assertNull(DiagnosticEvaluationSource.fromWire("NAO_EXISTE"))
        assertNull(DiagnosticEvaluationStatus.fromWire("NAO_EXISTE"))
        assertNull(DiagnosticEvaluationConfidence.fromWire("NAO_EXISTE"))
        assertNull(DiagnosticEvaluationSeverity.fromWire("NAO_EXISTE"))
        assertNull(DiagnosticEvaluationFindingCategory.fromWire("nao_existe"))
        assertNull(DiagnosticEvaluationFlow.fromWire("nao_existe"))
    }

    @Test
    fun `DiagnosticEvaluation completo preserva todos os campos por copy e equals`() {
        val finding =
            DiagnosticEvaluationFinding(
                findingCode = "WIFI-CANAL-CONGESTIONADO",
                category = DiagnosticEvaluationFindingCategory.WIFI_CANAL,
                severity = DiagnosticEvaluationSeverity.WARNING,
                confidence = DiagnosticEvaluationConfidence.MEDIUM,
                recommendationId = "REC-WIFI-CANAL-01",
                matchedRuleId = "rule-wifi-canal-congestionado",
                matchedRuleVersion = 3,
            )

        val original =
            DiagnosticEvaluation(
                resultSchemaVersion = 1,
                engineVersion = 2,
                rulesetVersion = 5,
                evaluationSource = DiagnosticEvaluationSource.REMOTE,
                overallStatus = DiagnosticEvaluationStatus.ATTENTION,
                score = 62,
                confidence = DiagnosticEvaluationConfidence.MEDIUM,
                matchedRules = listOf("rule-wifi-canal-congestionado"),
                findings = listOf(finding),
                recommendations = listOf("REC-WIFI-CANAL-01"),
                primaryFlow = DiagnosticEvaluationFlow.WIFI_LOCAL,
                secondaryFlows = listOf(DiagnosticEvaluationFlow.SAUDAVEL_MONITORAR),
                humanSummary = "Seu Wi-Fi esta num canal congestionado.",
                humanResolution = listOf("Troque para o canal 6 ou 11 nas configuracoes do roteador."),
                missingInputs = emptyList(),
                nextBestChecks = listOf("Repita o teste apos trocar o canal."),
                resolvableNow = true,
                evaluatedAt = "2026-07-26T12:00:00Z",
                traceId = "trace-123",
            )

        // Round trip via copy() sem nenhuma alteracao == mesma instancia logica.
        val roundTrip = original.copy()

        assertEquals(original, roundTrip)
        assertEquals(DiagnosticEvaluationFindingCategory.WIFI_CANAL, roundTrip.findings.single().category)
        assertEquals(
            "wifi-canal",
            roundTrip.findings
                .single()
                .category.wireValue,
        )
    }
}
