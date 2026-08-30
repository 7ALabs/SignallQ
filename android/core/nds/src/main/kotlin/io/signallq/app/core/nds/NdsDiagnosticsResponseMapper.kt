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
    explanationV2?.let { return toDiagnosticReportV2(it, input, geradoEmMs) }

    val scoring = resultFor("scoring")?.asScoring()
    val ai = resultFor("ai")?.asAi()
    val status = parseNdsVeredicto(scoring?.veredicto).toDiagnosticStatus()
    val dadosAusentes = results.flatMap { it.missingInputs }.distinct()
    val recomendacao = recommendation?.description ?: recommendationText
    val cards = results.flatMap { modulo -> modulo.cards.map { it.toDiagnosticResult(modulo.module) } }

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

/**
 * Ponte `NdsExplanationV2 -> DiagnosticReport` (feat/nds-client-v2). O contrato v2 não
 * devolve `results`/`recommendation`/`scoring` como o v1 -- só `explanation`, então o
 * relatório aqui é bem mais enxuto, mas reusa os MESMOS campos de [DiagnosticResult]
 * que a UI do Assist já lê hoje ([io.signallq.app.ui.screen.DiagnosticoGuiadoResultadoSection]):
 * `titulo`/`mensagemUsuario`/`evidencia` alimentam o card "O que encontramos" e
 * `recomendacaoPassos` alimenta os próximos passos sugeridos -- nenhuma UI nova.
 *
 * `explanation.sem_causa_identificada == true` vira [DiagnosticStatus.inconclusive] com
 * uma mensagem transparente (não um erro) -- o mesmo tom usado em
 * [DiagnosticStatus.inconclusive] no restante do app: reconhece a limitação sem inventar
 * causa. Sem essa marcação, o status é [DiagnosticStatus.attention] -- v2 não devolve
 * severidade granular (nada equivalente ao `scoring.veredicto` do v1), então "attention"
 * é o piso honesto para "o NDS encontrou algo a explicar", sem fingir um veredito mais
 * fino do que o contrato realmente entrega.
 */
private fun NdsDiagnosticsResponse.toDiagnosticReportV2(
    explanation: NdsExplanationV2,
    input: DiagnosticInput,
    geradoEmMs: Long,
): DiagnosticReport {
    val status = if (explanation.semCausaIdentificada) {
        DiagnosticStatus.inconclusive
    } else {
        DiagnosticStatus.attention
    }
    val dadosFormatados = explanation.dados.entries.joinToString(separator = "\n") { (chave, valor) -> "$chave: $valor" }
    val mensagem = listOfNotNull(
        explanation.descricao?.takeIf(String::isNotBlank),
        dadosFormatados.takeIf(String::isNotBlank),
    ).joinToString(separator = "\n\n").ifBlank {
        if (explanation.semCausaIdentificada) {
            "O NDS não conseguiu identificar uma causa provável para o problema com os dados coletados."
        } else {
            "Diagnóstico concluído."
        }
    }
    val decisao = DiagnosticResult(
        id = "nds:v2:${if (explanation.semCausaIdentificada) "sem_causa" else "explicado"}",
        titulo = explanation.titulo?.takeIf(String::isNotBlank) ?: "Diagnóstico via NDS",
        status = status,
        evidencia = mensagem,
        mensagemUsuario = mensagem,
        recomendacao = explanation.acaoUsuario,
        recomendacaoPassos = listOfNotNull(explanation.acaoUsuario?.takeIf(String::isNotBlank)),
        recomendacaoId = null,
        sourceFindingIds = emptyList(),
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
        dadosAusentes = emptyList(),
        limitacoesEquipamentoLocal = emptyList(),
        recomendacoes = if (explanation.acaoUsuario.isNullOrBlank()) emptyList() else listOf(decisao),
        perfisUsoSpeedtest = input.internet?.qualidadeUso,
        scoreEngineResultado = null,
        perfisUso = emptyList(),
        gameReadiness = emptyList(),
        geradoEmMs = geradoEmMs,
        executionId = input.executionId,
        evidenciasRemotas = emptyList(),
        modulosRemotos = mapOf("nds_v2" to rawV2),
        avisosRemotos = emptyMap(),
        context = input.context,
    )
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
