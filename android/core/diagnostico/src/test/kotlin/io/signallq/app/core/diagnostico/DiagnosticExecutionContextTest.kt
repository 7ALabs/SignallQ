package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1228 (Fase 3, executionId/rulesVersion) — [DiagnosticExecutionContext] nunca gera um
 * identificador novo, apenas agrupa um [DiagnosticExecutionContext.executionId] já existente
 * com a versão canônica de regras e o instante de início.
 */
class DiagnosticExecutionContextTest {
    @Test
    fun `iniciar preserva o executionId recebido, nunca gera outro`() {
        val contexto = DiagnosticExecutionContext.iniciar(executionId = "exec-original-001")

        assertEquals("exec-original-001", contexto.executionId)
    }

    @Test
    fun `iniciar com executionId nao vazio produz contexto com executionId nao vazio`() {
        val contexto = DiagnosticExecutionContext.iniciar(executionId = "exec-002")

        assertTrue(contexto.executionId.isNotBlank())
    }

    @Test
    fun `dois executionId diferentes produzem contextos com executionId diferentes`() {
        val contextoA = DiagnosticExecutionContext.iniciar(executionId = "exec-A")
        val contextoB = DiagnosticExecutionContext.iniciar(executionId = "exec-B")

        assertNotEquals(contextoA.executionId, contextoB.executionId)
    }

    @Test
    fun `iniciar usa sempre a versao canonica atual das regras`() {
        val contexto = DiagnosticExecutionContext.iniciar(executionId = "exec-003")

        assertEquals(DiagnosticRulesVersion.CURRENT, contexto.rulesVersion)
    }

    @Test
    fun `iniciar lanca excecao para executionId vazio`() {
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticExecutionContext.iniciar(executionId = "")
        }
    }

    @Test
    fun `iniciar lanca excecao para executionId em branco`() {
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticExecutionContext.iniciar(executionId = "   ")
        }
    }

    @Test
    fun `iniciar preserva o startedAtEpochMs informado`() {
        val contexto = DiagnosticExecutionContext.iniciar(executionId = "exec-004", startedAtEpochMs = 1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, contexto.startedAtEpochMs)
    }
}
