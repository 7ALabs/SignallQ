package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.ScoreResult

/**
 * Ponte `NdsDiagnosticsResponse -> DiagnosticReport` (NDS-02k, issue #1759, item 4
 * — secao 2a do inventario). Funcao pura, sem I/O — quem chama decide origem
 * ([DiagnosticReport.evaluationSource]) e `perfisUso`/`gameReadiness` (SEMPRE
 * locais, mesmo padrao ja usado por
 * [io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository.evaluate],
 * nao preenchidos aqui).
 *
 * ## Por que a maioria das listas de [DiagnosticReport] fica vazia
 * O NDS devolve, por avaliacao: UM `veredicto`/`score` (modulo `scoring`),
 * `matched_rules` SEM texto/status (modulo `diagnostics.wifi`), UMA explicacao
 * (modulo `ai`). [DiagnosticReport] tem 8 listas por dominio
 * (`wifiResultados`/`internetResultados`/...) que o motor LOCAL preenche com um
 * `DiagnosticResult` por metrica avaliada — o NDS nao devolve esse nivel de
 * granularidade (sem status/titulo/mensagem por metrica), entao nao ha como
 * construir um `DiagnosticResult` honesto por item. Ficam vazias por design; o
 * veredicto real desta avaliacao mora inteiro em [DiagnosticReport.decisao].
 * Gap documentado no inventario da issue #1759 — nao e bug desta fatia.
 *
 * ## `scoreEngineResultado`
 * Usa o `score` numerico (0-100) que o modulo `scoring` de fato devolve, com
 * `dimensoesUsadas` vazio (o NDS nao devolve a quebra por dimensao que o
 * `ScoreEngine` local calcula) — `dimensoesUsadas` vazio e honesto (significa
 * "sem essa quebra", nao um valor fabricado), diferente de omitir o score inteiro
 * e cair na tabela generica por status de [DiagnosticReport.scoreConexao].
 *
 * ## `recomendacoes`
 * Fica vazia — a lista estruturada (REC-01..REC-14) vem do
 * `RecomendacaoPraticaEngine` local (`feature/diagnostico`, fora do alcance deste
 * modulo `core`). O `recommendation.description` e seus `steps` entram em
 * [DiagnosticResult.recomendacao] e [DiagnosticResult.recomendacaoPassos].
 */
fun NdsDiagnosticsResponse.toDiagnosticReport(
    input: DiagnosticInput,
    geradoEmMs: Long,
): DiagnosticReport {
    val scoring = resultFor("scoring")?.asScoring()
    val ai = resultFor("ai")?.asAi()
    val status = parseNdsVeredicto(scoring?.veredicto).toDiagnosticStatus()
    val dadosAusentes = results.flatMap { it.missingInputs }.distinct()
    val recomendacao = recommendation?.description ?: recommendationText

    val decisao = DiagnosticResult(
        id = "nds:${scoring?.veredicto ?: "inconclusivo"}",
        titulo = ai?.explanation?.tituloAmigavel ?: "Diagnóstico via NDS",
        status = status,
        evidencia = null,
        mensagemUsuario = ai?.explanation?.resumoTecnicoTraduzido
            ?: recomendacao
            ?: "Diagnóstico concluído.",
        recomendacao = recomendacao,
        recomendacaoPassos = recommendation?.steps.orEmpty(),
        categoria = "nds",
        podeConcluir = status != DiagnosticStatus.inconclusive,
        categoriaOrigem = null,
    )

    return DiagnosticReport(
        wifiResultados = emptyList(),
        internetResultados = emptyList(),
        mobileResultados = emptyList(),
        fibraResultados = emptyList(),
        dnsResultados = emptyList(),
        historicoResultados = emptyList(),
        wifiCanalResultados = emptyList(),
        redeResultados = emptyList(),
        decisao = decisao,
        achadosSecundarios = emptyList(),
        hipotesesDescartadas = emptyList(),
        dadosAusentes = dadosAusentes,
        limitacoesEquipamentoLocal = emptyList(),
        recomendacoes = emptyList(),
        perfisUsoSpeedtest = input.internet?.qualidadeUso,
        scoreEngineResultado = scoring?.let {
            ScoreResult(score = it.score, dimensoesUsadas = emptyList(), dadosAusentes = dadosAusentes)
        },
        perfisUso = emptyList(),
        gameReadiness = emptyList(),
        geradoEmMs = geradoEmMs,
        executionId = input.executionId,
    )
}
