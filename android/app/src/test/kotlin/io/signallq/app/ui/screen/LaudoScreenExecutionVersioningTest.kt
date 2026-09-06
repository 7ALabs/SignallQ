package io.signallq.app.ui.screen

import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.ui.component.paraDecisaoDiagnosticoLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1228 (Fase 3, corrige P0-3) — regressão específica do risco confirmado na auditoria da
 * #1228: `LaudoScreen.gerarECompartilharLaudo()` podia combinar métricas de uma execução de
 * speedtest com o veredito/recomendação de um diagnóstico posterior e não relacionado
 * (`executionId` hardcoded para `""`). Cobre [diagnosticoCorrespondeAMedicao] (regra pura de
 * correspondência) e [montarSnapshotLaudo] (montagem do snapshot exportável).
 *
 * NDS-02e (#1754, ADR-017) — `montarSnapshotLaudo` passou a receber `DecisaoDiagnosticoLocal?`
 * em vez de `DiagnosticReport?` (seam `ui/component/DecisaoDiagnosticoLocal.kt`). As fixtures
 * seguem construindo `DiagnosticReport` (shape real do motor local) e convertem via
 * `.paraDecisaoDiagnosticoLocal()` no ponto de chamada, igual ao que `LaudoScreen.kt` faz — os
 * cenários de regressão do P0-3 continuam idênticos, só a fonte da conversão muda de lugar.
 */
class LaudoScreenExecutionVersioningTest {
    private fun decisaoFixture(titulo: String = "Conexão saudável") =
        DiagnosticResult(
            id = "DECISAO-TESTE",
            titulo = titulo,
            status = DiagnosticStatus.ok,
            evidencia = null,
            mensagemUsuario = "Tudo certo por aqui.",
            recomendacao = "Nenhuma ação necessária.",
            categoria = "decisao",
            podeConcluir = true,
        )

    private fun relatorioFixture(executionId: String) =
        DiagnosticReport(
            wifiResultados = emptyList(),
            internetResultados = emptyList(),
            fibraResultados = emptyList(),
            decisao = decisaoFixture(),
            geradoEmMs = 1_700_000_000_000L,
            executionId = executionId,
        )

    private fun decisaoLocalFixture(executionId: String) = relatorioFixture(executionId).paraDecisaoDiagnosticoLocal()

    private fun medicaoFixture(executionId: String) =
        MedicaoEntity(
            id = "medicao-1",
            timestampEpochMs = 1_700_000_000_000L,
            connectionType = "wifi",
            connectionTypeStart = null,
            connectionTypeEnd = null,
            contaminado = false,
            speedtestMode = "fast",
            specVersion = "3",
            downloadMbps = 100.0,
            uploadMbps = 20.0,
            latencyMs = 15.0,
            jitterMs = 2.0,
            perdaPercentual = 0.0,
            bufferbloatMs = 5.0,
            packetLossSource = null,
            vereditoStreaming = null,
            vereditoGamer = null,
            vereditoVideoChamada = null,
            gargaloPrimario = null,
            executionId = executionId,
        )

    // =========================================================================
    // diagnosticoCorrespondeAMedicao — regra pura
    // =========================================================================

    @Test
    fun `diagnosticoCorrespondeAMedicao e verdadeiro quando os dois executionId sao iguais e nao vazios`() {
        assertTrue(diagnosticoCorrespondeAMedicao("exec-A", "exec-A"))
    }

    @Test
    fun `diagnosticoCorrespondeAMedicao e falso quando os executionId sao diferentes`() {
        assertFalse(diagnosticoCorrespondeAMedicao("exec-A", "exec-B"))
    }

    @Test
    fun `diagnosticoCorrespondeAMedicao e falso quando o executionId do relatorio e nulo ou vazio`() {
        assertFalse(diagnosticoCorrespondeAMedicao(null, "exec-A"))
        assertFalse(diagnosticoCorrespondeAMedicao("", "exec-A"))
    }

    @Test
    fun `diagnosticoCorrespondeAMedicao e falso quando o executionId da medicao e nulo ou vazio`() {
        assertFalse(diagnosticoCorrespondeAMedicao("exec-A", null))
        assertFalse(diagnosticoCorrespondeAMedicao("exec-A", ""))
    }

    @Test
    fun `diagnosticoCorrespondeAMedicao e falso quando os dois lados sao desconhecidos (nunca vira match por omissao)`() {
        assertFalse(diagnosticoCorrespondeAMedicao(null, null))
        assertFalse(diagnosticoCorrespondeAMedicao("", ""))
    }

    // =========================================================================
    // montarSnapshotLaudo — regressao especifica do P0-3
    // =========================================================================

    @Test
    fun `laudo usa decisao e metricas da MESMA execucao quando os executionId correspondem`() {
        val snapshot =
            montarSnapshotLaudo(
                decisaoLocal = decisaoLocalFixture(executionId = "exec-123"),
                ultimaMedicao = medicaoFixture(executionId = "exec-123"),
                operadora = "Operadora Teste",
                ssid = null,
                ipLocal = null,
                ipPublico = null,
                velocidadeContratadaMbps = null,
                conectado = true,
                versaoApp = "1.0.0",
            )

        assertEquals("Conexão saudável", snapshot.veredito)
        assertEquals("Tudo certo por aqui.", snapshot.resumo)
        assertEquals(100.0, snapshot.downloadMbps)
        assertEquals("exec-123", snapshot.executionId)
    }

    @Test
    fun `laudo NUNCA combina metricas de uma execucao com veredito de outra (regressao P0-3)`() {
        // speedtest A (persistido) seguido de um diagnostico B (em memoria, nao relacionado)
        // -- exatamente o cenario "Frankenstein" documentado na auditoria da #1228.
        val snapshot =
            montarSnapshotLaudo(
                decisaoLocal = decisaoLocalFixture(executionId = "exec-B-diagnostico-novo"),
                ultimaMedicao = medicaoFixture(executionId = "exec-A-speedtest-antigo"),
                operadora = "Operadora Teste",
                ssid = null,
                ipLocal = null,
                ipPublico = null,
                velocidadeContratadaMbps = null,
                conectado = true,
                versaoApp = "1.0.0",
            )

        // As metricas continuam vindo da medicao persistida (fonte real dos numeros)...
        assertEquals(100.0, snapshot.downloadMbps)
        // ...mas o veredito/resumo da decisao (execucao B) NUNCA aparece combinado com elas.
        assertNull("veredito de uma execucao diferente nao pode aparecer no laudo", snapshot.veredito)
        assertNotNull("resumo deve avisar explicitamente a indisponibilidade, nunca ficar vazio/enganoso", snapshot.resumo)
        assertTrue(snapshot.resumo!!.contains("não disponível", ignoreCase = true))
    }

    @Test
    fun `laudo sem diagnostico em memoria nao mostra veredito, mas mantem as metricas`() {
        val snapshot =
            montarSnapshotLaudo(
                decisaoLocal = null,
                ultimaMedicao = medicaoFixture(executionId = "exec-A"),
                operadora = "",
                ssid = null,
                ipLocal = null,
                ipPublico = null,
                velocidadeContratadaMbps = null,
                conectado = true,
                versaoApp = "1.0.0",
            )

        assertNull(snapshot.veredito)
        assertNull(snapshot.resumo)
        assertEquals(100.0, snapshot.downloadMbps)
    }

    @Test
    fun `laudo sem nenhuma medicao persistida ainda mostra a decisao (nada para conflitar)`() {
        val snapshot =
            montarSnapshotLaudo(
                decisaoLocal = decisaoLocalFixture(executionId = "exec-unico"),
                ultimaMedicao = null,
                operadora = "",
                ssid = null,
                ipLocal = null,
                ipPublico = null,
                velocidadeContratadaMbps = null,
                conectado = true,
                versaoApp = "1.0.0",
            )

        assertEquals("Conexão saudável", snapshot.veredito)
        assertNull(snapshot.downloadMbps)
        assertEquals("", snapshot.executionId)
    }

    @Test
    fun `snapshot exportado usa o executionId da medicao, nunca vazio quando ela existe`() {
        val snapshot =
            montarSnapshotLaudo(
                decisaoLocal = decisaoLocalFixture(executionId = "exec-diagnostico"),
                ultimaMedicao = medicaoFixture(executionId = "exec-medicao-real"),
                operadora = "",
                ssid = null,
                ipLocal = null,
                ipPublico = null,
                velocidadeContratadaMbps = null,
                conectado = true,
                versaoApp = "1.0.0",
            )

        // GH#1228 (Fase 3) — nunca mais hardcoded "" quando ha uma medicao real persistida.
        assertEquals("exec-medicao-real", snapshot.executionId)
    }

    /**
     * GH#1228 (Fase 3), teste #17 do plano: concorrência entre duas execuções não pode
     * misturar resultados. Simula duas "execuções paralelas" (uma persistida como
     * `ultimaMedicao`, outra ainda em memória como `relatorio`, com identidades diferentes)
     * e confirma que o snapshot exportado nunca combina os dois lados.
     */
    @Test
    fun `execucoes concorrentes com identidades diferentes nao se misturam no laudo`() {
        val medicaoDaExecucaoX = medicaoFixture(executionId = "exec-X")
        val diagnosticoDaExecucaoY = decisaoLocalFixture(executionId = "exec-Y")

        val snapshot =
            montarSnapshotLaudo(
                decisaoLocal = diagnosticoDaExecucaoY,
                ultimaMedicao = medicaoDaExecucaoX,
                operadora = "",
                ssid = null,
                ipLocal = null,
                ipPublico = null,
                velocidadeContratadaMbps = null,
                conectado = true,
                versaoApp = "1.0.0",
            )

        assertEquals("exec-X", snapshot.executionId)
        assertNull(snapshot.veredito)
    }

    /**
     * GH#1228 (Fase 3), teste #13 do plano: chamar a montagem do snapshot mais de uma vez
     * (simulando recomposição/troca de tela) com os MESMOS dados de entrada nunca gera um
     * novo executionId — o valor vem sempre da medicao/relatorio recebidos, nunca de um
     * gerador interno.
     */
    @Test
    fun `chamadas repetidas com os mesmos dados nunca trocam o executionId`() {
        val decisaoLocal = decisaoLocalFixture(executionId = "exec-estavel")
        val medicao = medicaoFixture(executionId = "exec-estavel")

        val primeiraChamada =
            montarSnapshotLaudo(decisaoLocal, medicao, "", null, null, null, null, true, "1.0.0")
        val segundaChamada =
            montarSnapshotLaudo(decisaoLocal, medicao, "", null, null, null, null, true, "1.0.0")

        assertEquals(primeiraChamada.executionId, segundaChamada.executionId)
    }
}
