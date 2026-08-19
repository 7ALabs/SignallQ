package io.signallq.app

import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre [resolverResultadoAnaliseViaNds] -- parte pura de [MainViewModel.analisarProblema]
 * extraida especificamente para ser testavel sem instanciar o ViewModel inteiro (25+
 * dependencias Hilt/Android, mesma limitacao documentada em MainViewModelHistoricoTest/
 * MainViewModelLocalDeviceTest).
 *
 * NDS-02k PR2 (issue #1746, ADR-017) -- flag `consumer.diagnostico.nds_live_enabled` decide
 * se `analisarProblema()` chama o worker legado `ai-diagnosis-worker`
 * (`AiDiagnosisRepository.explainDiagnosis`, flag desligada -- comportamento hoje em todo
 * ambiente) ou deriva o resultado direto do relatorio ja avaliado pelo NDS
 * (`AiFallbackFactory.fromLocal`, flag ligada).
 */
class MainViewModelAnalisarProblemaNdsTest {
    /** Simula um [DiagnosticReport] cuja `decisao` ja veio do NDS -- `titulo`/`mensagemUsuario`
     *  populados a partir da narrativa do modulo `ai` do NDS (`tituloAmigavel`/
     *  `resumoTecnicoTraduzido`), exatamente como `NdsDiagnosticsResponseMapper.toDiagnosticReport`
     *  produz quando a flag esta ligada. */
    private fun relatorioNds(): DiagnosticReport {
        val decisao =
            DiagnosticResult(
                id = "nds:excelente",
                titulo = "Tudo certo com sua conexão",
                status = DiagnosticStatus.ok,
                evidencia = null,
                mensagemUsuario = "Conexão excelente, sem sinais de problema.",
                recomendacao = "Nenhuma ação necessária.",
                categoria = "nds",
                podeConcluir = true,
            )
        return DiagnosticReport(
            wifiResultados = emptyList(),
            internetResultados = emptyList(),
            mobileResultados = emptyList(),
            fibraResultados = emptyList(),
            dnsResultados = emptyList(),
            historicoResultados = emptyList(),
            wifiCanalResultados = emptyList(),
            decisao = decisao,
            perfisUsoSpeedtest = null,
            geradoEmMs = 1700000000000L,
        )
    }

    @Test
    fun `flag desligada -- devolve null e nao interfere no caminho antigo`() {
        val resultado =
            resolverResultadoAnaliseViaNds(
                ndsLiveEnabled = false,
                relatorio = relatorioNds(),
                problema = "sem internet",
            )
        assertNull(
            "com a flag desligada, analisarProblema() deve seguir o caminho antigo " +
                "(AiDiagnosisRepository.explainDiagnosis) -- nenhuma mudanca de comportamento",
            resultado,
        )
    }

    @Test
    fun `flag ligada -- deriva Resultado direto do relatorio, sem chamar worker legado`() {
        val relatorio = relatorioNds()

        val resultado =
            resolverResultadoAnaliseViaNds(
                ndsLiveEnabled = true,
                relatorio = relatorio,
                problema = null,
            )

        requireNotNull(resultado)
        // origem="local" e tecnicamente correto (nenhuma chamada ao ai-diagnosis-worker
        // aconteceu), mas o TEXTO exibido ja carrega a narrativa gerada pelo proprio NDS,
        // porque `relatorio.decisao` ja veio populado por NdsDiagnosticsResponseMapper.
        assertEquals("local", resultado.origem)
        assertEquals(relatorio.decisao.titulo, resultado.titulo)
        assertEquals(relatorio.decisao.mensagemUsuario, resultado.resumo)
        assertEquals(relatorio.decisao.mensagemUsuario, resultado.texto)
        assertEquals(1, resultado.acoes.size)
        assertEquals(relatorio.decisao.recomendacao, resultado.acoes.first().descricao)
    }

    @Test
    fun `flag ligada -- propaga o problema relatado pelo usuario para o Resultado`() {
        val resultado =
            resolverResultadoAnaliseViaNds(
                ndsLiveEnabled = true,
                relatorio = relatorioNds(),
                problema = "meu wifi está lento",
            )

        requireNotNull(resultado)
        assertEquals("meu wifi está lento", resultado.problemaRelatado)
    }

    @Test
    fun `flag ligada -- relatorio com status critico mapeia para origem local mesmo assim`() {
        val decisaoCritica =
            DiagnosticResult(
                id = "nds:critico",
                titulo = "Problema grave detectado",
                status = DiagnosticStatus.critical,
                evidencia = null,
                mensagemUsuario = "Sua conexão está com problema crítico.",
                recomendacao = null,
                categoria = "nds",
                podeConcluir = true,
            )
        val relatorio =
            DiagnosticReport(
                wifiResultados = emptyList(),
                internetResultados = emptyList(),
                mobileResultados = emptyList(),
                fibraResultados = emptyList(),
                dnsResultados = emptyList(),
                historicoResultados = emptyList(),
                wifiCanalResultados = emptyList(),
                decisao = decisaoCritica,
                perfisUsoSpeedtest = null,
                geradoEmMs = 1700000000000L,
            )

        val resultado =
            resolverResultadoAnaliseViaNds(
                ndsLiveEnabled = true,
                relatorio = relatorio,
                problema = null,
            )

        requireNotNull(resultado)
        assertEquals("local", resultado.origem)
        assertEquals("Problema grave detectado", resultado.titulo)
        // sem recomendacao -> nenhuma acao sintetica inventada
        assertTrue(resultado.acoes.isEmpty())
    }
}
