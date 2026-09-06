package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1228 (Fase 3, executionId/rulesVersion) — cobre a propagação de
 * [DiagnosticInput.executionId] -> [DiagnosticReport.executionId] e o preenchimento canônico
 * de [DiagnosticReport.rulesVersion] por [DiagnosticRunner.run], sem tocar nenhum threshold,
 * severidade ou texto de diagnóstico (zero mudança de comportamento classificatório — só os
 * dois campos novos de identidade/versão).
 */
class DiagnosticRunnerExecutionVersioningTest {
    private fun inputMinimo(executionId: String = "") =
        DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet =
                InternetDiagnosticInput(
                    downloadMbps = 100.0,
                    uploadMbps = 20.0,
                    latencyMs = 15.0,
                    jitterMs = 2.0,
                    perdaPercentual = 0.0,
                    bufferbloatMs = 2.0,
                ),
            executionId = executionId,
        )

    @Test
    fun `run propaga o executionId do input para o relatorio`() {
        val relatorio = DiagnosticRunner.run(inputMinimo(executionId = "exec-abc-123"))

        assertEquals("exec-abc-123", relatorio.executionId)
    }

    @Test
    fun `run nunca inventa um executionId quando o input nao propaga um`() {
        val relatorio = DiagnosticRunner.run(inputMinimo(executionId = ""))

        // Nunca gerar um novo id aqui -- vazio permanece vazio, nunca um UUID inventado.
        assertEquals("", relatorio.executionId)
    }

    @Test
    fun `dois inputs com executionId diferentes geram relatorios com executionId diferentes`() {
        val relatorioA = DiagnosticRunner.run(inputMinimo(executionId = "exec-A"))
        val relatorioB = DiagnosticRunner.run(inputMinimo(executionId = "exec-B"))

        assertNotEquals(relatorioA.executionId, relatorioB.executionId)
    }

    @Test
    fun `run sempre usa a versao canonica atual das regras`() {
        val relatorio = DiagnosticRunner.run(inputMinimo(executionId = "exec-qualquer"))

        assertEquals(DiagnosticRulesVersion.CURRENT, relatorio.rulesVersion)
    }

    @Test
    fun `rulesVersion nao varia por tipo de conexao ou entrada`() {
        val relatorioWifi = DiagnosticRunner.run(inputMinimo(executionId = "exec-1"))
        val relatorioMovel =
            DiagnosticRunner.run(
                DiagnosticInput(
                    connectionType = ConnectionType.mobile,
                    mobile = MobileDiagnosticInput(carrierName = "Operadora Teste"),
                    executionId = "exec-2",
                ),
            )

        assertEquals(relatorioWifi.rulesVersion, relatorioMovel.rulesVersion)
    }

    @Test
    fun `DiagnosticRulesVersion CURRENT nao e vazio e segue o formato documentado`() {
        assertTrue(DiagnosticRulesVersion.CURRENT.isNotBlank())
        assertTrue(DiagnosticRulesVersion.CURRENT.startsWith("diagnostic-rules-v"))
    }
}
