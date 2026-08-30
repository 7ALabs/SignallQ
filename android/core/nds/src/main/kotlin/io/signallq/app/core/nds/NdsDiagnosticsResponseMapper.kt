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
 * Cards NDS são convertidos em [DiagnosticResult] e chegam às listas por domínio e
 * [DiagnosticReport.evidenciasRemotas]. Módulos sem decoder tipado permanecem em
 * [DiagnosticReport.modulosRemotos], sem impedir a avaliação.
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
    val dadosAusentes = results.flatMap { it.missingInputs }.distinct()
    val recomendacao = recommendation?.description ?: recommendationText
    val cards = results.flatMap { modulo -> modulo.cards.map { it.toDiagnosticResult(modulo.module) } }
    val status = scoring?.let { parseNdsVeredicto(it.veredicto).toDiagnosticStatus() }
        ?: cards.maxByOrNull { it.status.v2SeverityRank() }?.status
        ?: DiagnosticStatus.inconclusive

    val decisao = DiagnosticResult(
        id = "nds:${scoring?.veredicto ?: "inconclusivo"}",
        titulo = explanation?.title ?: ai?.explanation?.tituloAmigavel ?: "Diagnóstico via NDS",
        status = status,
        evidencia = null,
        mensagemUsuario = explanation?.description ?: ai?.explanation?.resumoTecnicoTraduzido
            ?: recomendacao
            ?: "Diagnóstico concluído.",
        recomendacao = recomendacao,
        recomendacaoPassos = recommendation?.steps.orEmpty(),
        recomendacaoId = recommendation?.id,
        sourceFindingIds = recommendation?.sourceFindingIds.orEmpty(),
        categoria = "nds",
        podeConcluir = status != DiagnosticStatus.inconclusive,
        categoriaOrigem = null,
    )

    return DiagnosticReport(
        wifiResultados = cards.filter { it.categoria == "wifi" },
        internetResultados = cards.filter { it.categoria == "internet" || it.categoria == "connection" },
        mobileResultados = emptyList(),
        fibraResultados = cards.filter { it.categoria == "fibra" },
        dnsResultados = cards.filter { it.categoria == "dns" },
        historicoResultados = emptyList(),
        wifiCanalResultados = emptyList(),
        redeResultados = emptyList(),
        decisao = decisao,
        achadosSecundarios = cards,
        hipotesesDescartadas = emptyList(),
        dadosAusentes = dadosAusentes,
        limitacoesEquipamentoLocal = emptyList(),
        recomendacoes = if (recommendation == null) emptyList() else listOf(decisao),
        perfisUsoSpeedtest = input.internet?.qualidadeUso,
        scoreEngineResultado = scoring?.let {
            ScoreResult(score = it.score, dimensoesUsadas = emptyList(), dadosAusentes = dadosAusentes)
        },
        perfisUso = emptyList(),
        gameReadiness = emptyList(),
        geradoEmMs = geradoEmMs,
        executionId = input.executionId,
        evidenciasRemotas = cards,
        modulosRemotos = results.associate { it.module to it.result },
        avisosRemotos = results.associate { it.module to it.warnings },
        context = input.context,
    )
}

private fun DiagnosticStatus.v2SeverityRank(): Int = when (this) {
    DiagnosticStatus.critical -> 4
    DiagnosticStatus.attention -> 3
    DiagnosticStatus.info -> 2
    DiagnosticStatus.ok -> 1
    DiagnosticStatus.inconclusive -> 0
}

private fun Map<String, Any?>.string(key: String): String? = this[key] as? String

private fun Map<String, Any?>.boolean(key: String): Boolean = this[key] as? Boolean ?: false

private fun Map<String, Any?>.toDiagnosticResult(module: String): DiagnosticResult {
    val status = runCatching { DiagnosticStatus.valueOf(string("status") ?: "inconclusive") }
        .getOrDefault(DiagnosticStatus.inconclusive)
    val id = string("id") ?: "$module:card"
    val title = string("titulo") ?: string("title") ?: id
    val evidence = string("evidence") ?: string("evidencia")
    val message = string("mensagemUsuario") ?: string("message") ?: evidence ?: "Evidência recebida do NDS."
    return DiagnosticResult(
        id = id,
        titulo = title,
        status = status,
        evidencia = evidence,
        mensagemUsuario = message,
        recomendacao = null,
        categoria = string("categoria") ?: module,
        podeConcluir = boolean("podeConcluir") || status != DiagnosticStatus.inconclusive,
        categoriaOrigem = string("categoriaOrigem") ?: string("category"),
    )
}
