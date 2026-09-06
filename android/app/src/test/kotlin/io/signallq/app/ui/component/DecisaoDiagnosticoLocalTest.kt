package io.signallq.app.ui.component

import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Teste de caracterização — issue #1754 (NDS-02e), escrito ANTES de `LaudoScreen.kt` trocar a
 * leitura direta de `relatorio.decisao`/`relatorio.executionId`/`relatorio.scoreConexao`/
 * `relatorio.veredito` pelo seam [paraDecisaoDiagnosticoLocal]. Prova que a extração preserva
 * exatamente os mesmos valores que o acesso direto ao [DiagnosticReport] produzia — comportamento
 * idêntico, só a fonte da leitura muda de lugar (mesmo padrão de
 * `ClassificacaoMetricaLocalTest`, ver KDoc de `DecisaoDiagnosticoLocal.kt` para o racional
 * completo).
 */
class DecisaoDiagnosticoLocalTest {
    private fun decisaoFixture(
        titulo: String = "Conexão saudável",
        status: DiagnosticStatus = DiagnosticStatus.ok,
        mensagemUsuario: String = "Tudo certo por aqui.",
        recomendacao: String? = "Nenhuma ação necessária.",
    ) = DiagnosticResult(
        id = "DECISAO-TESTE",
        titulo = titulo,
        status = status,
        evidencia = null,
        mensagemUsuario = mensagemUsuario,
        recomendacao = recomendacao,
        categoria = "decisao",
        podeConcluir = true,
    )

    private fun relatorioFixture(
        executionId: String = "exec-123",
        decisao: DiagnosticResult = decisaoFixture(),
    ) = DiagnosticReport(
        wifiResultados = emptyList(),
        internetResultados = emptyList(),
        fibraResultados = emptyList(),
        decisao = decisao,
        geradoEmMs = 1_700_000_000_000L,
        executionId = executionId,
    )

    @Test
    fun `paraDecisaoDiagnosticoLocal converge com a leitura direta de relatorio decisao`() {
        val decisao = decisaoFixture()
        val relatorio = relatorioFixture(executionId = "exec-abc", decisao = decisao)

        val local = relatorio.paraDecisaoDiagnosticoLocal()

        assertEquals(decisao.status, local.status)
        assertEquals(decisao.titulo, local.titulo)
        assertEquals(decisao.mensagemUsuario, local.mensagemUsuario)
        assertEquals(decisao.recomendacao, local.recomendacao)
        assertEquals(relatorio.executionId, local.executionId)
        assertEquals(relatorio.scoreConexao, local.scoreConexao)
        assertEquals(relatorio.veredito, local.veredito)
    }

    @Test
    fun `paraDecisaoDiagnosticoLocal preserva recomendacao nula`() {
        val relatorio = relatorioFixture(decisao = decisaoFixture(recomendacao = null))

        val local = relatorio.paraDecisaoDiagnosticoLocal()

        assertEquals(null, local.recomendacao)
    }

    @Test
    fun `paraDecisaoDiagnosticoLocal converge scoreConexao e veredito para cada status`() {
        DiagnosticStatus.entries.forEach { status ->
            val relatorio = relatorioFixture(decisao = decisaoFixture(status = status))
            val local = relatorio.paraDecisaoDiagnosticoLocal()

            assertEquals(
                "scoreConexao divergente para status=$status",
                relatorio.scoreConexao,
                local.scoreConexao,
            )
            assertEquals(
                "veredito divergente para status=$status",
                relatorio.veredito,
                local.veredito,
            )
        }
    }

    @Test
    fun `paraDecisaoDiagnosticoLocal preserva executionId vazio`() {
        val relatorio = relatorioFixture(executionId = "")

        val local = relatorio.paraDecisaoDiagnosticoLocal()

        assertEquals("", local.executionId)
    }
}
